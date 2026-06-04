package com.uet.bidding.controller.main;

import com.uet.bidding.network.ClientService;
import com.uet.bidding.util.TimeManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class Main extends Application {
  private static Stage window;

  public static void changeScene(String fxmlFile, String title, int width, int height) {
    try {
      URL fxmlLocation = Main.class.getResource(fxmlFile);
      if (fxmlLocation == null) {
        System.err.println("❌ LỖI: Không tìm thấy file " + fxmlFile);
        return;
      }
      Parent pane = FXMLLoader.load(fxmlLocation);
      window.setTitle(title);
      window.setScene(new Scene(pane, width, height));
      window.centerOnScreen();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage primaryStage) {
    window = primaryStage;

    // Kết nối mạng ngay khi khởi động
    try {
      ClientService.getInstance().connect("18.136.197.107", 8888);
      System.out.println("✅ Kết nối Server thành công!");
      ClientService.getInstance().sendRequest("GET_SERVER_TIME", "")
          .thenAccept(response -> {
            if (response != null && "SERVER_TIME_RESPONSE".equals(response.getType())) {
              long serverTime = Long.parseLong(response.getData().toString());
              long clientTime = System.currentTimeMillis();

              // Tính khoảng lệch pha
              long offset = serverTime - clientTime;
              TimeManager.setTimeOffset(offset);
              System.out.println("⏰ Đã đồng bộ thời gian với Server. Độ lệch pha: " + offset + " ms");
            }
          }).exceptionally(ex -> {
            System.err.println("⚠️ Không thể đồng bộ thời gian, dùng giờ mặc định máy local.");
            return null;
          });
    } catch (IOException e) {
      // 1. Hiển thị thông báo lỗi
      showErrorAlert("Lỗi kết nối", "Không thể kết nối đến Server tại IP 127.0.0.1:8888. Vui lòng bật Server trước!");

      // 2. Dừng chương trình ngay lập tức
      Platform.exit();
      return;
    }
    // Dòng này sẽ KHÔNG bao giờ được chạy nếu rơi vào catch ở trên
    changeScene("/Login.fxml", "Đăng nhập hệ thống", 400, 500);
    window.show();
  }

  @Override
  public void stop() {
    ClientService.getInstance().disconnect();
  }

  private void showErrorAlert(String title, String content) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(content);
    alert.showAndWait();
  }
}