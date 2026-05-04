package com.uet.bidding.model;

import com.uet.bidding.dao.AuctionDAO;
import com.uet.bidding.exception.AuctionClosedException;
import com.uet.bidding.exception.InvalidBidException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class AuctionManager {

  private static volatile AuctionManager instance;
  private ConcurrentHashMap<Integer, Auction> auctions = new ConcurrentHashMap<>();
  private ConcurrentHashMap<Integer, ReentrantLock> locks = new ConcurrentHashMap<>();

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
    for (Auction a : savedAuctions) {
      addAuction(a);
    }
    System.out.println("Đã nạp " + auctions.size() + " phiên đấu giá từ file.");
  }

  public void addAuction(Auction auction) {
    auctions.put(auction.getId(), auction);
    locks.put(auction.getId(), new ReentrantLock());
  }

  // Tiện ích để ClientHandler lấy danh sách gửi về cho người dùng
  public List<Auction> getAllAuctions() {
    return new ArrayList<>(auctions.values());
  }

  public Auction getAuction(int id) {
    return auctions.get(id);
  }

  // ================== CORE LOGIC (SỬA ĐỂ LƯU FILE) ==================

  /**
   * Đặt giá an toàn (thread-safe) và cập nhật xuống file ngay lập tức
   */
  public boolean placeBid(int auctionId, String bidderName, BigDecimal amount)
      throws AuctionClosedException, InvalidBidException {

    Auction auction = auctions.get(auctionId);
    if (auction == null) throw new InvalidBidException("Không tìm thấy phiên đấu giá!");

    ReentrantLock lock = locks.get(auctionId);
    lock.lock();
    try {
      // Gọi logic placeBid trong class Auction (đã có check status và giá)
      // Chuyển BigDecimal sang double để khớp với phương thức cũ của bạn
      boolean success = auction.placeBid(bidderName, amount.doubleValue());

      if (success) {
        // 3. QUAN TRỌNG: Lưu ngay lập tức xuống file auctions.dat qua DAO
        auctionDAO.updateAuction(auction);
        System.out.println("[Server] " + bidderName + " bid thành công: " + amount + " cho ID: " + auctionId);
      }
      return success;

    } finally {
      lock.unlock();
    }
  }
}
