package com.uet.bidding.model;

import java.time.LocalDateTime;

public class Review {
  private int id;
  private int auctionId;
  private int sellerId;
  private Customer reviewer;  // Người đánh giá (Customer)
  private int stars;
  private String comment;
  private LocalDateTime createdAt;

  public Review() {}

  public Review(int auctionId, int sellerId, Customer reviewer, int stars, String comment) {
    this.auctionId = auctionId;
    this.sellerId = sellerId;
    this.reviewer = reviewer;
    this.stars = stars;
    this.comment = comment;
    this.createdAt = LocalDateTime.now();
  }

  // Getters & Setters
  public int getId() { return id; }
  public void setId(int id) { this.id = id; }

  public int getAuctionId() { return auctionId; }
  public void setAuctionId(int auctionId) { this.auctionId = auctionId; }

  public int getSellerId() { return sellerId; }
  public void setSellerId(int sellerId) { this.sellerId = sellerId; }

  public Customer getReviewer() { return reviewer; }
  public void setReviewer(Customer reviewer) { this.reviewer = reviewer; }

  public int getStars() { return stars; }
  public void setStars(int stars) { this.stars = stars; }

  public String getComment() { return comment; }
  public void setComment(String comment) { this.comment = comment; }

  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

  public String getReviewerName() {
    return reviewer != null ? reviewer.getFullName() : "Unknown";
  }
}