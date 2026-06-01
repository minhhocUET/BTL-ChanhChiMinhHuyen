package com.uet.bidding.dao;

import com.uet.bidding.exception.AuctionClosedException;
import com.uet.bidding.exception.InvalidBidException;
import com.uet.bidding.exception.UserException;
import com.uet.bidding.model.*;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * AuctionSqlDAO – quản lý phiên đấu giá, đặt giá, auto-bid, anti-sniping.
 * <p>
 * Các bảng sử dụng: auctions, bids, auto_bids, auction_registrations,
 * auction_results, sniping_logs, transactions.
 */
public class AuctionSqlDAO {

  private final UserSqlDAO userDao;
  private final BidSqlDAO bidDao;
  private final TransactionSqlDAO transactionDao;
  private final ItemSqlDAO itemDao;

  public AuctionSqlDAO() {
    this.userDao = new UserSqlDAO();
    this.bidDao = new BidSqlDAO();
    this.transactionDao = new TransactionSqlDAO();
    this.itemDao = new ItemSqlDAO();
  }

  // =========================================================
  //  CREATE – Tạo phiên đấu giá mới
  // =========================================================

  /**
   * Tạo phiên đấu giá mới, lưu vào DB.
   *
   * @param item         sản phẩm (đã có id)
   * @param startPrice   giá khởi điểm
   * @param startTime    thời gian bắt đầu
   * @param endTime      thời gian kết thúc dự kiến
   * @param bidIncrement bước giá tối thiểu (nếu null dùng 5.00)
   * @return Auction đã được gán id
   */
  public Auction createAuction(Item item, BigDecimal startPrice,
                               LocalDateTime startTime, LocalDateTime endTime,
                               BigDecimal bidIncrement) throws UserException {
    String sql = """
        INSERT INTO auctions
            (item_id, current_price, start_time, end_time, status,
             bid_increment, anti_snipe_window_minutes, anti_snipe_extension_minutes)
        VALUES (?, ?, ?, ?, 'RUNNING', ?, 2, 5)
        """;
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

      stmt.setInt(1, item.getId());
      stmt.setBigDecimal(2, startPrice);
      stmt.setTimestamp(3, Timestamp.valueOf(startTime));
      stmt.setTimestamp(4, Timestamp.valueOf(endTime));
      // 🎯 ĐIỂM CẦN SỬA: Đảm bảo không để mặc định là 5 hay 1000 nếu bidIncrement có giá trị
      // Nếu người dùng không nhập (null), ta mới để mặc định (ví dụ 10.000 VNĐ)
      BigDecimal finalIncrement = (bidIncrement != null && bidIncrement.compareTo(BigDecimal.ZERO) > 0)
          ? bidIncrement
          : BigDecimal.valueOf(10000);

      stmt.setBigDecimal(5, finalIncrement);
      stmt.executeUpdate();

      try (ResultSet gk = stmt.getGeneratedKeys()) {
        if (!gk.next()) throw new UserException("Không thể tạo phiên đấu giá.");
        int newId = gk.getInt(1);
        itemDao.setInAuction(item.getId(), true);
        return new Auction(newId, item, startPrice, startTime, endTime, "RUNNING");
      }
    } catch (SQLException e) {
      throw new UserException("Lỗi tạo phiên: " + e.getMessage());
    }
  }

  // =========================================================
  //  READ – Lấy thông tin phiên
  // =========================================================

  public Auction findById(int auctionId) throws UserException {
    String sql = "SELECT * FROM auctions WHERE id = ?";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, auctionId);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) return mapAuction(rs);
      }
    } catch (SQLException e) {
      throw new UserException("Lỗi truy vấn: " + e.getMessage());
    }
    throw new UserException("Không tìm thấy phiên id=" + auctionId);
  }

  public List<Auction> getAllAuctions() {
    return getAuctionsByStatusEnriched("RUNNING");
  }

  /** Phiên đang chạy cho sảnh đấu giá (không hiển thị FINISHED). */
  public List<Auction> getRunningAuctionsForHall() {
    return getAuctionsByStatusEnriched("RUNNING");
  }

  private List<Auction> getAuctionsByStatusEnriched(String status) {
    List<Auction> list = new ArrayList<>();
    String sql = "SELECT * FROM auctions WHERE status = ? ORDER BY start_time DESC";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, status);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          Auction auction = mapAuction(rs);
          enrichAuction(auction);
          list.add(auction);
        }
      }
    } catch (SQLException | UserException e) {
      System.err.println("Lỗi load auctions: " + e.getMessage());
    }
    return list;
  }

  public List<Auction> getAuctionsByStatus(String status) {
    List<Auction> list = new ArrayList<>();
    String sql = "SELECT * FROM auctions WHERE status = ? ORDER BY end_time";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, status);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) list.add(mapAuction(rs));
      }
    } catch (SQLException | UserException e) {
      System.err.println("Lỗi load auctions: " + e.getMessage());
    }
    return list;
  }

  /**
   * Phiên đấu giá của một seller, lọc theo trạng thái (RUNNING, FINISHED, ...).
   */
  public List<Auction> getAuctionsBySeller(int sellerId, String status) {
    List<Auction> list = new ArrayList<>();
    String sql =
        "SELECT a.* FROM auctions a " +
            "INNER JOIN items i ON a.item_id = i.id " +
            "WHERE i.seller_id = ? AND a.status = ? " +
            "ORDER BY a.end_time DESC";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, sellerId);
      stmt.setString(2, status);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          Auction auction = mapAuction(rs);
          enrichAuction(auction);
          list.add(auction);
        }
      }
    } catch (SQLException | UserException e) {
      System.err.println("Lỗi load auctions by seller: " + e.getMessage());
    }
    return list;
  }

  /**
   * Phiên RUNNING mà bidder đã đăng ký tham gia.
   */
  public List<Auction> getActiveAuctionsForBidder(int bidderId) {
    List<Auction> list = new ArrayList<>();
    String sql =
        "SELECT a.* FROM auctions a "
            + "INNER JOIN auction_registrations ar ON a.id = ar.auction_id "
            + "WHERE ar.bidder_id = ? AND a.status = 'RUNNING' "
            + "ORDER BY a.end_time ASC";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, bidderId);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          Auction auction = mapAuction(rs);
          enrichAuction(auction);
          list.add(auction);
        }
      }
    } catch (SQLException | UserException e) {
      System.err.println("Lỗi load active auctions for bidder: " + e.getMessage());
    }
    return list;
  }

  /**
   * Phiên FINISHED mà bidder đã tham gia (đăng ký, đặt giá hoặc thắng).
   */
  public List<Auction> getFinishedAuctionsForBidder(int bidderId) {
    List<Auction> list = new ArrayList<>();
    String sql =
        "SELECT DISTINCT a.* FROM auctions a "
            + "WHERE a.status = 'FINISHED' AND ("
            + "  EXISTS (SELECT 1 FROM auction_registrations r WHERE r.auction_id = a.id AND r.bidder_id = ?) "
            + "  OR EXISTS (SELECT 1 FROM bids b WHERE b.auction_id = a.id AND b.bidder_id = ?) "
            + "  OR EXISTS (SELECT 1 FROM auction_results ar WHERE ar.auction_id = a.id AND ar.winner_id = ?)"
            + ") ORDER BY a.end_time DESC";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, bidderId);
      stmt.setInt(2, bidderId);
      stmt.setInt(3, bidderId);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          Auction auction = mapAuction(rs);
          enrichAuction(auction);
          list.add(auction);
        }
      }
    } catch (SQLException | UserException e) {
      System.err.println("Lỗi load finished auctions for bidder: " + e.getMessage());
    }
    return list;
  }

  private void enrichAuction(Auction auction) {
    auction.setRegisteredCount(countRegistrations(auction.getId()));
  }

  // =========================================================
  //  UPDATE – Đặt giá (core logic)
  // =========================================================

  /**
   * Xử lý một lượt đặt giá từ người dùng (có thể là thủ công hoặc auto-bid).
   * <p>
   * Quy trình trong một transaction:
   * 1. Kiểm tra phiên còn hoạt động, giá hợp lệ.
   * 2. Ghi lượt bid vào bảng `bids`.
   * 3. Cập nhật `current_price` và `highest_bidder_id` trong `auctions`.
   * 4. Áp dụng anti‑sniping (gia hạn nếu cần).
   * 5. Kích hoạt auto‑bid cho những người dùng khác.
   * 6. (Tuỳ chọn) Trừ tiền tạm thời hoặc ghi transaction ở đây.
   *
   * @return true nếu thành công
   */
  public boolean placeBid(int auctionId, Customer bidder, BigDecimal bidAmount)
      throws AuctionClosedException, InvalidBidException, UserException {

    Auction auction = findById(auctionId);
    if (!"RUNNING".equals(auction.getStatus())) {
      throw new AuctionClosedException("Phiên đấu giá chưa bắt đầu hoặc đã kết thúc.");
    }

    // 🎯 FIX LỖI 1 VNĐ: Kiểm tra bước giá tối thiểu
    BigDecimal bidIncrement = auction.getBidIncrement() != null ? auction.getBidIncrement() : BigDecimal.valueOf(1000);
    BigDecimal minRequiredBid = auction.getCurrentPrice().add(bidIncrement);

    if (bidAmount.compareTo(minRequiredBid) < 0) {
      throw new InvalidBidException("Giá đặt không hợp lệ. Mức giá tối thiểu tiếp theo phải là "
          + minRequiredBid + " VNĐ (Giá hiện tại + bước giá " + bidIncrement + ")");
    }
    // 🎯 FIX LỖI SỐ DƯ: Kiểm tra tiền trong ví người dùng
    // Lưu ý: Phải lấy balance mới nhất từ DB, không dùng biến trong object bidder vì có thể cũ
    BigDecimal currentBalance = userDao.getBalance(bidder.getId());
    if (bidAmount.compareTo(currentBalance) > 0) {
      throw new InvalidBidException("Số dư tài khoản không đủ. Bạn cần " + bidAmount + " VNĐ nhưng hiện chỉ có " + currentBalance + " VNĐ.");
    }

    try (Connection conn = DatabaseConnection.getConnection()) {
      conn.setAutoCommit(false);
      int newBidId;
      try {
        // 1. Ghi bid
        newBidId = bidDao.addBid(conn, auctionId, bidder.getId(), bidAmount, LocalDateTime.now());

        // 2. Cập nhật auction
        auction.setCurrentPrice(bidAmount);
        auction.setHighestBidder(bidder);
        updateAuctionInTransaction(conn, auction);

        // 3. Anti-sniping
        LocalDateTime now = LocalDateTime.now();
        long minutesLeft = java.time.Duration.between(now, auction.getEndTime()).toMinutes();
        if (minutesLeft <= auction.getAntiSnipeWindowMinutes()) {
          LocalDateTime newEnd = auction.getEndTime().plusMinutes(auction.getAntiSnipeExtensionMinutes());
          auction.setEndTime(newEnd);
          updateAuctionInTransaction(conn, auction);
          logSniping(conn, auctionId, now, newEnd, auction.getAntiSnipeExtensionMinutes(), newBidId);
        }

        // 4. Auto-bid trigger
        triggerAutoBids(conn, auction, newBidId);

        conn.commit();
        return true;
      } catch (SQLException | UserException ex) {
        conn.rollback();
        throw new UserException("Lỗi xử lý đặt giá: " + ex.getMessage());
      } finally {
        conn.setAutoCommit(true);
      }
    } catch (SQLException e) {
      throw new UserException("Lỗi kết nối DB: " + e.getMessage());
    }
  }

  // =========================================================
  //  KẾT THÚC PHIÊN
  // =========================================================

  /**
   * Kết thúc phiên đấu giá, xác định người thắng, lưu kết quả và tạo transaction.
   */
  public void finishAuction(int auctionId) throws UserException {
    Auction auction = findById(auctionId);
    if ("FINISHED".equals(auction.getStatus()) || "CANCELED".equals(auction.getStatus())) {
      return;
    }
    auction.setStatus("FINISHED");
    try (Connection conn = DatabaseConnection.getConnection()) {
      conn.setAutoCommit(false);
      updateAuctionInTransaction(conn, auction);
      // Lưu kết quả
      String sqlResult = """
          INSERT INTO auction_results (auction_id, winner_id, final_price)
          VALUES (?, ?, ?)
          """;
      try (PreparedStatement stmt = conn.prepareStatement(sqlResult)) {
        stmt.setInt(1, auctionId);
        if (auction.getHighestBidder() != null) {
          stmt.setInt(2, auction.getHighestBidder().getId());
          stmt.setBigDecimal(3, auction.getCurrentPrice());
        } else {
          stmt.setNull(2, Types.INTEGER);
          stmt.setBigDecimal(3, auction.getCurrentPrice());
        }
        stmt.executeUpdate();
      }
      // Cập nhật trạng thái item
      itemDao.setInAuction(auction.getItem().getId(), false);
      // 2. ÉP TRẠNG THÁI SẢN PHẨM THÀNH "AUCTION_ENDED" TRONG DATABASE
      String updateItemSql = "UPDATE items SET status = 'AUCTION_ENDED' WHERE id = ?";
      try (PreparedStatement stmtItem = conn.prepareStatement(updateItemSql)) {
        stmtItem.setInt(1, auction.getItem().getId());
        stmtItem.executeUpdate();
      }

      conn.commit();

      // Tạo transaction cho người thắng và người bán (có thể sau commit)
      if (auction.getHighestBidder() != null) {
        createPaymentTransactions(auction);
      }
    } catch (SQLException e) {
      throw new UserException("Lỗi kết thúc phiên: " + e.getMessage());
    }
  }

  // =========================================================
  //  QUẢN LÝ AUTO-BID
  // =========================================================

  public void setAutoBid(int auctionId, int bidderId, BigDecimal maxBid) throws UserException {
    String sql = """
        INSERT INTO auto_bids (auction_id, bidder_id, max_bid, is_active)
        VALUES (?, ?, ?, TRUE)
        ON DUPLICATE KEY UPDATE max_bid = ?, is_active = TRUE
        """;
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, auctionId);
      stmt.setInt(2, bidderId);
      stmt.setBigDecimal(3, maxBid);
      stmt.setBigDecimal(4, maxBid);
      stmt.executeUpdate();
    } catch (SQLException e) {
      throw new UserException("Lỗi đăng ký auto-bid: " + e.getMessage());
    }
  }

  public void removeAutoBid(int auctionId, int bidderId) throws UserException {
    String sql = "UPDATE auto_bids SET is_active = FALSE WHERE auction_id = ? AND bidder_id = ?";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, auctionId);
      stmt.setInt(2, bidderId);
      stmt.executeUpdate();
    } catch (SQLException e) {
      throw new UserException("Lỗi xóa auto-bid: " + e.getMessage());
    }
  }

  // =========================================================
  //  QUẢN LÝ ĐĂNG KÝ THAM GIA
  // =========================================================

  public void registerBidderForAuction(int auctionId, int bidderId) throws UserException {
    String sql = "INSERT IGNORE INTO auction_registrations (auction_id, bidder_id) VALUES (?, ?)";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, auctionId);
      stmt.setInt(2, bidderId);
      stmt.executeUpdate();
    } catch (SQLException e) {
      throw new UserException("Lỗi đăng ký: " + e.getMessage());
    }
  }

  public boolean isBidderRegistered(int auctionId, int bidderId) {
    String sql = "SELECT 1 FROM auction_registrations WHERE auction_id = ? AND bidder_id = ? LIMIT 1";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, auctionId);
      stmt.setInt(2, bidderId);
      try (ResultSet rs = stmt.executeQuery()) {
        return rs.next();
      }
    } catch (SQLException e) {
      System.err.println("Lỗi kiểm tra đăng ký: " + e.getMessage());
    }
    return false;
  }

  public List<Integer> getRegisteredAuctionIdsForBidder(int bidderId) {
    List<Integer> list = new ArrayList<>();
    String sql = "SELECT auction_id FROM auction_registrations WHERE bidder_id = ? ORDER BY auction_id DESC";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, bidderId);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) list.add(rs.getInt("auction_id"));
      }
    } catch (SQLException e) {
      System.err.println("Lỗi lấy đăng ký của bidder: " + e.getMessage());
    }
    return list;
  }

  public List<Integer> getRegisteredBidders(int auctionId) {
    List<Integer> list = new ArrayList<>();
    String sql = "SELECT bidder_id FROM auction_registrations WHERE auction_id = ?";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, auctionId);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) list.add(rs.getInt(1));
      }
    } catch (SQLException e) {
      System.err.println("Lỗi lấy danh sách đăng ký: " + e.getMessage());
    }
    return list;
  }

  // =========================================================
  //  PRIVATE HELPERS
  // =========================================================

  public int getRegistrationCount(int auctionId) {
    return countRegistrations(auctionId);
  }

  private int countRegistrations(int auctionId) {
    String sql = "SELECT COUNT(*) AS cnt FROM auction_registrations WHERE auction_id = ?";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, auctionId);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) return rs.getInt("cnt");
      }
    } catch (SQLException e) {
      System.err.println("Lỗi đếm đăng ký phiên " + auctionId + ": " + e.getMessage());
    }
    return 0;
  }

  public boolean isUserRegistered(int auctionId, int bidderId) {
    String sql = "SELECT 1 FROM auction_registrations WHERE auction_id = ? AND bidder_id = ?";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, auctionId);
      stmt.setInt(2, bidderId);
      try (ResultSet rs = stmt.executeQuery()) {
        return rs.next(); // Nếu tìm thấy bản ghi nghĩa là đã đăng ký
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return false;
  }
  private Auction mapAuction(ResultSet rs) throws SQLException, UserException {
    int id = rs.getInt("id");
    int itemId = rs.getInt("item_id");
    Item item = itemDao.findById(itemId);
    BigDecimal currentPrice = rs.getBigDecimal("current_price");
    LocalDateTime start = rs.getTimestamp("start_time").toLocalDateTime();
    LocalDateTime end = rs.getTimestamp("end_time").toLocalDateTime();
    String status = rs.getString("status");
    Auction auction = new Auction(id, item, currentPrice, start, end, status);
    auction.setBidIncrement(rs.getBigDecimal("bid_increment"));
    auction.setAntiSnipeWindowMinutes(rs.getInt("anti_snipe_window_minutes"));
    auction.setAntiSnipeExtensionMinutes(rs.getInt("anti_snipe_extension_minutes"));

    int bidderId = rs.getInt("highest_bidder_id");
    if (!rs.wasNull()) {
      User u = userDao.findById(bidderId);
      if (u instanceof Customer) auction.setHighestBidder((Customer) u);
    }
    return auction;
  }

  private void updateAuctionInTransaction(Connection conn, Auction auction) throws SQLException {
    String sql = """
        UPDATE auctions
        SET current_price = ?, highest_bidder_id = ?, end_time = ?, status = ?
        WHERE id = ?
        """;
    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setBigDecimal(1, auction.getCurrentPrice());
      if (auction.getHighestBidder() != null)
        stmt.setInt(2, auction.getHighestBidder().getId());
      else
        stmt.setNull(2, Types.INTEGER);
      stmt.setTimestamp(3, Timestamp.valueOf(auction.getEndTime()));
      stmt.setString(4, auction.getStatus());
      stmt.setInt(5, auction.getId());
      stmt.executeUpdate();
    }
  }

  private void logSniping(Connection conn, int auctionId, LocalDateTime oldEnd, LocalDateTime newEnd,
                          int extendedMinutes, int triggeredByBidId) throws SQLException {
    String sql = "INSERT INTO sniping_logs (auction_id, old_end_time, new_end_time, extended_by_minutes, triggered_by_bid_id) VALUES (?, ?, ?, ?, ?)";
    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, auctionId);
      stmt.setTimestamp(2, Timestamp.valueOf(oldEnd));
      stmt.setTimestamp(3, Timestamp.valueOf(newEnd));
      stmt.setInt(4, extendedMinutes);
      stmt.setInt(5, triggeredByBidId);
      stmt.executeUpdate();
    }
  }

  private void triggerAutoBids(Connection conn, Auction auction, int triggeringBidId) throws SQLException, UserException {
    boolean changed;
    do {
      changed = false;
      String sql = """
          SELECT ab.id, ab.bidder_id, ab.max_bid
          FROM auto_bids ab
          WHERE ab.auction_id = ? AND ab.is_active = TRUE
            AND ab.bidder_id != ?
          ORDER BY ab.max_bid DESC
          """;
      try (PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setInt(1, auction.getId());
        stmt.setInt(2, auction.getHighestBidder().getId());
        try (ResultSet rs = stmt.executeQuery()) {
          while (rs.next()) {
            int autoBidId = rs.getInt("id");
            int bidderId = rs.getInt("bidder_id");
            BigDecimal maxBid = rs.getBigDecimal("max_bid");

            // 🎯 FIX LỖI "5 VNĐ": Lấy bước giá chuẩn của Seller (mặc định 1000 nếu chưa có)
            BigDecimal increment = auction.getBidIncrement() != null ? auction.getBidIncrement() : BigDecimal.valueOf(1000);
            BigDecimal nextBid = auction.getCurrentPrice().add(increment);

            // Kiểm tra xem mức giá tự động tiếp theo có vượt quá "Trần" của người dùng không
            if (nextBid.compareTo(maxBid) <= 0) {

              // 🎯 FIX LỖI SỐ DƯ: Kiểm tra xem người cài Auto-bid còn đủ tiền không
              BigDecimal autoBidderBalance = userDao.getBalance(bidderId);
              if (nextBid.compareTo(autoBidderBalance) > 0) {
                // Nếu không đủ tiền, tự động tắt Auto-bid của người này để tránh làm treo luồng
                String deactivateSql = "UPDATE auto_bids SET is_active = FALSE WHERE id = ?";
                try(PreparedStatement ps = conn.prepareStatement(deactivateSql)) {
                  ps.setInt(1, autoBidId);
                  ps.executeUpdate();
                }
                continue; // Bỏ qua người này, xét người tiếp theo
              }

              User user = userDao.findById(bidderId);
              if (!(user instanceof Customer)) continue;
              Customer bidder = (Customer) user;

              // Thực hiện đặt giá tự động
              int newBidId = bidDao.addBid(conn, auction.getId(), bidderId, nextBid, LocalDateTime.now());

              // Log lại việc auto-bid đã nhảy giá
              String logSql = "INSERT INTO auto_bid_logs (auto_bid_id, triggered_bid_id, bid_amount) VALUES (?, ?, ?)";
              try (PreparedStatement logStmt = conn.prepareStatement(logSql)) {
                logStmt.setInt(1, autoBidId);
                logStmt.setInt(2, newBidId);
                logStmt.setBigDecimal(3, nextBid);
                logStmt.executeUpdate();
              }

              // Cập nhật giá mới nhất cho Auction
              auction.setCurrentPrice(nextBid);
              auction.setHighestBidder(bidder);
              updateAuctionInTransaction(conn, auction);

              changed = true;
              break; // Có giá mới, phải scan lại từ đầu danh sách auto-bid
            }
          }
        }
      }
    } while (changed);
  }

  private void createPaymentTransactions(Auction auction) throws UserException {
    Customer winner = auction.getHighestBidder();
    if (winner == null) return;
    BigDecimal price = auction.getCurrentPrice();
    int sellerId = auction.getItem().getSellerId();

    // Trừ tiền người thắng
    try {
      userDao.updateBalance(winner.getId(), price.negate());
      Transaction payTx = new Transaction(winner.getId(), auction.getId(), price,
          TransactionType.PAY_FOR_AUCTION, TransactionStatus.SUCCESS);
      transactionDao.addTransaction(payTx);
    } catch (UserException | SQLException e) {
      throw new UserException("Lỗi tạo transaction thanh toán: " + e.getMessage());
    }

    // Cộng tiền cho seller
    try {
      userDao.updateBalance(sellerId, price);
      Transaction receiveTx = new Transaction(sellerId, auction.getId(), price,
          TransactionType.RECEIVE_FROM_AUCTION, TransactionStatus.SUCCESS);
      transactionDao.addTransaction(receiveTx);
    } catch (UserException | SQLException e) {
      throw new UserException("Lỗi tạo transaction nhận tiền: " + e.getMessage());
    }
  }

  // =========================================================
  //  DELETE – Chức năng dành riêng cho Admin
  // =========================================================

  /**
   * Xóa hoàn toàn phiên đấu giá khỏi hệ thống (Chức năng Admin).
   * Xử lý trọn gói trong một Transaction để dọn dẹp các bảng con trước, tránh lỗi Khóa Ngoại.
   */
  public void deleteAuction(int auctionId) throws UserException {
    // 1. Đọc thông tin phiên trước khi xóa để lấy item_id nhằm giải phóng sản phẩm sau đó
    Auction auction = findById(auctionId);

    // Định nghĩa các câu lệnh xóa sạch "tàn dư" ở các bảng liên quan
    String deleteRegistrations = "DELETE FROM auction_registrations WHERE auction_id = ?";
    String deleteAutoBidLogs = "DELETE FROM auto_bid_logs WHERE auto_bid_id IN (SELECT id FROM auto_bids WHERE auction_id = ?)";
    String deleteAutoBids = "DELETE FROM auto_bids WHERE auction_id = ?";
    String deleteSnipingLogs = "DELETE FROM sniping_logs WHERE auction_id = ?";
    String deleteBids = "DELETE FROM bids WHERE auction_id = ?";
    String deleteAuction = "DELETE FROM auctions WHERE id = ?";

    try (Connection conn = DatabaseConnection.getConnection()) {
      conn.setAutoCommit(false); // Bật Transaction
      try {
        // a. Xóa đăng ký tham gia của Bidder
        try (PreparedStatement stmt = conn.prepareStatement(deleteRegistrations)) {
          stmt.setInt(1, auctionId);
          stmt.executeUpdate();
        }

        // b. Xóa log của Auto-bid trước
        try (PreparedStatement stmt = conn.prepareStatement(deleteAutoBidLogs)) {
          stmt.setInt(1, auctionId);
          stmt.executeUpdate();
        }

        // c. Xóa cấu hình Auto-bid
        try (PreparedStatement stmt = conn.prepareStatement(deleteAutoBids)) {
          stmt.setInt(1, auctionId);
          stmt.executeUpdate();
        }

        // d. Xóa lịch sử Anti-sniping
        try (PreparedStatement stmt = conn.prepareStatement(deleteSnipingLogs)) {
          stmt.setInt(1, auctionId);
          stmt.executeUpdate();
        }

        // e. Xóa tất cả lượt đặt giá (bids) của phiên này
        try (PreparedStatement stmt = conn.prepareStatement(deleteBids)) {
          stmt.setInt(1, auctionId);
          stmt.executeUpdate();
        }

        // f. Cuối cùng, xóa phiên đấu giá chính gốc
        try (PreparedStatement stmt = conn.prepareStatement(deleteAuction)) {
          stmt.setInt(1, auctionId);
          int affectedRows = stmt.executeUpdate();
          if (affectedRows == 0) {
            throw new UserException("Không tìm thấy phiên đấu giá mang ID: " + auctionId + " để xóa.");
          }
        }

        // g. GIẢI PHÓNG SẢN PHẨM: Đưa trạng thái sản phẩm về tự do (is_in_auction = false)
        // Việc này giúp sản phẩm có cơ hội được đăng vào một phiên đấu giá hợp lệ khác!
        itemDao.setInAuction(auction.getItem().getId(), false);

        conn.commit(); // Hoàn tất cuộc dọn dẹp
        System.out.println("[DB Sync] Admin đã xóa sạch dữ liệu phiên đấu giá ID: " + auctionId);
      } catch (SQLException | UserException ex) {
        conn.rollback(); // Có biến cố xảy ra thì hủy bỏ toàn bộ thao tác, hoàn tác DB
        throw new UserException("Lỗi hệ thống khi dọn dẹp dữ liệu phiên đấu giá: " + ex.getMessage());
      } finally {
        conn.setAutoCommit(true);
      }
    } catch (SQLException e) {
      throw new UserException("Lỗi kết nối cơ sở dữ liệu: " + e.getMessage());
    }
  }
  // =========================================================
  //  THỐNG KÊ (Cho Admin Dashboard)
  // =========================================================

  /**
   * Đếm số lượng phiên đấu giá đang trong trạng thái hoạt động (RUNNING)
   */
  public int getActiveAuctionsCount() {
    String sql = "SELECT COUNT(*) FROM auctions WHERE status = 'RUNNING'";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql);
         ResultSet rs = stmt.executeQuery()) {
      if (rs.next()) {
        return rs.getInt(1);
      }
    } catch (SQLException e) {
      System.err.println("❌ Lỗi đếm số phiên đấu giá đang chạy: " + e.getMessage());
    }
    return 0;
  }

  /** * Hàm tối ưu hóa đặc biệt dành riêng cho Sảnh đấu giá (AuctionListController).
   * Chống lỗi N+1 Query và Over-fetching.
   */
  /**
   * Hàm tối ưu hóa đặc biệt dành riêng cho Sảnh đấu giá (AuctionListController).
   * Chống lỗi N+1 Query và kết hợp hoàn hảo với ItemFactory.
   */
  public List<Auction> getFastRunningAuctionsForHall() {
    List<Auction> list = new ArrayList<>();
    // Câu SQL lấy dữ liệu cần thiết (5 cột yêu cầu) + ID để map sau này
    String sql = """
        SELECT
            a.id, a.current_price, a.start_time, a.end_time, a.status,
            a.bid_increment, a.anti_snipe_window_minutes, a.anti_snipe_extension_minutes,
            i.id AS item_id, i.name AS item_name, i.type AS item_type, i.city AS item_city,
            (SELECT COUNT(*) FROM auction_registrations ar WHERE ar.auction_id = a.id) AS reg_count
        FROM auctions a
        INNER JOIN items i ON a.item_id = i.id
        WHERE a.status = 'RUNNING'
        ORDER BY a.start_time DESC
        """;

    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql);
         ResultSet rs = stmt.executeQuery()) {

      while (rs.next()) {
        // Khởi tạo Item cơ bản mà không cần gọi Factory phức tạp
        // Lưu ý: Item chỉ cần các field hiển thị trên TableView
        Item item = new Electronics(rs.getInt("item_id"), rs.getString("item_name"), null, null, null, 0, null, 0);
        item.setType(rs.getString("item_type"));
        item.setCity(rs.getString("item_city"));

        Auction auction = new Auction(
            rs.getInt("id"),
            item,
            rs.getBigDecimal("current_price"),
            rs.getTimestamp("start_time").toLocalDateTime(),
            rs.getTimestamp("end_time").toLocalDateTime(),
            rs.getString("status")
        );
        auction.setRegisteredCount(rs.getInt("reg_count"));
        list.add(auction);
      }
    } catch (SQLException e) {
      System.err.println("❌ Lỗi truy vấn sảnh đấu giá: " + e.getMessage());
    }
    return list;
  }
  public boolean registerForAuction(int auctionId, int userId) throws UserException {
    String sql = "INSERT INTO auction_registrations (auction_id, user_id) VALUES (?, ?)";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, auctionId);
      stmt.setInt(2, userId);
      int rows = stmt.executeUpdate();
      return rows > 0;
    } catch (SQLException e) {
      if (e.getErrorCode() == 1062) { // Mã lỗi Duplicate entry của MySQL
        throw new UserException("ALREADY_REGISTERED");
      }
      throw new UserException("Lỗi lưu đăng ký: " + e.getMessage());
    }
  }

}