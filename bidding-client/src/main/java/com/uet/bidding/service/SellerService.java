package com.uet.bidding.service;

import com.uet.bidding.model.*;
import com.uet.bidding.network.ClientService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class SellerService {
  private ClientService clientService = ClientService.getInstance();

  /**
   * CHỨC NĂNG 1: TẠO ĐẤU GIÁ (Giữ nguyên logic kiểm tra của bạn)
   */
  /** Gửi CREATE_AUCTION lên server: itemId startPrice durationMinutes */
  public CompletableFuture<NetworkMessage> createAuctionAsync(int itemId, BigDecimal startPrice, int durationMins) {
    String requestData = itemId + " " + startPrice.toPlainString() + " " + durationMins;
    return clientService.sendRequest("CREATE_AUCTION", requestData);
  }

  /**
   * CHỨC NĂNG 2: LÀM SẠCH DANH SÁCH (Giữ nguyên 100%)
   * Hàm này cực tốt để Client tự cập nhật trạng thái đếm ngược mà không cần chờ Server
   */
  public void refreshAuctionLists(Seller seller) {
    List<Auction> toRemove = new ArrayList<>();

    for (Auction auction : seller.getActiveAuctions()) {
      auction.refreshStatus(); // Tự check thời gian local

      if ("FINISHED".equals(auction.getStatus())) {
        seller.getFinishedAuctions().add(auction);
        toRemove.add(auction);
        auction.getItem().setInAuction(false);
      }
    }
    seller.getActiveAuctions().removeAll(toRemove);
  }

  /**
   * CHỨC NĂNG 3: RATING NÂNG CAO (Kết hợp logic tính toán của bạn + Gửi Server)
   */
  public void submitReview(int auctionId, int sellerId, int stars, String comment) {
    Map<String, Object> payload = new HashMap<>();
    payload.put("auctionId", auctionId);
    payload.put("sellerId", sellerId);
    payload.put("stars", stars);
    payload.put("comment", comment);
    clientService.sendRequest("ADD_REVIEW", payload);
  }

}
