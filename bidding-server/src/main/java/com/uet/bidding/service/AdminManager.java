package com.uet.bidding.service;

import com.uet.bidding.dao.AuctionSqlDAO; // Hoặc UserSqlDAO tùy bạn đặt tên
import com.uet.bidding.model.Admin;
import com.uet.bidding.model.Auction;

import java.util.List;

public class AdminManager {
  private static AdminManager instance;
  private AuctionSqlDAO auctionDao; // Giả sử dùng DAO của bạn

  private AdminManager() {
  }

  public static synchronized AdminManager getInstance() {
    if (instance == null) {
      instance = new AdminManager();
    }
    return instance;
  }

  public void initialize(AuctionSqlDAO auctionDao) {
    this.auctionDao = auctionDao;
  }

  /**
   * Hàm trung gian gọi logic từ Admin Model và hạ lệnh cho DAO lưu xuống File/DB
   */
  public boolean executeRemoveAuction(Admin admin, int auctionId, List<Auction> currentAuctions) {
    // 1. Chạy logic kiểm tra nghiệp vụ ở lớp Model
    boolean isRemovedFromRam = admin.removeInvalidAuction(auctionId, currentAuctions);

    // 2. Nếu Model đồng ý cho xóa, hạ lệnh cho DAO đồng bộ xuống file cứng (JSON/SQL)
    if (isRemovedFromRam && auctionDao != null) {
      try {
        // Giả sử DAO của bạn có hàm delete
        auctionDao.deleteAuction(auctionId);
        System.out.println("[DB Sync] Đã đồng bộ xóa phiên đấu giá " + auctionId + " xuống cơ sở dữ liệu.");
        return true;
      } catch (Exception e) {
        System.err.println("❌ Lỗi đồng bộ DB: " + e.getMessage());
        return false;
      }
    }
    return false;
  }
}