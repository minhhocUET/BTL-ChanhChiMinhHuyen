package com.uet.bidding.service;

import com.uet.bidding.model.Customer;
import com.uet.bidding.model.NetworkMessage;
import com.uet.bidding.model.User;
import com.uet.bidding.network.ClientService;
import com.uet.bidding.util.UserSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class UserServiceTest {

  private MockedStatic<ClientService> mockedClientService;
  private MockedStatic<UserSession> mockedUserSession;
  private ClientService mockClientInstance;
  private UserService userService;

  @BeforeEach
  void setUp() {
    // 1. Mock ClientService
    mockClientInstance = mock(ClientService.class);
    mockedClientService = Mockito.mockStatic(ClientService.class);
    mockedClientService.when(ClientService::getInstance).thenReturn(mockClientInstance);

    // 2. Mock UserSession
    mockedUserSession = Mockito.mockStatic(UserSession.class);

    // 3. Init Service
    userService = new UserService();

    // Mặc định cho ClientService trả về thành công
    when(mockClientInstance.sendRequest(anyString(), any()))
        .thenReturn(CompletableFuture.completedFuture(new NetworkMessage("SUCCESS", null)));
  }

  @AfterEach
  void tearDown() {
    mockedClientService.close();
    mockedUserSession.close();
  }

  // ==========================================
  // TESTS CHO ĐĂNG NHẬP (LOGIN)
  // ==========================================

  @Test
  void testLogin_NullParams() throws ExecutionException, InterruptedException {
    // Test khi username null
    CompletableFuture<NetworkMessage> future1 = userService.login(null, "password");
    assertNull(future1.get(), "Nên trả về null nếu username là null");

    // Test khi password null
    CompletableFuture<NetworkMessage> future2 = userService.login("admin", null);
    assertNull(future2.get(), "Nên trả về null nếu password là null");

    // Verify rằng sendRequest chưa bao giờ được gọi
    verify(mockClientInstance, never()).sendRequest(anyString(), any());
  }

  @Test
  void testLogin_Success() {
    // Test login với chuỗi có dấu cách ở đầu/cuối (trim behavior)
    userService.login("  admin  ", "123456");

    // Xác minh gọi đúng lệnh và chuỗi được trim cẩn thận
    verify(mockClientInstance).sendRequest("LOGIN", "admin 123456");
  }

  // ==========================================
  // TESTS CHO ĐĂNG KÝ (REGISTER)
  // ==========================================

  @Test
  void testRegister_NullParams() throws ExecutionException, InterruptedException {
    CompletableFuture<NetworkMessage> future = userService.register(null, null);
    assertNull(future.get());
    verify(mockClientInstance, never()).sendRequest(anyString(), any());
  }

  @Test
  void testRegister_Success() {
    userService.register("newuser", "securePass");
    verify(mockClientInstance).sendRequest("REGISTER", "newuser securePass");
  }

  // ==========================================
  // TESTS CHO NẠP TIỀN (ADD BALANCE)
  // ==========================================

  @Test
  void testAddBalance_InvalidAmounts() {
    // Test null
    userService.addBalance(null);
    // Test giá trị 0
    userService.addBalance(BigDecimal.ZERO);
    // Test số âm
    userService.addBalance(new BigDecimal("-50000"));

    // Tất cả trường hợp trên đều KHÔNG được phép gửi request lên mạng
    verify(mockClientInstance, never()).sendRequest(anyString(), any());
  }

  @Test
  void testAddBalance_Success() {
    BigDecimal amount = new BigDecimal("100000");
    userService.addBalance(amount);

    verify(mockClientInstance).sendRequest("ADD_BALANCE", amount);
  }

  // ==========================================
  // TESTS CHO UPDATE USER (PHÂN NHÁNH INSTANCEOF)
  // ==========================================

  @Test
  void testUpdateUser_NullUser() {
    userService.updateUser(null);
    verify(mockClientInstance, never()).sendRequest(anyString(), any());
  }

  @Test
  void testUpdateUser_IsCustomer_CompleteProfile() {
    // Mock một đối tượng Customer (Subclass của User)
    Customer mockCustomer = mock(Customer.class);

    // Giả lập profile đã điền đủ thông tin
    when(mockCustomer.hasCompleteProfile()).thenReturn(true);

    userService.updateUser(mockCustomer);

    // Xác minh rằng hàm setProfileComplete(true) ĐÃ được gọi
    verify(mockCustomer).setProfileComplete(true);
    // Xác minh gửi request
    verify(mockClientInstance).sendRequest("UPDATE_PROFILE", mockCustomer);
  }

  @Test
  void testUpdateUser_IsCustomer_IncompleteProfile() {
    Customer mockCustomer = mock(Customer.class);

    // Giả lập profile còn thiếu thông tin
    when(mockCustomer.hasCompleteProfile()).thenReturn(false);

    userService.updateUser(mockCustomer);

    // Xác minh hàm setProfileComplete(true) KHÔNG được gọi
    verify(mockCustomer, never()).setProfileComplete(anyBoolean());
    // Nhưng vẫn phải gửi request cập nhật lên server
    verify(mockClientInstance).sendRequest("UPDATE_PROFILE", mockCustomer);
  }

  @Test
  void testUpdateUser_NotCustomer() {
    // Mock một class ẩn danh kế thừa thẳng từ User (không phải Customer)
    User mockStandardUser = mock(User.class);

    userService.updateUser(mockStandardUser);

    // Gửi thẳng request mà không qua logic check profile của Customer
    verify(mockClientInstance).sendRequest("UPDATE_PROFILE", mockStandardUser);
  }

  // ==========================================
  // TESTS CHO ĐĂNG XUẤT (LOGOUT)
  // ==========================================

  @Test
  void testLogout() {
    userService.logout();

    // 1. Phải gửi tín hiệu LOGOUT lên server
    verify(mockClientInstance).sendRequest("LOGOUT", "");

    // 2. Phải gọi hàm static clear() của UserSession để xóa RAM local
    mockedUserSession.verify(UserSession::clear, times(1));
  }
}