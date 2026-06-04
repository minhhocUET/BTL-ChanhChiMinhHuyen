package com.uet.bidding.controller.admin;

import com.google.gson.JsonObject;
import com.uet.bidding.network.ClientService;
import com.uet.bidding.model.NetworkMessage;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
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

  @Start
  public void start(Stage stage) throws Exception {
    // 1. Tạo Mock đóng băng cổng mạng trước khi FXML kịp load và gọi initialize()
    mockClientService = mock(ClientService.class);
    networkFuture = new CompletableFuture<>();

    lenient().when(mockClientService.sendRequest(anyString(), any())).thenReturn(networkFuture);
    lenient().when(mockClientService.getGson()).thenReturn(new com.google.gson.Gson());

    // Inject thực thể mock vào hệ thống thông qua Reflection
    Field instanceField = ClientService.class.getDeclaredField("instance");
    instanceField.setAccessible(true);
    instanceField.set(null, mockClientService);

    // 2. Tải giao diện lên luồng UI (Hàm initialize() sẽ tự chạy tại đây)
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/AdminOverview.fxml"));
    Parent root = loader.load();
    controller = loader.getController();

    stage.setScene(new Scene(root));
    stage.show();
  }

  @AfterEach
  public void tearDown() throws Exception {
    Field instanceField = ClientService.class.getDeclaredField("instance");
    instanceField.setAccessible(true);
    instanceField.set(null, null);
  }

  @Test
  public void testInitialize_KhiChuaCoDuLieuMang_HienThiDauBaCham() {
    assertEquals("...", controller.getTxtTotalUsers().getText());
    assertEquals("...", controller.getTxtActiveAuctions().getText());
    assertEquals("...", controller.getTxtPendingItems().getText());
  }

  @Test
  public void testRequestStatsFromServer_KhiServerTraVeThanhCong_CapNhatUIMuorMa() {
    // GIVEN: Tạo dữ liệu giả định cấu trúc Json trả về từ Server giống hệt Controller yêu cầu
    JsonObject dummyStats = new JsonObject();
    dummyStats.addProperty("totalUsers", 150);
    dummyStats.addProperty("activeAuctions", 45);
    dummyStats.addProperty("pendingItems", 12);

    NetworkMessage mockResponse = mock(NetworkMessage.class);
    when(mockResponse.getType()).thenReturn("GET_SYSTEM_STATS_SUCCESS");
    when(mockResponse.getData()).thenReturn(dummyStats);

    // WHEN: Đẩy dữ liệu vào Future (Kích hoạt trực tiếp luồng .thenAccept ngầm bên trong Controller)
    networkFuture.complete(mockResponse);

    // Chờ luồng vẽ giao diện Platform.runLater() của JavaFX hoàn tất xử lý đồ họa
    WaitForAsyncUtils.waitForFxEvents();

    // THEN: Xác thực các Label hiển thị chính xác kết quả
    assertEquals("150", controller.getTxtTotalUsers().getText(), "Tổng số user hiển thị sai!");
    assertEquals("45", controller.getTxtActiveAuctions().getText(), "Số cuộc đấu giá hiển thị sai!");
    assertEquals("12", controller.getTxtPendingItems().getText(), "Số sản phẩm chờ duyệt hiển thị sai!");
  }

  @Test
  public void testRequestStatsFromServer_KhiMangGapSuCo_KhongLamSupUngDung() {
    // GIVEN: Giả lập lỗi kết nối bất đồng bộ bắn lỗi (.exceptionally)
    networkFuture.completeExceptionally(new RuntimeException("Mất kết nối Internet!"));

    WaitForAsyncUtils.waitForFxEvents();

    // THEN: Chương trình rơi vào khối catch/exceptionally an toàn, UI giữ nguyên trạng thái "..." ban đầu
    assertEquals("...", controller.getTxtTotalUsers().getText());
  }
}