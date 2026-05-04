package com.uet.bidding.model;

import java.math.BigDecimal;

/**
 * Factory pattern: tạo Item dựa theo type
 * Đã tối ưu để phối hợp với hệ thống Serialization
 */
public class ItemFactory {

  // 1. Hàm này dùng khi nạp dữ liệu (Load) hoặc cập nhật từ Server
  public static Item createItem(String type,
                                int id,
                                String name,
                                String description,
                                BigDecimal startingPrice,
                                int sellerId) {
    if (type == null) throw new IllegalArgumentException("Type cannot be null");

    if (type.equalsIgnoreCase("ART")) {
      return new Art(id, name, description, startingPrice, sellerId);
    }
    if (type.equalsIgnoreCase("ELECTRONICS")) {
      return new Electronics(id, name, description, startingPrice, sellerId);
    }
    throw new IllegalArgumentException("Loại sản phẩm không hợp lệ: " + type);
  }

  // 2. Hàm này dùng khi người dùng tạo mới sản phẩm từ giao diện (Client)
  public static Item createItem(String type,
                                String name,
                                String description,
                                BigDecimal startingPrice,
                                int sellerId) {

    if (type == null) throw new IllegalArgumentException("Type cannot be null");

    if (type.equalsIgnoreCase("ART")) {
      return new Art(name, description, startingPrice, sellerId);
    }

    if (type.equalsIgnoreCase("ELECTRONICS")) {
      return new Electronics(name, description, startingPrice, sellerId);
    }

    throw new IllegalArgumentException("Invalid item type: " + type);
  }
}
