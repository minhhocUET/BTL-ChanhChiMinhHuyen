package com.uet.bidding.controller.admin;

import com.uet.bidding.model.NetworkMessage;
import com.uet.bidding.network.ClientService;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane; // Hoặc AnchorPane/Pane tùy thuộc vào kiểu của contentArea trong project của ông
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(ApplicationExtension.class)
public class AdminDashboardControllerTest {

  private AdminDashboardController controller;
  private ClientService mockClientService;

  // Các biến đại diện cho thành phần đồ họa để phục vụ Assert (Thay đổi kiểu dữ liệu nếu cần cho đúng file FXML)
  private Button btnUserManagement;
  private Button btnOverview;
  private StackPane contentArea;

  @Start
  public void start(Stage stage) throws Exception {
    // 1. Khởi tạo Mock ClientService để chặn đứng toàn bộ các truy vấn mạng từ các sub-controllers
    mockClientService = mock(ClientService.class);

    // Tạo sẵn một Future thành công chung để trả về cho mọi yêu cầu mạng (Tránh lỗi mất kết nối máy chủ)
    CompletableFuture<NetworkMessage> successFuture = new CompletableFuture<>();
    NetworkMessage mockResponse = mock(NetworkMessage.class);
    lenient().when(mockResponse.getType()).thenReturn("SUCCESS");
    lenient().when(mockResponse.getData()).thenReturn(new ArrayList<>());
    successFuture.complete(mockResponse);

    // Cấu hình luật: Bất cứ khi nào có hàm sendRequest() gọi lên từ trang Dashboard, Overview, hay UserManagement, đều trả về successFuture
    lenient().when(mockClientService.sendRequest(anyString(), any())).thenReturn(successFuture);
    lenient().when(mockClientService.getGson()).thenReturn(new com.google.gson.Gson());

    // Gi giả lập hàm bóc tách dữ liệu rỗng để tránh văng NullPointerException ở các controller con
    lenient().when(mockClientService.parseUser(any())).thenReturn(mock(com.uet.bidding.model.User.class));

    // 2. Inject Mock vào thực thể Singleton ClientService bằng cơ chế Reflection
    Field instanceField = ClientService.class.getDeclaredField("instance");
    instanceField.setAccessible(true);
    instanceField.set(null, mockClientService);

    // 3. Nạp giao diện FXML chính của Dashboard
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/AdminDashboard.fxml"));
    Parent root = loader.load();
    controller = loader.getController();

    // 4. Trích xuất các thành phần giao diện từ Controller để phục vụ cho các câu lệnh kiểm thử Assert
    // Sử dụng Reflection nếu các trường này đặt thuộc tính private hoặc lấy trực tiếp nếu là public/protected
    try {
      Field btnUserField = AdminDashboardController.class.getDeclaredField("btnUserManagement");
      btnUserField.setAccessible(true);
      btnUserManagement = (Button) btnUserField.get(controller);

      Field btnOverviewField = AdminDashboardController.class.getDeclaredField("btnOverview");
      btnOverviewField.setAccessible(true);
      btnOverview = (Button) btnOverviewField.get(controller);

      Field contentAreaField = AdminDashboardController.class.getDeclaredField("contentArea");
      contentAreaField.setAccessible(true);
      contentArea = (StackPane) contentAreaField.get(controller); // Đổi thành AnchorPane nếu FXML dùng AnchorPane
    } catch (Exception e) {
      // Dự phòng nếu trong class test của ông đã khai báo gán sẵn bằng cách khác hoặc các trường đang công khai
    }

    stage.setScene(new Scene(root, 1024, 768));
    stage.show();
  }

  @AfterEach
  public void tearDown() throws Exception {
    // Dọn dẹp Mock Singleton sau khi hoàn thành test để bảo vệ bộ nhớ không bị rò rỉ
    Field instanceField = ClientService.class.getDeclaredField("instance");
    instanceField.setAccessible(true);
    instanceField.set(null, null);
  }

  @Test
  public void testShowUserManagementPage_ThayĐổiStyleNútVàChuyểnGiaoDiệnCon() {
    // Đảm bảo các thành phần UI đồ họa đã được liên kết thành công qua FXML trước khi test logic
    assertNotNull(controller, "Controller không được null");
    assertNotNull(btnUserManagement, "Nút User Management không được null");
    assertNotNull(btnOverview, "Nút Overview không được null");
    assertNotNull(contentArea, "Vùng contentArea không được null");

    // 🎯 FIX CHÍNH: Ép câu lệnh chuyển trang chạy trực tiếp trên JavaFX Application Thread thông qua Platform.runLater
    Platform.runLater(() -> {
      controller.showUserManagementPage();
    });

    // Bắt luồng Test dừng lại chờ cho vòng lặp JavaFX Thread xử lý xong tác vụ vẽ và nạp FXML con vào vùng chứa
    WaitForAsyncUtils.waitForFxEvents();

    // Kiểm tra xem CSS style của nút được click chọn có cập nhật font chữ in đậm (bold) lên không
    String userStyle = btnUserManagement.getStyle();
    String overviewStyle = btnOverview.getStyle();

    assertTrue(userStyle.contains("-fx-font-weight: bold;"), "Nút đang chọn phải có chữ đậm (bold)");
    assertTrue(overviewStyle.contains("-fx-font-weight: normal;"), "Nút không chọn phải đưa về bình thường (normal)");

    // Kiểm tra xem vùng hiển thị chính đã nạp thành công giao diện con của trang quản lý người dùng vào chưa
    assertFalse(contentArea.getChildren().isEmpty(), "Vùng nội dung phải chứa giao diện con sau khi bấm");
  }
}