package com.uet.bidding.controller.admin;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class AdminController {

  @FXML
  private StackPane mainContent; // Vùng sẽ thay đổi nội dung khi bấm nút

  /**
   * Hàm này sẽ tự động chạy khi giao diện Admin được bật lên.
   * Mặc định hiển thị luôn tab Quản lý User để màn hình không bị trống.
   */
  @FXML
  public void initialize() {
    handleManageUsers();
  }

  @FXML
  private void handleManageUsers() {
    System.out.println("Switching to User Management View...");
    // Điền file fxml quản lý người dùng của bạn vào đây (ví dụ: /AdminUserManagement.fxml)
    loadView("/AdminUserManagement.fxml");
  }

  @FXML
  private void handleManageAuctions() {
    System.out.println("Switching to Auction Management View...");
    // Đã sửa kết nối chính xác tới file thực tế của bạn
    loadView("/AdminItemManagement.fxml");
  }

  @FXML
  private void handleSystemStatistics() {
    System.out.println("Showing system reports...");
    // Điền file fxml thống kê của bạn vào đây (ví dụ: /AdminStatistics.fxml)
    loadView("/AdminStatistics.fxml");
  }

  @FXML
  private void handleLogout() {
    System.out.println("Logging out Admin...");
    try {
      // 1. Tải lại giao diện màn hình Đăng nhập ban đầu
      Parent root = FXMLLoader.load(getClass().getResource("/Login.fxml"));

      // 2. Lấy Stage (cửa sổ) hiện tại thông qua vùng mainContent
      Stage stage = (Stage) mainContent.getScene().getWindow();

      // 3. Thay đổi scene thành màn hình Login
      stage.setScene(new Scene(root));
      stage.setTitle("Hệ thống Đấu giá - Đăng nhập");
      stage.show();

    } catch (Exception e) {
      e.printStackTrace();
      showAlert("Lỗi", "Không thể quay lại màn hình đăng nhập: " + e.getMessage());
    }
  }

  // ─── HÀM BỔ TRỢ LOADVIEW ĐÃ ĐƯỢC THÊM VÀO ĐỂ SỬA LỖI ────────────────

  /**
   * Hàm dùng chung để nạp các sub-view FXML vào vùng trung tâm (mainContent)
   *
   * @param fxmlPath Đường dẫn tới file FXML nằm trong thư mục resources
   */
  private void loadView(String fxmlPath) {
    try {
      // Khởi tạo loader để đọc file FXML con
      FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
      Parent view = loader.load();

      // Dọn sạch view cũ trong StackPane và nhét view mới vào
      mainContent.getChildren().setAll(view);

    } catch (Exception e) {
      System.err.println("❌ Lỗi khi tải file giao diện: " + fxmlPath);
      e.printStackTrace();
      showAlert("Lỗi hệ thống", "Không thể hiển thị giao diện: " + fxmlPath
          + "\nHãy chắc chắn rằng file FXML này đã được tạo trong thư mục resources và viết đúng chính tả.");
    }
  }

  /**
   * Hàm bổ trợ nhanh hiển thị thông báo lỗi
   */
  private void showAlert(String title, String content) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(content);
    alert.showAndWait();
  }
}