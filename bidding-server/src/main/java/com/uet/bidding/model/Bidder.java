package com.uet.bidding.model;

import java.math.BigDecimal;

public class Bidder extends User {

  private static final long serialVersionUID = 1L;

  // Các thuộc tính riêng của người đi đấu giá (nếu cần)
  private int totalBids;    // Tổng số lần đã tham gia đặt giá
  private int auctionsWon;  // Số phiên đấu giá đã thắng

  // ================= CONSTRUCTORS =================

  // Constructor trống
  public Bidder() {
    super();
  }

  /**
   * Constructor dùng khi tạo mới Bidder (chưa có ID)
   * Thêm tham số email theo yêu cầu của bạn
   */
  public Bidder(String username, String password, BigDecimal balance, String email) {
    // Gọi constructor của User (chỉ nhận 3 tham số theo code bạn gửi)
    super(username, password, balance);

    // Vì User có hàm setEmail, ta gọi luôn để gán giá trị
    this.setEmail(email);

    // Mặc định ban đầu mới tạo thì số lần đấu giá là 0
    this.totalBids = 0;
    this.auctionsWon = 0;
  }

  /**
   * Constructor dùng khi đọc từ DB (đã có ID)
   */
  public Bidder(int id, String username, String password, BigDecimal balance, String email) {
    super(id, username, password, balance);
    this.setEmail(email);
  }

  // ================= OVERRIDE =================

  /**
   * Ghi đè hàm getRole() để phân biệt với Admin và Seller
   */
  @Override
  public String getRole() {
    return "BIDDER";
  }

  // ================= GETTERS AND SETTERS =================

  public int getTotalBids() { return totalBids; }
  public void setTotalBids(int totalBids) { this.totalBids = totalBids; }

  public int getAuctionsWon() { return auctionsWon; }
  public void setAuctionsWon(int auctionsWon) { this.auctionsWon = auctionsWon; }

  // ================= TO STRING =================

  @Override
  public String toString() {
    return "Bidder {" +
            "username = '" + getUsername() + '\'' +
            ", email = '" + getEmail() + '\'' +
            ", balance = " + getBalance() +
            ", totalBids = " + totalBids +
            '}';
  }
}