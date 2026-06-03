package com.uet.bidding.controller.admin;

import com.google.gson.JsonObject;
import com.uet.bidding.network.ClientService;
import com.uet.bidding.model.NetworkMessage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.lang.reflect.Field;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(ApplicationExtension.class)
public class AdminOverviewControllerTest {

  private AdminOverviewController controller;
  private ClientService mockClientService;
  private CompletableFuture<NetworkMessage> networkFuture;

  @BeforeEach
  public void setUp() throws Exception {
    // 1. Khởi tạo đối tượng giả lập
    mockClientService = mock(ClientService.class);
    networkFuture = new CompletableFuture<>();

    // Chặn hàm sendRequest
    when(mockClientService.sendRequest(anyString(), any())).thenReturn(networkFuture);

    // Hỗ trợ thêm cho hàm getGson() nếu Controller của bạn gọi đến nó
    when(mockClientService.getGson()).thenReturn(new com.google.gson.Gson());

    // 🌟 BÍ QUYẾT TẠI ĐÂY: Dùng Reflection ép ClientService sử dụng đối tượng mock
    // Cách này hoạt động trên mọi Thread, giải quyết triệt để lỗi Thread-Local của JavaFX
    Field instanceField = ClientService.class.getDeclaredField("instance");
    instanceField.setAccessible(true);
    instanceField.set(null, mockClientService);
  }

  @AfterEach
  public void tearDown() throws Exception {
    // Dọn dẹp lại ClientService về null để không làm hỏng các file test khác
    Field instanceField = ClientService.class.getDeclaredField("instance");
    instanceField.setAccessible(true);
    instanceField.set(null, null);
  }

  @Start
  public void start(Stage stage) throws Exception {
    // Tải giao diện lên luồng UI
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/AdminOverview.fxml"));
    Parent root = loader.load();
    controller = loader.getController();

    stage.setScene(new Scene(root));
    stage.show();
  }

  @Test
  public void testInitialize_KhiChuaCoDuLieuMang_HienThiDauBaCham() {
    assertEquals("...", controller.getTxtTotalUsers().getText());
    assertEquals("...", controller.getTxtActiveAuctions().getText());
    assertEquals("...", controller.getTxtPendingItems().getText());
  }

  @Test
  public void testRequestStatsFromServer_KhiServerTraVeThanhCong_CapNhatUIMuorMa() {
    // GIVEN: Tạo dữ liệu giả
    JsonObject dummyStats = new JsonObject();
    dummyStats.addProperty("totalUsers", 150);
    dummyStats.addProperty("activeAuctions", 45);
    dummyStats.addProperty("pendingItems", 12);

    NetworkMessage mockResponse = mock(NetworkMessage.class);
    when(mockResponse.getType()).thenReturn("GET_SYSTEM_STATS_SUCCESS");
    when(mockResponse.getData()).thenReturn(dummyStats);

    // WHEN: Gửi kết quả hoàn thành vào Future
    networkFuture.complete(mockResponse);

    // Đợi JavaFX Thread cập nhật UI xong
    WaitForAsyncUtils.waitForFxEvents();

    // THEN: Kiểm tra hiển thị
    assertEquals("150", controller.getTxtTotalUsers().getText(), "Tổng số user hiển thị sai!");
    assertEquals("45", controller.getTxtActiveAuctions().getText(), "Số cuộc đấu giá hiển thị sai!");
    assertEquals("12", controller.getTxtPendingItems().getText(), "Số sản phẩm chờ duyệt hiển thị sai!");
  }

  @Test
  public void testRequestStatsFromServer_KhiMangGapSuCo_KhongLamSupUngDung() {
    // GIVEN: Giả lập lỗi mạng văng ra
    networkFuture.completeExceptionally(new RuntimeException("Mất kết nối Internet!"));

    WaitForAsyncUtils.waitForFxEvents();

    // THEN: Ứng dụng vẫn phải sống, Label giữ nguyên trạng thái an toàn
    assertEquals("...", controller.getTxtTotalUsers().getText());
  }
}