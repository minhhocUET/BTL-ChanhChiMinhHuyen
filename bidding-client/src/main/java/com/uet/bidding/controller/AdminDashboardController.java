package com.uet.bidding.controller;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.uet.bidding.model.NetworkMessage;
import com.uet.bidding.util.ClientApp; // Giả định đây là nơi giữ Socket kết nối
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label; // Quan trọng
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.Map;

public class AdminDashboardController {

  // 1. Thêm các @FXML Label để điều khiển con số trên giao diện
  @FXML private Label lblTotalUsers;      // Ô màu xanh (125)
  @FXML private Label lblActiveSessions;  // Ô màu xanh dương (12)
  @FXML private Label lblPendingItems;    // Ô màu đỏ (8)

  @FXML
  private StackPane contentArea;

  private final Gson gson = new Gson();

  /**
   * 2. Hàm initialize() sẽ tự động chạy khi giao diện Admin hiện lên
   */
  @FXML
  public void initialize() {
    // Cập nhật số liệu ngay lập tức khi vừa mở trang
    refreshStatistics();

    // Thiết lập tự động cập nhật mỗi 5 giây một lần (Timeline)
    Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(5), event -> {
      refreshStatistics();
    }));
    timeline.setCycleCount(Animation.INDEFINITE);
    timeline.play();
  }

  /**
   * 3. Gửi yêu cầu lấy số liệu từ Server và hiển thị lên UI
   */
  private void refreshStatistics() {
    // Chạy ngầm để không bị treo giao diện (UI Thread)
    new Thread(() -> {
      try {
        // Gửi lệnh lấy thống kê đến Server
        // (Giả sử ClientApp.sendRequest là hàm bạn dùng để gửi/nhận tin nhắn Socket)
        NetworkMessage request = new NetworkMessage("GET_SYSTEM_STATS", "");

        // Gửi qua socket (Bạn cần thay đoạn này bằng hàm gửi nhận thực tế của bạn)
        ClientApp.sendRequest(request, response -> {
          if ("SYSTEM_STATS_RESPONSE".equals(response.getType())) {
            // Chuyển dữ liệu JSON nhận được thành Map
            Map<String, Double> stats = gson.fromJson(
                    gson.toJson(response.getData()),
                    new TypeToken<Map<String, Double>>(){}.getType()
            );

            // CẬP NHẬT GIAO DIỆN (Bắt buộc dùng Platform.runLater)
            Platform.runLater(() -> {
              lblTotalUsers.setText(String.valueOf(stats.get("totalUsers").intValue()));
              lblActiveSessions.setText(String.valueOf(stats.get("activeSessions").intValue()));
              lblPendingItems.setText(String.valueOf(stats.get("pendingItems").intValue()));
            });
          }
        });
      } catch (Exception e) {
        System.err.println("Không thể cập nhật thống kê: " + e.getMessage());
      }
    }).start();
  }

  // --- CÁC HÀM XỬ LÝ SỰ KIỆN CŨ CỦA BẠN GIỮ NGUYÊN ---

  @FXML
  public void handleManageUsers(ActionEvent event) {
    loadSubView("/AdminUserManagement.fxml");
  }

  @FXML
  public void handleManageItems(ActionEvent event) {
    loadSubView("/AdminItemManagement.fxml");
  }

  @FXML
  public void handleSystemReports(ActionEvent event) {
    loadSubView("/AdminReports.fxml");
  }

  @FXML
  public void handleLogout(ActionEvent event) {
    try {
      Parent root = FXMLLoader.load(getClass().getResource("/Login.fxml"));
      Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
      stage.setScene(new Scene(root));
      stage.setTitle("Đăng nhập Hệ thống");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void loadSubView(String fxmlPath) {
    try {
      Parent node = FXMLLoader.load(getClass().getResource(fxmlPath));
      contentArea.getChildren().setAll(node);
    } catch (IOException e) {
      System.err.println("Chưa tạo file FXML: " + fxmlPath);
    }
  }
}