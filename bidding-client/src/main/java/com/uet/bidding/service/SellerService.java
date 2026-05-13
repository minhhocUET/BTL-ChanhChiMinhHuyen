package com.uet.bidding.service;

import com.uet.bidding.model.*;
import com.uet.bidding.network.ClientService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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
  public void addReviewToSeller(Customer rater, Seller seller, int stars, String comment) {
    // 1. Logic tính toán của bạn (Giữ nguyên)
    Review newReview = new Review(rater, stars, comment);
    seller.getReviews().add(newReview);

    double totalStars = 0;
    for (Review r : seller.getReviews()) {
      totalStars += r.getStars();
    }
    double newAverage = totalStars / seller.getReviews().size();
    seller.setSellerRating(newAverage);

    // 2. Gửi kết quả cuối cùng lên Server để lưu vào Database vĩnh viễn
    // Gửi Object chứa: Tên cửa hàng, số sao mới, và comment
    clientService.sendRequest("UPDATE_SELLER_RATING", new Object[]{
        seller.getStoreName(),
        newAverage,
        comment
    });

    System.out.println("Đã cập nhật & đồng bộ Rating cho " + seller.getStoreName());
  }
}