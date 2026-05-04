package com.uet.bidding.model;

public class Auction {
    private int auctionId;
    private Item item;            // Món đồ được đem ra đấu giá
    private double startPrice;    // Giá khởi điểm
    private double currentPrice;  // Giá cao nhất hiện tại
    private String status;        // Trạng thái: OPEN, RUNNING, FINISHED

    public Auction(int auctionId, Item item, double startPrice) {
        this.auctionId = auctionId;
        this.item = item;
        this.startPrice = startPrice;
        this.currentPrice = startPrice; // Lúc mới tạo, giá hiện tại = giá khởi điểm
        this.status = "OPEN";
    }

    // Các hàm Getter / Setter cơ bản
    public int getAuctionId() {
        return auctionId;
    }

    public Item getItem() {
        return item;
    }

    public double getStartPrice() {
        return startPrice;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    // Hàm này rất quan trọng để cập nhật giá khi có người đặt cao hơn
    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}