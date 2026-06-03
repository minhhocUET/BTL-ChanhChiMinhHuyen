package com.uet.bidding.controller;

import com.uet.bidding.model.Admin;
import com.uet.bidding.model.User;
import com.uet.bidding.network.ClientService;
import com.uet.bidding.service.UserService;
import com.uet.bidding.util.UserSession;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

  @FXML
  private TextField usernameField;
  @FXML
  private PasswordField passwordField;
  @FXML
  private Label messageLabel;

  private UserService userService = new UserService();

  @FXML
  public void handleLogin() {
    String username = usernameField.getText().trim();
    String password = passwordField.getText().trim();

    if (username.isEmpty() || password.isEmpty()) {
      messageLabel.setStyle("-fx-text-fill: red;");
      messageLabel.setText("Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu!");
      return;
    }

    messageLabel.setStyle("-fx-text-fill: #2563eb;");
    messageLabel.setText("Đang kiểm tra thông tin...");

    userService.login(username, password).thenAccept(response -> {
      Platform.runLater(() -> {
        if ("LOGIN_SUCCESS".equals(response.getType())) {

          // Dùng thẳng ClientService để parse (AN TOÀN 100% VÌ ĐÃ CÓ GSON FACTORY)
          User user = ClientService.getInstance().parseUser(response.getData());

          if (user != null) {
            UserSession.setCurrentUser(user);

            try {
              if (user instanceof Admin) {
                System.out.println("Chào sếp! Đang vào Admin Panel...");
                Main.changeScene("/AdminDashboard.fxml", "Admin Dashboard", 1100, 800);
              } else {
                System.out.println("Chào khách hàng! Đang vào Sàn đấu giá...");
                Main.changeScene("/AuctionList.fxml", "Sàn Đấu Giá", 1000, 700);
              }
            } catch (Exception e) {
              messageLabel.setStyle("-fx-text-fill: red;");
              messageLabel.setText("Lỗi giao diện: Không tìm thấy file FXML!");
              e.printStackTrace();
            }
          } else {
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText("Lỗi: Không thể xác định quyền người dùng!");
          }
        } else {
          messageLabel.setStyle("-fx-text-fill: red;");
          messageLabel.setText(String.valueOf(response.getData()));
        }
      });
    }).exceptionally(ex -> {
      Platform.runLater(() -> {
        messageLabel.setStyle("-fx-text-fill: red;");
        messageLabel.setText("Lỗi kết nối đến máy chủ!");
      });
      return null;
    });
  }

  @FXML
  public void goToRegister() {
    Main.changeScene("/Register.fxml", "Hệ thống Đấu giá VNU - Đăng ký", 400, 500);
  }
}