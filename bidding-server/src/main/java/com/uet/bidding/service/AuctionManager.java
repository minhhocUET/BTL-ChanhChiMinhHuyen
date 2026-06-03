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
  private final ConcurrentHashMap<Integer, Auction> auctions = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<Integer, ReentrantLock> locks = new ConcurrentHashMap<>();

  // Cache lưu cấu hình Auto-Bid ngay trên RAM để Bot quét với tốc độ cao
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
    List<Auction> savedAuctions = dao.getAllAuctions();

    for (Auction a : savedAuctions) {
      addAuctionInternal(a);

      // Nạp cấu hình Auto-Bid từ DB lên RAM khi hệ thống khởi động
      List<RemoteAutoBid> activeBids = dao.getAutoBidsByAuctionId(a.getId());
      if (!activeBids.isEmpty()) {
        autoBidCache.put(a.getId(), activeBids);
      }
    }
    System.out.println("Đã nạp " + auctions.size() + " phiên đấu giá và cấu hình Auto-Bid từ dữ liệu.");
  }

  private void addAuctionInternal(Auction auction) {
    auctions.put(auction.getId(), auction);
    locks.put(auction.getId(), new ReentrantLock());
  }

  // ================== QUẢN LÝ PHIÊN ĐẤU GIÁ ==================

  public Auction createAuction(Item item, BigDecimal startPrice, LocalDateTime endTime, BigDecimal bidIncrement) throws UserException {
    LocalDateTime startTime = LocalDateTime.now();
    Auction newAuction = auctionSqlDAO.createAuction(item, startPrice, startTime, endTime, bidIncrement);
    addAuctionInternal(newAuction);
    return newAuction;
  }

  public void updateAuctionStatus(int auctionId, String status) {
    ReentrantLock lock = locks.get(auctionId);
    if (lock == null) return;

    lock.lock();
    try {
      if ("FINISHED".equals(status)) {
        try {
          auctionSqlDAO.finishAuction(auctionId);
          Auction updatedAuction = auctionSqlDAO.findById(auctionId);
          auctions.put(auctionId, updatedAuction);
          autoBidCache.remove(auctionId); // Phiên đóng thì dọn cache AutoBid luôn
          System.out.println("Phiên #" + auctionId + " đã KẾT THÚC và xử lý giao dịch thành công.");
        } catch (UserException e) {
          System.err.println("Lỗi khi kết thúc phiên đấu giá: " + e.getMessage());
        }
      } else {
        Auction auction = auctions.get(auctionId);
        if (auction != null) {
          auction.setStatus(status);
          System.out.println("Phiên #" + auctionId + " chuyển sang trạng thái: " + status);
        }
      }
    } finally {
      lock.unlock();
    }
  }

  // ================== CORE LOGIC: ĐẶT GIÁ AN TOÀN ==================

  public boolean placeBid(int auctionId, Customer customer, BigDecimal amount)
      throws AuctionClosedException, InvalidBidException, UserException {

    Auction auction = auctions.get(auctionId);
    if (auction == null) {
      throw new InvalidBidException("Không tìm thấy phiên đấu giá trên hệ thống!");
    }

    if (!"RUNNING".equals(auction.getStatus())) {
      throw new AuctionClosedException("Phiên đấu giá không ở trạng thái sẵn sàng (Đã đóng hoặc chưa mở)!");
    }

    // Kiểm tra đăng ký (Khớp với hàm check của bạn dưới DB)
    boolean isRegistered = auctionSqlDAO.isBidderRegistered(auctionId, customer.getId());
    if (!isRegistered) {
      throw new InvalidBidException("Bạn chưa đăng ký tham gia phiên đấu giá này! Vui lòng ấn nút đăng ký trước.");
    }

    ReentrantLock lock = locks.computeIfAbsent(auctionId, k -> new ReentrantLock());
    lock.lock();
    try {
      // Thực hiện đặt giá xuống DB (Đã bao gồm bọc Transaction cô lập)
      boolean success = auctionSqlDAO.placeBid(auctionId, customer, amount);

      if (success) {
        Auction updatedAuction = auctionSqlDAO.findById(auctionId);
        if (updatedAuction != null) {
          auctions.put(auctionId, updatedAuction);
        }
        System.out.println("[Server] " + customer.getUsername() + " đặt giá thành công: " + amount);
        // Proxy bids resolved inside AuctionSqlDAO.placeBid transaction (one-shot)
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

  /** Refresh in-memory auction + auto-bid cache after DB proxy resolution (no-op if not initialized). */
  public void syncAutoBidCache(int auctionId) {
    if (auctionSqlDAO == null) {
      return;
    }
    List<RemoteAutoBid> updatedList = auctionSqlDAO.getAutoBidsByAuctionId(auctionId);
    autoBidCache.put(auctionId, updatedList);
    Auction updatedAuction = auctionSqlDAO.findById(auctionId);
    if (updatedAuction != null) {
      auctions.put(auctionId, updatedAuction);
    }
  }

  // ================== GETTERS ==================

  public List<Auction> getAllAuctions() {
    return new ArrayList<>(auctions.values());
  }

  public Auction getAuction(int id) {
    return auctions.get(id);
  }

  public void refreshAuctionFromDb(int auctionId) throws UserException {
    if (auctionSqlDAO == null) return;
    Auction updated = auctionSqlDAO.findById(auctionId);
    auctions.put(auctionId, updated);
    locks.computeIfAbsent(auctionId, k -> new ReentrantLock());
  }
}