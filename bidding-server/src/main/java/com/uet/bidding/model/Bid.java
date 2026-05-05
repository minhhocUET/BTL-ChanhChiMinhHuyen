package com.uet.bidding.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Bid implements Serializable {

  private static final long serialVersionUID = 1L;

  private String bidderName;
  private BigDecimal amount;
  private LocalDateTime time;

  public Bid(String bidderName, BigDecimal amount) {
    this.bidderName = bidderName;
    this.amount = amount;
    this.time = LocalDateTime.now();
  }

  // Getter/Setter...
  public BigDecimal getAmount() {
    return amount;
  }

  public String getBidderName() {
    return bidderName;
  }
}