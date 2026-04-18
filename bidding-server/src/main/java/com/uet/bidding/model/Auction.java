package com.uet.bidding.model;

import java.util.ArrayList;
import java.util.List;

public class Auction {
    private String itemID;
    private String itemName;
    private double currentPrice;
    private String leadBidder;
    private AuctionState state;
    private List<Bid> bidHistory = new ArrayList<>();

    // Danh sách những người đang theo dõi phiên đấu giá này
    private transient List<AuctionObserver> observers = new ArrayList<>();

    public Auction(String itemID, String itemName, double startingPrice) {
        this.itemID = itemID;
        this.itemName = itemName;
        this.currentPrice = startingPrice;
        this.state = AuctionState.OPEN;
    }

    // --- XỬ LÝ ĐA LUỒNG (CONCURRENCY) ---
    // Dùng synchronized để tránh 2 người cùng đặt giá 1 lúc gây sai lệch
    public synchronized boolean placeBid(String bidderName, double bidAmount) {
        // 1. Kiểm tra trạng thái: Chỉ cho đặt giá khi đang RUNNING
        if (this.state != AuctionState.RUNNING) {
            System.out.println("Lỗi: Phiên đấu giá chưa bắt đầu hoặc đã kết thúc!");
            return false;
        }

        // 2. Kiểm tra giá: Phải cao hơn giá hiện tại
        if (bidAmount > this.currentPrice) {
            this.currentPrice = bidAmount;
            this.leadBidder = bidderName;
            this.bidHistory.add(new Bid(bidderName, bidAmount));

            System.out.println("✅ Đặt giá thành công: " + bidderName + " đặt " + bidAmount);

            // 3. THỰC HIỆN OBSERVER: Thông báo cho mọi người
            notifyObservers();
            return true;
        } else {
            System.out.println("❌ Giá đặt phải cao hơn giá hiện tại (" + currentPrice + ")");
            return false;
        }
    }

    // Hàm chuyển trạng thái logic
    public void startAuction() { this.state = AuctionState.RUNNING; }
    public void finishAuction() { this.state = AuctionState.FINISHED; }

    // Getters...
    public double getCurrentPrice() { return currentPrice; }

    // --- CÁC HÀM CỦA OBSERVER PATTERN ---
    // 1. Cho phép một người đăng ký theo dõi
    public void addObserver(AuctionObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    // 2. Hủy theo dõi
    public void removeObserver(AuctionObserver observer) {
        observers.remove(observer);
    }

    // 3. Cập nhật giá mới cho tất cả những người đang theo dõi
    private void notifyObservers() {
        for (AuctionObserver observer : observers) {
            observer.updatePrice(this.itemName, this.currentPrice, this.leadBidder);
        }
    }
}