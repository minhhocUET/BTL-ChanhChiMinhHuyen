package com.uet.bidding.model;

import java.math.BigDecimal;

public class Seller extends User {

  private static final long serialVersionUID = 1L;

  public Seller(int id, String username, String password, BigDecimal balance) {
    super(id, username, password, balance);
  }

  @Override
  public String getRole() {
    return "SELLER";
  }

  // Hành động đặc thù của người bán
  public void createItem(String itemName, String description) {
    System.out.println(getUsername() + " vừa tạo món đồ mới: " + itemName);
  }
}