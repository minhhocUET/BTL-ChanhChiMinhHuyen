package com.uet.bidding.model;

import java.time.LocalDateTime;

public class Review {
    private Customer customer; // Tên người đánh giá
    private int stars;         // 1-5 sao
    private String comment;    // Nội dung bình luận
    private LocalDateTime createdAt;

    public Review(Customer customer, int stars, String comment) {
        this.customer = customer;
        this.stars = stars;
        this.comment = comment;
        this.createdAt = LocalDateTime.now();
    }

    // Getters
    public int getStars() { return stars; }
    public String getComment() { return comment; }
    public String getBidderName() { return this.customer.getFullName(); }
}