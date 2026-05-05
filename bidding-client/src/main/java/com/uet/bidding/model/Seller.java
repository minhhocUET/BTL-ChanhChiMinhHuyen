package com.uet.bidding.model;

import java.math.BigDecimal;

public class Seller extends User {

  private static final long  serialVersionUID = 1L;

  public Seller(int id, String username, String password, BigDecimal balance) {
    // Thay vì gọi super(), ta dùng setter
    this.setId(id);
    this.setUsername(username);
    this.setPassword(password);
    this.setBalance(balance);
  }

  // Đã xóa @Override để sửa lỗi
  public String getRole() {
    return "SELLER";
  }

  // Hành động đặc thù của người bán (giữ nguyên code cũ của bạn)
  public void createItem(String itemName, String description) {
    System.out.println(getUsername() + " vừa tạo món đồ mới: " + itemName);
  }
}