package com.uet.bidding.service;

import com.uet.bidding.model.Auction;
import com.uet.bidding.model.Item;
import com.uet.bidding.model.NetworkMessage;
import com.uet.bidding.model.Seller;
import com.uet.bidding.network.ClientService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SellerServiceTest {

  private MockedStatic<ClientService> mockedClientService;
  private ClientService mockClientInstance;
  private SellerService sellerService;

  @BeforeEach
  void setUp() {
    // 1. Mock ClientService Singleton
    mockClientInstance = mock(ClientService.class);
    mockedClientService = Mockito.mockStatic(ClientService.class);
    mockedClientService.when(ClientService::getInstance).thenReturn(mockClientInstance);

    // 2. Khởi tạo Service cần test
    sellerService = new SellerService();
  }

  @AfterEach
  void tearDown() {
    // Luôn đóng MockedStatic để tránh leak memory giữa các class test
    mockedClientService.close();
  }

  // ==========================================
  // TEST: CHỨC NĂNG 1 - TẠO ĐẤU GIÁ
  // ==========================================
  @Test
  void testCreateAuctionAsync() {
    int itemId = 101;
    BigDecimal startPrice = new BigDecimal("500000.50");
    int durationMins = 120;
    BigDecimal bidIncrement = new BigDecimal("50000");

    // Giả lập request thành công
    when(mockClientInstance.sendRequest(eq("CREATE_AUCTION"), anyString()))
        .thenReturn(CompletableFuture.completedFuture(new NetworkMessage("SUCCESS", null)));

    sellerService.createAuctionAsync(itemId, startPrice, durationMins, bidIncrement);

    // Xác minh chuỗi nối (String concatenation) hoạt động chính xác
    String expectedData = "101 500000.50 120 50000";
    verify(mockClientInstance).sendRequest("CREATE_AUCTION", expectedData);
  }

  // ==========================================
  // TEST: CHỨC NĂNG 2 - LÀM SẠCH DANH SÁCH (LOGIC PHỨC TẠP NHẤT)
  // ==========================================
  @Test
  void testRefreshAuctionLists() {
    // Chuẩn bị Mock Objects
    Seller mockSeller = mock(Seller.class);
    Auction runningAuction = mock(Auction.class);
    Auction finishedAuction = mock(Auction.class);
    Item mockItem = mock(Item.class);

    // Giả lập danh sách ban đầu của Seller có 1 phiên đang chạy, 1 phiên đã xong
    List<Auction> activeAuctions = new ArrayList<>();
    activeAuctions.add(runningAuction);
    activeAuctions.add(finishedAuction);

    List<Auction> finishedAuctions = new ArrayList<>();

    when(mockSeller.getActiveAuctions()).thenReturn(activeAuctions);
    when(mockSeller.getFinishedAuctions()).thenReturn(finishedAuctions);

    // Giả lập hành vi của các phiên sau khi hàm refreshStatus() được gọi
    when(runningAuction.getStatus()).thenReturn("RUNNING");
    when(finishedAuction.getStatus()).thenReturn("FINISHED");
    when(finishedAuction.getItem()).thenReturn(mockItem);

    // Thực thi hàm cần test
    sellerService.refreshAuctionLists(mockSeller);

    // 1. Xác minh tất cả các phiên đều được gọi hàm refresh
    verify(runningAuction).refreshStatus();
    verify(finishedAuction).refreshStatus();

    // 2. Xác minh sản phẩm của phiên kết thúc đã được giải phóng
    verify(mockItem).setInAuction(false);

    // 3. Xác minh danh sách đã được hoán đổi đúng chuẩn
    assertEquals(1, activeAuctions.size());
    assertTrue(activeAuctions.contains(runningAuction), "Phiên RUNNING phải được giữ lại");

    assertEquals(1, finishedAuctions.size());
    assertTrue(finishedAuctions.contains(finishedAuction), "Phiên FINISHED phải được đẩy sang list kết thúc");
  }

  // ==========================================
  // TEST: CHỨC NĂNG 3 - RATING NÂNG CAO
  // ==========================================
  @Test
  @SuppressWarnings("unchecked")
  void testSubmitReview() {
    int auctionId = 15;
    int sellerId = 22;
    int stars = 5;
    String comment = "Sản phẩm đóng gói rất cẩn thận!";

    sellerService.submitReview(auctionId, sellerId, stars, comment);

    // Dùng ArgumentCaptor để "tóm" gọn cái Map mà hàm submitReview vừa tạo ra
    ArgumentCaptor<Map<String, Object>> mapCaptor = ArgumentCaptor.forClass(Map.class);
    verify(mockClientInstance).sendRequest(eq("ADD_REVIEW"), mapCaptor.capture());

    Map<String, Object> capturedPayload = mapCaptor.getValue();

    // Xác minh toàn bộ dữ liệu nhét vào Map có chính xác không
    assertEquals(15, capturedPayload.get("auctionId"));
    assertEquals(22, capturedPayload.get("sellerId"));
    assertEquals(5, capturedPayload.get("stars"));
    assertEquals("Sản phẩm đóng gói rất cẩn thận!", capturedPayload.get("comment"));
  }
}