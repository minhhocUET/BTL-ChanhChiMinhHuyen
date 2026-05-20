package com.uet.bidding.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

public class FileUtil {
  private static final String UPLOAD_DIR = "server_storage/items/";

  public static String saveImageFromBase64(String base64Data, String fileName) throws IOException {
    if (base64Data == null || base64Data.isEmpty()) return null;

    // Tạo thư mục nếu chưa tồn tại
    Files.createDirectories(Paths.get(UPLOAD_DIR));

    // Giải mã chuỗi Base64
    byte[] imageBytes = Base64.getDecoder().decode(base64Data);
    String filePath = UPLOAD_DIR + fileName + ".jpg";

    // Ghi dữ liệu ra file vật lý
    try (FileOutputStream fos = new FileOutputStream(filePath)) {
      fos.write(imageBytes);
    }

    return filePath; // Trả về đường dẫn để lưu vào cột image_path
  }
}