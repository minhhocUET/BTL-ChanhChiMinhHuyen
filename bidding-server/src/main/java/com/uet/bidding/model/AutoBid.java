package com.uet.bidding.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AutoBid {
  private int id;
  private int auctionId;
  private int bidderId;
  private BigDecimal maxBid;
  private boolean isActive;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public AutoBid() {
  }

  public AutoBid(int auctionId, int bidderId, BigDecimal maxBid, boolean isActive) {
    this.auctionId = auctionId;
    this.bidderId = bidderId;
    this.maxBid = maxBid;
    this.isActive = isActive;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

  // Getters & Setters
  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public int getAuctionId() {
    return auctionId;
  }

  public void setAuctionId(int auctionId) {
    this.auctionId = auctionId;
  }

  public int getBidderId() {
    return bidderId;
  }

  public void setBidderId(int bidderId) {
    this.bidderId = bidderId;
  }

  public BigDecimal getMaxBid() {
    return maxBid;
  }

  public void setMaxBid(BigDecimal maxBid) {
    this.maxBid = maxBid;
  }

  public boolean isActive() {
    return isActive;
  }

  public void setActive(boolean active) {
    isActive = active;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}