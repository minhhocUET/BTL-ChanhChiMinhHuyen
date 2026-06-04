package com.uet.bidding.controller;

import com.uet.bidding.controller.auth.RegisterController;
import com.uet.bidding.model.NetworkMessage; // Nhớ đổi đúng package nếu bị sai
import com.uet.bidding.service.UserService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.testfx.framework.junit5.ApplicationExtension;

import java.lang.reflect.Field;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Bật môi trường JavaFX lên để khởi tạo được Giao diện Thật
@ExtendWith(ApplicationExtension.class)
public class RegisterControllerTest {

  private RegisterController controller;

  // SỬ DỤNG GIAO DIỆN THẬT (Tuyệt đối không mock để né lỗi Java 21)
  private TextField realUsernameField;
  private PasswordField realPasswordField;
  private PasswordField realConfirmPasswordField;
  private Label realMessageLabel;

  // Service giao tiếp Server thì làm giả (Mock)
  private UserService mockUserService;

  // Mock static Platform để lách luồng
  private MockedStatic<Platform> platformMock;

  @BeforeEach
  public void setUp() throws Exception {
    controller = new RegisterController();

    // 1. Dựng đồ thật
    realUsernameField = new TextField();
    realPasswordField = new PasswordField();
    realConfirmPasswordField = new PasswordField();
    realMessageLabel = new Label();

    // Tiêm đồ thật vào Controller
    injectPrivateField(controller, "usernameField", realUsernameField);
    injectPrivateField(controller, "passwordField", realPasswordField);
    injectPrivateField(controller, "confirmPasswordField", realConfirmPasswordField);
    injectPrivateField(controller, "messageLabel", realMessageLabel);

    // 2. Dựng UserService giả
    mockUserService = mock(UserService.class);
    injectPrivateField(controller, "userService", mockUserService);

    // 3. Ép Platform.runLater chạy code bên trong nó ngay lập tức, không cho đẩy sang luồng khác
    platformMock = mockStatic(Platform.class);
    platformMock.when(() -> Platform.runLater(any(Runnable.class)))
        .thenAnswer(invocation -> {
          Runnable runnable = invocation.getArgument(0);
          runnable.run();
          return null;
        });
  }

  @AfterEach
  public void tearDown() {
    // Đừng quên đóng cửa sổ chặn luồng
    platformMock.close();
  }

  @Test
  public void testHandleRegister_BoTrongThongTin_BaoLoiDo() {
    // GIVEN: Người dùng để trống
    realUsernameField.setText("");
    realPasswordField.setText("123456");
    realConfirmPasswordField.setText("");

    // WHEN: Bấm Đăng ký (Truyền null cho Event vì chưa cần dùng đến)
    controller.handleRegister(null);

    // THEN: Nhãn báo lỗi màu ĐỎ
    assertEquals("Vui lòng nhập đầy đủ thông tin!", realMessageLabel.getText());
    assertEquals(Color.RED, realMessageLabel.getTextFill());

    // Đảm bảo chưa gửi thông tin lên mạng
    verifyNoInteractions(mockUserService);
  }

  @Test
  public void testHandleRegister_MatKhauKhongKhop_BaoLoiDo() {
    // GIVEN: Gõ mật khẩu và xác nhận bị lệch nhau
    realUsernameField.setText("testuser");
    realPasswordField.setText("123456");
    realConfirmPasswordField.setText("654321"); // Lệch

    // WHEN
    controller.handleRegister(null);

    // THEN
    assertEquals("Mật khẩu nhập lại không khớp!", realMessageLabel.getText());
    assertEquals(Color.RED, realMessageLabel.getTextFill());
    verifyNoInteractions(mockUserService);
  }

