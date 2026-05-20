package com.uet.bidding.model;

import java.util.ArrayList;
import java.util.List;

public class Seller {
  private String storeName;
  private String description;
  private double sellerRating;

  // 1. Danh sách chứa Item (Kho hàng của người bán)
  private List<Item> inventory;
  // 2. Danh sách các phiên đấu giá đang diễn ra
  private List<Auction> activeAuctions;
  // 3. Danh sách các phiên đấu giá đã kết thúc
  private List<Auction> finishedAuctions;
  // 4. Danh sách review từ khách hàng
  private List<Review> reviews;

  public Seller() {
    this.sellerRating = 0.0;
    this.inventory = new ArrayList<>();
    this.activeAuctions = new ArrayList<>();
    this.finishedAuctions = new ArrayList<>();
  }

  // --- Chức năng quản lý Item ---
  // Khả năng thêm Item vào kho
  public void addItem(Item item) {
    this.inventory.add(item);
  }

  public List<Item> getInventory() {
    return inventory;
  }

  // Getter/Setter cho storeName và rating
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getStoreName() {
    return storeName;
  }

  public void setStoreName(String storeName) {
    this.storeName = storeName;
  }

  public double getSellerRating() {
    return sellerRating;
  }

  public void setSellerRating(double sellerRating) {
    this.sellerRating = sellerRating;
  }

  public List<Auction> getActiveAuctions() {
    return activeAuctions;
  }

  public List<Auction> getFinishedAuctions() {
    return finishedAuctions;
  }

  public List<Review> getReviews() {
    return reviews;
  }
}
