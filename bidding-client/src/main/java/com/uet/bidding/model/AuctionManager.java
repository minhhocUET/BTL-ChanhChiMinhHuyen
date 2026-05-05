package com.uet.bidding.model;

import com.uet.bidding.dao.AuctionDAO;
import com.uet.bidding.exception.AuctionClosedException;
import com.uet.bidding.exception.InvalidBidException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class AuctionManager {

  private static volatile AuctionManager instance;
  private ConcurrentHashMap<Integer, Auction> auctions = new ConcurrentHashMap<>();
  private ConcurrentHashMap<Integer, ReentrantLock> locks = new ConcurrentHashMap<>();

  // Quản lý ID tự động tăng an toàn trong môi trường đa luồng
  private AtomicInteger auctionIdCounter = new AtomicInteger(1);

  // 1. THÊM DAO: Để đọc/ghi file .dat
  private AuctionDAO auctionDAO;

  private AuctionManager() {
    System.out.println("Hệ thống quản lý đấu giá đã được khởi động!");
  }

  public static AuctionManager getInstance() {
    if (instance == null) {
      synchronized (AuctionManager.class) {
        if (instance == null) instance = new AuctionManager();
      }
    }
    return instance;
  }

  /**
   * 2. HÀM KHỞI TẠO DỮ LIỆU: Nạp từ file auctions.dat lên RAM khi Server bật
   */
  public void initialize(AuctionDAO dao) {
    this.auctionDAO = dao;
    List<Auction> savedAuctions = dao.getAllAuctions();

    int maxId = 0;
    for (Auction a : savedAuctions) {
      addAuction(a);
      if (a.getId() > maxId) {
        maxId = a.getId(); // Tìm ID lớn nhất đang có trong file
      }
    }

    // Chỉnh lại bộ đếm ID cho phiên đấu giá tạo mới tiếp theo
    auctionIdCounter.set(maxId + 1);
    System.out.println("Đã nạp " + auctions.size() + " phiên đấu giá từ file.");
  }

  private void addAuction(Auction auction) {
    auctions.put(auction.getId(), auction);
    locks.put(auction.getId(), new ReentrantLock());
  }

  public List<Auction> getAllAuctions() {
    return new ArrayList<>(auctions.values());
  }

  public Auction getAuction(int id) {
    return auctions.get(id);
  }

  // ================== CÁC HÀM TẠO VÀ QUẢN LÝ TRẠNG THÁI ==================

  public Auction createAuction(Item item, LocalDateTime endTime) {
    int newId = auctionIdCounter.getAndIncrement();
    BigDecimal startPrice = item.getStartingPrice();
    LocalDateTime startTime = LocalDateTime.now();

    Auction newAuction = new Auction(item, startPrice, startTime, endTime);
    newAuction.setId(newId);

    addAuction(newAuction);

    // Lưu ngay xuống file
    if (auctionDAO != null) {
      auctionDAO.updateAuction(newAuction);
    }
    return newAuction;
  }

  public void startAuction(int auctionId) {
    ReentrantLock lock = locks.get(auctionId);
    if (lock == null) return;

    lock.lock();
    try {
      Auction auction = auctions.get(auctionId);
      if (auction != null) {
        auction.setStatus("RUNNING");
        if (auctionDAO != null) auctionDAO.updateAuction(auction); // Update file
        System.out.println("Phiên đấu giá #" + auctionId + " chính thức bắt đầu!");
      }
    } finally {
      lock.unlock();
    }
  }

  public void endAuction(int auctionId) {
    ReentrantLock lock = locks.get(auctionId);
    if (lock == null) return;

    lock.lock();
    try {
      Auction auction = auctions.get(auctionId);
      if (auction != null) {
        auction.setStatus("FINISHED");
        if (auctionDAO != null) auctionDAO.updateAuction(auction); // Update file
        System.out.println("--- KẾT THÚC PHIÊN #" + auctionId + " ---");
      }
    } finally {
      lock.unlock();
    }
  }

  // ================== CORE LOGIC: ĐẶT GIÁ AN TOÀN ==================

  /**
   * Đặt giá an toàn (thread-safe) và cập nhật xuống file ngay lập tức
   * (Đã sửa tham số và logic gọi hàm cho khớp với bản cập nhật mới nhất)
   */
  public boolean placeBid(int auctionId, Bidder bidder, BigDecimal amount)
      throws AuctionClosedException, InvalidBidException {

    Auction auction = auctions.get(auctionId);
    if (auction == null) throw new InvalidBidException("Không tìm thấy phiên đấu giá!");

    ReentrantLock lock = locks.get(auctionId);
    lock.lock();
    try {
      // Gọi logic placeNewBid (Bản sửa dùng Object Bidder và BigDecimal)
      boolean success = auction.placeNewBid(bidder, amount);

      if (success) {
        // 3. QUAN TRỌNG: Lưu ngay lập tức xuống file auctions.dat qua DAO
        if (auctionDAO != null) {
          auctionDAO.updateAuction(auction);
        }
        System.out.println("[Server] " + bidder.getUsername() + " bid thành công: " + amount + " cho ID: " + auctionId);
      }
      return success;

    } finally {
      lock.unlock();
    }
  }
}