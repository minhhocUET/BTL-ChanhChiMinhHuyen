package com.uet.bidding.ui; // Thêm package cho khớp với thư mục của bạn

import com.uet.bidding.network.ClientService; // Import cái "hệ thần kinh" bạn vừa tạo
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class Main extends Application {

  // Biến toàn cục lưu trữ Cửa sổ (Window) chính
  private static Stage window;

  // 2. Hàm dùng chung để chuyển đổi màn hình (có bắt lỗi an toàn)
  public static void changeScene(String fxmlFile, String title, int width, int height) {
    try {
      // Tìm file FXML
      URL fxmlLocation = Main.class.getResource(fxmlFile);

      // Nếu không tìm thấy file, in ra cảnh báo đỏ để dễ sửa
      if (fxmlLocation == null) {
        System.err.println("LỖI NGHIÊM TRỌNG: Không tìm thấy file FXML -> " + fxmlFile);
        System.err.println("Hãy chắc chắn file " + fxmlFile + " nằm trong thư mục src/main/resources/");
        return;
      }

      // Load file và set Scene
      Parent pane = FXMLLoader.load(fxmlLocation);
      window.setTitle(title);
      window.setScene(new Scene(pane, width, height));
      window.centerOnScreen(); // Tự động căn giữa màn hình

    } catch (IOException e) {
      System.err.println("LỖI: Có vấn đề bên trong file FXML hoặc Controller của: " + fxmlFile);
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage primaryStage) {
    window = primaryStage;

    // --- PHẦN THÊM VÀO: KẾT NỐI MẠNG ---
    try {
      // Thử kết nối đến Server (localhost, cổng 8888)
      ClientService.getInstance().connect("127.0.0.1", 8888);
      System.out.println("✅ Network initialized successfully.");
    } catch (IOException e) {
      // Nếu không thấy Server, hiện thông báo lỗi cho người dùng
      showErrorAlert("Không thể kết nối đến Server!",
          "Vui lòng kiểm tra xem Server đã được bật chưa trước khi chạy Client.");
      // Tùy chọn: Dừng app luôn nếu không có mạng
      // System.exit(0);
    }
    // ------------------------------------

    changeScene("/Login.fxml", "Hệ thống Đấu giá VNU - Đăng nhập", 400, 500);
    window.show();
  }

  // Hàm bổ trợ để hiện thông báo lỗi đẹp hơn
  private void showErrorAlert(String title, String content) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle("Lỗi kết nối");
    alert.setHeaderText(title);
    alert.setContentText(content);
    alert.showAndWait();
  }
}
