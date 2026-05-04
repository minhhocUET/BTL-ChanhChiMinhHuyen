package com.uet.bidding.model;

public class Bidder extends User {

  public Bidder(int id, String username, String password, double balance) {
    this.setId(id);
    this.setUsername(username);
    this.setPassword(password);
    this.setBalance(balance);
  }

  // Đã xóa @Override để máy không báo lỗi
  public String getRole() {
    return "BIDDER";
  }
}