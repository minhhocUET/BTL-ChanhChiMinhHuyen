package com.uet.bidding.model;

public class AuctionManager {
    // 1. Tạo một biến static private chứa thể hiện duy nhất của class
    private static AuctionManager instance;

    // 2. Chặn không cho tạo object bừa bãi bằng cách để Constructor là private
    private AuctionManager() {
        System.out.println("Hệ thống quản lý đấu giá đã được khởi động!");
    }

    // 3. Cung cấp một cổng duy nhất để lấy ông quản lý này ra dùng
    public static AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
    }

    // Các hàm nghiệp vụ sau này sẽ viết ở đây
    public void startAuction(int auctionId) {
        // Logic mở phiên đấu giá
    }
}