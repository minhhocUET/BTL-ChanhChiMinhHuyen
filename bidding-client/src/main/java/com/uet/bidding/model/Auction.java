package com.uet.bidding.model;

import java.time.LocalDateTime;

public class Auction {
  private int auctionId;
  private Item item;                  // Sản phẩm được mang ra đấu giá (Art hoặc Electronics đều được)
  private double currentHighestBid;   // Giá cao nhất hiện tại
  private Bidder highestBidder;       // Người đang trả giá cao nhất (Class Bidder của bạn)
  private LocalDateTime endTime;      // Thời gian kết thúc phiên đấu giá

  // Constructor
  public Auction(int auctionId, Item item, LocalDateTime endTime) {
    this.auctionId = auctionId;
    this.item = item;
    this.endTime = endTime;
    this.currentHighestBid = item.getStartingPrice(); // Ban đầu giá cao nhất chính là giá khởi điểm
    this.highestBidder = null; // Chưa có ai đấu giá
  }

  // Các hàm Getters và Setters
  public int getAuctionId() { return auctionId; }

  public Item getItem() { return item; }

  public double getCurrentHighestBid() { return currentHighestBid; }
  public void setCurrentHighestBid(double currentHighestBid) { this.currentHighestBid = currentHighestBid; }

  public Bidder getHighestBidder() { return highestBidder; }
  public void setHighestBidder(Bidder highestBidder) { this.highestBidder = highestBidder; }

  public LocalDateTime getEndTime() { return endTime; }
  public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

  // Thêm hàm logic phụ: Trả giá mới
  public boolean placeNewBid(Bidder bidder, double bidAmount) {
    if (bidAmount > currentHighestBid && LocalDateTime.now().isBefore(endTime)) {
      this.currentHighestBid = bidAmount;
      this.highestBidder = bidder;
      return true; // Trả giá thành công
    }
    return false; // Trả giá thất bại (giá thấp hơn hiện tại hoặc đã hết giờ)
  }
}