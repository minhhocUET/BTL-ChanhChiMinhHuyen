package com.uet.bidding.model;

import java.io.Serializable;import java.time.LocalDateTime;

public class Bid implements Serializable {

  private static final long serialVersionUID = 1L;

  private String bidderName;
  private double amount;
  private LocalDateTime time;

  public Bid(String bidderName, double amount) {
    this.bidderName = bidderName;
    this.amount = amount;
    this.time = LocalDateTime.now();
  }

  // Getter/Setter...
  public double getAmount() {
    return amount;
  }

  public String getBidderName() {
    return bidderName;
  }
}