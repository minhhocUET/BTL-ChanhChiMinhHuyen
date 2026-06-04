package com.uet.bidding.controller.admin;

import com.uet.bidding.model.*;
import com.uet.bidding.network.ClientService;
import com.uet.bidding.util.ImageUtils;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.testfx.framework.junit5.ApplicationExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(ApplicationExtension.class)
public class AdminItemManagementControllerTest {

  private AdminItemManagementController controller;

  private Label realLblDetailInfo;
  private ImageView realImgPreview;
  private TableView<Item> realTablePendingItems;
  private TableColumn<Item, Integer> colId;
  private TableColumn<Item, String> colName;
  private TableColumn<Item, Integer> colSellerId;
  private TableColumn<Item, BigDecimal> colPrice;

  private ClientService mockClientService;
  private MockedStatic<Platform> platformMock;
  private MockedStatic<ImageUtils> imageUtilsMock;

  @BeforeEach
  public void setUp() throws Exception {
    controller = new AdminItemManagementController();

    realLblDetailInfo = new Label();
    realImgPreview = new ImageView();
    realTablePendingItems = new TableView<>();
    colId = new TableColumn<>();
    colName = new TableColumn<>();
    colSellerId = new TableColumn<>();
    colPrice = new TableColumn<>();

    injectPrivateField(controller, "lblDetailInfo", realLblDetailInfo);
    injectPrivateField(controller, "imgPreview", realImgPreview);
    injectPrivateField(controller, "tablePendingItems", realTablePendingItems);
    injectPrivateField(controller, "colId", colId);
    injectPrivateField(controller, "colName", colName);
    injectPrivateField(controller, "colSellerId", colSellerId);
    injectPrivateField(controller, "colPrice", colPrice);

    // 1. Giả lập Mạng
    mockClientService = mock(ClientService.class);
    Field instanceField = ClientService.class.getDeclaredField("instance");
    instanceField.setAccessible(true);
    instanceField.set(null, mockClientService);

    // 2. Chặn JavaFX Thread: ÉP TẤT CẢ CHẠY TRÊN 1 LUỒNG (QUAN TRỌNG NHẤT)
    platformMock = mockStatic(Platform.class);
    platformMock.when(() -> Platform.runLater(any(Runnable.class)))
        .thenAnswer(invocation -> {
          Runnable runnable = invocation.getArgument(0);
          runnable.run(); // Chạy ngay lập tức, không delay!
          return null;
        });

    // 3. Chặn tải ảnh Internet
    imageUtilsMock = mockStatic(ImageUtils.class);
    imageUtilsMock.when(() -> ImageUtils.loadItemImage(any(), any())).thenAnswer(invocation -> null);
  }

  @AfterEach
  public void tearDown() throws Exception {
    platformMock.close();
    imageUtilsMock.close();
    Field instanceField = ClientService.class.getDeclaredField("instance");
    instanceField.setAccessible(true);
    instanceField.set(null, null);
  }

  // =========================================================================
  // KHỞI TẠO VÀ RENDER
  // =========================================================================

  @Test
  public void testInitialize_RequestDataFail_ShowAlert() {
    CompletableFuture<NetworkMessage> future = new CompletableFuture<>();
    when(mockClientService.sendRequest("GET_PENDING_ITEMS", "")).thenReturn(future);

    try (MockedConstruction<Alert> mockedAlert = mockConstruction(Alert.class, (mock, context) -> {
      when(mock.showAndWait()).thenReturn(Optional.of(ButtonType.OK));
    })) {
      controller.initialize();

      // Ép server trả lỗi
      future.complete(new NetworkMessage("ERROR", "Lỗi server"));

      // Nhờ chạy chung 1 luồng, Mockito dễ dàng túm cổ được cái Alert!
      assertEquals(1, mockedAlert.constructed().size(), "Phải hiện 1 Alert báo lỗi tải danh sách");
    }
  }

  @Test
  public void testRenderItemPreview_Vehicle() {
    // Mặc định cho danh sách rỗng để né lỗi Gson
    CompletableFuture<NetworkMessage> initFuture = new CompletableFuture<>();
    initFuture.complete(new NetworkMessage("GET_PENDING_ITEMS_SUCCESS", new ArrayList<>()));
    when(mockClientService.sendRequest("GET_PENDING_ITEMS", "")).thenReturn(initFuture);
    controller.initialize();

    Vehicle v = mock(Vehicle.class);
    when(v.getBrand()).thenReturn("Toyota");
    when(v.getModel()).thenReturn("Camry");
    when(v.getMileage()).thenReturn(Double.valueOf(15000)); // Kiểu int hay double đều OK
    when(v.getEngineType()).thenReturn("2.0G");
    when(v.getFuelType()).thenReturn("Xăng");

    // Gán thẳng vào UI
    realTablePendingItems.getItems().add(v);
    realTablePendingItems.getSelectionModel().select(0);

    String detailText = realLblDetailInfo.getText();
    assertTrue(detailText.contains("Phương tiện"));
    assertTrue(detailText.contains("Toyota Camry"));
  }

