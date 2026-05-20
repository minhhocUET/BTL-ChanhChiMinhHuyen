package com.uet.bidding.model;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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

  public static List<Item> parseItemsFromJson(String json, Gson gson) {
    List<Item> items = new ArrayList<>();
    JsonArray arr = gson.fromJson(json, JsonArray.class);
    if (arr == null) return items;
    for (JsonElement el : arr) {
      items.add(parseItemFromJsonObject(el.getAsJsonObject()));
    }
    return items;
  }

  private static Item parseItemFromJsonObject(JsonObject o) {
    String type = detectItemType(o);
    int id = o.get("id").getAsInt();
    String name = o.get("name").getAsString();
    String description = o.has("description") ? o.get("description").getAsString() : "";
    BigDecimal price = o.get("startingPrice").getAsBigDecimal();
    String image = o.has("imagePath") ? o.get("imagePath").getAsString() : "";
    int sellerId = o.has("sellerId") ? o.get("sellerId").getAsInt() : 0;

    Item item = switch (type.toUpperCase()) {
      case "ELECTRONICS" -> createItemFromDb(type, id, name, description, price, image, sellerId,
          o.has("brand") ? o.get("brand").getAsString() : "Unknown",
          o.has("warrantyMonths") ? o.get("warrantyMonths").getAsInt() : 12);
      case "ART" -> createItemFromDb(type, id, name, description, price, image, sellerId,
          o.has("author") ? o.get("author").getAsString() : "Unknown",
          o.has("creationYear") ? o.get("creationYear").getAsInt() : 2000,
          o.has("material") ? o.get("material").getAsString() : "");
      case "VEHICLE" -> createItemFromDb(type, id, name, description, price, image, sellerId,
          o.has("brand") ? o.get("brand").getAsString() : "",
          o.has("model") ? o.get("model").getAsString() : "",
          o.has("manufacturingYear") ? o.get("manufacturingYear").getAsInt() : null,
          o.has("mileage") && !o.get("mileage").isJsonNull() ? o.get("mileage").getAsDouble() : null,
          o.has("engineType") ? o.get("engineType").getAsString() : "",
          o.has("fuelType") ? o.get("fuelType").getAsString() : "");
      default -> throw new IllegalArgumentException("Loại sản phẩm không xác định: " + type);
    };
    if (o.has("city") && !o.get("city").isJsonNull()) {
      item.setCity(o.get("city").getAsString());
    }
    if (o.has("imageData") && !o.get("imageData").isJsonNull()) {
      item.setImageData(o.get("imageData").getAsString());
    }
    return item;
  }

  private static String detectItemType(JsonObject o) {
    if (o.has("itemType")) return o.get("itemType").getAsString();
    if (o.has("warrantyMonths")) return "ELECTRONICS";
    if (o.has("author")) return "ART";
    if (o.has("manufacturingYear") || o.has("mileage")) return "VEHICLE";
    throw new IllegalArgumentException("Không xác định được loại sản phẩm từ JSON");
  }
}