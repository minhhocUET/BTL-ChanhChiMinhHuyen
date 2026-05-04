package com.uet.bidding.model;

public class ItemFactory {
  public static Item createItem(String type, int id, String name) {
    if (type.equalsIgnoreCase("ELECTRONICS")) {
      // Electronics cần đúng 7 tham số theo code bạn gửi:
      // (int id, String name, String description, double startingPrice, String imagePath, String brand, int warrantyMonths)
      return new Electronics(
          id,                     // 1. id
          name,                   // 2. name
          "Mô tả đồ điện tử",      // 3. description
          0.0,                    // 4. startingPrice
          "electronics_icon.png", // 5. imagePath
          "Generic Brand",        // 6. brand
          12                      // 7. warrantyMonths
      );
    } else if (type.equalsIgnoreCase("ART")) {
      // Art cần đúng 8 tham số theo code bạn gửi:
      // (int id, String name, String description, double startingPrice, String imagePath, String author, int creationYear, String material)
      return new Art(
          id,                         // 1. id
          name,                       // 2. name
          "Mô tả tác phẩm nghệ thuật",  // 3. description
          0.0,                        // 4. startingPrice
          "art_icon.png",             // 5. imagePath
          "Unknown Artist",           // 6. author
          2024,                       // 7. creationYear
          "Canvas"                    // 8. material
      );
    }
    throw new IllegalArgumentException("Loại sản phẩm không hợp lệ: " + type);
  }
}