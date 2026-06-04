package com.uet.bidding.network;

import com.google.gson.Gson;
import com.uet.bidding.controller.auction.AuctionListController;
import com.uet.bidding.controller.main.Main;
import com.uet.bidding.controller.auction.ProductDetailController;
import com.uet.bidding.controller.admin.AdminItemManagementController;
import com.uet.bidding.controller.admin.AdminUserManagementController;
import com.uet.bidding.model.*;
import com.uet.bidding.util.UserSession;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ClientServiceTest {

  private MockedStatic<Platform> mockedPlatform;
  private MockedStatic<UserSession> mockedUserSession;
  private MockedStatic<Main> mockedMain;

  // Mocks cho các Controller để pass qua các câu lệnh if(Controller.getInstance() != null)
  private MockedStatic<AdminItemManagementController> mockedAdminItemCtrl;
  private MockedStatic<AuctionListController> mockedAuctionListCtrl;
  private MockedStatic<ProductDetailController> mockedProductDetailCtrl;

  private ClientService clientService;
  private PrintWriter mockOut;
  private BufferedReader mockIn;
  private Socket mockSocket;

  @BeforeEach
  void setUp() throws Exception {
    // 1. Reset Singleton instance
    Field instanceField = ClientService.class.getDeclaredField("instance");
    instanceField.setAccessible(true);
    instanceField.set(null, null);

    // 🌟 THAY ĐỔI Ở ĐÂY: Dùng SPY thay vì lấy instance trực tiếp
    clientService = Mockito.spy(ClientService.getInstance());

    // Gắn ngược Spy vào lại Singleton để các lớp khác cũng dùng bản Spy này
    instanceField.set(null, clientService);

    // 🌟 CHẶN HÀM SHOW ALERT NẾU ĐƯỢC GỌI
    Mockito.doNothing().when(clientService).showAlert(anyString(), anyString(), any());

    // 2. MOCK JAVAFX PLATFORM
    mockedPlatform = mockStatic(Platform.class);
    mockedPlatform.when(() -> Platform.runLater(any(Runnable.class))).thenAnswer(invocation -> {
      Runnable r = invocation.getArgument(0);
      r.run();
      return null;
    });

    // 3. Mock UserSession & Main
    mockedUserSession = mockStatic(UserSession.class);
    mockedMain = mockStatic(Main.class);

    // 4. Bơm I/O Streams giả vào ClientService
    mockSocket = mock(Socket.class);
    mockOut = mock(PrintWriter.class);
    mockIn = mock(BufferedReader.class);

    Field socketField = ClientService.class.getDeclaredField("socket");
    socketField.setAccessible(true);
    socketField.set(clientService, mockSocket);

    Field outField = ClientService.class.getDeclaredField("out");
    outField.setAccessible(true);
    outField.set(clientService, mockOut);

    Field inField = ClientService.class.getDeclaredField("in");
    inField.setAccessible(true);
    inField.set(clientService, mockIn);

    Field isRunningField = ClientService.class.getDeclaredField("isRunning");
    isRunningField.setAccessible(true);
    isRunningField.setBoolean(clientService, true);
  }

  @AfterEach
  void tearDown() {
    mockedPlatform.close();
    mockedUserSession.close();
    mockedMain.close();

    if (mockedAdminItemCtrl != null) mockedAdminItemCtrl.close();
    if (mockedAuctionListCtrl != null) mockedAuctionListCtrl.close();
    if (mockedProductDetailCtrl != null) mockedProductDetailCtrl.close();
  }

  // ==========================================
  // TEST: KẾT NỐI VÀ NGẮT KẾT NỐI
  // ==========================================

  @Test
  void testDisconnect() throws IOException {
    clientService.disconnect();
    verify(mockIn).close();
    verify(mockOut).close();
    verify(mockSocket).close();
  }

  // ==========================================
  // TEST: GỬI REQUEST
  // ==========================================

  @Test
  void testSendRequest_Success() throws Exception {
    CompletableFuture<NetworkMessage> future = clientService.sendRequest("PING", "Data");

    // Kiểm tra Request được đẩy vào out
    verify(mockOut).println(anyString());

    // Kiểm tra pendingRequests đã lưu future
    Field pendingField = ClientService.class.getDeclaredField("pendingRequests");
    pendingField.setAccessible(true);
    Map<?, ?> pendingMap = (Map<?, ?>) pendingField.get(clientService);
    assertEquals(1, pendingMap.size());
  }

  @Test
  void testSendRequest_NoConnection() throws Exception {
    // Cố tình làm mất kết nối
    Field outField = ClientService.class.getDeclaredField("out");
    outField.setAccessible(true);
    outField.set(clientService, null);

    CompletableFuture<NetworkMessage> future = clientService.sendRequest("PING", "Data");
    assertTrue(future.isCompletedExceptionally());
  }

  // ==========================================
  // TEST: PARSE USER (Đa hình JSON)
  // ==========================================

  @Test
  void testParseUser() {
    Gson gson = clientService.getGson();

    // 1. Test Admin có trường role
    String adminJson = "{\"role\":\"ADMIN\", \"username\":\"admin1\"}";
    User parsedAdmin = clientService.parseUser(gson.fromJson(adminJson, Object.class));
    assertTrue(parsedAdmin instanceof Admin);

    // 2. Test Customer có trường role
    String customerJson = "{\"role\":\"CUSTOMER\", \"balance\":500}";
    User parsedCustomer = clientService.parseUser(gson.fromJson(customerJson, Object.class));
    assertTrue(parsedCustomer instanceof Customer);

    // 3. Test nhận dạng qua đặc điểm (fallback)
    String fallbackCustomer = "{\"balance\":1000}";
    User fallbackUser = clientService.parseUser(gson.fromJson(fallbackCustomer, Object.class));
    assertTrue(fallbackUser instanceof Customer);

    // 4. Test dữ liệu rác
    assertNull(clientService.parseUser(null));
  }

  // ==========================================
  // TEST: XỬ LÝ RESPONSE BẰNG REFLECTION
  // ==========================================
  // Bắn trực tiếp vào hàm private handleResponse để test mọi rẽ nhánh

  private void invokeHandleResponse(NetworkMessage msg) throws Exception {
    Method method = ClientService.class.getDeclaredMethod("handleResponse", NetworkMessage.class);
    method.setAccessible(true);
    method.invoke(clientService, msg);
  }

  @Test
  void testHandleResponse_LoginAdmin() throws Exception {
    String adminJson = "{\"role\":\"ADMIN\", \"username\":\"superadmin\"}";
    NetworkMessage msg = new NetworkMessage("LOGIN_SUCCESS", clientService.getGson().fromJson(adminJson, Object.class));

    invokeHandleResponse(msg);

    // Xác nhận đã lưu session và chuyển trang Admin
    mockedUserSession.verify(() -> UserSession.setCurrentUser(any(Admin.class)));
    mockedMain.verify(() -> Main.changeScene(eq("/AdminDashboard.fxml"), anyString(), anyInt(), anyInt()));
  }

  @Test
  void testHandleResponse_UpdateBalance() throws Exception {
    String customerJson = "{\"role\":\"CUSTOMER\", \"balance\":150000}";
    NetworkMessage msg = new NetworkMessage("UPDATE_BALANCE_SUCCESS", clientService.getGson().fromJson(customerJson, Object.class));

    // Mock ProductDetailController để test luồng refresh UI
    mockedProductDetailCtrl = mockStatic(ProductDetailController.class);
    ProductDetailController mockCtrl = mock(ProductDetailController.class);
    mockedProductDetailCtrl.when(ProductDetailController::getInstance).thenReturn(mockCtrl);

    invokeHandleResponse(msg);

    mockedUserSession.verify(() -> UserSession.setCurrentUser(any(Customer.class)));
    verify(mockCtrl).refreshWalletBalanceLabel();
  }

  @Test
  void testHandleResponse_ServerBroadcastNewItem() throws Exception {
    String itemJson = "{\"itemType\":\"ELECTRONICS\", \"name\":\"Laptop ASUS\"}";
    NetworkMessage msg = new NetworkMessage("SERVER_BROADCAST_NEW_ITEM", clientService.getGson().fromJson(itemJson, Object.class));

    // Mock AdminItemManagementController
    mockedAdminItemCtrl = mockStatic(AdminItemManagementController.class);
    AdminItemManagementController mockCtrl = mock(AdminItemManagementController.class);
    mockedAdminItemCtrl.when(AdminItemManagementController::getInstance).thenReturn(mockCtrl);

    invokeHandleResponse(msg);

    // Xác minh item đã được parse thành Electronics và ném cho Controller
    verify(mockCtrl).addPendingItemRealtime(any(Electronics.class));
  }

  @Test
  void testHandleResponse_AuctionUpdated() throws Exception {
    // Giả lập số nguyên bị chuyển thành double (lỗi Gson kinh điển)
    String auctionJson = "{\"id\": 99.0, \"registeredCount\": 5.0}";
    NetworkMessage msg = new NetworkMessage("AUCTION_UPDATED", clientService.getGson().fromJson(auctionJson, Object.class));

    // Mock AuctionListController
    mockedAuctionListCtrl = mockStatic(AuctionListController.class);
    AuctionListController mockCtrl = mock(AuctionListController.class);
    mockedAuctionListCtrl.when(AuctionListController::getInstance).thenReturn(mockCtrl);

    invokeHandleResponse(msg);

    // Đảm bảo hàm xử lý broadcast được gọi
    verify(mockCtrl).handleAuctionBroadcast(any(Auction.class));
  }

  // ==========================================
  // TESTS CHO CÁC CASE BỔ SUNG (PROFILE, AVATAR, NEW_USER, ERROR)
  // ==========================================

  @Test
  void testHandleResponse_UpdateProfileSuccess() throws Exception {
    // Chuẩn bị dữ liệu JSON giả lập
    String userJson = "{\"role\":\"CUSTOMER\", \"username\":\"testuser\"}";
    NetworkMessage msg = new NetworkMessage("UPDATE_PROFILE_SUCCESS", clientService.getGson().fromJson(userJson, Object.class));

    // Gọi hàm (Sử dụng Reflection invoke đã tạo ở trên)
    invokeHandleResponse(msg);

    // 1. Xác minh UserSession đã được cập nhật bản User mới
    mockedUserSession.verify(() -> UserSession.setCurrentUser(any(User.class)));

    // 2. Xác minh Alert Thành Công đã được gọi chính xác (Spy chặn Alert bung lên màn hình)
    verify(clientService).showAlert(
        eq("Thành công"),
        eq("Hồ sơ của bạn đã được cập nhật đầy đủ lên hệ thống!"),
        eq(Alert.AlertType.INFORMATION)
    );
  }

  @Test
  void testHandleResponse_UpdateAvatarSuccess() throws Exception {
    String userJson = "{\"role\":\"CUSTOMER\", \"username\":\"avataruser\"}";
    NetworkMessage msg = new NetworkMessage("UPDATE_AVATAR_SUCCESS", clientService.getGson().fromJson(userJson, Object.class));

    invokeHandleResponse(msg);

    // Xác minh chỉ update Session, không có Alert nào được gọi
    mockedUserSession.verify(() -> UserSession.setCurrentUser(any(User.class)));
    verify(clientService, never()).showAlert(anyString(), anyString(), any());
  }

  @Test
  void testHandleResponse_NewUserRegistered() throws Exception {
    NetworkMessage msg = new NetworkMessage("NEW_USER_REGISTERED", null);

    // Mock AdminUserManagementController (Chỉ tồn tại trong phạm vi bài Test này)
    try (MockedStatic<AdminUserManagementController> mockedCtrl = mockStatic(AdminUserManagementController.class)) {
      AdminUserManagementController mockInstance = mock(AdminUserManagementController.class);
      mockedCtrl.when(AdminUserManagementController::getInstance).thenReturn(mockInstance);

      invokeHandleResponse(msg);

      // Xác minh Platform.runLater đã ép lệnh tải lại danh sách chạy thành công
      verify(mockInstance).loadUsersFromServer();
    }
  }

  @Test
  void testHandleResponse_Error() throws Exception {
    NetworkMessage msg = new NetworkMessage("ERROR", "Tài khoản đã tồn tại hoặc sai mật khẩu!");

    invokeHandleResponse(msg);

    // Xác minh showAlert Error đã được gọi với đúng nội dung truyền từ Server
    verify(clientService).showAlert(
        eq("Thất bại"),
        eq("Tài khoản đã tồn tại hoặc sai mật khẩu!"),
        eq(Alert.AlertType.ERROR)
    );
  }
}