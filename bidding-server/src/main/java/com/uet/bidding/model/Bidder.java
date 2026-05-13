package com.uet.bidding.model;

import java.util.ArrayList;
import java.util.List;

public class Bidder {

  // 1. Danh sách các phiên đang đăng ký tham gia (Đang diễn ra)
  private List<Integer> registeredAuctionIds;

  // 2. Danh sách các phiên đã từng tham gia (Đã kết thúc)
  private List<Integer> auctionHistoryIds;

  public Bidder() {
    this.registeredAuctionIds = new ArrayList<>();
    this.auctionHistoryIds = new ArrayList<>();
  }

  // --- Getter và Setter ---
  public List<Integer> getRegisteredAuctionIds() {
    return registeredAuctionIds;
  }

  public void setRegisteredAuctionIds(List<Integer> registeredAuctionIds) {
    this.registeredAuctionIds = registeredAuctionIds;
  }

  public List<Integer> getAuctionHistoryIds() {
    return auctionHistoryIds;
  }

  public void setAuctionHistoryIds(List<Integer> auctionHistoryIds) {
    this.auctionHistoryIds = auctionHistoryIds;
  }

  // --- Các hàm hỗ trợ logic (Helper methods) ---

  // Khi người dùng nhấn nút "Đăng ký tham gia" một phiên mới
  public void registerForAuction(int auctionId) {
    if (!registeredAuctionIds.contains(auctionId)) {
      this.registeredAuctionIds.add(auctionId);
    }
  }

  // Khi một phiên đấu giá kết thúc, chuyển nó từ "Đang tham gia" sang "Lịch sử"
  public void completeAuction(int auctionId) {
    if (this.registeredAuctionIds.contains(auctionId)) {
      this.registeredAuctionIds.remove(Integer.valueOf(auctionId));
      if (!this.auctionHistoryIds.contains(auctionId)) {
        this.auctionHistoryIds.add(auctionId);
      }
    }
  }
}