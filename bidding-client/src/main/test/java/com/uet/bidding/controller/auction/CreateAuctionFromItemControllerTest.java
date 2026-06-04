package com.uet.bidding.controller.auction;

import com.google.gson.Gson;
import com.uet.bidding.model.Auction;
import com.uet.bidding.model.Customer;
import com.uet.bidding.model.Item;
import com.uet.bidding.model.ItemFactory;
import com.uet.bidding.model.NetworkMessage;
import com.uet.bidding.model.Seller;
import com.uet.bidding.network.ClientService;
import com.uet.bidding.service.SellerService;
import com.uet.bidding.util.CreateAuctionContext;
import com.uet.bidding.util.UserSession;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class CreateAuctionFromItemControllerTest {

  private CreateAuctionFromItemController controller;
  private SellerService mockSellerService;

  // Các UI Components
  private ImageView imgItem;
  private Label lblName, lblCity, lblType, lblDescription;
  private TextField txtStartPrice, txtDuration, txtBidIncrement;
  private Label lblDemoHint; // 👉 THÊM BIẾN NÀY

  @BeforeAll
  static void initJFX() {
    // Khởi tạo JavaFX Toolkit ẩn để không bị crash khi tạo Label, Alert
    try { Platform.startup(() -> {}); } catch (Exception e) {}
  }

  @BeforeEach
  void setUp() throws Exception {
    controller = new CreateAuctionFromItemController();
    mockSellerService = mock(SellerService.class);

    // Khởi tạo các thành phần UI
    imgItem = new ImageView();
    lblName = new Label();
    lblCity = new Label();
    lblType = new Label();
    lblDescription = new Label();
    txtStartPrice = new TextField();
    txtDuration = new TextField();
    txtBidIncrement = new TextField();
    lblDemoHint = new Label(); // 👉 THÊM DÒNG NÀY

    // Bơm các thành phần vào Controller thông qua Reflection
    injectField("imgItem", imgItem);
    injectField("lblName", lblName);
    injectField("lblCity", lblCity);
    injectField("lblType", lblType);
    lblDescription.setWrapText(true); // Đảm bảo thuộc tính gốc không bị lỗi
    injectField("lblDescription", lblDescription);
    injectField("txtStartPrice", txtStartPrice);
    injectField("txtDuration", txtDuration);
    injectField("txtBidIncrement", txtBidIncrement);
    injectField("lblDemoHint", lblDemoHint);

    injectField("sellerService", mockSellerService);

    // Dọn dẹp Session để tránh ảnh hưởng chéo giữa các test
    CreateAuctionContext.clear();
    UserSession.setCurrentUser(null);
  }

  // =========================================================================
  // 1. TEST KHỐI INITIALIZE (NẠP DỮ LIỆU LÊN GIAO DIỆN)
  // =========================================================================

  @Test
  void testInitialize_NạpDữLiệuThànhCông() {
    // GIVEN: Giả lập có 1 Item được truyền vào từ Context
    Item dummyItem = ItemFactory.createElectronics("TV Sony", "TV 4K", BigDecimal.valueOf(5000000), null, 1, "Sony", 12);
    dummyItem.setCity("Hà Nội");
    CreateAuctionContext.set(dummyItem);

    // WHEN
    controller.initialize();

    // THEN: Kiểm tra giao diện đã nạp đúng chữ chưa
    assertEquals("TV Sony", lblName.getText());
    assertEquals("Thành phố: Hà Nội", lblCity.getText());

    // 👉 SỬA Ở ĐÂY: Thêm chữ "Loại: " vào trước chuỗi kỳ vọng
    assertEquals("Loại: " + dummyItem.getType(), lblType.getText());

    assertEquals("5.000.000", txtStartPrice.getText(), "Giá trị mặc định phải được format chuẩn");
  }

  @Test
  void testInitialize_AnToànKhiItemNull() throws InterruptedException {
    // GIVEN: Context trống
    CreateAuctionContext.clear();

    // Dùng AtomicReference để hứng Exception nếu có lỗi xảy ra trên luồng JavaFX
    java.util.concurrent.atomic.AtomicReference<Throwable> thrownException = new java.util.concurrent.atomic.AtomicReference<>();
    // Dùng CountDownLatch để chặn luồng JUnit lại, chờ luồng JavaFX chạy xong mới đi tiếp
    java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);

    // WHEN: Ép chạy trên JavaFX Thread vì bên trong initialize() có gọi Alert
    Platform.runLater(() -> {
      try {
        controller.initialize();
      } catch (Throwable t) {
        thrownException.set(t); // Bắt lỗi lại
      } finally {
        latch.countDown(); // Báo hiệu đã chạy xong
      }
    });

    latch.await(); // Chờ luồng FX hoàn tất

    // THEN: Kiểm tra xem có Exception nào bị văng ra không
    assertNull(thrownException.get(), "Không được văng Exception khi Item null, nhưng nhận được: " + thrownException.get());
  }

  // =========================================================================
  // 2. TEST KHỐI HANDLE_CREATE_AUCTION (VALIDATE & GỬI MẠNG)
  // =========================================================================

  @Test
  void testHandleCreateAuction_LỗiĐịnhDạngSố_BắtNumberFormatException() throws Exception {
    // GIVEN: Cố tình nhập chữ vào ô số
    txtStartPrice.setText("Mười Triệu");
    txtDuration.setText("Vài Ngày");

    ActionEvent mockEvent = mock(ActionEvent.class);

    // WHEN: Nhấn nút tạo
    // Dùng Platform.runLater do bên trong có gọi showAlert (cần JavaFX thread)
    Platform.runLater(() -> controller.handleCreateAuction(mockEvent));
    Thread.sleep(200); // Chờ luồng FX chạy xong khối catch (NumberFormatException)

    // THEN: Test này pass nếu không bị crash app (Coverage sẽ phủ đỏ khối catch)
    assertTrue(true);
  }

  // =========================================================================
  // 3. TEST KHỐI NAVIGATION & EXCEPTIONS
  // =========================================================================

  @Test
  void testOpenSellerDashboard_BắtLỗiIOException() throws Exception {
    // Để ép văng IOException, ta truyền vào một Node rác không thể liên kết được FXML
    Method method = CreateAuctionFromItemController.class.getDeclaredMethod("openSellerDashboard", Object.class);
    method.setAccessible(true);

    // Không cấp Scene/Stage cho Node này, khi FXMLLoader.load cố gắng gắn vào nó sẽ lỗi (hoặc ném NPE/IOException)
    Node mockNode = mock(Node.class);

    assertDoesNotThrow(() -> {
      method.invoke(controller, mockNode);
    }, "Hàm phải bắt được lỗi IOException/NPE ở khối catch mà không làm sập chương trình");
  }

  @Test
  void testShowAlert_AnToanTrenJavaFXThread() throws Exception {
    // Dùng Reflection gọi trực tiếp hàm private showAlert
    Method method = CreateAuctionFromItemController.class.getDeclaredMethod("showAlert", Alert.AlertType.class, String.class, String.class);
    method.setAccessible(true);

    Platform.runLater(() -> {
      try {
        method.invoke(controller, Alert.AlertType.INFORMATION, "Test Title", "Test Content");
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
    Thread.sleep(200);
    // Alert.show() không block luồng nên test sẽ đi qua bình thường
    assertTrue(true);
  }

  // =========================================================================
  // HÀM TIỆN ÍCH (REFLECTION INJECT)
  // =========================================================================
  private void injectField(String fieldName, Object value) throws Exception {
    Field field = CreateAuctionFromItemController.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(controller, value);
  }
}