  @Test
  public void testRenderItemPreview_Electronics() {
    CompletableFuture<NetworkMessage> initFuture = new CompletableFuture<>();
    initFuture.complete(new NetworkMessage("GET_PENDING_ITEMS_SUCCESS", new ArrayList<>()));
    when(mockClientService.sendRequest("GET_PENDING_ITEMS", "")).thenReturn(initFuture);
    controller.initialize();

    Electronics e = mock(Electronics.class);
    when(e.getBrand()).thenReturn("Sony");
    when(e.getWarrantyMonths()).thenReturn(12);

    realTablePendingItems.getItems().add(e);
    realTablePendingItems.getSelectionModel().select(0);

    assertTrue(realLblDetailInfo.getText().contains("Đồ điện tử"));
    assertTrue(realLblDetailInfo.getText().contains("Sony"));
  }

  @Test
  public void testRenderItemPreview_Art() {
    CompletableFuture<NetworkMessage> initFuture = new CompletableFuture<>();
    initFuture.complete(new NetworkMessage("GET_PENDING_ITEMS_SUCCESS", new ArrayList<>()));
    when(mockClientService.sendRequest("GET_PENDING_ITEMS", "")).thenReturn(initFuture);
    controller.initialize();

    Art a = mock(Art.class);
    when(a.getAuthor()).thenReturn("Picasso");

    realTablePendingItems.getItems().add(a);
    realTablePendingItems.getSelectionModel().select(0);

    assertTrue(realLblDetailInfo.getText().contains("Tác phẩm nghệ thuật"));
    assertTrue(realLblDetailInfo.getText().contains("Picasso"));
  }

  // =========================================================================
  // THAO TÁC NÚT BẤM (APPROVE / REJECT)
  // =========================================================================

  @Test
  public void testHandleApproveAction_NoSelection() {
    realTablePendingItems.getSelectionModel().clearSelection();
    try (MockedConstruction<Alert> mockedAlert = mockConstruction(Alert.class, (mock, context) -> {
      when(mock.showAndWait()).thenReturn(Optional.of(ButtonType.OK));
    })) {
      controller.handleApproveAction();
      assertEquals(1, mockedAlert.constructed().size(), "Cảnh báo chưa chọn SP");
    }
  }

  @Test
  public void testHandleApproveAction_SuccessAndFail() {
    CompletableFuture<NetworkMessage> initFuture = new CompletableFuture<>();
    initFuture.complete(new NetworkMessage("GET_PENDING_ITEMS_SUCCESS", new ArrayList<>()));
    when(mockClientService.sendRequest("GET_PENDING_ITEMS", "")).thenReturn(initFuture);
    controller.initialize();

    Electronics e = mock(Electronics.class);
    when(e.getId()).thenReturn(999);
    when(e.getName()).thenReturn("PC");

    realTablePendingItems.getItems().add(e);
    realTablePendingItems.getSelectionModel().select(0);

    CompletableFuture<NetworkMessage> future = new CompletableFuture<>();
    when(mockClientService.sendRequest("APPROVE_ITEM", 999)).thenReturn(future);

    try (MockedConstruction<Alert> mockedAlert = mockConstruction(Alert.class, (mock, context) -> {
      when(mock.showAndWait()).thenReturn(Optional.of(ButtonType.OK));
    })) {
      // Lần 1: Thất bại do Server báo lỗi
      controller.handleApproveAction();
      future.complete(new NetworkMessage("ERROR", "Lỗi Server"));
      assertEquals(1, realTablePendingItems.getItems().size(), "Lỗi mạng nên SP vẫn còn trong bảng");

      // Lần 2: Thành công
      future = new CompletableFuture<>();
      when(mockClientService.sendRequest("APPROVE_ITEM", 999)).thenReturn(future);
      controller.handleApproveAction();
      future.complete(new NetworkMessage("APPROVE_SUCCESS", ""));
      assertEquals(0, realTablePendingItems.getItems().size(), "Duyệt thành công phải xóa khỏi bảng");
    }
  }

