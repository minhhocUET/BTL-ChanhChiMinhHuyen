package com.uet.bidding.controller;

import com.uet.bidding.exception.UserException;
import com.uet.bidding.model.Customer;
import com.uet.bidding.model.User;
import com.uet.bidding.network.ClientService;
import com.uet.bidding.service.UserService;
import com.uet.bidding.util.UserSession;
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
  @FXML
  private Label lblBankStatus;

  private Customer currentUser;
  private UserService userService = new UserService();

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    // Sử dụng hàm helper bạn đã viết trong UserSession
    this.currentUser = UserSession.getLoggedInCustomer();

    if (currentUser != null) {
      fillDataToFields();
      updateBalanceLabel();

      // Thông báo nhắc nhở nếu hồ sơ chưa hoàn thiện
      if (currentUser.getFullName() == null || currentUser.getFullName().isEmpty()) {
        showAlert(Alert.AlertType.WARNING, "Yêu cầu cập nhật",
            "Vui lòng hoàn thiện TẤT CẢ thông tin để có thể tham gia đấu giá hoặc đăng bán sản phẩm.");
      } else {
      // Nếu là Admin đi lạc vào đây thì đá ra ngoài hoặc báo lỗi
        System.err.println("Lỗi: Admin không có hồ sơ khách hàng!");
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
      showAlert(Alert.AlertType.WARNING, "Cập nhật hồ sơ", "Vui lòng điền ĐẦY ĐỦ tất cả thông tin cá nhân và liên kết ngân hàng trước khi tham gia đấu giá!");
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
  public void handleSaveInfo(ActionEvent event) {
    if (currentUser == null) return;

    // 1. Lấy dữ liệu từ giao diện
    String fullName = txtName.getText().trim();
    String email = txtEmail.getText().trim();
    String phone = txtPhone.getText().trim();
    String address = txtAddress.getText().trim();

    // 2. Kiểm tra hợp lệ (Validation)
    if (fullName.isEmpty() || email.isEmpty() || phone.isEmpty() || address.isEmpty()) {
      showAlert(Alert.AlertType.ERROR, "Thiếu thông tin", "Vui lòng nhập đầy đủ thông tin cá nhân và ngân hàng!");
      return;
    }

    // 3. Tạo một bản sao hoặc cập nhật tạm thời vào object gửi đi
    // Đừng cập nhật thẳng vào currentUser ngay nếu bạn muốn an toàn tuyệt đối
    currentUser.setFullName(fullName);
    currentUser.setEmail(email);
    currentUser.setPhone(phone);
    currentUser.setAddress(address);
    currentUser.setProfileComplete(true);

    // 4. CHỈ GỬI YÊU CẦU
    ClientService.getInstance().sendRequest("UPDATE_PROFILE", currentUser);
  }

  @FXML
  public void handleAddFunds(ActionEvent event) {

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
  // XỬ LÝ CHUYỂN TRANG
  // ==========================================

  @FXML
  public void handleBack(ActionEvent event) {
    switchScene(event, "/AuctionList.fxml", "Hệ thống Đấu giá VNU");
  }

  @FXML
  public void handleGoToMyManagement(ActionEvent event) {
    switchScene(event, "/MyManagement.fxml", "Quản lý của tôi");
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