  @Test
  public void testHandleRegister_ServerBaoLoi_BaoLoiDo() {
    // GIVEN: Gõ đúng định dạng
    realUsernameField.setText("user_ton_tai");
    realPasswordField.setText("123456");
    realConfirmPasswordField.setText("123456");

    // Giả lập hệ thống mạng
    CompletableFuture<NetworkMessage> future = new CompletableFuture<>();
    when(mockUserService.register("user_ton_tai", "123456")).thenReturn(future);

    // WHEN
    controller.handleRegister(null);

    // Trả kết quả lỗi từ Server
    future.complete(new NetworkMessage("ERROR", "Tên đăng nhập đã tồn tại!"));

    // THEN
    assertEquals("Tên đăng nhập đã tồn tại!", realMessageLabel.getText());
    assertEquals(Color.RED, realMessageLabel.getTextFill());
  }

  @Test
  public void testHandleRegister_ThanhCong_ChuyenChuXanhVaXoaForm() {
    // GIVEN: Gõ hoàn hảo
    realUsernameField.setText("new_user");
    realPasswordField.setText("123456");
    realConfirmPasswordField.setText("123456");

    CompletableFuture<NetworkMessage> future = new CompletableFuture<>();
    when(mockUserService.register("new_user", "123456")).thenReturn(future);

    // WHEN
    controller.handleRegister(null);

    // Server trả kết quả Thành Công!
    future.complete(new NetworkMessage("REGISTER_SUCCESS", "Data"));

    // THEN: 1. Kiểm tra hiển thị màu XANH LÁ
    assertEquals("Đăng ký thành công! Đang chuyển về Đăng nhập...", realMessageLabel.getText());
    assertEquals(Color.GREEN, realMessageLabel.getTextFill());

    // THEN: 2. Kiểm tra form đã được reset trắng chưa
    assertTrue(realUsernameField.getText().isEmpty(), "Phải xóa trắng ô Username");
    assertTrue(realPasswordField.getText().isEmpty(), "Phải xóa trắng ô Password");
    assertTrue(realConfirmPasswordField.getText().isEmpty(), "Phải xóa trắng ô ConfirmPassword");

        /* Chú thích nhỏ:
           Luồng Thread.sleep(1500) ngầm sẽ tự chạy và kết thúc trong im lặng
           mà không đánh sập test nhờ khối try-catch an toàn của sếp.
        */
  }
  @Test
  public void testGoToLogin_ChuyenTrangThanhCong() throws Exception {
    // 1. Chặn lệnh load FXML thật
    try (MockedStatic<javafx.fxml.FXMLLoader> fxmlMock = mockStatic(javafx.fxml.FXMLLoader.class)) {

      // 🎯 ĐIỂM SỬA CHỐT HẠ Ở ĐÂY: Dùng một VBox thật thay vì dùng mock()
      // VBox kế thừa Parent, nên JavaFX sẽ đọc được StyleClass bình thường, không bị quăng lỗi Null!
      javafx.scene.Parent dummyRoot = new javafx.scene.layout.VBox();
      fxmlMock.when(() -> javafx.fxml.FXMLLoader.load(any(java.net.URL.class))).thenReturn(dummyRoot);

      // 2. Làm giả cú click chuột (ActionEvent) và cửa sổ (Stage)
      ActionEvent mockEvent = mock(ActionEvent.class);
      javafx.scene.Node mockNode = mock(javafx.scene.Node.class);
      javafx.scene.Scene mockScene = mock(javafx.scene.Scene.class);
      javafx.stage.Stage mockStage = mock(javafx.stage.Stage.class);

      // Chuỗi liên hoàn: Lấy Node -> Lấy Scene -> Lấy Window(Stage)
      when(mockEvent.getSource()).thenReturn(mockNode);
      when(mockNode.getScene()).thenReturn(mockScene);
      when(mockScene.getWindow()).thenReturn(mockStage);

      // 3. WHEN: Gọi hàm goToLogin
      controller.goToLogin(mockEvent);

      // 4. THEN: Xác minh cửa sổ Stage đã được set một Scene (chứa cái VBox) và gọi lệnh show()
      verify(mockStage).setScene(any(javafx.scene.Scene.class));
      verify(mockStage).show();
    }
  }

  // --- HÀM HỖ TRỢ (Reflection) ---
  private void injectPrivateField(Object target, String fieldName, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }
}