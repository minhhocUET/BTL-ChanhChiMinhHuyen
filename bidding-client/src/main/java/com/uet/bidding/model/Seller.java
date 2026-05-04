package com.uet.bidding.model;

public class Seller extends User {

  public Seller(int id, String username, String password, double balance) {
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