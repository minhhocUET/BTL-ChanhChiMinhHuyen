package com.uet.bidding.controller.admin;

import com.uet.bidding.model.User;
import com.uet.bidding.model.NetworkMessage;
import com.uet.bidding.network.ClientService;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.lang.reflect.Field;
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
  private CompletableFuture<NetworkMessage> networkFuture;

  @Start
  public void start(Stage stage) throws Exception {
    // 1. Khởi tạo Mock mạng và luồng treo trước khi FXML nạp
    mockClientService = mock(ClientService.class);
    networkFuture = new CompletableFuture<>();

    lenient().when(mockClientService.sendRequest(anyString(), any())).thenReturn(networkFuture);
    lenient().when(mockClientService.getGson()).thenReturn(new com.google.gson.Gson());

    // 2. Inject Mock vào Singleton thực thể ClientService bằng Reflection
    Field instanceField = ClientService.class.getDeclaredField("instance");
    instanceField.setAccessible(true);
    instanceField.set(null, mockClientService);

    // 3. Nạp giao diện FXML (Kích hoạt initialize() chạy ngầm lắng nghe networkFuture)
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/AdminUserManagement.fxml"));
    Parent root = loader.load();
    controller = loader.getController();

    // 🎯 Đặt kích thước Scene đủ lớn để TableView có không gian render hiển thị toàn bộ các hàng thật ra màn hình
    stage.setScene(new Scene(root, 800, 600));
    stage.show();
  }

  @AfterEach
  public void tearDown() throws Exception {
    // Giải phóng bộ nhớ Mock tránh rò rỉ sang các class test khác
    Field instanceField = ClientService.class.getDeclaredField("instance");
    instanceField.setAccessible(true);
    instanceField.set(null, null);
  }

  @Test
  public void testLoadUsersFromServer_KhiServerTraVeThanhCong_HienThiChinhXacLenBang(FxRobot robot) {
    // GIVEN: 1. Giả lập gói tin mạng chứa mảng thô gồm 3 phần tử đổ về
    List<Object> rawDataList = new ArrayList<>();
    rawDataList.add(new Object());
    rawDataList.add(new Object());
    rawDataList.add(new Object());

    NetworkMessage mockResponse = mock(NetworkMessage.class);
    when(mockResponse.getType()).thenReturn("GET_ALL_USERS_SUCCESS");
    when(mockResponse.getData()).thenReturn(rawDataList);

    // GIVEN: 2. Thiết lập cấu trúc dữ liệu chi tiết cho 3 đối tượng người dùng Mock
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

    // Cấu hình mock ClientService phân giải tuần tự ra đúng 3 thực thể này
    when(mockClientService.parseUser(any()))
        .thenReturn(activeCustomer)
        .thenReturn(bannedCustomer)
        .thenReturn(adminUser);

    // WHEN: Bơm gói dữ liệu thành công vào đường ống mạng
    networkFuture.complete(mockResponse);

    // Ép luồng Test dừng lại chờ JavaFX Thread thực thi xong Platform.runLater() và RENDER xong toàn bộ UI thật lên màn hình
    WaitForAsyncUtils.waitForFxEvents();

    // THEN: 1. Kiểm tra số lượng dòng nạp vào ObservableList của Controller
    assertEquals(3, controller.getUserObservableList().size(), "Bảng phải nạp đủ 3 người dùng");

    // THEN: 2. Kiểm tra định dạng chuỗi chuyển đổi trạng thái (colStatus) bằng cách truyền CellDataFeature giả lập
    String statusActive = controller.getColStatus().getCellValueFactory().call(
        new TableColumn.CellDataFeatures<>(controller.getUserTable(), controller.getColStatus(), activeCustomer)
    ).getValue();

    String statusBanned = controller.getColStatus().getCellValueFactory().call(
        new TableColumn.CellDataFeatures<>(controller.getUserTable(), controller.getColStatus(), bannedCustomer)
    ).getValue();

    assertEquals("✅ Hoạt động", statusActive);
    assertEquals("❌ Đã khóa", statusBanned);

    // THEN: 3. KIỂM TRA LOGIC TẠO NÚT BẤM BẰNG CƠ CHẾ LOOKUP CỦA TESTFX (AN TOÀN TUYỆT ĐỐI)
    TableView<User> table = controller.getUserTable();

    // Sử dụng robot của TestFX để tìm tất cả các đối tượng Button đang nằm bên trong TableView thực tế trên Scene
    List<Button> actionButtons = new ArrayList<>(robot.from(table).lookup(".button").queryAllAs(Button.class));

    // Vì dòng 0 và dòng 1 sinh nút bấm, còn dòng 2 (Admin) không sinh nút bấm đồ họa (setGraphic(null))
    // Do đó danh sách Button tìm thấy trên bảng bắt buộc phải có kích thước bằng đúng 2!
    assertEquals(2, actionButtons.size(), "Hệ thống chỉ được phép sinh ra đúng 2 nút bấm thao tác cho 2 Customer");

    // Nút bấm thứ 1 ứng với dòng 0 (Active Customer) -> Text hiển thị phải là "Khóa"
    Button btnActive = actionButtons.get(0);
    assertNotNull(btnActive, "Customer bình thường phải có nút thao tác hiển thị");
    assertEquals("Khóa", btnActive.getText(), "Customer đang hoạt động phải hiển thị nút 'Khóa'");

    // Nút bấm thứ 2 ứng với dòng 1 (Banned Customer) -> Text hiển thị phải là "Mở khóa"
    Button btnBanned = actionButtons.get(1);
    assertNotNull(btnBanned, "Customer bị khóa phải có nút thao tác hiển thị");
    assertEquals("Mở khóa", btnBanned.getText(), "Customer đang bị khóa phải hiển thị nút 'Mở khóa'");
  }

  @Test
  public void testLoadUsersFromServer_KhiServerBaoLoi_KhongHienThiDuLieuLenBang() {
    // GIVEN: Giả lập gói tin lỗi từ Server truyền về đúng cấu trúc chuỗi định danh "ERROR"
    NetworkMessage mockResponse = mock(NetworkMessage.class);
    when(mockResponse.getType()).thenReturn("ERROR");
    when(mockResponse.getData()).thenReturn("Lỗi bảo mật session!");

    // WHEN: Đẩy gói tin lỗi vào đường ống mạng
    networkFuture.complete(mockResponse);
    WaitForAsyncUtils.waitForFxEvents();

    // THEN: Kiểm tra danh sách hiển thị trên bảng, bắt buộc phải trống rỗng, không hiển thị dữ liệu rác
    assertTrue(controller.getUserObservableList().isEmpty(), "Bảng phải trống rỗng khi server báo lỗi");
  }
}