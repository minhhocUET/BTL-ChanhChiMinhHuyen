package com.uet.bidding.service;

import com.uet.bidding.dao.AuctionSqlDAO;
import com.uet.bidding.exception.AuctionClosedException;
import com.uet.bidding.exception.InvalidBidException;
import com.uet.bidding.model.Auction;
import com.uet.bidding.model.Customer;
import com.uet.bidding.model.Item;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class AuctionManager {

  private static volatile AuctionManager instance;
  private final ConcurrentHashMap<Integer, Auction> auctions = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<Integer, ReentrantLock> locks = new ConcurrentHashMap<>();
  private final AtomicInteger auctionIdCounter = new AtomicInteger(1);
  private AuctionSqlDAO auctionSqlDAO;

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

    int maxId = 0;
    for (Auction a : savedAuctions) {
      addAuctionInternal(a);
      if (a.getId() > maxId) maxId = a.getId();
    }

    auctionIdCounter.set(maxId + 1);
    System.out.println("Đã nạp " + auctions.size() + " phiên đấu giá từ dữ liệu.");
  }

  // Hàm phụ dùng nội bộ để đăng ký auction và lock
  private void addAuctionInternal(Auction auction) {
    auctions.put(auction.getId(), auction);
    locks.put(auction.getId(), new ReentrantLock());
  }

  // ================== QUẢN LÝ PHIÊN ĐẤU GIÁ ==================

  public Auction createAuction(Item item, LocalDateTime endTime) {
    int newId = auctionIdCounter.getAndIncrement();
    // Lấy giá khởi điểm từ Item (Giả sử Item dùng BigDecimal)
    BigDecimal startPrice = item.getStartingPrice();
    LocalDateTime startTime = LocalDateTime.now();

    Auction newAuction = new Auction(item, startPrice, startTime, endTime);
    newAuction.setId(newId);

    addAuctionInternal(newAuction);
    saveToDatabase(newAuction);

    return newAuction;
  }

  public void updateAuctionStatus(int auctionId, String status) {
    ReentrantLock lock = locks.get(auctionId);
    if (lock == null) return;

    lock.lock();
    try {
      Auction auction = auctions.get(auctionId);
      if (auction != null) {
        auction.setStatus(status);
        saveToDatabase(auction);
        System.out.println("Phiên #" + auctionId + " chuyển sang trạng thái: " + status);
      }
    } finally {
      lock.unlock();
    }
  }

  // ================== CORE LOGIC: ĐẶT GIÁ AN TOÀN ==================

  /**
   * Chỉnh sửa tham số nhận vào Customer để khớp với logic mới
   */
  public boolean placeBid(int auctionId, Customer customer, BigDecimal amount)
      throws AuctionClosedException, InvalidBidException {

    Auction auction = auctions.get(auctionId);
    if (auction == null) throw new InvalidBidException("Không tìm thấy phiên đấu giá!");

    ReentrantLock lock = locks.get(auctionId);
    if (lock == null) return false;

    lock.lock();
    try {
      // Gọi logic placeNewBid đã sửa (nhận Customer, trả về boolean)
      boolean success = auction.placeNewBid(customer, amount);

      if (success) {
        // Lưu vào Database ngay khi có người trả giá mới thành công
        saveToDatabase(auction);
        System.out.println("[Server] " + customer.getUsername() + " đặt giá thành công: " + amount);
      }
      return success;
    } finally {
      lock.unlock();
    }
  }

  // Hàm phụ để tránh lặp code lưu DB
  private void saveToDatabase(Auction auction) {
    if (auctionSqlDAO != null) {
      try {
        auctionSqlDAO.updateAuction(auction);
      } catch (Exception e) {
        System.err.println("❌ Lỗi Database: " + e.getMessage());
      }
    }
  }

  // Getter cơ bản
  public List<Auction> getAllAuctions() {
    return new ArrayList<>(auctions.values());
  }

  public Auction getAuction(int id) {
    return auctions.get(id);
  }
}