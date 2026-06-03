package com.uet.bidding.controller.admin;

import com.uet.bidding.model.Electronics;
import com.uet.bidding.model.Item;
import com.uet.bidding.model.NetworkMessage; // Giả định class chứa response của bạn
import com.uet.bidding.network.ClientService;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(ApplicationExtension.class)
public class AdminItemManagementControllerTest {

  private AdminItemManagementController controller;
  private TableView<Item> tableView;

  // Đối tượng mock dùng để cấu hình phản hồi tĩnh từ luồng mạng
  private ClientService mockClientService;
  private MockedStatic<ClientService> staticClientServiceMock;

  @BeforeEach
  public void setUp() {
    // 1. Giả lập chặn đứng Singleton ClientService trước khi màn hình FXML được nạp
    mockClientService = mock(ClientService.class);
    staticClientServiceMock = mockStatic(ClientService.class);
    staticClientServiceMock.when(ClientService::getInstance).thenReturn(mockClientService);

    // Tạo sẵn một Future rỗng mặc định để tránh lỗi NullPointer khi initialize gọi mạng
    CompletableFuture<NetworkMessage> defaultFuture = new CompletableFuture<>();
    NetworkMessage mockMsg = mock(NetworkMessage.class);
    when(mockMsg.getType()).thenReturn("GET_PENDING_ITEMS_SUCCESS");
    when(mockMsg.getData()).thenReturn(new ArrayList<>());
    defaultFuture.complete(mockMsg);

    when(mockClientService.sendRequest(anyString(), any())).thenReturn(defaultFuture);
  }

  @AfterEach
  public void tearDown() {
    // Giải phóng Mock Static sau mỗi ca kiểm thử để tránh xung đột luồng
    staticClientServiceMock.close();
  }

  @Start
  public void start(Stage stage) throws Exception {
    // 2. Nạp file FXML thật để thực hiện ép các node @FXML injection vào hoạt động
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/AdminItemManagement.fxml"));
    Parent root = loader.load();
    controller = loader.getController();

    // Lấy quyền truy cập bảng thông qua phản chiếu nội bộ (Reflection) hoặc tìm trực tiếp trong View
    tableView = (TableView<Item>) root.lookup("#tablePendingItems");

    stage.setScene(new Scene(root));
    stage.show();
  }

  @Test
  public void testUpdatePendingItemsUI_NạpDanhSachThànhCông() {
    // GIVEN: Tạo danh sách hàng hóa giả lập
    List<Item> dummyItems = new ArrayList<>();
    dummyItems.add(new Electronics(1, "iPhone 15", "Màu đen", new BigDecimal("20000000"), "img.png", 10, "Apple", 12));
    dummyItems.add(new Electronics(2, "Laptop Dell", "Core i7", new BigDecimal("15000000"), "dell.png", 11, "Dell", 24));

    // WHEN: Gọi hàm cập nhật UI
    controller.updatePendingItemsUI(dummyItems);

    // Chờ JavaFX flush hết các sự kiện render trên luồng UI (Platform.runLater)
    WaitForAsyncUtils.waitForFxEvents();

    // THEN: Kiểm tra xem TableView đã hiển thị đúng số dòng chưa
    assertEquals(2, tableView.getItems().size(), "Bảng phải hiển thị đúng 2 sản phẩm chờ duyệt");
    assertEquals("iPhone 15", tableView.getItems().get(0).getName());
  }

  @Test
  public void testAddPendingItemRealtime_ĐẩySảnPhẩmMớiLênĐầuMànHình() {
    // GIVEN: Bảng đang có sẵn 1 phần tử cố định
    List<Item> existingItems = new ArrayList<>();
    existingItems.add(new Electronics(1, "Sản phẩm cũ", "Mô tả", new BigDecimal("1000"), "old.png", 1, "Brand", 12));
    controller.updatePendingItemsUI(existingItems);
    WaitForAsyncUtils.waitForFxEvents();

    // WHEN: Có sản phẩm mới kích hoạt Realtime từ Server bắn về
    Item newItem = new Electronics(999, "Hàng Độc Đắc Realtime", "Mới tinh", new BigDecimal("999999"), "new.png", 5, "Hiếm", 6);
    controller.addPendingItemRealtime(newItem);
    WaitForAsyncUtils.waitForFxEvents();

    // THEN: Sản phẩm mới phải nhảy lên đứng đầu bảng (Vị trí index = 0)
    assertEquals(2, tableView.getItems().size());
    assertEquals(999, tableView.getItems().get(0).getId(), "Sản phẩm realtime phải đứng đầu danh sách (Index 0)");
  }

  @Test
  public void testAddPendingItemRealtime_ChốngTrùngLậpID() {
    // GIVEN: Sản phẩm ID 100 đã nằm trong bảng từ trước
    Item duplicateItem = new Electronics(100, "Sản phẩm trùng", "Mô tả", new BigDecimal("1000"), "img.png", 1, "Brand", 12);
    List<Item> list = List.of(duplicateItem);
    controller.updatePendingItemsUI(list);
    WaitForAsyncUtils.waitForFxEvents();

    // WHEN: Cố tình gọi hàm addRealtime cùng ID 100 một lần nữa
    controller.addPendingItemRealtime(duplicateItem);
    WaitForAsyncUtils.waitForFxEvents();

    // THEN: Kích thước bảng vẫn giữ nguyên là 1, không được tăng tiến dòng trùng lặp
    assertEquals(1, tableView.getItems().size(), "Hàm phải chặn không cho add sản phẩm trùng ID");
  }
}