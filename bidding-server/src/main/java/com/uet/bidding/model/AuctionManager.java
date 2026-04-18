package com.uet.bidding.model;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * AuctionManager
 * - Singleton
 * - Quản lý tất cả auction
 * - Xử lý concurrent bidding
 */
public class AuctionManager {

    // ================== SINGLETON ==================
    private static volatile AuctionManager instance;

    private AuctionManager() {
        System.out.println("Hệ thống quản lý đấu giá đã được khởi động!");
    }

    public static AuctionManager getInstance() {
        if (instance == null) {
            synchronized (AuctionManager.class) {
                if (instance == null) {
                    instance = new AuctionManager();
                }
            }
        }
        return instance;
    }

    // ================== DATA ==================

    // Lưu auction
    private ConcurrentHashMap<Integer, Auction> auctions = new ConcurrentHashMap<>();

    // Lock riêng cho từng auction
    private ConcurrentHashMap<Integer, ReentrantLock> locks = new ConcurrentHashMap<>();

    // ================== QUẢN LÝ ==================

    public void addAuction(Auction auction) {
        auctions.put(auction.getId(), auction);
        locks.put(auction.getId(), new ReentrantLock());
    }

    public Auction getAuction(int id) {
        return auctions.get(id);
    }

    public void startAuction(int auctionId) {
        Auction auction = auctions.get(auctionId);
        if (auction != null) {
            auction.setStatus("RUNNING");
        }
    }

    // ================== CORE LOGIC ==================

    /**
     * Đặt giá an toàn (thread-safe)
     */
    public boolean placeBid(int auctionId, Bidder bidder, BigDecimal amount) {
        Auction auction = auctions.get(auctionId);
        if (auction == null) return false;

        ReentrantLock lock = locks.get(auctionId);

        lock.lock(); // 🔥 khóa
        try {
            // 1. kiểm tra trạng thái
            if (!auction.isActive()) return false;

            // 2. kiểm tra giá
            if (amount.compareTo(auction.getCurrentPrice()) <= 0) return false;

            // 3. kiểm tra tiền
            if (!bidder.withdraw(amount)) return false;

            // 4. update
            auction.setCurrentPrice(amount);

            System.out.println(bidder.getUsername() + " bid: " + amount);

            return true;

        } finally {
            lock.unlock(); // 🔥 luôn unlock
        }
    }
}