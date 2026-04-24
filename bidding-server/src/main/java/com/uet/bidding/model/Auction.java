package com.uet.bidding.model;

import com.uet.bidding.exception.AuctionNotRunningException;
import com.uet.bidding.exception.InvalidBidException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Auction {
    // Các biến phục vụ Database
    private int id;
    private int itemId;
    private BigDecimal currentPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;

    // Các biến phục vụ Logic & Observer Pattern (Đã được khôi phục)
    private String leadBidder;
    private List<Bid> bidHistory = new ArrayList<>();
    private transient List<AuctionObserver> observers = new ArrayList<>();

    // Constructor dùng khi đọc DB
    public Auction(int id, int itemId, BigDecimal currentPrice,
                   LocalDateTime startTime, LocalDateTime endTime, String status) {
        this.id = id;
        this.itemId = itemId;
        this.currentPrice = currentPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
    }

    // Constructor khi tạo mới
    public Auction(int itemId, BigDecimal startPrice,
                   LocalDateTime startTime, LocalDateTime endTime) {
        this.itemId = itemId;
        this.currentPrice = startPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = "OPEN";
    }

    // ================== GETTER & SETTER ==================
    public int getId() { return id; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public String getStatus() { return status; }
    public String getLeadBidder() { return leadBidder; }

    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }
    public void setStatus(String status) { this.status = status; }

    // ================== LOGIC ==================
    public boolean isActive() {
        return status.equals("OPEN") || status.equals("RUNNING");
    }

    // --- XỬ LÝ ĐA LUỒNG & NGOẠI LỆ ---
    public synchronized boolean placeBid(String bidderName, double bidAmount)
            throws AuctionNotRunningException, InvalidBidException {

        // Chuyển double sang BigDecimal để so sánh chuẩn xác với Database
        BigDecimal offer = BigDecimal.valueOf(bidAmount);

        // 1. Kiểm tra trạng thái (Dùng String status)
        if (!this.status.equals("RUNNING")) {
            throw new AuctionNotRunningException("Phiên đấu giá chưa bắt đầu hoặc đã kết thúc!");
        }

        // 2. Kiểm tra giá (Dùng compareTo: trả về <= 0 nghĩa là nhỏ hơn hoặc bằng)
        if (offer.compareTo(this.currentPrice) <= 0) {
            throw new InvalidBidException("Giá đặt (" + offer + ") phải cao hơn giá hiện tại (" + currentPrice + ")!");
        }

        // 3. Nếu qua được 2 ải trên thì cập nhật giá thành công
        this.currentPrice = offer;
        this.leadBidder = bidderName;
        this.bidHistory.add(new Bid(bidderName, bidAmount));

        System.out.println("✅ " + bidderName + " đặt giá thành công: " + offer);

        notifyObservers();
        return true;
    }

    // --- CÁC HÀM CỦA OBSERVER PATTERN ---
    public void addObserver(AuctionObserver observer) {
        if (!observers.contains(observer)) observers.add(observer);
    }

    public void removeObserver(AuctionObserver observer) {
        observers.remove(observer);
    }

    private void notifyObservers() {
        for (AuctionObserver observer : observers) {
            // Ép ngược BigDecimal về double để tương thích với Interface Observer cũ
            observer.updatePrice("Sản phẩm ID: " + this.itemId, this.currentPrice.doubleValue(), this.leadBidder);
        }
    }
}