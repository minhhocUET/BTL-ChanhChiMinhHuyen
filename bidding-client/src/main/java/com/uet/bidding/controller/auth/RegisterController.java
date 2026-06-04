package com.uet.bidding.controller.auth;

import com.uet.bidding.service.UserService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;

public class RegisterController {

  @FXML
  private TextField usernameField;

  @FXML
  private PasswordField passwordField;

  @FXML
  private PasswordField confirmPasswordField;

  @FXML
  private Label messageLabel;

  // 1. GỌI SERVICE: Phải có Service thì mới nói chuyện được với Database
  private UserService userService = new UserService();

  @FXML
  public void handleRegister(ActionEvent event) {
    String username = usernameField.getText().trim();
    String password = passwordField.getText().trim();
    String confirmPassword = confirmPasswordField.getText().trim();

    // 2. Kiểm tra giao diện
    if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
      messageLabel.setTextFill(Color.RED);
      messageLabel.setText("Vui lòng nhập đầy đủ thông tin!");
      return;
    }

    if (!password.equals(confirmPassword)) {
      messageLabel.setTextFill(Color.RED);
      messageLabel.setText("Mật khẩu nhập lại không khớp!");
      return;
    }

    // 3. THỰC HIỆN LƯU VÀO DATABASE BẰNG TRY-CATCH
    userService.register(username, password).thenAccept(response -> {
      Platform.runLater(() -> {
        if ("REGISTER_SUCCESS".equals(response.getType())) {
          messageLabel.setTextFill(Color.GREEN);
          messageLabel.setText("Đăng ký thành công! Đang chuyển về Đăng nhập...");

          // Xóa form
          usernameField.clear();
          passwordField.clear();
          confirmPasswordField.clear();

          // Đợi 1.5s rồi chuyển về Login cho người dùng kịp đọc thông báo
          new Thread(() -> {
            try {
              Thread.sleep(1500);
            } catch (Exception ignored) {
            }
            Platform.runLater(() -> {
              try {
                goToLogin(event);
              } catch (Exception ignored) {
              }
            });
          }).start();

        } else {
          messageLabel.setTextFill(Color.RED);
          messageLabel.setText(String.valueOf(response.getData()));
        }
      });
    });
  }

  @FXML
  public void goToLogin(ActionEvent event) throws java.io.IOException {
    javafx.scene.Parent root = javafx.fxml.FXMLLoader.load(getClass().getResource("/Login.fxml"));
    javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
    stage.setScene(new javafx.scene.Scene(root, 350, 450));
    stage.show();
  }
}