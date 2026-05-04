package com.uet.bidding.model;

import java.math.BigDecimal;

/**
 * Factory pattern: tạo Item dựa theo type
 */
public class ItemFactory {

  public static Item createItem(String type,
                                int id,
                                String name,
                                String description,
                                BigDecimal startingPrice,
                                int sellerId) {
    if (type.equalsIgnoreCase("ART")) {
      return new Art(id, name, description, startingPrice, sellerId);
    }
    if (type.equalsIgnoreCase("ELECTRONICS")) {
      return new Electronics(id, name, description, startingPrice, sellerId);
    }
    throw new IllegalArgumentException("Loại sản phẩm không hợp lệ: " + type);
  }

  // Dùng khi tạo mới (chưa có id)
  public static Item createItem(String type,
                                String name,
                                String description,
                                BigDecimal startingPrice,
                                int sellerId) {

    if (type.equalsIgnoreCase("ART")) {
      return new Art(name, description, startingPrice, sellerId);
    }

    if (type.equalsIgnoreCase("ELECTRONICS")) {
      return new Electronics(name, description, startingPrice, sellerId);
    }

    throw new IllegalArgumentException("Invalid item type: " + type);
  }
}