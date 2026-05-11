package com.uet.bidding.controller;

import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

public class AdminController {

  @FXML
  private StackPane mainContent; // Vùng sẽ thay đổi nội dung khi bấm nút

  @FXML
  private void handleManageUsers() {
    System.out.println("Switching to User Management View...");
    // Ở Tuần 9, bạn chỉ cần in ra console hoặc load một label mẫu
    // mainContent.getChildren().setAll(new Label("Giao diện Quản lý User"));
  }

  @FXML
  private void handleManageAuctions() {
    System.out.println("Switching to Auction Management View...");
    // Nơi Admin duyệt/xóa các món đồ (Items) [cite: 19]
  }

  @FXML
  private void handleSystemStatistics() {
    System.out.println("Showing system reports...");
    // Nơi xem tổng số phiên đấu giá, người dùng đang online
  }

  @FXML
  private void handleLogout() {
    System.out.println("Logging out Admin...");
    // Quay lại màn hình Login
  }
}