package com.uet.bidding.model;

import java.time.LocalDateTime;

public class AuctionRegistration {
  private int auctionId;
  private int bidderId;
  private LocalDateTime registeredAt;

  public AuctionRegistration() {}

  public AuctionRegistration(int auctionId, int bidderId) {
    this.auctionId = auctionId;
    this.bidderId = bidderId;
    this.registeredAt = LocalDateTime.now();
  }

  // Getters & Setters
  public int getAuctionId() { return auctionId; }
  public void setAuctionId(int auctionId) { this.auctionId = auctionId; }

  public int getBidderId() { return bidderId; }
  public void setBidderId(int bidderId) { this.bidderId = bidderId; }

  public LocalDateTime getRegisteredAt() { return registeredAt; }
  public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }
}