package com.uet.bidding.model;

import java.math.BigDecimal;

/**
 * Người tham gia đấu giá
 */
public class Bidder extends User {

  public Bidder(int id, String username, String password, BigDecimal balance) {
    super(id, username, password, balance);
  }

  /**
   * Logic đặt giá
   */
  public boolean placeBid(Auction auction, BigDecimal amount) {
    // Kiểm tra giá hợp lệ
    if (amount.compareTo(auction.getCurrentPrice()) <= 0) {
      return false;
    }

    // Kiểm tra đủ tiền
    if (!withdraw(amount)) {
      return false;
    }

    // Cập nhật giá mới
    auction.setCurrentPrice(amount);
    return true;
  }

  @Override
  public String getRole() {
    return "BIDDER";
  }
}