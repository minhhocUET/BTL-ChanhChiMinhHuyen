package com.uet.bidding.service;

import com.uet.bidding.model.Customer;
import com.uet.bidding.model.NetworkMessage;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AutoBidServiceTest {

  private MockedStatic<ClientService> mockedClientService;
  private MockedStatic<UserSession> mockedUserSession;
  private ClientService mockClientInstance;
  private AutoBidService autoBidService;

  @BeforeEach
  void setUp() {
    // 1. Mock ClientService Singleton TRƯỚC KHI khởi tạo Service
    mockClientInstance = mock(ClientService.class);
    mockedClientService = Mockito.mockStatic(ClientService.class);
    mockedClientService.when(ClientService::getInstance).thenReturn(mockClientInstance);

    // 2. Khởi tạo đối tượng cần test
    autoBidService = new AutoBidService();

    // 3. Chuẩn bị Mock cho UserSession
    mockedUserSession = Mockito.mockStatic(UserSession.class);

    // Mặc định giả lập ClientService gửi mạng luôn thành công
    when(mockClientInstance.sendRequest(anyString(), any()))
        .thenReturn(CompletableFuture.completedFuture(new NetworkMessage("SUCCESS", null)));
  }

  @AfterEach
  void tearDown() {
    // Giải phóng tài nguyên MockedStatic để không ảnh hưởng đến các class test khác
    mockedClientService.close();
    mockedUserSession.close();
  }

  // ==========================================
  // TESTS CHO HÀM ENABLE (Bật Auto-bid)
  // ==========================================

  @Test
  void testEnable_NotLoggedIn() {
    // Giả lập trạng thái chưa đăng nhập (getLoggedInCustomer trả về null)
    mockedUserSession.when(UserSession::getLoggedInCustomer).thenReturn(null);

    CompletableFuture<NetworkMessage> future = autoBidService.enable(101, new BigDecimal("50000"));

    // Xác nhận CompletableFuture trả về trạng thái thất bại (Failed)
    assertTrue(future.isCompletedExceptionally());

    // Xác nhận chính xác thông báo lỗi văng ra
    Exception exception = assertThrows(ExecutionException.class, future::get);
    assertTrue(exception.getCause().getMessage().contains("Đăng nhập trước!"));
  }

  @Test
  void testEnable_Success() {
    // Giả lập trạng thái đã đăng nhập
    Customer mockCustomer = mock(Customer.class);
    mockedUserSession.when(UserSession::getLoggedInCustomer).thenReturn(mockCustomer);

    // Thực thi hàm với auctionId = 5, maxBid = 15000.50
    autoBidService.enable(5, new BigDecimal("15000.50"));

    // Xác minh ClientService đã được gọi với cú pháp nối chuỗi chính xác
    verify(mockClientInstance).sendRequest("SET_AUTO_BID", "5 15000.50");
  }

  // ==========================================
  // TESTS CHO CÁC HÀM DELEGATE ĐƠN GIẢN
  // ==========================================

  @Test
  void testDisable() {
    // Thực thi
    autoBidService.disable(99);

    // Xác minh xem số Integer có được ép về String chuẩn xác không
    verify(mockClientInstance).sendRequest("REMOVE_AUTO_BID", "99");
  }

  @Test
  void testCheckStatus() {
    // Thực thi
    autoBidService.checkStatus(42);

    // Xác minh Server được gọi với Object Integer (không phải String)
    verify(mockClientInstance).sendRequest("CHECK_AUTO_BID", 42);
  }
}