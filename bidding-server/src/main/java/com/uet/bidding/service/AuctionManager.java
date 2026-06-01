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

  // Xóa bỏ AtomicInteger auctionIdCounter vì Database sẽ tự lo việc sinh ID (AUTO_INCREMENT)
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

    for (Auction a : savedAuctions) {
      addAuctionInternal(a);
    }
    System.out.println("Đã nạp " + auctions.size() + " phiên đấu giá từ dữ liệu.");
  }

  // Hàm phụ dùng nội bộ để đăng ký auction và lock vào RAM
  private void addAuctionInternal(Auction auction) {
    auctions.put(auction.getId(), auction);
    locks.put(auction.getId(), new ReentrantLock());
  }

  // ================== QUẢN LÝ PHIÊN ĐẤU GIÁ ==================

  // Trong file AuctionManager.java
  public Auction createAuction(Item item, BigDecimal startPrice, LocalDateTime endTime, BigDecimal bidIncrement) throws UserException {
    LocalDateTime startTime = LocalDateTime.now();

    // 🎯 Gọi DAO với giá trị thực tế từ người dùng, KHÔNG dùng default 5.00 nữa
    Auction newAuction = auctionSqlDAO.createAuction(item, startPrice, startTime, endTime, bidIncrement);

    // Lưu vào cache RAM của Server
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
          // Dùng hàm chuyên dụng của DAO để kết thúc phiên, chia tiền, lưu kết quả
          auctionSqlDAO.finishAuction(auctionId);

          // Đồng bộ lại RAM sau khi DB đã xử lý xong
          Auction updatedAuction = auctionSqlDAO.findById(auctionId);
          auctions.put(auctionId, updatedAuction);
          System.out.println("Phiên #" + auctionId + " đã KẾT THÚC và xử lý giao dịch thành công.");
        } catch (UserException e) {
          System.err.println("Lỗi khi kết thúc phiên đấu giá: " + e.getMessage());
        }
      } else {
        // Nếu chỉ là đổi trạng thái thông thường (OPEN/CANCELED) trên RAM
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

    // 1. Kiểm tra tồn tại và trạng thái sơ bộ trên RAM (Fast-fail)
    Auction auction = auctions.get(auctionId);
    if (auction == null) {
      throw new InvalidBidException("Không tìm thấy phiên đấu giá trên hệ thống!");
    }


    // Chấp nhận cả việc kiểm tra RUNNING để chặt chẽ hơn
    if (!"RUNNING".equals(auction.getStatus())) {
      throw new AuctionClosedException("Phiên đấu giá không ở trạng thái sẵn sàng (Đã đóng hoặc chưa mở)!");
    }
    // 🎯 2. BỔ SUNG: Kiểm tra người dùng đã đăng ký tham gia chưa (Chặn đứng từ vòng gửi xe)
    // Lưu ý: customer.getId() lấy ra ID của người dùng đang thực hiện đặt giá
    boolean isRegistered = auctionSqlDAO.isUserRegistered(auctionId, customer.getId());
    if (!isRegistered) {
      throw new InvalidBidException("Bạn chưa đăng ký tham gia phiên đấu giá này! Vui lòng ấn nút đăng ký trước.");
    }

    // 2. Lấy hoặc tạo Lock an toàn
    ReentrantLock lock = locks.computeIfAbsent(auctionId, k -> new ReentrantLock());

    // 3. Bắt đầu khóa luồng
    lock.lock();
    try {
      // Kiểm tra lại trạng thái một lần nữa sau khi đã có lock để đảm bảo tính nhất quán (Double-check)
      // (Optional nhưng nên có nếu hệ thống yêu cầu độ chính xác tuyệt đối)

      boolean success = auctionSqlDAO.placeBid(auctionId, customer, amount);

      if (success) {
        Auction updatedAuction = auctionSqlDAO.findById(auctionId);
        if (updatedAuction != null) {
          auctions.put(auctionId, updatedAuction);
        }
        System.out.println("[Server] " + customer.getUsername() + " đặt giá thành công: " + amount);
      }
      return success;
    } finally {
      lock.unlock();
    }
  }

  // ================== GETTERS ==================

  public List<Auction> getAllAuctions() {
    return new ArrayList<>(auctions.values());
  }

  public Auction getAuction(int id) {
    return auctions.get(id);
  }

  /** Reload auction from DB into in-memory cache (e.g. after seller ends early). */
  public void refreshAuctionFromDb(int auctionId) throws UserException {
    if (auctionSqlDAO == null) return;
    Auction updated = auctionSqlDAO.findById(auctionId);
    auctions.put(auctionId, updated);
    locks.computeIfAbsent(auctionId, k -> new ReentrantLock());
  }
}