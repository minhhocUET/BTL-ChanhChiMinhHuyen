package com.uet.bidding.model;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * GsonFactory – tạo Gson instance dùng chung cho cả Client và Server.
 */
public class GsonFactory {

  private static Gson instance;

  public static Gson getInstance() {
    if (instance == null) {

      // 1. Định nghĩa bộ Deserializer bóc tách tính đa hình cho lớp trừu tượng Item
      JsonDeserializer<Item> itemDeserializer = new JsonDeserializer<Item>() {
        @Override
        public Item deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
          JsonObject jsonObject = json.getAsJsonObject();

          // Kiểm tra an toàn để tránh lỗi NullPointerException
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
        }
      };

      // 2. Cấu hình chính thức và đăng ký Adapter cho GsonBuilder
      instance = new GsonBuilder()
          .registerTypeAdapter(Item.class, itemDeserializer)

          // Đăng ký bộ xử lý LocalDateTime trực tiếp qua ISO chuẩn
          .registerTypeAdapter(LocalDateTime.class, new JsonSerializer<LocalDateTime>() {
            @Override
            public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
              return new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
          })
          .registerTypeAdapter(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
            @Override
            public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
              return LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
          })

          // Đăng ký bộ xử lý LocalDate
          .registerTypeAdapter(LocalDate.class, new JsonSerializer<LocalDate>() {
            @Override
            public JsonElement serialize(LocalDate src, Type typeOfSrc, JsonSerializationContext context) {
              return new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE));
            }
          })
          .registerTypeAdapter(LocalDate.class, new JsonDeserializer<LocalDate>() {
            @Override
            public LocalDate deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
              return LocalDate.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE);
            }
          })
          .create();
    }
    return instance;
  }
}