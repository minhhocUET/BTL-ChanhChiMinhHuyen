package com.uet.bidding.model;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ItemFactory {

  public static Item createElectronics(String name, String description, BigDecimal startingPrice,
                                       String imagePath, int sellerId, String brand, int warrantyMonths) {
    return new Electronics(name, description, startingPrice, imagePath, sellerId, brand, warrantyMonths);
  }

  public static Item createArt(String name, String description, BigDecimal startingPrice,
                               String imagePath, int sellerId, String author, int creationYear, String material) {
    return new Art(name, description, startingPrice, imagePath, sellerId, author, creationYear, material);
  }

  public static Item createVehicle(String name, String description, BigDecimal startingPrice, String imagePath, int sellerId,
                                   String brand, String model, Integer year, Double mileage, String engine, String fuel) {
    return new Vehicle(name, description, startingPrice, imagePath, sellerId, brand, model, year, mileage, engine, fuel);
  }

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
      if (el.isJsonObject()) {
        items.add(parseItemFromJsonObject(el.getAsJsonObject()));
      }
    }
    return items;
  }

  private static Item parseItemFromJsonObject(JsonObject o) {
    String type = detectItemType(o);

    // Đọc an toàn các trường cơ bản
    int id = o.has("id") && !o.get("id").isJsonNull() ? o.get("id").getAsInt() : 0;
    String name = o.has("name") && !o.get("name").isJsonNull() ? o.get("name").getAsString() : "";
    String description = o.has("description") && !o.get("description").isJsonNull() ? o.get("description").getAsString() : "";
    BigDecimal price = o.has("startingPrice") && !o.get("startingPrice").isJsonNull() ? o.get("startingPrice").getAsBigDecimal() : BigDecimal.ZERO;
    String image = o.has("imagePath") && !o.get("imagePath").isJsonNull() ? o.get("imagePath").getAsString() : "";
    int sellerId = o.has("sellerId") && !o.get("sellerId").isJsonNull() ? o.get("sellerId").getAsInt() : 0;

    Item item = switch (type.toUpperCase()) {
      case "ELECTRONICS" -> createItemFromDb(type, id, name, description, price, image, sellerId,
          o.has("brand") && !o.get("brand").isJsonNull() ? o.get("brand").getAsString() : "Unknown",
          o.has("warrantyMonths") && !o.get("warrantyMonths").isJsonNull() ? o.get("warrantyMonths").getAsInt() : 0);

      case "ART" -> createItemFromDb(type, id, name, description, price, image, sellerId,
          o.has("author") && !o.get("author").isJsonNull() ? o.get("author").getAsString() : "Unknown",
          o.has("creationYear") && !o.get("creationYear").isJsonNull() ? o.get("creationYear").getAsInt() : 0,
          o.has("material") && !o.get("material").isJsonNull() ? o.get("material").getAsString() : "");

      case "VEHICLE" -> createItemFromDb(type, id, name, description, price, image, sellerId,
          o.has("brand") && !o.get("brand").isJsonNull() ? o.get("brand").getAsString() : "",
          o.has("model") && !o.get("model").isJsonNull() ? o.get("model").getAsString() : "",
          o.has("manufacturingYear") && !o.get("manufacturingYear").isJsonNull() ? o.get("manufacturingYear").getAsInt() : null,
          o.has("mileage") && !o.get("mileage").isJsonNull() ? o.get("mileage").getAsDouble() : null,
          o.has("engineType") && !o.get("engineType").isJsonNull() ? o.get("engineType").getAsString() : "",
          o.has("fuelType") && !o.get("fuelType").isJsonNull() ? o.get("fuelType").getAsString() : "");

      default -> throw new IllegalArgumentException("Loại sản phẩm không xác định: " + type);
    };

    // --- CÁC TRƯỜNG BỔ SUNG ---
    if (o.has("city") && !o.get("city").isJsonNull()) {
      item.setCity(o.get("city").getAsString());
    }
    if (o.has("imageData") && !o.get("imageData").isJsonNull()) {
      item.setImageData(o.get("imageData").getAsString());
    }

    // 🔥 FIX QUAN TRỌNG: Đọc thuộc tính status và inAuction
    if (o.has("status") && !o.get("status").isJsonNull()) {
      item.setStatus(o.get("status").getAsString());
    } else {
      item.setStatus("PENDING"); // Gán mặc định nếu JSON thiếu
    }

    if (o.has("rejectionReason") && !o.get("rejectionReason").isJsonNull()) {
      item.setRejectionReason(o.get("rejectionReason").getAsString());
    }

    if (o.has("inAuction") && !o.get("inAuction").isJsonNull()) {
      item.setInAuction(o.get("inAuction").getAsBoolean());
    } else if (o.has("in_auction") && !o.get("in_auction").isJsonNull()) {
      item.setInAuction(o.get("in_auction").getAsBoolean());
    }

    return item;
  }

  private static String detectItemType(JsonObject o) {
    // Ưu tiên tìm các key định danh loại
    if (o.has("itemType") && !o.get("itemType").isJsonNull()) return o.get("itemType").getAsString();
    if (o.has("type") && !o.get("type").isJsonNull()) return o.get("type").getAsString(); // Dự phòng Gson dùng tên biến "type"

    // Nếu không có, đoán dựa trên cấu trúc (Fallback)
    if (o.has("warrantyMonths")) return "ELECTRONICS";
    if (o.has("author")) return "ART";
    if (o.has("manufacturingYear") || o.has("mileage") || o.has("engineType")) return "VEHICLE";

    throw new IllegalArgumentException("Không xác định được loại sản phẩm từ JSON");
  }
}