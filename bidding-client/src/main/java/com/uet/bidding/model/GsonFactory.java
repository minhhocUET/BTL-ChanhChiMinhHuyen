package com.uet.bidding.model;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * GsonFactory – tạo Gson instance dùng chung cho cả Client và Server.
 * <p>
 * Lý do cần class này:
 * Gson mặc định không xử lý được java.time.LocalDateTime vì các field
 * bên trong nó là private → ném lỗi "Failed making field accessible".
 * Giải pháp: đăng ký TypeAdapter để serialize/deserialize thủ công
 * dưới dạng chuỗi ISO-8601 (ví dụ: "2025-06-01T10:30:00").
 * <p>
 * Cách dùng:
 * // Thay vì: private final Gson gson = new Gson();
 * private final Gson gson = GsonFactory.create();
 */
public class GsonFactory {

  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  private static Gson instance;

  public static Gson getInstance() {
    if (instance == null) {
      instance = new GsonBuilder()
          // Dạy Gson cách xử lý LocalDateTime
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
          // Dạy Gson cách xử lý LocalDate (Phòng hờ)
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

  // ── Serializer: LocalDateTime → JSON String ──────────────────────
  private static class LocalDateTimeSerializer
      implements JsonSerializer<LocalDateTime> {
    @Override
    public JsonElement serialize(LocalDateTime src, Type typeOfSrc,
                                 JsonSerializationContext context) {
      return new JsonPrimitive(src.format(FORMATTER));
    }
  }

  // ── Deserializer: JSON String → LocalDateTime ─────────────────────
  private static class LocalDateTimeDeserializer
      implements JsonDeserializer<LocalDateTime> {
    @Override
    public LocalDateTime deserialize(JsonElement json, Type typeOfT,
                                     JsonDeserializationContext context)
        throws JsonParseException {
      return LocalDateTime.parse(json.getAsString(), FORMATTER);
    }
  }
}