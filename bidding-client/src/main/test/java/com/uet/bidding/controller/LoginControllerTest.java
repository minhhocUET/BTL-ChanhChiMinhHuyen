package com.uet.bidding.controller;

import com.uet.bidding.model.Admin;
import com.uet.bidding.model.Customer;
import com.uet.bidding.model.NetworkMessage;
import com.uet.bidding.network.ClientService;
import com.uet.bidding.service.UserService;
import com.uet.bidding.util.UserSession;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
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

// Kích hoạt Engine JavaFX để cho phép dùng "new TextField()"
@ExtendWith(ApplicationExtension.class)
public class LoginControllerTest {

  private LoginController controller;

  // 🎯 SỬ DỤNG GIAO DIỆN THẬT (KHÔNG MOCK NỮA)
  private TextField realUsernameField;
  private PasswordField realPasswordField;
  private Label realMessageLabel;

  // Service bị làm giả
  private ClientService mockClientService;
  private UserService mockUserService;

  // Quản lý MockStatic
  private MockedStatic<Main> mainMock;
  private MockedStatic<UserSession> sessionMock;
  private MockedStatic<Platform> platformMock;

  @BeforeEach
  public void setUp() throws Exception {
    controller = new LoginController();

    // 1. Dùng UI THẬT thay vì mock để né lỗi bảo mật của Java 21
    realUsernameField = new TextField();
    realPasswordField = new PasswordField();
    realMessageLabel = new Label();

    injectPrivateField(controller, "usernameField", realUsernameField);
    injectPrivateField(controller, "passwordField", realPasswordField);
    injectPrivateField(controller, "messageLabel", realMessageLabel);

    // 2. Khởi tạo đối tượng giả cho Mạng & Service
    mockUserService = mock(UserService.class);
    injectPrivateField(controller, "userService", mockUserService);

    mockClientService = mock(ClientService.class);
    Field instanceField = ClientService.class.getDeclaredField("instance");
    instanceField.setAccessible(true);
    instanceField.set(null, mockClientService);

    // 3. ĐIỂM CHỐT HẠ: Ép Platform.runLater chạy code ngay lập tức trên luồng Test
    platformMock = mockStatic(Platform.class);
    platformMock.when(() -> Platform.runLater(any(Runnable.class)))
        .thenAnswer(invocation -> {
          Runnable runnable = invocation.getArgument(0);
          runnable.run(); // Chạy thẳng tắp không qua luồng Đồ họa
          return null;
        });

    mainMock = mockStatic(Main.class);
    sessionMock = mockStatic(UserSession.class);
  }

  @AfterEach
  public void tearDown() throws Exception {
    platformMock.close();
    mainMock.close();
    sessionMock.close();

    Field instanceField = ClientService.class.getDeclaredField("instance");
    instanceField.setAccessible(true);
    instanceField.set(null, null);
  }

  @Test
  public void testHandleLogin_ThieuThongTin_HienThiLoi() {
    // Gắn dữ liệu thật vào UI thật
    realUsernameField.setText("");
    realPasswordField.setText("");

    controller.handleLogin();

    // Đọc giá trị thật từ UI để kiểm tra
    assertEquals("Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu!", realMessageLabel.getText());
    assertTrue(realMessageLabel.getStyle().contains("red"));
    verifyNoInteractions(mockUserService); // Chắc chắn chưa gọi lên Server
  }

  @Test
  public void testHandleLogin_DangNhapThanhCong_ChuyenSangAdminDashboard() {
    realUsernameField.setText("admin");
    realPasswordField.setText("123");

    CompletableFuture<NetworkMessage> future = new CompletableFuture<>();
    when(mockUserService.login("admin", "123")).thenReturn(future);

    Admin mockAdmin = new Admin();
    when(mockClientService.parseUser(any())).thenReturn(mockAdmin);

    controller.handleLogin();
    future.complete(new NetworkMessage("LOGIN_SUCCESS", "Data"));

    assertEquals("Đang kiểm tra thông tin...", realMessageLabel.getText());
    sessionMock.verify(() -> UserSession.setCurrentUser(mockAdmin));
    mainMock.verify(() -> Main.changeScene("/AdminDashboard.fxml", "Admin Dashboard", 1100, 800));
  }

