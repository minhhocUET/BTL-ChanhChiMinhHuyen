package com.uet.bidding.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Auction {
    private int id;
    private int itemId;
    private BigDecimal currentPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;

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

    // ================== GETTER ==================
    public int getId() { return id; }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public String getStatus() {
        return status;
    }

    // ================== SETTER ==================
    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // ================== LOGIC ==================

    public boolean isActive() {
        return status.equals("OPEN") || status.equals("RUNNING");
    }
}