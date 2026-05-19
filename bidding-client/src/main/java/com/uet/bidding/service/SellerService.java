package com.uet.bidding.service;

import com.uet.bidding.model.*;
import com.uet.bidding.network.ClientService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SellerService {
  private ClientService clientService = ClientService.getInstance();

  /**
   * CHỨC NĂNG 1: TẠO ĐẤU GIÁ (Giữ nguyên logic kiểm tra của bạn)
   */
  public Auction createAndStartAuction(Seller seller, Item item, BigDecimal startPrice, int durationMins) {
    // --- GIỮ NGUYÊN LOGIC CŨ CỦA BẠN ---
    if (!seller.getInventory().contains(item)) {
      System.err.println("Lỗi: Item không thuộc kho hàng!");
      return null;
    }

    if (item.isInAuction()) {
      System.err.println("Lỗi: Item đang trong một phiên khác!");
      return null;
    }

    // --- THÊM PHẦN GIAO TIẾP MẠNG ---
    // Gửi lệnh lên Server để mọi người cùng thấy phiên này
    // Ta gửi: ID món hàng, giá khởi điểm, và thời gian
    String requestData = item.getId() + " " + startPrice + " " + durationMins;
    clientService.sendRequest("CREATE_AUCTION", requestData);

    // Logic local: Khởi tạo tạm trên RAM Client để hiển thị ngay lập tức (Responsive UI)
    Auction newAuction = new Auction(item, startPrice, durationMins);
    newAuction.setStatus("RUNNING");
    item.setInAuction(true);
    seller.getActiveAuctions().add(newAuction);

    return newAuction;
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
