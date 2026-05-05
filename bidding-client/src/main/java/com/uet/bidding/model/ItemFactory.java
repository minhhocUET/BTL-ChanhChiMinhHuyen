package com.uet.bidding.model;

import java.math.BigDecimal;

public class ItemFactory {

  // 1. Hàm này dùng khi nạp dữ liệu (Load) hoặc cập nhật từ Server
  // Đã thêm brand và warrantyMonths vào tham số
  public static Item createItem(String type, int id, String name, String description, BigDecimal startingPrice,
                                String imagePath, int sellerId,
                                String author, int creationYear, String material,
                                String brand, int warrantyMonths) {

    if (type == null) throw new IllegalArgumentException("Type cannot be null");

    if (type.equalsIgnoreCase("ART")) {
      return new Art(id, name, description, startingPrice, imagePath, sellerId, author, creationYear, material);
    }
    if (type.equalsIgnoreCase("ELECTRONICS")) {
      // Đã truyền đủ tham số cho Electronics
      return new Electronics(id, name, description, startingPrice, imagePath, sellerId, brand, warrantyMonths);
    }
    throw new IllegalArgumentException("Loại sản phẩm không hợp lệ: " + type);
  }

  // 2. Hàm này dùng khi người dùng tạo mới sản phẩm từ giao diện (Client)
  // Đã thêm đầy đủ tham số để khớp với Constructor của Art và Electronics
  public static Item createItem(String type, String name, String description, BigDecimal startingPrice,
                                String imagePath, int sellerId,
                                String author, int creationYear, String material,
                                String brand, int warrantyMonths) {

    if (type == null) throw new IllegalArgumentException("Type cannot be null");

    if (type.equalsIgnoreCase("ART")) {
      // Đã truyền đủ tham số cho Art
      return new Art(name, description, startingPrice, imagePath, sellerId, author, creationYear, material);
    }

    if (type.equalsIgnoreCase("ELECTRONICS")) {
      // Đã truyền đủ tham số cho Electronics
      return new Electronics(name, description, startingPrice, imagePath, sellerId, brand, warrantyMonths);
    }

    throw new IllegalArgumentException("Invalid item type: " + type);
  }
}