  @Test
  public void testHandleLogin_DangNhapThanhCong_ChuyenSangSanDauGiaCustomer() {
    realUsernameField.setText("khach");
    realPasswordField.setText("456");

    CompletableFuture<NetworkMessage> future = new CompletableFuture<>();
    when(mockUserService.login("khach", "456")).thenReturn(future);

    Customer mockCustomer = new Customer();
    when(mockClientService.parseUser(any())).thenReturn(mockCustomer);

    controller.handleLogin();
    future.complete(new NetworkMessage("LOGIN_SUCCESS", "Data"));

    sessionMock.verify(() -> UserSession.setCurrentUser(mockCustomer));
    mainMock.verify(() -> Main.changeScene("/AuctionList.fxml", "Sàn Đấu Giá", 1000, 700));
  }

  @Test
  public void testHandleLogin_SaiMatKhau_HienThiThongBaoTuServer() {
    realUsernameField.setText("user");
    realPasswordField.setText("sai");

    CompletableFuture<NetworkMessage> future = new CompletableFuture<>();
    when(mockUserService.login("user", "sai")).thenReturn(future);

    controller.handleLogin();
    future.complete(new NetworkMessage("ERROR", "Tài khoản hoặc mật khẩu không chính xác!"));

    assertEquals("Tài khoản hoặc mật khẩu không chính xác!", realMessageLabel.getText());
    assertTrue(realMessageLabel.getStyle().contains("red"));
    mainMock.verifyNoInteractions(); // Chắc chắn không hề nhảy màn hình
  }

  @Test
  public void testHandleLogin_MatKetNoiMayChu_HienThiLoi() {
    realUsernameField.setText("test");
    realPasswordField.setText("test");

    CompletableFuture<NetworkMessage> future = new CompletableFuture<>();
    when(mockUserService.login("test", "test")).thenReturn(future);

    controller.handleLogin();
    future.completeExceptionally(new RuntimeException("Mất kết nối"));

    assertEquals("Lỗi kết nối đến máy chủ!", realMessageLabel.getText());
    assertTrue(realMessageLabel.getStyle().contains("red"));
  }

  // --- HELPER METHOD ---
  private void injectPrivateField(Object target, String fieldName, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }
  @Test
  public void testHandleLogin_KhongXacDinhDuocUser_TraVeNull() {
    // GIVEN: Nhập đúng
    realUsernameField.setText("user");
    realPasswordField.setText("123");

    CompletableFuture<NetworkMessage> future = new CompletableFuture<>();
    when(mockUserService.login("user", "123")).thenReturn(future);

    // KỊCH BẢN: Server trả về thành công, nhưng bộ giải mã User bị lỗi sinh ra null
    when(mockClientService.parseUser(any())).thenReturn(null);

    // WHEN
    controller.handleLogin();
    future.complete(new NetworkMessage("LOGIN_SUCCESS", "Data bị hỏng"));

    // THEN: Phải nhảy vào nhánh else (user == null)
    assertEquals("Lỗi: Không thể xác định quyền người dùng!", realMessageLabel.getText());
    assertTrue(realMessageLabel.getStyle().contains("red"));
  }

  @Test
  public void testHandleLogin_ChuyenTrangLoi_VaoCatch() {
    realUsernameField.setText("admin");
    realPasswordField.setText("123");

    CompletableFuture<NetworkMessage> future = new CompletableFuture<>();
    when(mockUserService.login("admin", "123")).thenReturn(future);

    Admin mockAdmin = new Admin();
    when(mockClientService.parseUser(any())).thenReturn(mockAdmin);

    // KỊCH BẢN: Giả lập hàm chuyển trang (Main.changeScene) bị văng lỗi (ví dụ không tìm thấy FXML)
    mainMock.when(() -> Main.changeScene(anyString(), anyString(), anyInt(), anyInt()))
        .thenThrow(new RuntimeException("Lỗi giả lập từ Test"));

    // WHEN
    controller.handleLogin();
    future.complete(new NetworkMessage("LOGIN_SUCCESS", "Data"));

    // THEN: Phải bị khối try-catch bắt lại
    assertEquals("Lỗi giao diện: Không tìm thấy file FXML!", realMessageLabel.getText());
    assertTrue(realMessageLabel.getStyle().contains("red"));
  }

  @Test
  public void testGoToRegister_GoiHamChuyenTrangThanhCong() {
    // WHEN: Người dùng bấm vào nút chuyển sang màn hình Đăng ký
    controller.goToRegister();

    // THEN: Xác minh rằng hàm đổi màn hình của Main đã được gọi chính xác
    mainMock.verify(() -> Main.changeScene("/Register.fxml", "Hệ thống Đấu giá VNU - Đăng ký", 400, 500));
  }
}