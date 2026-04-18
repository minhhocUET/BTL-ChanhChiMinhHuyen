package com.uet.bidding.model;

import java.time.LocalDateTime;

public class Bid {
    private String bidderName;
    private double amount;
    private LocalDateTime time;

    public Bid(String bidderName, double amount) {
        this.bidderName = bidderName;
        this.amount = amount;
        this.time = LocalDateTime.now();
    }
    // Getter/Setter...
    public double getAmount() { return amount; }
    public String getBidderName() { return bidderName; }
}