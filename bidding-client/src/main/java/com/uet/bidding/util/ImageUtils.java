package com.uet.bidding.util;

import com.uet.bidding.model.Item;
import javafx.geometry.Rectangle2D;
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

  public static void loadAvatarFromUrl(ImageView view, Label fallbackLabel, String url) {
    if (view == null) return;

    // Nếu không có URL (hoặc chưa đổi ảnh bao giờ) -> Hiện chữ cái đại diện
    if (url == null || url.isBlank()) {
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
      // 🎯 ÁP DỤNG MA THUẬT CLOUDINARY: Tự động sửa URL để chống ngược/xoay ảnh
      if (url.startsWith("http://") || url.startsWith("https://")) {
        if (url.contains("cloudinary.com") && url.contains("/upload/")) {
          url = url.replace("/upload/", "/upload/a_auto/");
        }

        // load bất đồng bộ (true) giúp mượt UI sảnh sập
        Image image = new Image(url, true);
        view.setImage(image);
        view.setVisible(true);
        view.setManaged(true);

        if (fallbackLabel != null) {
          fallbackLabel.setVisible(false);
          fallbackLabel.setManaged(false);
        }
        // 🎯 THÊM MA THUẬT CẮT ẢNH THÔNG MINH Ở ĐÂY
        // Bắt sự kiện khi ảnh load mạng xong 100% thì mới tính toán cắt ảnh
        image.progressProperty().addListener((obs, oldVal, newVal) -> {
          if (newVal.doubleValue() == 1.0 && !image.isError()) {
            double w = image.getWidth();
            double h = image.getHeight();
            // Lấy cạnh ngắn nhất làm chuẩn để tạo hình vuông
            double size = Math.min(w, h);
            // Tính tọa độ X, Y để lấy đúng tâm bức ảnh
            double x = (w - size) / 2;
            double y = (h - size) / 2;

            // Ép ImageView chỉ hiển thị phần hình vuông ở tâm
            view.setViewport(new Rectangle2D(x, y, size, size));
          }
        });
      } else {
        // Fallback nếu là đường dẫn ổ đĩa cục bộ cũ
        File imgFile = new File(url);
        if (imgFile.exists()) {
          view.setImage(new Image(imgFile.toURI().toString()));
          view.setVisible(true);
          view.setManaged(true);
          if (fallbackLabel != null) {
            fallbackLabel.setVisible(false);
            fallbackLabel.setManaged(false);
          }
        } else {
          throw new Exception("Đường dẫn không hợp lệ");
        }
      }
    } catch (Exception e) {
      // Nếu lỗi load mạng -> Quay về hiện chữ cái đại diện cho an toàn
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