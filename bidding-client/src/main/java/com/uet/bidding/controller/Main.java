package com.uet.bidding.controller;

import com.uet.bidding.network.ClientService;
import javafx.application.Application;
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

  @Override
  public void start(Stage primaryStage) {
    window = primaryStage;

    // Kết nối mạng ngay khi khởi động
    try {
      // Thay "26.95.102.74" bằng "localhost" nếu chạy cùng máy
      ClientService.getInstance().connect("127.0.0.1", 8888);
      System.out.println("✅ Kết nối Server thành công!");
    } catch (IOException e) {
      showErrorAlert("Lỗi kết nối", "Không thể kết nối đến Server tại IP 26.95.102.74:8888");
      // Có thể dừng app hoặc cho phép chạy offline tùy bạn
    }

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

  public static void main(String[] args) {
    launch(args);
  }
}