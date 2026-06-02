package com.uet.bidding.util;

import com.uet.bidding.model.Item;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.Base64;

public class ImageUtils {

  public static String encodeFileToBase64(File file) {
    try {
      byte[] fileContent = Files.readAllBytes(file.toPath());
      return Base64.getEncoder().encodeToString(fileContent);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public static void loadAvatarFromBase64(ImageView view, Label fallbackLabel, String base64) {
    if (view == null) return;
    if (base64 == null || base64.isBlank()) {
      view.setImage(null);
      view.setVisible(false);
      view.setManaged(false);
      if (fallbackLabel != null) {
        fallbackLabel.setVisible(true);
        fallbackLabel.setManaged(true);
      }
      return;
    }
    try {
      byte[] bytes = Base64.getDecoder().decode(base64);
      Image image = new Image(new ByteArrayInputStream(bytes));
      view.setImage(image);
      view.setVisible(true);
      view.setManaged(true);
      if (fallbackLabel != null) {
        fallbackLabel.setVisible(false);
        fallbackLabel.setManaged(false);
      }
    } catch (Exception e) {
      view.setImage(null);
      view.setVisible(false);
      view.setManaged(false);
      if (fallbackLabel != null) {
        fallbackLabel.setVisible(true);
        fallbackLabel.setManaged(true);
      }
    }
  }

  public static void loadItemImage(ImageView view, Item item) {
    if (view == null || item == null) return;

    String path = item.getImagePath();

    if (path == null || path.isBlank()) {
      view.setImage(null);
      return;
    }

    try {
      // NẾU LÀ LINK MẠNG TỪ CLOUDINARY (Chạy trên mọi máy)
      if (path.startsWith("http://") || path.startsWith("https://")) {
        // ----- BẮT ĐẦU ĐOẠN FIX XOAY ẢNH -----
        // Nếu là link Cloudinary, ta thêm ma thuật "/a_auto/" vào URL
        // Ví dụ link cũ: .../upload/v12345/item_1.jpg
        // Link mới: .../upload/a_auto/v12345/item_1.jpg
        if (path.contains("cloudinary.com") && path.contains("/upload/")) {
          path = path.replace("/upload/", "/upload/a_auto/");
        }
        // ----- KẾT THÚC ĐOẠN FIX XOAY ẢNH -----

        // true = load bất đồng bộ cho đỡ lag giao diện
        view.setImage(new Image(path, true));
      }
      // NẾU LÀ ẢNH CŨ TRONG Ổ CỨNG (Fallback)
      else {
        File imgFile = new File(path);
        if (imgFile.exists()) {
          view.setImage(new Image(imgFile.toURI().toString()));
        } else {
          view.setImage(null);
        }
      }
    } catch (Exception e) {
      System.err.println("Lỗi load ảnh: " + e.getMessage());
      view.setImage(null);
    }
  }
}