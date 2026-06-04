package com.uet.bidding.model;

// Bất kỳ ai muốn theo dõi giá của phiên đấu giá đều phải tuân thủ luật (implement) interface này
public interface AuctionObserver {
  // Hàm này sẽ tự động được gọi mỗi khi có giá mới
  void updatePrice(String itemName, double newPrice, String topBidder);
}