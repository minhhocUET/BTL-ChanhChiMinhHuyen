package com.uet.bidding.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Bid {

  private Bidder bidder; // Thay vì chỉ lưu String tên, ta lưu cả Object Bidder
  private BigDecimal amount; // Đổi từ double sang BigDecimal
  private LocalDateTime time;

  public Bid() {
  }

  // Cập nhật Constructor khớp với lời gọi bên Auction.java
  public Bid(Bidder bidder, BigDecimal amount, LocalDateTime time) {
    this.bidder = bidder;
    this.amount = amount;
    this.time = time;
  }

  // Các hàm Getters
  public BigDecimal getAmount() {
    return amount;
  }

  public Bidder getBidder() {
    return bidder;
  }

  public LocalDateTime getTime() {
    return time;
  }
}