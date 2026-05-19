package com.uet.bidding.util;

public class ReviewContext {
    public static int auctionId;
    public static int sellerId;
    public static String storeName;
    public static boolean showAddForm; // true = màn đánh giá mới, false = chỉ xem

    public static void set(int auctionId, int sellerId, String storeName, boolean showAddForm) {
        ReviewContext.auctionId = auctionId;
        ReviewContext.sellerId = sellerId;
        ReviewContext.storeName = storeName;
        ReviewContext.showAddForm = showAddForm;
    }
}