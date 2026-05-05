package com.uet.bidding.model;

import com.uet.bidding.exception.AuctionClosedException;
import com.uet.bidding.exception.InvalidBidException;

import java.math.BigDecimal;

/**
 * Người tham gia đấu giá
 */
public class Bidder extends User {

  private static final long serialVersionUID = 1L;

  public Bidder(int id, String username, String password, BigDecimal balance) {
    super(id, username, password, balance);
  }

  /**
   * Logic đặt giá
   */
  public boolean placeBid(Auction auction, BigDecimal amount) throws InvalidBidException, AuctionClosedException {
    // 1. Giả sử class User của bạn có hàm withdraw trả về boolean
    // Kiểm tra xem có rút đủ tiền không
    if (!withdraw(amount)) {
      return false;
    }

    // 2. Giao lại việc kiểm tra giá, thời gian, lưu lịch sử cho Auction xử lý
    boolean isSuccess = auction.placeNewBid(this, amount);

    // 3. Nếu đặt giá thất bại (do có người đặt cao hơn cùng lúc hoặc hết giờ), phải hoàn tiền lại
    if (!isSuccess) {
      addFunds(amount); // Giả sử class User có hàm để cộng lại tiền
    }

    return isSuccess;
  }

  public String getRole() {
    return "BIDDER";
  }
}