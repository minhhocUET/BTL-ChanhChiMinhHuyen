package com.uet.bidding.service;

import com.uet.bidding.model.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class SellerService {
    /**
     * CHỨC NĂNG 1: TẠO ĐẤU GIÁ
     */
    public Auction createAndStartAuction(Seller seller, Item item, BigDecimal startPrice, int durationMins) {
        // 1. Kiểm tra Item thuộc kho của Seller
        if (!seller.getInventory().contains(item)) {
            System.out.println("Lỗi: Item không thuộc kho hàng của người bán này!");
            return null;
        }

        // 2. Kiểm tra trạng thái Item (Sử dụng hàm setInAuction bạn vừa tạo)
        if (item.isInAuction()) {
            System.out.println("Lỗi: Item đang trong một phiên đấu giá khác!");
            return null;
        }

        // 3. Khởi tạo Auction
        Auction newAuction = new Auction(item, startPrice, durationMins);
        newAuction.setStatus("RUNNING");

        // 4. Cập nhật dữ liệu: Khóa Item và thêm vào danh sách Active
        item.setInAuction(true);
        seller.getActiveAuctions().add(newAuction);

        return newAuction;
    }

    /**
     * CHỨC NĂNG 2: LÀM SẠCH DANH SÁCH (Dọn dẹp các phiên đã hết giờ)
     * Hàm này nên được gọi mỗi khi Seller vào trang quản lý đấu giá
     */
    public void refreshAuctionLists(Seller seller) {
        List<Auction> toRemove = new ArrayList<>();

        for (Auction auction : seller.getActiveAuctions()) {
            // Cập nhật trạng thái dựa trên thời gian thực
            auction.refreshStatus();

            if ("FINISHED".equals(auction.getStatus())) {
                seller.getFinishedAuctions().add(auction); //DANH SÁCH FINISHED (Đã cập nhật)
                toRemove.add(auction);

                // Giải phóng Item để Seller có thể đấu giá lại nếu muốn (tùy logic app)
                auction.getItem().setInAuction(false);
            }
        }

        // Xóa các phiên đã kết thúc khỏi danh sách Active
        seller.getActiveAuctions().removeAll(toRemove);
    }

    /**
     * CHỨC NĂNG 3: RATING NÂNG CAO (Có bình luận và tính toán trung bình)
     */
    public void addReviewToSeller(Customer rater, Seller seller, int stars, String comment) {
        // 1. Tạo đối tượng Review mới
        Review newReview = new Review(rater.getUsername(), stars, comment);
        // 2. Thêm vào danh sách của Seller
        List<Review> allReviews = seller.getReviews();
        allReviews.add(newReview);
        // 3. Tính toán lại Rating trung bình (Logic quan trọng)
        double totalStars = 0;
        for (Review r : allReviews) {
            totalStars += r.getStars();
        }
        double newAverage = totalStars / allReviews.size();
        // 4. Cập nhật lại vào Model Seller
        seller.setSellerRating(newAverage);
        System.out.println("Đã cập nhật đánh giá cho " + seller.getStoreName() + ": " + String.format("%.1f", newAverage) + "⭐");
    }
}
