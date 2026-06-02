package com.uet.bidding.model;

import com.uet.bidding.exception.AuctionClosedException;
import com.uet.bidding.exception.InvalidBidException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Auction {

  // Các biến phục vụ dữ liệu
  private int id;
  private Item item; // Đổi int itemId thành Object Item để lấy được toàn bộ thông tin sản phẩm
  private BigDecimal currentPrice;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private String status;
  private int registeredCount;
  // Thêm các biến
  private BigDecimal bidIncrement;
  private int antiSnipeWindowMinutes;
  private int antiSnipeExtensionMinutes;

  // Các biến phục vụ Logic & Observer Pattern
  private Customer highestBidder; // Thay String leadBidder bằng Object Bidder
  private List<Bid> bidHistory = new ArrayList<>();
  private transient List<AuctionObserver> observers = new ArrayList<>();

  public Auction() {
  }

  /**
   * CONSTRUCTOR 1: Dùng khi nạp dữ liệu từ Database hoặc File (Cần ID)
   */
  public Auction(int id, Item item, BigDecimal currentPrice,
                 LocalDateTime startTime, LocalDateTime endTime, String status) {
    this.id = id;
    this.item = item;
    this.currentPrice = currentPrice;
    this.startTime = startTime;
    this.endTime = endTime;
    this.status = status;
    this.bidHistory = new ArrayList<>();
    this.observers = new ArrayList<>();
  }

  /**
   * CONSTRUCTOR 2: Dùng cho chức năng "Tạo phiên đấu giá" (Từ phía Seller)
   * Thường dùng thời gian bắt đầu là ngay bây giờ.
   */
  public Auction(Item item, BigDecimal startPrice, int durationMinutes) {
    this.item = item;
    this.currentPrice = startPrice;
    this.startTime = LocalDateTime.now(); // Bắt đầu ngay lập tức
    this.endTime = this.startTime.plusMinutes(durationMinutes); // Tự tính thời gian kết thúc
    this.status = "OPEN";
    this.bidHistory = new ArrayList<>();
    this.observers = new ArrayList<>();
  }

  /**
   * CONSTRUCTOR 3: Đầy đủ tham số (Dành cho các trường hợp đặc biệt)
   */
  public Auction(Item item, BigDecimal startPrice, LocalDateTime startTime, LocalDateTime endTime) {
    this.item = item;
    this.currentPrice = startPrice;
    this.startTime = startTime;
    this.endTime = endTime;
    this.status = "OPEN";
    this.bidHistory = new ArrayList<>();
    this.observers = new ArrayList<>();
  }

  /**
   * Cần thiết cho Serialization vì transient sẽ làm observers bị null
   * khi bạn nạp dữ liệu từ file .ser/.dat lên.
   */
  private Object readResolve() {
    if (this.observers == null) this.observers = new ArrayList<>();
    return this;
  }

  public void refreshStatus() {
    LocalDateTime now = LocalDateTime.now();

    // Nếu đang OPEN nhưng đã quá giờ kết thúc
    if (("OPEN".equals(this.status) || "RUNNING".equals(this.status)) && now.isAfter(endTime)) {
      this.status = "FINISHED";
    }
  }

  // ================== GETTER & SETTER ==================
  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public Item getItem() {
    return item;
  }

  public void setItem(Item item) {
    this.item = item;
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

  public int getRegisteredCount() {
    return registeredCount;
  }

  public void setRegisteredCount(int registeredCount) {
    this.registeredCount = registeredCount;
  }

  public Customer getHighestBidder() {
    return highestBidder;
  }

  public void setHighestBidder(Customer highestBidder) {
    this.highestBidder = highestBidder;
  }

  public LocalDateTime getStartTime() {
    return startTime;
  }

  public LocalDateTime getEndTime() {
    return endTime;
  }

  public void setEndTime(LocalDateTime endTime) {
    this.endTime = endTime;
  }

  public BigDecimal getBidIncrement() {
    return bidIncrement;
  }

  public void setBidIncrement(BigDecimal bidIncrement) {
    this.bidIncrement = bidIncrement;
  }

  public int getAntiSnipeWindowMinutes() {
    return antiSnipeWindowMinutes;
  }

  public void setAntiSnipeWindowMinutes(int antiSnipeWindowMinutes) {
    this.antiSnipeWindowMinutes = antiSnipeWindowMinutes;
  }

  public int getAntiSnipeExtensionMinutes() {
    return antiSnipeExtensionMinutes;
  }

  public void setAntiSnipeExtensionMinutes(int antiSnipeExtensionMinutes) {
    this.antiSnipeExtensionMinutes = antiSnipeExtensionMinutes;
  }

  // ================== LOGIC ==================
  public boolean isActive() {
    return status.equals("OPEN") || status.equals("RUNNING");
  }

  // --- XỬ LÝ ĐA LUỒNG & NGOẠI LỆ ---

  public synchronized boolean placeNewBid(Customer customer, BigDecimal bidAmount)
      throws AuctionClosedException, InvalidBidException {

    // 1. Kiểm tra trạng thái phiên đấu giá
    if (!"RUNNING".equals(this.status)) {
      throw new AuctionClosedException("Phiên đấu giá chưa bắt đầu hoặc đã kết thúc!");
    }

    // 2. Kiểm tra số tiền đặt giá
    if (bidAmount.compareTo(this.currentPrice) <= 0) {
      throw new InvalidBidException("Giá đặt (" + bidAmount + ") phải cao hơn giá hiện tại (" + currentPrice + ")!");
    }

    // 3. Cập nhật thông tin người dẫn đầu
    this.currentPrice = bidAmount;
    this.highestBidder = customer;

    // 4. Lưu vào lịch sử Bid (Sử dụng hồ sơ Bidder từ Customer)
    // Giả sử Constructor của Bid nhận (Bidder bidder, BigDecimal amount, LocalDateTime time)
    this.bidHistory.add(new Bid(customer.getBidderProfile(), bidAmount, LocalDateTime.now()));

    System.out.println("✅ " + customer.getUsername() + " đặt giá thành công: " + bidAmount);

    notifyObservers();
    return true;
  }

  private void notifyObservers() {
    if (observers == null) return;

    for (AuctionObserver observer : observers) {
      // Lấy username từ đối tượng Customer đang dẫn đầu
      String bidderName = (this.highestBidder != null) ? this.highestBidder.getUsername() : "Chưa có";

      // Gửi thông báo đến Observer
      observer.updatePrice(
          "Sản phẩm: " + this.item.getName(),
          this.currentPrice.doubleValue(),
          bidderName
      );
    }
  }
}