  @Test
  public void testHandleRejectAction_SuccessAndFail() {
    CompletableFuture<NetworkMessage> initFuture = new CompletableFuture<>();
    initFuture.complete(new NetworkMessage("GET_PENDING_ITEMS_SUCCESS", new ArrayList<>()));
    when(mockClientService.sendRequest("GET_PENDING_ITEMS", "")).thenReturn(initFuture);
    controller.initialize();

    Electronics e = mock(Electronics.class);
    when(e.getId()).thenReturn(888);
    when(e.getName()).thenReturn("Laptop");

    realTablePendingItems.getItems().add(e);
    realTablePendingItems.getSelectionModel().select(0);

    CompletableFuture<NetworkMessage> future = new CompletableFuture<>();
    when(mockClientService.sendRequest("REJECT_ITEM", "888|Rác")).thenReturn(future);

    try (MockedConstruction<TextInputDialog> mockedDialog = mockConstruction(TextInputDialog.class, (mock, context) -> {
      when(mock.showAndWait()).thenReturn(Optional.of("Rác"));
    });
         MockedConstruction<Alert> mockedAlert = mockConstruction(Alert.class, (mock, context) -> {
           when(mock.showAndWait()).thenReturn(Optional.of(ButtonType.OK));
         })) {

      // Lần 1: Bị lỗi mạng
      controller.handleRejectAction();
      future.complete(new NetworkMessage("ERROR", "Lỗi"));
      assertEquals(1, realTablePendingItems.getItems().size(), "Lỗi nên chưa bị xóa");

      // Lần 2: Thành công
      future = new CompletableFuture<>();
      when(mockClientService.sendRequest("REJECT_ITEM", "888|Rác")).thenReturn(future);
      controller.handleRejectAction();
      future.complete(new NetworkMessage("REJECT_SUCCESS", ""));
      assertEquals(0, realTablePendingItems.getItems().size(), "Đã từ chối nên phải xóa");
    }
  }

  @Test
  public void testAddPendingItemRealtime() {
    CompletableFuture<NetworkMessage> initFuture = new CompletableFuture<>();
    initFuture.complete(new NetworkMessage("GET_PENDING_ITEMS_SUCCESS", new ArrayList<>()));
    when(mockClientService.sendRequest("GET_PENDING_ITEMS", "")).thenReturn(initFuture);
    controller.initialize();

    Electronics item = mock(Electronics.class);
    when(item.getId()).thenReturn(101);

    controller.addPendingItemRealtime(item);
    assertEquals(1, realTablePendingItems.getItems().size());

    // Cố tình đẩy thêm Item trùng ID
    controller.addPendingItemRealtime(item);
    assertEquals(1, realTablePendingItems.getItems().size(), "Chống trùng ID thất bại");
  }

  @Test
  public void testSelectionListener_ClearSelection_Coverage() {
    // Hàm này quét nhánh if (newValue == null) khi người dùng không chọn gì trong bảng
    CompletableFuture<NetworkMessage> initFuture = new CompletableFuture<>();
    initFuture.complete(new NetworkMessage("GET_PENDING_ITEMS_SUCCESS", new ArrayList<>()));
    when(mockClientService.sendRequest("GET_PENDING_ITEMS", "")).thenReturn(initFuture);
    controller.initialize();

    // Cố tình xóa lựa chọn để kích hoạt listener cập nhật giao diện
    realTablePendingItems.getSelectionModel().clearSelection();

    // Đảm bảo code chạy qua không bị crash (Nó thường set text Label về rỗng)
    assertNotNull(realLblDetailInfo.getText());
  }

  @Test
  public void testHandleRejectAction_UserCancelDialog_Coverage() {
    // Hàm này quét nhánh người dùng bấm Hủy (Cancel) khi được hỏi lý do từ chối
    CompletableFuture<NetworkMessage> initFuture = new CompletableFuture<>();
    initFuture.complete(new NetworkMessage("GET_PENDING_ITEMS_SUCCESS", new ArrayList<>()));
    when(mockClientService.sendRequest("GET_PENDING_ITEMS", "")).thenReturn(initFuture);
    controller.initialize();

    Electronics e = mock(Electronics.class);
    when(e.getId()).thenReturn(888);
    realTablePendingItems.getItems().add(e);
    realTablePendingItems.getSelectionModel().select(0);

    // Giả lập Dialog trả về Optional.empty() tương đương với việc bấm Cancel
    try (MockedConstruction<TextInputDialog> mockedDialog = mockConstruction(TextInputDialog.class, (mock, context) -> {
      when(mock.showAndWait()).thenReturn(Optional.empty());
    })) {
      controller.handleRejectAction();

      // Khẳng định 100% rằng hệ thống KHÔNG gửi lệnh REJECT_ITEM nào lên server vì user đã Hủy
      verify(mockClientService, never()).sendRequest(eq("REJECT_ITEM"), any());
    }
  }

  // --- HELPER METHOD ---
  private void injectPrivateField(Object target, String fieldName, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }
}