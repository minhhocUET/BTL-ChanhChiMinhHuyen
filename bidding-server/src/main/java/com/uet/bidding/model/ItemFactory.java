package com.uet.bidding.model;

import java.math.BigDecimal;

public class ItemFactory {

  /**
   * 1. Tạo mới ĐIỆN TỬ (Dùng khi Client đăng bán sản phẩm)
   */
  public static Item createElectronics(String name, String description, BigDecimal startingPrice,
                                       String imagePath, int sellerId, String brand, int warrantyMonths) {
    return new Electronics(name, description, startingPrice, imagePath, sellerId, brand, warrantyMonths);
  }

  /**
   * 2. Tạo mới NGHỆ THUẬT (Dùng khi Client đăng bán sản phẩm)
   */
  public static Item createArt(String name, String description, BigDecimal startingPrice,
                               String imagePath, int sellerId, String author, int creationYear, String material) {
    return new Art(name, description, startingPrice, imagePath, sellerId, author, creationYear, material);
  }

  /**
   * 3. Tạo mới XE CỘ (Dùng khi Client đăng bán sản phẩm)
   * Khớp chính xác với Constructor 11 tham số trong class Vehicle của bạn
   */
  public static Item createVehicle(String name, String description, BigDecimal startingPrice, String imagePath, int sellerId,
                                   String brand, String model, Integer year, Double mileage, String engine, String fuel) {
    return new Vehicle(name, description, startingPrice, imagePath, sellerId, brand, model, year, mileage, engine, fuel);
  }

  /**
   * 4. Hàm tổng quát tạo từ Database (Có ID)
   * Dùng switch-case để xử lý logic nạp dữ liệu hàng loạt từ DB
   */
  public static Item createItemFromDb(String type, int id, String name, String description, BigDecimal price,
                                      String image, int sellerId, Object... extra) {
    if (type == null) throw new IllegalArgumentException("Type cannot be null");

    return switch (type.toUpperCase()) {
      case "ELECTRONICS" -> new Electronics(id, name, description, price, image, sellerId,
          (String) extra[0], (Integer) extra[1]);

      case "ART" -> new Art(id, name, description, price, image, sellerId,
          (String) extra[0], (Integer) extra[1], (String) extra[2]);

      case "VEHICLE" -> new Vehicle(id, name, description, price, image, sellerId,
          (String) extra[0], (String) extra[1], (Integer) extra[2],
          (Double) extra[3], (String) extra[4], (String) extra[5]);

      default -> throw new IllegalArgumentException("Loại sản phẩm không xác định: " + type);
    };
  }
}