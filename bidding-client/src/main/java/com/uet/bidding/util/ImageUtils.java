package com.uet.bidding.util;

import com.uet.bidding.model.Item;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.io.File;
import java.nio.file.Files;
import java.util.Base64;

public class ImageUtils {

  // Hàm 1: Chuyển file thành chuỗi Base64 (Dùng khi Seller chọn ảnh để gửi lên Server)
  public static String encodeFileToBase64(File file) {
    try {
      byte[] fileContent = Files.readAllBytes(file.toPath());
      return Base64.getEncoder().encodeToString(fileContent);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  // Hàm 2: Load ảnh từ file vật lý (Dùng để hiển thị ảnh cho Admin và Seller)
  public static void loadItemImage(ImageView view, Item item) {
    if (view == null || item == null) return;

    // Lấy đường dẫn ảnh thay vì lấy chuỗi Base64
    String path = item.getImagePath();

    if (path == null || path.isBlank()) {
      view.setImage(null);
      return;
    }

    try {
      File imgFile = new File(path);
      if (imgFile.exists()) {
        view.setImage(new Image(imgFile.toURI().toString()));
      } else {
        view.setImage(null); // Tránh lỗi văng app nếu lỡ tay xóa mất file ảnh trên ổ cứng
      }
    } catch (Exception e) {
      view.setImage(null);
    }
  }
}