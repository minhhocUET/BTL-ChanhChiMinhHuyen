package com.uet.bidding.controller.admin;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ApplicationExtension.class)
public class AdminDashboardControllerTest {

  // 1. Chỉ KHAI BÁO các biến làm thuộc tính của Class Test ở đây thôi (Hợp lệ)
  private AdminDashboardController controller;
  private StackPane contentArea;
  private Button btnOverview;
  private Button btnUserManagement;

  @Start
  public void start(Stage stage) throws Exception {
    // Tải giao diện FXML giả lập lên để chạy test
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/AdminDashboard.fxml"));
    Parent root = loader.load();

    // Lấy controller tương ứng
    controller = loader.getController();

    // 2. Các câu lệnh GÁN và THỰC THI phải nằm NGOAN NGOÃN trong hàm start này (Hợp lệ)
    // (Lưu ý: Hãy chắc chắn bạn đã thêm các hàm Getter tương ứng này ở AdminDashboardController chạy thật nhé)
    contentArea = controller.getContentArea();
    btnOverview = controller.getBtnOverview();
    btnUserManagement = controller.getBtnUserManagement();

    // Hiển thị màn hình test lên cửa sổ ảo
    stage.setScene(new Scene(root));
    stage.show();
  }

  @Test
  public void testShowUserManagementPage_ThayĐổiStyleNútVàChuyểnGiaoDiệnCon() {
    // Kiểm tra bảo vệ xem các nút đã được liên kết thành công chưa trước khi test logic
    assertNotNull(controller, "Controller không được null");
    assertNotNull(btnUserManagement, "Nút User Management không được null");
    assertNotNull(btnOverview, "Nút Overview không được null");
    assertNotNull(contentArea, "Vùng contentArea không được null");

    // Gửi lệnh click / chọn trang giả lập
    controller.showUserManagementPage();
    WaitForAsyncUtils.waitForFxEvents(); // Chờ giao diện render xong

    // Kiểm tra xem CSS của nút được click có in đậm lên không
    String userStyle = btnUserManagement.getStyle();
    String overviewStyle = btnOverview.getStyle();

    assertTrue(userStyle.contains("-fx-font-weight: bold;"), "Nút đang chọn phải có chữ đậm (bold)");
    assertTrue(overviewStyle.contains("-fx-font-weight: normal;"), "Nút không chọn phải đưa về bình thường (normal)");

    // Kiểm tra xem vùng hiển thị chính có nạp giao diện con vào không
    assertFalse(contentArea.getChildren().isEmpty(), "Vùng nội dung phải chứa giao diện con sau khi bấm");
  }
}