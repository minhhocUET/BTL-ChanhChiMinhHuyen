package com.uet.bidding.controller.admin;

import com.uet.bidding.model.User;
import com.uet.bidding.model.NetworkMessage; // Thay đổi package cho đúng dự án của bạn
import com.uet.bidding.network.ClientService;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(ApplicationExtension.class)
public class AdminUserManagementControllerTest {

  private AdminUserManagementController controller;

  private ClientService mockClientService;
  private MockedStatic<ClientService> staticClientServiceMock;
  private CompletableFuture<NetworkMessage> networkFuture;

  @BeforeEach
  public void setUp() {
    // 1. Khởi tạo Mock và chặn luồng tĩnh (Static) của Singleton ClientService trước khi nạp FXML
    mockClientService = mock(ClientService.class);
    staticClientServiceMock = mockStatic(ClientService.class);
    staticClientServiceMock.when(ClientService::getInstance).thenReturn(mockClientService);

    // 2. Giả lập một Future xử lý mạng
    networkFuture = new CompletableFuture<>();
    when(mockClientService.sendRequest(anyString(), any())).thenReturn(networkFuture);
  }

  @AfterEach
  public void tearDown() {
    // Giải phóng Mock tĩnh sau mỗi ca kiểm thử nhằm tránh rò rỉ bộ nhớ
    staticClientServiceMock.close();
  }

  @Start
  public void start(Stage stage) throws Exception {
    // Tải giao diện FXML lên luồng đồ họa ảo của TestFX
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/AdminUserManagement.fxml"));
    Parent root = loader.load();
    controller = loader.getController();

    stage.setScene(new Scene(root));
    stage.show();
  }

  @Test
  public void testLoadUsersFromServer_KhiServerTraVeThanhCong_HienThiChinhXacLenBang() {
    // GIVEN: Tạo danh sách người dùng thô giả lập phản hồi mạng
    List<Object> rawDataList = new ArrayList<>();
    rawDataList.add(new Object()); // Giả lập phần tử thứ 1
    rawDataList.add(new Object()); // Giả lập phần tử thứ 2
    rawDataList.add(new Object()); // Giả lập phần tử thứ 3

    NetworkMessage mockResponse = mock(NetworkMessage.class);
    when(mockResponse.getType()).thenReturn("GET_ALL_USERS_SUCCESS");
    when(mockResponse.getData()).thenReturn(rawDataList);

    // Giả lập cơ chế phân giải đối tượng parseUser đặc thù của ClientService
    User activeCustomer = mock(User.class);
    when(activeCustomer.getId()).thenReturn(1);
    when(activeCustomer.getUsername()).thenReturn("customer1");
    when(activeCustomer.getRole()).thenReturn("CUSTOMER");
    when(activeCustomer.isBanned()).thenReturn(false);

    User bannedCustomer = mock(User.class);
    when(bannedCustomer.getId()).thenReturn(2);
    when(bannedCustomer.getUsername()).thenReturn("customer2");
    when(bannedCustomer.getRole()).thenReturn("CUSTOMER");
    when(bannedCustomer.isBanned()).thenReturn(true);

    User adminUser = mock(User.class);
    when(adminUser.getId()).thenReturn(3);
    when(adminUser.getUsername()).thenReturn("admin1");
    when(adminUser.getRole()).thenReturn("ADMIN");
    when(adminUser.isBanned()).thenReturn(false);

    // Cấu hình mock nhận diện lần lượt 3 người dùng
    when(mockClientService.parseUser(any()))
        .thenReturn(activeCustomer)
        .thenReturn(bannedCustomer)
        .thenReturn(adminUser);

    // WHEN: Kích hoạt mạng phản hồi dữ liệu về
    networkFuture.complete(mockResponse);
    WaitForAsyncUtils.waitForFxEvents(); // Chờ luồng UI vẽ xong bảng

    // THEN: 1. Kiểm tra số lượng dòng được nạp vào bảng dữ liệu công khai
    assertEquals(3, controller.getUserObservableList().size(), "Bảng phải nạp đủ 3 người dùng");

    // THEN: 2. Kiểm tra định dạng chuỗi chuyển đổi trạng thái (colStatus)
    String statusActive = controller.getColStatus().getCellData(0);
    String statusBanned = controller.getColStatus().getCellData(1);
    assertEquals("✅ Hoạt động", statusActive);
    assertEquals("❌ Đã khóa", statusBanned);

    // THEN: 3. Kiểm tra logic tạo nút bấm tự động của cột Hành động (colUserAction)
    // Lấy ô hiển thị (TableCell) từ cột Hành động dựa theo dòng dữ liệu
    TableCell<User, Void> cellActiveCustomer = (TableCell<User, Void>) controller.getColUserAction().getCellFactory().call(controller.getColUserAction());
    cellActiveCustomer.updateIndex(0); // Dòng 0: Customer đang hoạt động
    Button btnActive = (Button) cellActiveCustomer.getGraphic();
    assertNotNull(btnActive, "Customer bình thường phải có nút thao tác");
    assertEquals("Khóa", btnActive.getText(), "Customer đang hoạt động phải hiển thị nút 'Khóa'");

    TableCell<User, Void> cellBannedCustomer = (TableCell<User, Void>) controller.getColUserAction().getCellFactory().call(controller.getColUserAction());
    cellBannedCustomer.updateIndex(1); // Dòng 1: Customer đã bị khóa
    Button btnBanned = (Button) cellBannedCustomer.getGraphic();
    assertNotNull(btnBanned, "Customer bị khóa phải có nút thao tác");
    assertEquals("Mở khóa", btnBanned.getText(), "Customer đang bị khóa phải hiển thị nút 'Mở khóa'");

    TableCell<User, Void> cellAdmin = (TableCell<User, Void>) controller.getColUserAction().getCellFactory().call(controller.getColUserAction());
    cellAdmin.updateIndex(2); // Dòng 2: Tài khoản Admin quản trị
    assertNull(cellAdmin.getGraphic(), "Hệ thống bảo vệ không cho phép sinh nút thao tác lên tài khoản ADMIN khác");
  }

  @Test
  public void testLoadUsersFromServer_KhiServerBaoLoi_KhongHienThiDuLieuLenBang() {
    // GIVEN: Giả lập gói tin báo lỗi gửi về từ Server hệ thống
    NetworkMessage mockResponse = mock(NetworkMessage.class);
    when(mockResponse.getType()).thenReturn("ERROR");
    when(mockResponse.getData()).thenReturn("Lỗi bảo mật hoặc mất quyền truy cập Session!");

    // WHEN: Mạng phản hồi gói lỗi về
    networkFuture.complete(mockResponse);
    WaitForAsyncUtils.waitForFxEvents();

    // THEN: Bảng dữ liệu người dùng phải trống rỗng, không hiển thị rác
    assertTrue(controller.getUserObservableList().isEmpty(), "Bảng phải trống khi server báo lỗi");
  }
}