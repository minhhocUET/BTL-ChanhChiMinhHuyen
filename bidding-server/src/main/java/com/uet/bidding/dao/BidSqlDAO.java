package com.uet.bidding.dao;

import com.uet.bidding.exception.UserException;
import com.uet.bidding.model.Bid;
import com.uet.bidding.model.Bidder;
import com.uet.bidding.model.Customer;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * BidSqlDAO – thao tác với bảng bids.
 * <p>
 * Bảng bids: id, auction_id, bidder_id, bid_amount, bid_time.
 */
public class BidSqlDAO {

  private final UserSqlDAO userDao = new UserSqlDAO();

  // =========================================================
  //  CREATE
  // =========================================================

  /**
   * Thêm một lượt bid (dùng connection riêng, tự commit).
   *
   * @return id của bản ghi bid vừa tạo.
   */
  public int addBid(int auctionId, int bidderId, BigDecimal amount, LocalDateTime time) throws SQLException {
    try (Connection conn = DatabaseConnection.getConnection()) {
      // Gọi luôn lại hàm addBid bên dưới truyền conn vào, code gọn đi một nửa!
      return addBid(conn, auctionId, bidderId, amount, time);
    }
  }

  /**
   * Thêm lượt bid trong một transaction đã mở (dùng chung connection).
   *
   * @return id của bản ghi bid vừa tạo.
   */
  public int addBid(Connection conn, int auctionId, int bidderId, BigDecimal amount, LocalDateTime time)
      throws SQLException {
    String sql = "INSERT INTO bids (auction_id, bidder_id, bid_amount, bid_time) VALUES (?, ?, ?, ?)";
    try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      stmt.setInt(1, auctionId);
      stmt.setInt(2, bidderId);
      stmt.setBigDecimal(3, amount);
      stmt.setTimestamp(4, Timestamp.valueOf(time));
      stmt.executeUpdate();

      try (ResultSet rs = stmt.getGeneratedKeys()) {
        if (rs.next()) return rs.getInt(1);
        throw new SQLException("Không thể lấy id của bid vừa tạo.");
      }
    }
  }

  // =========================================================
  //  READ
  // =========================================================

  /**
   * Lấy danh sách tất cả lượt bid của một phiên đấu giá.
   * Đã tối ưu hóa bằng INNER JOIN để tránh lỗi N+1 Query gây lag Server.
   *
   * @return List<Bid> danh sách lịch sử đặt giá.
   */
  public List<Bid> getBidsByAuction(int auctionId) {
    List<Bid> bids = new ArrayList<>();

    // 🎯 SỬ DỤNG JOIN: Lấy luôn username từ bảng users trong cùng 1 câu lệnh
    String sql = """
        SELECT b.bidder_id, b.bid_amount, b.bid_time, u.username 
        FROM bids b 
        INNER JOIN users u ON b.bidder_id = u.id 
        WHERE b.auction_id = ? 
        ORDER BY b.bid_amount DESC
        """;

    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setInt(1, auctionId);

      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          int bidderId = rs.getInt("bidder_id");
          BigDecimal amount = rs.getBigDecimal("bid_amount");
          LocalDateTime time = rs.getTimestamp("bid_time").toLocalDateTime();
          String username = rs.getString("username");

          // 🎯 TẠO OBJECT SIÊU NHẸ: Thay vì bắt DB phải tìm kiếm lại toàn bộ thông tin User,
          // ta chỉ tạo một Object giả lập (Dummy Object) chứa đúng những thông tin Client cần để hiển thị.
          Customer lightweightCustomer = new Customer();
          lightweightCustomer.setId(bidderId);
          lightweightCustomer.setUsername(username);

          // Lấy profile trống hoặc gán ID tùy theo cấu trúc Model của sếp
          Bidder bidder = new Bidder();
          // bidder.setId(bidderId); // Mở comment này nếu class Bidder của sếp cần truyền ID

          Bid bid = new Bid(bidder, amount, time);
          bid.setBidderUsername(username); // Truyền thẳng username lấy từ câu SQL vào

          bids.add(bid);
        }
      }
    } catch (SQLException e) {
      System.err.println("❌ Lỗi khi lấy danh sách bid: " + e.getMessage());
    }

    return bids;
  }

  /**
   * Lấy giá cao nhất hiện tại của phiên (dùng MAX).
   */
  public BigDecimal getCurrentMaxBid(int auctionId) {
    String sql = "SELECT MAX(bid_amount) FROM bids WHERE auction_id = ?";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, auctionId);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          BigDecimal max = rs.getBigDecimal(1);
          return max != null ? max : BigDecimal.ZERO;
        }
      }
    } catch (SQLException e) {
      System.err.println("Lỗi lấy giá cao nhất: " + e.getMessage());
    }
    return BigDecimal.ZERO;
  }

  /**
   * Lấy số lượt bid của một phiên.
   */
  /**
   * Giá cao nhất mà một bidder đã đặt trong phiên (null nếu chưa đặt).
   */
  public BigDecimal getMaxBidByBidder(int auctionId, int bidderId) {
    String sql = "SELECT MAX(bid_amount) AS max_bid FROM bids WHERE auction_id = ? AND bidder_id = ?";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, auctionId);
      stmt.setInt(2, bidderId);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return rs.getBigDecimal("max_bid");
        }
      }
    } catch (SQLException e) {
      System.err.println("Lỗi lấy giá bid của bidder: " + e.getMessage());
    }
    return null;
  }

  public int getBidCount(int auctionId) {
    String sql = "SELECT COUNT(*) FROM bids WHERE auction_id = ?";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, auctionId);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) return rs.getInt(1);
      }
    } catch (SQLException e) {
      System.err.println("Lỗi đếm bid: " + e.getMessage());
    }
    return 0;
  }

  // =========================================================
  //  DELETE (tuỳ chọn)
  // =========================================================

  /**
   * Xóa tất cả lượt bid của một phiên (dùng khi phiên bị hủy).
   */
  public void deleteBidsByAuction(int auctionId) throws SQLException {
    String sql = "DELETE FROM bids WHERE auction_id = ?";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, auctionId);
      stmt.executeUpdate();
    }
  }
}