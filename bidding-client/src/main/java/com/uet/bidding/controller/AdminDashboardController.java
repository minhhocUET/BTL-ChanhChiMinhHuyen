package com.uet.bidding.controller;

import com.google.gson.Gson;
import com.uet.bidding.network.ClientService; // Dùng lớp này
import com.uet.bidding.model.NetworkMessage;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.Map;

public class AdminDashboardController {

  private final Gson gson = new Gson();

  // Lưu instance vào một biến static để ClientService có thể gọi ngược lại và cập nhật UI
  private static AdminDashboardController instance;

  @FXML private Label lblTotalUsers;
  @FXML private Label lblActiveSessions;
  @FXML private Label lblPendingItems;
  @FXML private StackPane contentArea;

  public static AdminDashboardController getInstance() {
    return instance;
  }

  @FXML
  public void initialize() {
    instance = this; // Gán instance khi UI khởi tạo

    refreshStatistics();

    // Tự động yêu cầu cập nhật mỗi 5 giây
    Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(5), event -> {
      refreshStatistics();
    }));
    timeline.setCycleCount(Animation.INDEFINITE);
    timeline.play();
  }

  private void refreshStatistics() {
    // Gửi yêu cầu qua ClientService (Kết nối duy nhất đang mở)
    ClientService.getInstance().sendRequest("GET_SYSTEM_STATS", "");
  }

  /**
   * Hàm này sẽ được ClientService gọi khi có dữ liệu đổ về
   */
  public void updateStatsUI(Map<String, Double> stats) {
    Platform.runLater(() -> {
      lblTotalUsers.setText(String.valueOf(stats.get("totalUsers").intValue()));
      lblActiveSessions.setText(String.valueOf(stats.get("activeSessions").intValue()));
      lblPendingItems.setText(String.valueOf(stats.get("pendingItems").intValue()));
    });
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
    // Thay vì tự load FXML, hãy dùng hàm bạn đã viết ở Main
    Main.changeScene("/Login.fxml", "Đăng nhập", 400, 500);
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