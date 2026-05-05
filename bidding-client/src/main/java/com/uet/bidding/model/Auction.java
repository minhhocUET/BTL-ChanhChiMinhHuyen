package com.uet.bidding.model;

import com.uet.bidding.exception.AuctionClosedException;
import com.uet.bidding.exception.InvalidBidException;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Auction implements Serializable {

  // Thêm serialVersionUID để bảo vệ dữ liệu file
  private static final long serialVersionUID = 1L;

  // Các biến phục vụ dữ liệu
  private int id;
  private Item item; // Đổi int itemId thành Object Item để lấy được toàn bộ thông tin sản phẩm
  private BigDecimal currentPrice;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private String status;

  // Các biến phục vụ Logic & Observer Pattern
  private Bidder highestBidder; // Thay String leadBidder bằng Object Bidder
  private List<Bid> bidHistory = new ArrayList<>();
  private transient List<AuctionObserver> observers = new ArrayList<>();

  // Constructor dùng khi đọc dữ liệu (từ DB hoặc File)
  public Auction(int id, Item item, BigDecimal currentPrice,
                 LocalDateTime startTime, LocalDateTime endTime, String status) {
    this.id = id;
    this.item = item;
    this.currentPrice = currentPrice;
    this.startTime = startTime;
    this.endTime = endTime;
    this.status = status;
    this.observers = new ArrayList<>(); // khởi tạo lại để tránh null
  }

  // Constructor khi tạo mới
  public Auction(Item item, BigDecimal startPrice,
                 LocalDateTime startTime, LocalDateTime endTime) {
    this.item = item;
    this.currentPrice = startPrice;
    this.startTime = startTime;
    this.endTime = endTime;
    this.status = "OPEN";
    this.observers = new ArrayList<>(); // Khởi tạo lại để tránh null
  }

  /**
   * Cần thiết cho Serialization vì transient sẽ làm observers bị null
   * khi bạn nạp dữ liệu từ file .ser/.dat lên.
   */
  private Object readResolve() {
    if (this.observers == null) this.observers = new ArrayList<>();
    return this;
  }

  // ================== GETTER & SETTER ==================
  public int getId() { return id; }
  public void setId(int id) { this.id = id; }

  public Item getItem() { return item; }
  public void setItem(Item item) { this.item = item; }

  public BigDecimal getCurrentPrice() { return currentPrice; }
  public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }

  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }

  public Bidder getHighestBidder() { return highestBidder; }

  public LocalDateTime  getStartTime() { return startTime; }
  public LocalDateTime getEndTime() { return endTime; }

  // ================== LOGIC ==================
  public boolean isActive() {
    return status.equals("OPEN") || status.equals("RUNNING");
  }

  // --- XỬ LÝ ĐA LUỒNG & NGOẠI LỆ ---
  // Đã đổi tên hàm và tham số cho khớp với class Bidder
  public synchronized boolean placeNewBid(Bidder bidder, BigDecimal bidAmount)
      throws AuctionClosedException, InvalidBidException {

    // 1. Kiểm tra trạng thái
    if (!this.status.equals("RUNNING")) {
      throw new AuctionClosedException("Phiên đấu giá chưa bắt đầu hoặc đã kết thúc!");
    }

    // 2. Kiểm tra giá (Dùng compareTo: trả về <= 0 nghĩa là nhỏ hơn hoặc bằng)
    if (bidAmount.compareTo(this.currentPrice) <= 0) {
      throw new InvalidBidException("Giá đặt (" + bidAmount + ") phải cao hơn giá hiện tại (" + currentPrice + ")!");
    }

    // 3. Nếu qua được 2 ải trên thì cập nhật giá thành công
    this.currentPrice = bidAmount;
    this.highestBidder = bidder;

    // Cập nhật lại cách tạo mới Bid (truyền đủ 3 tham số)
    this.bidHistory.add(new Bid(bidder, bidAmount, LocalDateTime.now()));

    System.out.println("✅ " + bidder.getUsername() + " đặt giá thành công: " + bidAmount);

    notifyObservers();
    return true;
  }

  // --- CÁC HÀM CỦA OBSERVER PATTERN ---
  public void addObserver(AuctionObserver observer) {
    if (!observers.contains(observer)) observers.add(observer);
  }

  public void removeObserver(AuctionObserver observer) {
    observers.remove(observer);
  }

  private void notifyObservers() {
    for (AuctionObserver observer : observers) {
      // Lấy tên người trả giá cao nhất (nếu chưa có thì để "Chưa có")
      String bidderName = (this.highestBidder != null) ? this.highestBidder.getUsername() : "Chưa có";

      // Ép ngược BigDecimal về double để tương thích với Interface Observer cũ của bạn
      observer.updatePrice("Sản phẩm: " + this.item.getName(), this.currentPrice.doubleValue(), bidderName);
    }
  }
}