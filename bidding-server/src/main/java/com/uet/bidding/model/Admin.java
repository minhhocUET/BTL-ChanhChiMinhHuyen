package com.uet.bidding.model;

import java.math.BigDecimal;

public class Admin extends User {

  private int adminLevel;
  private String department;

  public Admin() {
    super();
  }

  public Admin(String username, String password, BigDecimal balance) {
    super(username, password, BigDecimal.ZERO);
  }

  public Admin(int id, String username, String password, BigDecimal balance) {
    super(id, username, password, BigDecimal.ZERO);
  }

  @Override
  public String getRole() {
    return "ADMIN";
  }

  public int getAdminLevel() {
    return adminLevel;
  }

  public void setAdminLevel(int adminLevel) {
    this.adminLevel = adminLevel;
  }

  public String getDepartment() {
    return department;
  }

  public void setDepartment(String department) {
    this.department = department;
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