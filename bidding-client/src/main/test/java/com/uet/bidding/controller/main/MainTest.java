package com.uet.bidding.controller.main;

import com.uet.bidding.network.ClientService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.junit.jupiter.api.*;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class MainTest {

  private Main mainApp;
  private Stage mockStage;

  @BeforeAll
  static void initJFX() {
    // Khởi động trước Engine của JavaFX ngầm để khỏi bị lỗi "Toolkit not initialized"
    try {
      Platform.startup(() -> {});
    } catch (IllegalStateException e) {
      // Nếu đã khởi tạo rồi thì bỏ qua
    }
  }

  @BeforeEach
  void setUp() {
    mainApp = new Main();
    mockStage = mock(Stage.class);
  }

  @Test
  void testMainMethod() {
    // Đánh chặn lệnh Application.launch để hệ thống không bật cửa sổ App thật
    try (MockedStatic<Application> mockedApp = Mockito.mockStatic(Application.class)) {
      String[] args = {"test"};
      Main.main(args);
      mockedApp.verify(() -> Application.launch(args), times(1));
    }
  }

  @Test
  void testStart_KetNoiThanhCong() throws Exception {
    ClientService mockClient = mock(ClientService.class);

    // 🌟 KHẮC PHỤC LỖI: Mock hàm sendRequest trả về một CompletableFuture hợp lệ
    // Giả lập Server trả về kết quả SERVER_TIME_RESPONSE kèm timestamp hiện tại
    com.uet.bidding.model.NetworkMessage mockResponse =
        new com.uet.bidding.model.NetworkMessage("SERVER_TIME_RESPONSE", String.valueOf(System.currentTimeMillis()));
    // Lưu ý: Ông kiểm tra lại hàm setCmd/setCommand và setData của NetworkMessage xem tên chính xác trong dự án là gì nhé
    mockResponse.setType("SERVER_TIME_RESPONSE");
    mockResponse.setData(String.valueOf(System.currentTimeMillis()));

    // Ép mockClient trả về một luồng đã hoàn thành chứa mockResponse khi gọi GET_SERVER_TIME
    when(mockClient.sendRequest(eq("GET_SERVER_TIME"), any()))
        .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(mockResponse));

    CountDownLatch latch = new CountDownLatch(1);
    final Throwable[] errors = new Throwable[1]; // Dùng để hứng lỗi từ luồng FX báo về luồng Test

    // Đưa TOÀN BỘ quá trình Mock và Verify vào chung một luồng JavaFX
    Platform.runLater(() -> {
      try (MockedStatic<ClientService> mockedCS = Mockito.mockStatic(ClientService.class);
           MockedStatic<FXMLLoader> mockedFxml = Mockito.mockStatic(FXMLLoader.class)) {

        mockedCS.when(ClientService::getInstance).thenReturn(mockClient);
        // Giả vờ load FXML thành công trả về 1 Pane trống
        mockedFxml.when(() -> FXMLLoader.load(any(URL.class))).thenReturn(new Pane());

        // 1. Chạy Start
        mainApp.start(mockStage);

        // 2. Kiểm tra xem đã kết nối đúng IP chưa, đã gửi request đồng bộ giờ chưa và đã show cửa sổ chưa
        verify(mockClient).connect("18.136.197.107", 8888);
        verify(mockClient).sendRequest(eq("GET_SERVER_TIME"), any()); // Xác minh có gọi đồng bộ thời gian
        verify(mockStage).show();

      } catch (Throwable t) {
        // Nếu có lỗi (dù là lỗi code hay lỗi verify thất bại), bắt lại để báo cho JUnit
        errors[0] = t;
      } finally {
        latch.countDown();
      }
    });

    // Chờ luồng JavaFX chạy xong (tối đa 5 giây) để tránh test bị lướt qua qua nhanh
    latch.await(5, java.util.concurrent.TimeUnit.SECONDS);

    // Nếu trong luồng JavaFX có bắt được bất kỳ lỗi gì, thảy ra cho JUnit làm tạch test để hiển thị log chuẩn
    if (errors[0] != null) {
      fail("Test thất bại do lỗi trong luồng JavaFX: ", errors[0]);
    }
  }

  @Test
  void testStart_KetNoiThatBai() throws Exception {
    ClientService mockClient = mock(ClientService.class);
    // Ép mạng văng lỗi IOException
    doThrow(new IOException("Lỗi mạng mô phỏng")).when(mockClient).connect(anyString(), anyInt());

    try (MockedStatic<ClientService> mockedCS = Mockito.mockStatic(ClientService.class);
         MockedStatic<Platform> mockedPlatform = Mockito.mockStatic(Platform.class); // Chặn Platform.exit()
         MockedConstruction<Alert> mockedAlert = Mockito.mockConstruction(Alert.class)) { // Chặn Popup

      mockedCS.when(ClientService::getInstance).thenReturn(mockClient);

      // Vì đã chặn Platform.exit nên ta có thể gọi start trực tiếp trên luồng Test
      mainApp.start(mockStage);

      // Kiểm tra xem hệ thống có hiển thị 1 cái Alert và gọi lệnh thoát app không
      assertEquals(1, mockedAlert.constructed().size(), "Phải hiện popup thông báo lỗi!");
      mockedPlatform.verify(Platform::exit, times(1));
      // Cửa sổ chắc chắn KHÔNG được hiển thị (vì đã bị ngắt sớm)
      verify(mockStage, never()).show();
    }
  }

  @Test
  void testStop() {
    ClientService mockClient = mock(ClientService.class);
    try (MockedStatic<ClientService> mockedCS = Mockito.mockStatic(ClientService.class)) {
      mockedCS.when(ClientService::getInstance).thenReturn(mockClient);

      mainApp.stop();

      verify(mockClient).disconnect();
    }
  }

  @Test
  void testChangeScene_KhongTimThayFile() throws Exception {
    // Nhét mockStage vào biến tĩnh "window" bằng java reflection
    java.lang.reflect.Field windowField = Main.class.getDeclaredField("window");
    windowField.setAccessible(true);
    windowField.set(null, mockStage);

    // WHEN: Truyền 1 file chắc chắn không tồn tại
    Main.changeScene("/KhongTonTai.fxml", "Test Title", 400, 500);

    // THEN: Thoát hàm sớm, không hề gọi cài đặt tiêu đề
    verify(mockStage, never()).setTitle(anyString());
  }

  @Test
  void testChangeScene_LoadFXMLThanhCong() throws Exception {
    // Cài đặt mockStage vào biến tĩnh 'window'
    java.lang.reflect.Field windowField = Main.class.getDeclaredField("window");
    windowField.setAccessible(true);
    windowField.set(null, mockStage);

    CountDownLatch latch = new CountDownLatch(1);
    final Throwable[] errors = new Throwable[1];

    // Sử dụng file FXML mà code gốc của ông đang có để getResource() CHẮC CHẮN không bị null
    String dummyValidPath = "/Login.fxml";

    Platform.runLater(() -> {
      // Đưa khối MockedStatic vào bên trong luồng JavaFX để đồng bộ
      try (MockedStatic<FXMLLoader> mockedFxml = Mockito.mockStatic(FXMLLoader.class)) {

        // Giả lập khi load FXML sẽ trả về một Pane trống hợp lệ
        mockedFxml.when(() -> FXMLLoader.load(any(URL.class))).thenReturn(new Pane());

        // WHEN: Thực hiện gọi hàm thay đổi Scene
        Main.changeScene(dummyValidPath, "Success Title", 800, 600);

        // THEN: Xác thực các hành vi được gọi thành công ngay trên luồng JavaFX
        verify(mockStage).setTitle("Success Title");
        verify(mockStage).setScene(any(Scene.class));
        verify(mockStage).centerOnScreen();

      } catch (Throwable t) {
        errors[0] = t; // Bắt lại mọi lỗi verify thất bại để đẩy về JUnit
      } finally {
        latch.countDown();
      }
    });

    latch.await(3, TimeUnit.SECONDS);

    // Kiểm tra xem luồng JavaFX chạy có phát sinh lỗi hay không
    assertNull(errors[0], "Test thất bại do lỗi verify trên luồng FX: " + errors[0]);
  }

  @Test
  void testChangeScene_LoiIOExceptionKhiLoad() throws Exception {
    java.lang.reflect.Field windowField = Main.class.getDeclaredField("window");
    windowField.setAccessible(true);
    windowField.set(null, mockStage);

    String dummyValidPath = "Main.class";

    // Lần này KHÔNG mock FXMLLoader nữa -> Nó sẽ nỗ lực đọc file .class dưới dạng FXML XML
    // Kết quả: Văng lỗi định dạng -> Rơi thẳng vào catch(IOException)
    CountDownLatch latch = new CountDownLatch(1);
    Platform.runLater(() -> {
      Main.changeScene(dummyValidPath, "Crash Title", 800, 600);
      latch.countDown();
    });
    latch.await(3, TimeUnit.SECONDS);

    // Bị dính Catch nên Stage sẽ không được setTitle
    verify(mockStage, never()).setTitle(anyString());
  }
}