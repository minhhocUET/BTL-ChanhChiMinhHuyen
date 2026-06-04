package com.uet.bidding.service;

import com.uet.bidding.dao.AuctionSqlDAO;
import com.uet.bidding.exception.AuctionClosedException;
import com.uet.bidding.exception.InvalidBidException;
import com.uet.bidding.exception.UserException;
import com.uet.bidding.model.Auction;
import com.uet.bidding.model.Customer;
import com.uet.bidding.model.Item;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class AuctionManager {

  private static volatile AuctionManager instance;

  // Cache lưu cấu hình ngay trên RAM để tăng tốc độ phản hồi Realtime
  private final ConcurrentHashMap<Integer, Auction> auctions = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<Integer, ReentrantLock> locks = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<Integer, List<RemoteAutoBid>> autoBidCache = new ConcurrentHashMap<>();

  private AuctionSqlDAO auctionSqlDAO;

  // DTO gọn nhẹ nằm ngay trong Manager để không làm bẩn package model
  public static class RemoteAutoBid {
    public int id;
    public int bidderId;
    public BigDecimal maxBid;
    public RemoteAutoBid(int id, int bidderId, BigDecimal maxBid) {
      this.id = id;
      this.bidderId = bidderId;
      this.maxBid = maxBid;
    }
  }

  private AuctionManager() {
    System.out.println("Hệ thống quản lý đấu giá UET đã được khởi động!");
  }

  public static AuctionManager getInstance() {
    if (instance == null) {
      synchronized (AuctionManager.class) {
        if (instance == null) instance = new AuctionManager();
      }
    }
    return instance;
  }

  public void initialize(AuctionSqlDAO dao) {
    this.auctionSqlDAO = dao;
    // 🎯 SỬA CHỖ NÀY: Dùng hàm của Admin để nạp TOÀN BỘ phiên (kể cả đã kết thúc/hủy) lên RAM
    List<Auction> savedAuctions = dao.getAllAuctionsForAdmin();

    for (Auction a : savedAuctions) {
      addAuctionInternal(a);

      // Nạp cấu hình Auto-Bid từ DB lên RAM khi hệ thống khởi động
      List<RemoteAutoBid> activeBids = dao.getAutoBidsByAuctionId(a.getId());
      if (activeBids != null && !activeBids.isEmpty()) {
        autoBidCache.put(a.getId(), activeBids);
      }
    }
    System.out.println("Đã nạp " + auctions.size() + " phiên đấu giá và cấu hình Auto-Bid vào Cache RAM.");
  }

  private void addAuctionInternal(Auction auction) {
    auctions.put(auction.getId(), auction);
    locks.computeIfAbsent(auction.getId(), k -> new ReentrantLock());
  }

  // ================== CÁC THAO TÁC CỦA ADMIN (ĐỒNG BỘ RAM & DB) ==================

  public boolean updateAuctionStatus(int auctionId, String status) {
    ReentrantLock lock = locks.computeIfAbsent(auctionId, k -> new ReentrantLock());
    lock.lock();
    try {
      boolean success = false;
      if ("FINISHED".equals(status)) {
        try {
          auctionSqlDAO.finishAuction(auctionId);
          success = true;
          autoBidCache.remove(auctionId); // Phiên đóng thì dọn cache AutoBid luôn
          System.out.println("Phiên #" + auctionId + " đã KẾT THÚC và xử lý giao dịch thành công.");
        } catch (UserException e) {
          System.err.println("Lỗi khi kết thúc phiên đấu giá: " + e.getMessage());
        }
      } else {
        // 🎯 SỬA CHỖ NÀY: Gọi DB cập nhật trạng thái thực sự thay vì chỉ set trên RAM
        success = auctionSqlDAO.updateAuctionState(auctionId, status);
      }

      // Nếu dưới DB cập nhật thành công, đồng bộ lại RAM ngay lập tức
      if (success) {
        try {
          refreshAuctionFromDb(auctionId);
          System.out.println("[RAM Sync] Đã đồng bộ trạng thái mới '" + status + "' cho phiên #" + auctionId);
        } catch (UserException e) {
          auctions.remove(auctionId);
        }
      }
      return success;
    } finally {
      lock.unlock();
    }
  }

  /**
   * 🎯 HÀM ADMIN KHÔI PHỤC: Xóa cứng hoàn toàn phiên đấu giá (DB + RAM)
   */
  public void deleteAuction(int auctionId) throws UserException {
    ReentrantLock lock = locks.get(auctionId);
    if (lock != null) {
      lock.lock();
    }
    try {
      // 1. Thực hiện lệnh xóa an toàn bọc transaction dọn 6 bảng dưới DB
      auctionSqlDAO.deleteAuction(auctionId);

      // 2. Dọn sạch tàn dư trên Cache RAM để tránh lệch pha dữ liệu
      auctions.remove(auctionId);
      autoBidCache.remove(auctionId);
      locks.remove(auctionId);

      System.out.println("[RAM Sync] Admin đã dọn sạch RAM và DB cho phiên bị xóa ID: " + auctionId);
    } finally {
      if (lock != null && lock.isHeldByCurrentThread()) {
        lock.unlock();
      }
    }
  }

  // ================== CORE LOGIC: ĐẶT GIÁ AN TOÀN ==================

  public Auction createAuction(Item item, BigDecimal startPrice, LocalDateTime endTime, BigDecimal bidIncrement) throws UserException {
    LocalDateTime startTime = LocalDateTime.now();
    Auction newAuction = auctionSqlDAO.createAuction(item, startPrice, startTime, endTime, bidIncrement);
    addAuctionInternal(newAuction);
    return newAuction;
  }

  public boolean placeBid(int auctionId, Customer customer, BigDecimal amount)
      throws AuctionClosedException, InvalidBidException, UserException {

    Auction auction = auctions.get(auctionId);
    if (auction == null) {
      throw new InvalidBidException("Không tìm thấy phiên đấu giá trên hệ thống!");
    }

    if (!"RUNNING".equals(auction.getStatus())) {
      throw new AuctionClosedException("Phiên đấu giá không ở trạng thái sẵn sàng (Đã đóng hoặc chưa mở)!");
    }

    // Kiểm tra đăng ký
    boolean isRegistered = auctionSqlDAO.isBidderRegistered(auctionId, customer.getId());
    if (!isRegistered) {
      throw new InvalidBidException("Bạn chưa đăng ký tham gia phiên đấu giá này! Vui lòng ấn nút đăng ký trước.");
    }

    ReentrantLock lock = locks.computeIfAbsent(auctionId, k -> new ReentrantLock());
    lock.lock();
    try {
      // Thực hiện đặt giá xuống DB
      boolean success = auctionSqlDAO.placeBid(auctionId, customer, amount);

      if (success) {
        // Đồng bộ lại thông tin Auction mới nhất (giá mới, bidder mới) lên RAM
        refreshAuctionFromDb(auctionId);
        System.out.println("[Server] " + customer.getUsername() + " đặt giá thành công: " + amount);
      }
      return success;
    } finally {
      lock.unlock();
    }
  }

  // Đăng ký/Cập nhật cấu hình Auto-Bid từ UI Controller
  public void enableAutoBid(int auctionId, int bidderId, BigDecimal maxBid) throws UserException {
    auctionSqlDAO.setAutoBid(auctionId, bidderId, maxBid);
    syncAutoBidCache(auctionId);
  }

  public void syncAutoBidCache(int auctionId) {
    if (auctionSqlDAO == null) return;
    List<RemoteAutoBid> updatedList = auctionSqlDAO.getAutoBidsByAuctionId(auctionId);
    autoBidCache.put(auctionId, updatedList);
    try {
      refreshAuctionFromDb(auctionId);
    } catch (UserException e) {
      auctions.remove(auctionId);
    }
  }

  // ================== GETTERS ==================

  public List<Auction> getAllAuctions() {
    List<Auction> list = new ArrayList<>(auctions.values());
    // 🎯 SỬA CHỖ NÀY: Sắp xếp ID giảm dần (mới nhất lên đầu) để Giao diện Admin hiển thị chuẩn
    list.sort((a1, a2) -> Integer.compare(a2.getId(), a1.getId()));
    return list;
  }

  public Auction getAuction(int id) {
    return auctions.get(id);
  }

  public void refreshAuctionFromDb(int auctionId) throws UserException {
    if (auctionSqlDAO == null) return;
    Auction updated = auctionSqlDAO.findById(auctionId);
    if (updated != null) {
      auctions.put(auctionId, updated);
      locks.computeIfAbsent(auctionId, k -> new ReentrantLock());
    }
  }
}