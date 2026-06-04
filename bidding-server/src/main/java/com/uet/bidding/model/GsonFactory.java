package com.uet.bidding.model;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * GsonFactory – Tạo Gson instance dùng chung cho cả Client và Server.
 * Đã được nâng cấp khả năng chống chịu lỗi Date-Time từ Database.
 */
public class GsonFactory {

  private static Gson instance;

  public static Gson getInstance() {
    if (instance == null) {

      // 1. 🎯 BỘ XỬ LÝ ĐA HÌNH CHO ITEM (Sản phẩm)
      JsonDeserializer<Item> itemDeserializer = (json, typeOfT, context) -> {
        JsonObject jsonObject = json.getAsJsonObject();

        String type = null;
        if (jsonObject.has("type") && !jsonObject.get("type").isJsonNull()) {
          type = jsonObject.get("type").getAsString();
        } else if (jsonObject.has("item_type") && !jsonObject.get("item_type").isJsonNull()) {
          type = jsonObject.get("item_type").getAsString();
        } else if (jsonObject.has("itemType") && !jsonObject.get("itemType").isJsonNull()) {
          type = jsonObject.get("itemType").getAsString();
        }

        if (type == null) {
          throw new JsonParseException("Không tìm thấy thuộc tính phân loại sản phẩm trong JSON!");
        }

        return switch (type.toUpperCase()) {
          case "ART" -> context.deserialize(jsonObject, Art.class);
          case "ELECTRONICS" -> context.deserialize(jsonObject, Electronics.class);
          case "VEHICLE" -> context.deserialize(jsonObject, Vehicle.class);
          default -> throw new JsonParseException("Loại sản phẩm không hợp lệ: " + type);
        };
      };

      // 2. 🎯 BỘ XỬ LÝ ĐA HÌNH CHO USER (Phân biệt Admin và Customer)
      JsonDeserializer<User> userDeserializer = (json, typeOfT, context) -> {
        JsonObject jsonObject = json.getAsJsonObject();
        String role = null;
        if (jsonObject.has("role") && !jsonObject.get("role").isJsonNull()) {
          role = jsonObject.get("role").getAsString();
        }

        if ("ADMIN".equalsIgnoreCase(role)) {
          return context.deserialize(jsonObject, Admin.class);
        }
        // Nếu không phải ADMIN thì mặc định ép kiểu về Customer
        return context.deserialize(jsonObject, Customer.class);
      };

      // 3. 🎯 CẤU HÌNH GSON CHÍNH THỨC
      instance = new GsonBuilder()
          .registerTypeAdapter(Item.class, itemDeserializer)
          .registerTypeAdapter(User.class, userDeserializer) // Đăng ký nhận diện User

          // 🕒 Xử lý LocalDateTime BẤT TỬ (Chống lại mọi định dạng dị thường từ DB)
          .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
              new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))

          .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) -> {
            String timeStr = json.getAsString();
            // MA THUẬT: Đổi dấu cách của SQL thành chữ 'T' của chuẩn ISO
            timeStr = timeStr.replace(" ", "T");
            try {
              return LocalDateTime.parse(timeStr);
            } catch (Exception e) {
              // Dự phòng: Lấy 19 ký tự đầu tiên nếu chuỗi trả về dài dòng (VD có mili-giây)
              return LocalDateTime.parse(timeStr.substring(0, 19));
            }
          })

          // 📅 Xử lý LocalDate (Ngày tháng cơ bản)
          .registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>) (src, typeOfSrc, context) ->
              new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE)))

          .registerTypeAdapter(LocalDate.class, (JsonDeserializer<LocalDate>) (json, typeOfT, context) ->
              LocalDate.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE))
          .create();
    }
    return instance;
  }

  // =====================================================================
  // 🚀 CÁC HÀM TIỆN ÍCH STATIC (FIX LỖI BỊ GẠCH ĐỎ Ở CÁC FILE CONTROLLER)
  // =====================================================================

  public static String toJson(Object src) {
    return getInstance().toJson(src);
  }

  public static <T> T fromJson(String json, Class<T> classOfT) {
    return getInstance().fromJson(json, classOfT);
  }

  public static <T> T fromJson(String json, Type typeOfT) {
    return getInstance().fromJson(json, typeOfT);
  }
}