package com.uet.bidding.model;

import java.math.BigDecimal;

public class Admin extends User {

  public Admin() {
    super();
  }

  public Admin(String username, String password, BigDecimal balance) {
    super(username, password, BigDecimal.ZERO);
  }

  public Admin(int id, String username, String password, BigDecimal balance) {
    super(id, username, password, BigDecimal.ZERO);
  }

  /**
   * Các phương thức đặc thù của Admin (để trống để xử lý logic sau)
   * Đây là nơi thể hiện sự khác biệt giữa Admin và Bidder/Seller
   */
  public void banUser(int userId) {
    // Logic khóa tài khoản người dùng
  }

  public void removeInvalidAuction(int auctionId) {
    // Logic xóa phiên đấu giá vi phạm
  }
}