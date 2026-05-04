package com.uet.bidding.model;

import java.math.BigDecimal;

/**
 * Electronics Item (điện tử)
 */
public class Electronics extends Item {

  // Constructor khi đọc từ DB
  public Electronics(int id, String name, String description, BigDecimal startingPrice, int sellerId) {
    super(id, name, description, startingPrice, sellerId);
  }

  // Constructor khi tạo mới
  public Electronics(String name, String description, BigDecimal startingPrice, int sellerId) {
    super(name, description, startingPrice, sellerId);
  }

  @Override
  public String getType() {
    return "ELECTRONICS";
  }
}