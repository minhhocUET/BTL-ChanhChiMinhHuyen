package com.uet.bidding.model;

import com.uet.bidding.exception.AuctionClosedException;
import com.uet.bidding.exception.InvalidBidException;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Auction implements Serializable {

  //Thêm serialVersionUID để bảo vệ dữ liệu file
  private static final long serialVersionUID = 1L;

  // Các biến phục vụ dữ liệu
  private int id;
  private int itemId;
  private BigDecimal currentPrice;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private String status;

  // Các biến phục vụ Logic & Observer Pattern (Đã được khôi phục)
  private String leadBidder;
  private List<Bid> bidHistory = new ArrayList<>();
  private transient List<AuctionObserver> observers = new ArrayList<>();

  // Constructor dùng khi đọc dữ liệu
  public Auction(int id, int itemId, BigDecimal currentPrice,
                 LocalDateTime startTime, LocalDateTime endTime, String status) {
    this.id = id;
    this.itemId = itemId;
    this.currentPrice = currentPrice;
    this.startTime = startTime;
    this.endTime = endTime;
    this.status = status;
    this.observers = new ArrayList<>(); //khởi tạo lại để tránh null
  }

  // Constructor khi tạo mới
  public Auction(int itemId, BigDecimal startPrice,
                 LocalDateTime startTime, LocalDateTime endTime) {
    this.itemId = itemId;
    this.currentPrice = startPrice;
    this.startTime = startTime;
    this.endTime = endTime;
    this.status = "OPEN";
    this.observers = new ArrayList<>(); // Khởi tạo lại để tránh null
  }

  /**
   * HÀM MỚI: Cần thiết cho Serialization vì transient sẽ làm observers bị null
   * khi bạn nạp dữ liệu từ file .ser/.dat lên.
   */
  private Object readResolve() {
    if (this.observers == null) this.observers = new ArrayList<>();
    return this;
  }

  // ================== GETTER & SETTER ==================
  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public BigDecimal getCurrentPrice() {
    return currentPrice;
  }

  public void setCurrentPrice(BigDecimal currentPrice) {
    this.currentPrice = currentPrice;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getLeadBidder() {
    return leadBidder;
  }

  // ================== LOGIC ==================
  public boolean isActive() {
    return status.equals("OPEN") || status.equals("RUNNING");
  }

  // --- XỬ LÝ ĐA LUỒNG & NGOẠI LỆ ---
  public synchronized boolean placeBid(String bidderName, BigDecimal bidAmount)
      throws AuctionClosedException, InvalidBidException {

    // 1. Kiểm tra trạng thái (Dùng String status)
    if (!this.status.equals("RUNNING")) {
      throw new AuctionClosedException("Phiên đấu giá chưa bắt đầu hoặc đã kết thúc!");
    }

    // 2. Kiểm tra giá (Dùng compareTo: trả về <= 0 nghĩa là nhỏ hơn hoặc bằng)
    if (bidAmount.compareTo(this.currentPrice) <= 0) {
      throw new InvalidBidException("Giá đặt (" + bidAmount + ") phải cao hơn giá hiện tại (" + currentPrice + ")!");
    }

    // 3. Nếu qua được 2 ải trên thì cập nhật giá thành công
    this.currentPrice = bidAmount;
    this.leadBidder = bidderName;
    this.bidHistory.add(new Bid(bidderName, bidAmount));

    System.out.println("✅ " + bidderName + " đặt giá thành công: " + bidAmount);

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
      // Ép ngược BigDecimal về double để tương thích với Interface Observer cũ
      observer.updatePrice("Sản phẩm ID: " + this.itemId, this.currentPrice.doubleValue(), this.leadBidder);
    }
  }
}