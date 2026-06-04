package com.uet.bidding.controller.user;

import com.uet.bidding.exception.UserException;
import com.uet.bidding.model.Customer;
import com.uet.bidding.network.ClientService;
import com.uet.bidding.service.UserService;
import com.uet.bidding.util.UserSession;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.net.URL;
import java.util.ResourceBundle;

public class UserProfileController implements Initializable {

  @FXML
  private TextField txtName;
  @FXML
  private TextField txtEmail;
  @FXML
  private TextField txtPhone;
  @FXML
  private TextArea txtAddress;
  @FXML
  private Label lblBalance;

  // --- CÁC FIELD MỚI CHO MẬT KHẨU ---
  @FXML private PasswordField txtOldPassword;
  @FXML private PasswordField txtNewPassword;
  @FXML private PasswordField txtConfirmPassword;

  private Customer currentUser;
  private UserService userService = new UserService();

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    this.currentUser = UserSession.getLoggedInCustomer();

    if (currentUser != null) {
      fillDataToFields();
      updateBalanceLabel();

      // 1. Kiểm tra nếu thực sự là quyền ADMIN đi lạc vào đây (Dựa vào getRole() của bạn)
      if ("ADMIN".equalsIgnoreCase(currentUser.getRole())) {
        System.err.println("Lỗi: Admin không có hồ sơ khách hàng!");
        // Bạn có thể tắt màn hình hoặc chuyển hướng Admin ra chỗ khác ở đây nếu muốn
        return;
      }
    }
  }

  private void fillDataToFields() {
    txtName.setText(currentUser.getFullName() != null ? currentUser.getFullName() : "");
    txtEmail.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "");
    txtPhone.setText(currentUser.getPhone() != null ? currentUser.getPhone() : "");
    txtAddress.setText(currentUser.getAddress() != null ? currentUser.getAddress() : "");
  }

  public void setUserData(Customer user) { // ĐỔI User THÀNH Customer Ở ĐÂY
    this.currentUser = user;

    // 1. Kiểm tra xem tài khoản có phải là mới (thiếu thông tin) không
    boolean isMissingInfo = (user.getFullName() == null || user.getEmail() == null);

    if (isMissingInfo) {
      txtName.setPromptText("Bắt buộc nhập họ tên");
      txtEmail.setPromptText("Bắt buộc nhập email");
      txtPhone.setPromptText("Bắt buộc nhập số điện thoại");
      txtAddress.setPromptText("Bắt buộc nhập địa chỉ");
    }

    // 2. Đổ dữ liệu cũ lên giao diện (nếu là null thì set thành chuỗi rỗng để tránh lỗi)
    txtName.setText(user.getFullName() != null ? user.getFullName() : "");
    txtEmail.setText(user.getEmail() != null ? user.getEmail() : "");
    txtPhone.setText(user.getPhone() != null ? user.getPhone() : "");
    txtAddress.setText(user.getAddress() != null ? user.getAddress() : "");

    updateBalanceLabel();
  }

  private void updateBalanceLabel() {
    if (currentUser != null && currentUser.getBalance() != null) {
      lblBalance.setText(String.format("%,.0f VNĐ", currentUser.getBalance()));
    } else {
      lblBalance.setText("0 VNĐ");
    }
  }

  @FXML
  public void handleSaveInfo() {
    if (currentUser == null) return;

    // 1. Lấy dữ liệu từ giao diện
    String fullName = txtName.getText().trim();
    String email = txtEmail.getText().trim();
    String phone = txtPhone.getText().trim();
    String address = txtAddress.getText().trim();

    // 2. Kiểm tra hợp lệ (Validation)
    if (fullName.isEmpty() || email.isEmpty() || phone.isEmpty() || address.isEmpty()) {
      showAlert(Alert.AlertType.ERROR, "Thiếu thông tin", "Vui lòng nhập đầy đủ thông tin cá nhân!");
      return;
    }

    // 3. Cập nhật tạm thời vào object gửi đi
    currentUser.setFullName(fullName);
    currentUser.setEmail(email);
    currentUser.setPhone(phone);
    currentUser.setAddress(address);
    currentUser.setProfileComplete(true);

    // 4. GỬI YÊU CẦU VÀ LẮNG NGHE PHẢN HỒI TỪ SERVER
    ClientService.getInstance()
        .sendRequest("UPDATE_PROFILE", currentUser)
        .thenAccept(response -> {
          Platform.runLater(() -> {
            // Kiểm tra tín hiệu xử lý thành công từ Backend
            if ("UPDATE_PROFILE_SUCCESS".equals(response.getType())) {
              showAlert(Alert.AlertType.INFORMATION, "Thành công", "Cập nhật thông tin cá nhân thành công! 🎉");
            } else {
              // Hiển thị lỗi từ Server (nếu có, ví dụ: Trùng Email, sai định dạng...)
              String errorDetail = response.getData() != null ? String.valueOf(response.getData()) : "Không rõ nguyên nhân.";
              showAlert(Alert.AlertType.ERROR, "Thất bại", "Không thể lưu thông tin: " + errorDetail);
            }
          });
        })
        .exceptionally(ex -> {
          Platform.runLater(() ->
              showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", "Không thể kết nối đến máy chủ để lưu thông tin: " + ex.getMessage())
          );
          return null;
        });
  }

  @FXML
  public void handleAddFunds() {

    TextInputDialog dialog = new TextInputDialog("100000"); // Mặc định 100k
    dialog.setTitle("Nạp tiền");
    dialog.setHeaderText("Nạp tiền vào tài khoản");
    dialog.setContentText("Nhập số tiền (VNĐ):");

    dialog.showAndWait().ifPresent(amountStr -> {
      try {
        BigDecimal amount = new BigDecimal(amountStr);

        // 1. Gửi lệnh nạp tiền lên Server (Truyền biến amount, không truyền ZERO)
        // Lưu ý: Đảm bảo userService.addBalance của bạn có gọi sang ClientService.sendRequest
        userService.addBalance(amount);

        // 2. Cập nhật số dư tạm thời trong bộ nhớ để hiển thị ngay
        BigDecimal newBalance = currentUser.getBalance().add(amount);
        currentUser.setBalance(newBalance);

        // 3. Cập nhật lại nhãn trên giao diện
        updateBalanceLabel();

        showAlert(Alert.AlertType.INFORMATION, "Thành công",
            "Đã nạp thành công: " + String.format("%,.0f VNĐ", amount));

      } catch (NumberFormatException e) {
        showAlert(Alert.AlertType.ERROR, "Lỗi", "Số tiền không hợp lệ!");
      } catch (UserException e) {
        showAlert(Alert.AlertType.ERROR, "Lỗi nạp tiền", e.getMessage());
      }
    });
  }

  // ==========================================
  // 🔐 XỬ LÝ ĐỔI MẬT KHẨU
  // ==========================================
  @FXML
  public void handleChangePassword() {
    String oldPass = txtOldPassword.getText();
    String newPass = txtNewPassword.getText();
    String confirmPass = txtConfirmPassword.getText();

    // 1. Chỉ kiểm tra rỗng cơ bản tại Client để tránh gửi chuỗi trống bừa bãi
    if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
      showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng nhập đầy đủ các trường mật khẩu!");
      return;
    }

    // 2. Kiểm tra độ dài cơ bản
    if (newPass.length() < 6) {
      showAlert(Alert.AlertType.WARNING, "Mật khẩu yếu", "Mật khẩu mới phải có ít nhất 6 ký tự!");
      return;
    }

    // 🎯 QUAN TRỌNG: Ghép ĐẦY ĐỦ cả 3 trường gửi lên Server: oldPass|newPass|confirmPass
    String payload = oldPass + "|" + newPass + "|" + confirmPass;

    ClientService.getInstance().sendRequest("CHANGE_PASSWORD", payload).thenAccept(response -> {
      Platform.runLater(() -> {
        if ("SUCCESS".equals(response.getType())) {
          showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đổi mật khẩu thành công!");
          txtOldPassword.clear();
          txtNewPassword.clear();
          txtConfirmPassword.clear();
        } else {
          // Nếu Server trả về lỗi (Ví dụ: "Mật khẩu cũ không chính xác!"), nó sẽ hiển thị ở đây
          showAlert(Alert.AlertType.ERROR, "Đổi mật khẩu thất bại", String.valueOf(response.getData()));
        }
      });
    }).exceptionally(ex -> {
      Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi mạng", "Không thể kết nối đến máy chủ!"));
      return null;
    });
  }

  // ==========================================
  // XỬ LÝ CHUYỂN TRANG
  // ==========================================

  @FXML
  public void handleBack(ActionEvent event) {
    switchScene(event, "/AuctionList.fxml", "Hệ thống Đấu giá VNU");
  }

  // Hàm bổ trợ chuyển trang để tránh lặp code
  private void switchScene(ActionEvent event, String fxmlPath, String title) {
    try {
      Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
      Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
      stage.setScene(new Scene(root));
      stage.setTitle(title);
      stage.show();
    } catch (Exception e) {
      showAlert(Alert.AlertType.ERROR, "Lỗi điều hướng", "Không thể chuyển trang: " + e.getMessage());
    }
  }

  private void showAlert(Alert.AlertType type, String title, String content) {
    Alert alert = new Alert(type);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(content);
    alert.showAndWait();
  }
}