package com.uet.bidding.controller.admin;

import java.io.IOException;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class AdminDashboardController {

  @FXML
  private StackPane contentArea; // "Khung ảnh" bên phải

  // Inject các nút từ FXML vào để xử lý giao diện
  @FXML
  private Button btnOverview;
  @FXML
  private Button btnUserManagement;
  @FXML
  private Button btnItemApproval;

  // 🚀 KHAI BÁO THÊM NÚT QUẢN LÝ PHIÊN
  @FXML
  private Button btnAuctionManagement;

  @FXML
  private Button btnLogout;

  @FXML
  public void initialize() {
    // CHÍ CHỐNG LỖI: Chờ giao diện dựng xong hoàn toàn rồi mới kích hoạt trang đầu tiên
    Platform.runLater(() -> {
      showOverviewPage();
    });
  }

  @FXML
  public void showOverviewPage() {
    setActiveMenu(btnOverview);
    changePage("/AdminOverview.fxml");
  }

  @FXML
  public void showUserManagementPage() {
    setActiveMenu(btnUserManagement);
    changePage("/AdminUserManagement.fxml");
  }

  @FXML
  public void showItemApprovalPage() {
    setActiveMenu(btnItemApproval);
    changePage("/AdminItemManagement.fxml");
  }

  // 🚀 HÀM GỌI RA MÀN HÌNH QUẢN LÝ PHIÊN ĐẤU GIÁ MỚI LÀM
  @FXML
  public void showAuctionManagementPage() {
    setActiveMenu(btnAuctionManagement);
    // Lưu ý: Đảm bảo tên file FXML anh em mình vừa tạo ở trên khớp với chuỗi này nhé
    changePage("/AdminAuctionManagement.fxml");
  }

  /**
   * Xử lý sự kiện khi Admin bấm nút Đăng xuất.
   */
  @FXML
  public void handleLogout() {
    try {
      System.out.println("[Admin Dashboard] Đang đăng xuất và quay về màn hình Login...");

      // 1. Tải giao diện màn hình Đăng nhập
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/Login.fxml"));
      Parent loginRoot = loader.load();

      // 2. Tạo một Scene mới cho màn hình Đăng nhập
      Scene loginScene = new Scene(loginRoot);

      // 3. Lấy Stage (cửa sổ) hiện tại từ nút Đăng xuất và gán Scene mới vào
      Stage currentStage = (Stage) btnLogout.getScene().getWindow();
      currentStage.setScene(loginScene);

      // 4. Căn giữa lại cửa sổ màn hình đăng nhập cho đẹp (tùy chọn)
      currentStage.centerOnScreen();
      currentStage.show();

    } catch (IOException e) {
      System.err.println("Không thể quay lại màn hình Login -> Lỗi: " + e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Hàm chịu trách nhiệm bật hiệu ứng nền mờ bo góc cho nút đang được chọn,
   * đồng thời đưa các nút khác về trạng thái bình thường mà không lo bị thay đổi kích thước.
   *
   * @param activeButton nút đang được chọn
   */
  private void setActiveMenu(Button activeButton) {
    // 1. Định dạng chuẩn cho tất cả các nút
    String normalStyle = "-fx-background-color: transparent; "
        + "-fx-text-fill: white; "
        + "-fx-font-family: 'Segoe UI'; "
        + "-fx-font-size: 14px; "
        + "-fx-font-weight: normal; "
        + "-fx-cursor: hand; "
        + "-fx-background-radius: 8; "
        + "-fx-focused-background-color: transparent; "
        + "-fx-focus-color: transparent;";

    btnOverview.setStyle(normalStyle);
    btnUserManagement.setStyle(normalStyle);
    btnItemApproval.setStyle(normalStyle);

    // 🚀 NHỚ RESET CẢ MÀU CỦA NÚT QUẢN LÝ PHIÊN NỮA NHÉ
    if(btnAuctionManagement != null) {
      btnAuctionManagement.setStyle(normalStyle);
    }

    // 2. Định dạng ĐẬM và MỜ XANH cho riêng nút được click (Active)
    String activeStyle = "-fx-background-color: #2a4ecb; "
        + "-fx-text-fill: white; "
        + "-fx-font-family: 'Segoe UI'; "
        + "-fx-font-size: 14px; "
        + "-fx-font-weight: bold; "
        + "-fx-cursor: hand; "
        + "-fx-background-radius: 8; "
        + "-fx-focused-background-color: #2a4ecb; "
        + "-fx-focus-color: transparent;";

    if(activeButton != null) {
      activeButton.setStyle(activeStyle);
    }
  }

  // Hàm dùng chung để dọn trang cũ, đập trang mới vào khungArea
  private void changePage(String fxmlPath) {
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
      Parent node = loader.load();
      contentArea.getChildren().setAll(node);
    } catch (IOException e) {
      System.err.println("Không thể chuyển sang trang: " + fxmlPath
          + " -> Lỗi: " + e.getMessage());
      e.printStackTrace();
    }
  }

  // ─── CÁC PHƯƠNG THỨC GETTER ĐƯỢC BỔ SUNG ĐỂ PHỤC VỤ UNIT TEST ────────────────

  public StackPane getContentArea() {
    return contentArea;
  }

  public Button getBtnOverview() {
    return btnOverview;
  }

  public Button getBtnUserManagement() {
    return btnUserManagement;
  }

  public Button getBtnItemApproval() {
    return btnItemApproval;
  }

  // Getter cho nút mới
  public Button getBtnAuctionManagement() {
    return btnAuctionManagement;
  }

  public Button getBtnLogout() {
    return btnLogout;
  }
}