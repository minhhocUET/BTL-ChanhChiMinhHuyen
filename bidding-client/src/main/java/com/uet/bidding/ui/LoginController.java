package com.uet.bidding.ui;

import com.uet.bidding.model.User;
import com.uet.bidding.service.UserService;
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

  // 1. Khởi tạo Service để giao tiếp với Cơ sở dữ liệu
  private UserService userService = new UserService();

  @FXML
  public void handleLogin(ActionEvent event) {
    String username = usernameField.getText().trim();
    String password = passwordField.getText().trim();

    if (username.isEmpty() || password.isEmpty()) {
      messageLabel.setStyle("-fx-text-fill: red;");
      messageLabel.setText("Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu!");
      return;
    }

    messageLabel.setStyle("-fx-text-fill: #2563eb;"); // Màu xanh blue báo trạng thái
    messageLabel.setText("Đang kiểm tra thông tin...");

    userService.login(username, password);
    // try {
    // 2. Gọi UserService để xác thực với DB THẬT
    // User loggedInUser = userService.login(username, password);

    // === BƯỚC QUAN TRỌNG: Lưu người dùng vào Session ===
    // UserSession.setCurrentUser(loggedInUser);
    // ==========================================
    // ĐĂNG NHẬP THÀNH CÔNG: CHUYỂN THẲNG SANG AUCTION LIST
    // ==========================================
    // System.out.println("Đăng nhập thành công, vào thẳng Dashboard...");
    // if ("ADMIN".equals(loggedInUser.getRole())) {
    // Nếu là Admin -> Qua Dashboard quản trị
    // loadNextScene(event, "/AdminDashboard.fxml", "Admin Control Panel", loggedInUser);
    // } else {
    // Nếu là người dùng thường -> Vào danh sách đấu giá
    // loadNextScene(event, "/AuctionList.fxml", "Hệ thống Đấu giá VNU", loggedInUser);
    // }

    // } catch (AuthenticationException e) {
    // Bắt lỗi từ Database và in ra màn hình
    // messageLabel.setStyle("-fx-text-fill: red;");
    // messageLabel.setText(e.getMessage()); // Sẽ hiện "Sai mật khẩu!" hoặc "Tài khoản không tồn tại!"
    // passwordField.clear(); // Tiện ích UX: Xóa pass sai đi để người dùng tiện nhập lại
    // }
  }

  /**
   * Hàm Helper: Hỗ trợ load FXML mới, đổi Scene và truyền Object User sang Controller tiếp theo
   */
  private void loadNextScene(ActionEvent event, String fxmlPath, String title, User user) {
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
      Parent root = loader.load();

      // Lấy Controller của màn hình sắp chuyển tới và truyền dữ liệu
      Object controller = loader.getController();

      // Nếu trang AuctionList của bạn có hàm set dữ liệu User, có thể bỏ comment đoạn dưới đây:
      /*
      if (controller instanceof AuctionListController) {
          ((AuctionListController) controller).setCurrentUser(user);
      }
      */

      // Chuyển cửa sổ
      Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
      Scene scene = new Scene(root);
      stage.setScene(scene);
      stage.setTitle(title);
      stage.show();

    } catch (IOException e) {
      e.printStackTrace();
      messageLabel.setStyle("-fx-text-fill: red;");
      messageLabel.setText("Lỗi hệ thống: Không thể tải giao diện " + fxmlPath);
    }
  }

  @FXML
  public void goToRegister(ActionEvent event) {
    Main.changeScene("/Register.fxml", "Hệ thống Đấu giá VNU - Đăng ký", 400, 500);
  }
}