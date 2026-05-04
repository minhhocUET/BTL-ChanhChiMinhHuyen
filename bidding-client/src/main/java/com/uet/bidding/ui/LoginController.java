package com.uet.bidding.ui;

import com.uet.bidding.model.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

  @FXML
  private TextField usernameField;
  @FXML
  private PasswordField passwordField;
  @FXML
  private Label messageLabel;

  @FXML
  public void handleLogin(ActionEvent event) {
    String username = usernameField.getText();
    String password = passwordField.getText();

    if (username.isEmpty() || password.isEmpty()) {
      messageLabel.setText("Vui lòng nhập đầy đủ thông tin!");
      return;
    }

    messageLabel.setText("Đang kiểm tra thông tin...");

    // 1. Xác thực và lấy dữ liệu User
    User loggedInUser = authenticate(username, password);

    if (loggedInUser != null) {
      try {
        // 2. Load file FXML của trang UserProfile
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/UserProfile.fxml"));
        Parent root = loader.load();

        // 3. Truyền dữ liệu User sang UserProfileController
        UserProfileController profileController = loader.getController();
        profileController.setUserData(loggedInUser);

        // 4. Chuyển Scene
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Thông tin cá nhân - " + username);
        stage.show();

      } catch (IOException e) {
        e.printStackTrace();
        messageLabel.setText("Lỗi: Không tìm thấy file UserProfile.fxml");
      }
    } else {
      messageLabel.setText("Sai tài khoản hoặc mật khẩu!");
    }
  }

  private User authenticate(String username, String password) {
    // Tạm thời chấp nhận mọi login để test giao diện
    User user = new User();
    user.setId(1);
    user.setUsername(username);
    user.setFullName("Nguyễn Tuấn Hùng");
    user.setEmail(username + "@vnu.edu.vn");
    user.setPhone("0912345678");
    user.setAddress("Hà Nội, Việt Nam");
    user.setBalance(5000000.0);
    user.setLinkedBank("Chưa liên kết");
    return user;
  }

  @FXML
  public void goToRegister(ActionEvent event) {
    Main.changeScene("/Register.fxml", "Hệ thống Đấu giá VNU - Đăng ký", 400, 500);
  }
}