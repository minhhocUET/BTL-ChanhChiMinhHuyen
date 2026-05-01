package com.uet.bidding.model;

public class AuctionItem {
    private String name;
    private double currentPrice;
    private String timeLeft;

    // Hàm khởi tạo (Constructor)
    public AuctionItem(String name, double currentPrice, String timeLeft) {
        this.name = name;
        this.currentPrice = currentPrice;
        this.timeLeft = timeLeft;
    }

    // Các hàm Getter (Bắt buộc phải có để TableView đọc được dữ liệu)
    public String getName() {
        return name;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public String getTimeLeft() {
        return timeLeft;
    }

    // Các hàm Setter (Nếu bạn muốn thay đổi dữ liệu sau này)
    public void setName(String name) { this.name = name; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }
    public void setTimeLeft(String timeLeft) { this.timeLeft = timeLeft; }
}