package com.uet.bidding.model;

import java.math.BigDecimal;

class Art extends Item {

  // Constructor dùng khi đọc DB
  public Art(int id, String name, String description, BigDecimal startingPrice, int sellerId) {
    super(id, name, description, startingPrice, sellerId);
  }

  // Constructor dùng khi tạo mới
  public Art(String name, String description, BigDecimal startingPrice, int sellerId) {
    super(name, description, startingPrice, sellerId);
  }

  @Override
  public String getType() {
    return "ART";
  }
}