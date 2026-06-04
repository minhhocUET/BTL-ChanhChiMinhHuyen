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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BidderServiceTest {

  private MockedStatic<ClientService> mockedClientService;
  private MockedStatic<UserSession> mockedUserSession;
  private ClientService mockClientInstance;
  private BidderService bidderService;
  private Customer mockCustomer;

  @BeforeEach
  void setUp() {
    // 1. Mock ClientService Singleton TRƯỚC KHI khởi tạo BidderService
    mockClientInstance = mock(ClientService.class);
    mockedClientService = Mockito.mockStatic(ClientService.class);
    mockedClientService.when(ClientService::getInstance).thenReturn(mockClientInstance);

    // 2. Khởi tạo Service cần test
    bidderService = new BidderService();

    // 3. Chuẩn bị Mock cho UserSession và Customer
    mockedUserSession = Mockito.mockStatic(UserSession.class);
    mockCustomer = mock(Customer.class);

    // Mặc định cho ClientService trả về một CompletableFuture thành công
    when(mockClientInstance.sendRequest(anyString(), any())).thenReturn(CompletableFuture.completedFuture(new NetworkMessage("SUCCESS", null)));
  }

  @AfterEach
  void tearDown() {
    // Phải đóng MockedStatic sau mỗi test để tránh lỗi memory leak hoặc conflict giữa các test
    mockedClientService.close();
    mockedUserSession.close();
  }

  @Test
  void testFetchAllAuctions() {
    bidderService.fetchAllAuctions();
    verify(mockClientInstance).sendRequest("GET_ALL_AUCTIONS", "");
  }

  @Test
  void testFetchAuctionsByCity_NullCity() throws ExecutionException, InterruptedException {
    CompletableFuture<NetworkMessage> future = bidderService.fetchAuctionsByCity(null);
    assertNull(future.get());
  }

  @Test
  void testFetchAuctionsByCity_ValidCity() {
    bidderService.fetchAuctionsByCity("  Hà Nội  ");
    verify(mockClientInstance).sendRequest("GET_BY_CITY", "Hà Nội");
  }

  // ==========================================
  // TESTS CHO PLACE BID
  // ==========================================

  @Test
  void testPlaceBid_NotLoggedIn() {
    mockedUserSession.when(UserSession::getLoggedInCustomer).thenReturn(null);

    CompletableFuture<NetworkMessage> future = bidderService.placeBid(1, new BigDecimal("1000"));
    assertTrue(future.isCompletedExceptionally());

    Exception exception = assertThrows(ExecutionException.class, future::get);
    assertTrue(exception.getCause().getMessage().contains("Vui lòng đăng nhập!"));
  }

  @Test
  void testPlaceBid_IncompleteProfile() {
    mockedUserSession.when(UserSession::getLoggedInCustomer).thenReturn(mockCustomer);
    when(mockCustomer.hasCompleteProfile()).thenReturn(false);

    CompletableFuture<NetworkMessage> future = bidderService.placeBid(1, new BigDecimal("1000"));
    assertTrue(future.isCompletedExceptionally());

    Exception exception = assertThrows(ExecutionException.class, future::get);
    assertTrue(exception.getCause().getMessage().contains("Hoàn thiện hồ sơ trước khi đặt giá!"));
  }

  @Test
  void testPlaceBid_Success() {
    mockedUserSession.when(UserSession::getLoggedInCustomer).thenReturn(mockCustomer);
    when(mockCustomer.hasCompleteProfile()).thenReturn(true);

    bidderService.placeBid(5, new BigDecimal("50000.50"));
    verify(mockClientInstance).sendRequest("BID", "5 50000.50");
  }

  // ==========================================
  // TESTS CHO REGISTER FOR AUCTION
  // ==========================================

  @Test
  void testRegisterForAuction_NotLoggedIn() {
    mockedUserSession.when(UserSession::getLoggedInCustomer).thenReturn(null);

    CompletableFuture<NetworkMessage> future = bidderService.registerForAuction(1);
    assertTrue(future.isCompletedExceptionally());
  }

  @Test
  void testRegisterForAuction_IncompleteProfile() {
    mockedUserSession.when(UserSession::getLoggedInCustomer).thenReturn(mockCustomer);
    when(mockCustomer.hasCompleteProfile()).thenReturn(false);

    CompletableFuture<NetworkMessage> future = bidderService.registerForAuction(1);
    assertTrue(future.isCompletedExceptionally());
  }

  @Test
  void testRegisterForAuction_Success() {
    mockedUserSession.when(UserSession::getLoggedInCustomer).thenReturn(mockCustomer);
    when(mockCustomer.hasCompleteProfile()).thenReturn(true);

    bidderService.registerForAuction(10);
    verify(mockClientInstance).sendRequest("REGISTER_FOR_AUCTION", 10);
  }

  // ==========================================
  // TESTS CHO CÁC HÀM DELEGATE ĐƠN GIẢN
  // ==========================================

  @Test
  void testCheckRegistration() {
    bidderService.checkRegistration(99);
    verify(mockClientInstance).sendRequest("IS_REGISTERED_FOR_AUCTION", 99);
  }

  @Test
  void testLoadMyRegistrations_NotLoggedIn() {
    mockedUserSession.when(UserSession::getLoggedInCustomer).thenReturn(null);
    assertTrue(bidderService.loadMyRegistrations().isCompletedExceptionally());
  }

  @Test
  void testLoadMyRegistrations_Success() {
    mockedUserSession.when(UserSession::getLoggedInCustomer).thenReturn(mockCustomer);
    when(mockCustomer.getId()).thenReturn(123);

    bidderService.loadMyRegistrations();
    verify(mockClientInstance).sendRequest("GET_MY_REGISTRATIONS", 123);
  }

  @Test
  void testLoadActiveAuctions_NotLoggedIn() {
    mockedUserSession.when(UserSession::getLoggedInCustomer).thenReturn(null);
    assertTrue(bidderService.loadActiveAuctions().isCompletedExceptionally());
  }

  @Test
  void testLoadActiveAuctions_Success() {
    mockedUserSession.when(UserSession::getLoggedInCustomer).thenReturn(mockCustomer);
    when(mockCustomer.getId()).thenReturn(456);

    bidderService.loadActiveAuctions();
    verify(mockClientInstance).sendRequest("GET_BIDDER_ACTIVE_AUCTIONS", 456);
  }

  @Test
  void testLoadBidderHistory_NotLoggedIn() {
    mockedUserSession.when(UserSession::getLoggedInCustomer).thenReturn(null);
    assertTrue(bidderService.loadBidderHistory().isCompletedExceptionally());
  }

  @Test
  void testLoadBidderHistory_Success() {
    mockedUserSession.when(UserSession::getLoggedInCustomer).thenReturn(mockCustomer);
    when(mockCustomer.getId()).thenReturn(789);

    bidderService.loadBidderHistory();
    verify(mockClientInstance).sendRequest("GET_BIDDER_HISTORY", 789);
  }

  @Test
  void testLoadReviewsForSeller() {
    bidderService.loadReviewsForSeller(55);
    verify(mockClientInstance).sendRequest("GET_REVIEWS_BY_SELLER", 55);
  }
}