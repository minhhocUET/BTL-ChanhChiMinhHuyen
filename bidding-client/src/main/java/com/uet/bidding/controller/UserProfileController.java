package com.uet.bidding.controller;

import com.uet.bidding.exception.UserException;
// Thay đổi import từ User sang Customer
import com.uet.bidding.model.Customer;
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

  // 1. Đổi kiểu dữ liệu từ User thành Customer
  private Customer currentUser;
  private UserService userService = new UserService();

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    // 2. Ép kiểu dữ liệu từ Session về Customer (Giả sử UserSession trả về User hoặc Customer)
    Object sessionUser = UserSession.getCurrentUser();
    if (sessionUser instanceof Customer) {
      this.currentUser = (Customer) sessionUser;
    }

    if (currentUser != null) {
      fillDataToFields();
      updateBalanceLabel();
      updateBankStatusDisplay();

      if (currentUser.getFullName() == null || currentUser.getFullName().isEmpty()) {
        showAlert(Alert.AlertType.WARNING, "Yêu cầu cập nhật",
            "Vui lòng hoàn thiện TẤT CẢ thông tin để có thể tham gia đấu giá hoặc đăng bán sản phẩm.");
      }
    }
  }

  private void fillDataToFields() {
    txtName.setText(currentUser.getFullName() != null ? currentUser.getFullName() : "");
    txtEmail.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "");
    txtPhone.setText(currentUser.getPhone() != null ? currentUser.getPhone() : "");
    txtAddress.setText(currentUser.getAddress() != null ? currentUser.getAddress() : "");
  }

  private void updateBankStatusDisplay() {
    if (currentUser.getLinkedBank() != null && !currentUser.getLinkedBank().trim().isEmpty()) {
      lblBankStatus.setText("Trạng thái: Đã liên kết (" + currentUser.getLinkedBank() + ")");
      lblBankStatus.setStyle("-fx-text-fill: #059669; -fx-font-weight: bold;");
    } else {
      lblBankStatus.setText("Trạng thái: Chưa liên kết (Bắt buộc)");
      lblBankStatus.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
    }
  }

  // 3. Đổi tham số truyền vào từ User thành Customer
  public void setUserData(Customer user) {
    this.currentUser = user;

    boolean isMissingInfo = (user.getFullName() == null || user.getEmail() == null);

    if (isMissingInfo) {
      txtName.setPromptText("Bắt buộc nhập họ tên");
      txtEmail.setPromptText("Bắt buộc nhập email");
      txtPhone.setPromptText("Bắt buộc nhập số điện thoại");
      txtAddress.setPromptText("Bắt buộc nhập địa chỉ");
      showAlert(Alert.AlertType.WARNING, "Cập nhật hồ sơ", "Vui lòng điền ĐẦY ĐỦ tất cả thông tin cá nhân và liên kết ngân hàng trước khi tham gia đấu giá!");
    }

    txtName.setText(user.getFullName() != null ? user.getFullName() : "");
    txtEmail.setText(user.getEmail() != null ? user.getEmail() : "");
    txtPhone.setText(user.getPhone() != null ? user.getPhone() : "");
    txtAddress.setText(user.getAddress() != null ? user.getAddress() : "");

    updateBankStatusDisplay();
    updateBalanceLabel();
  }

  private void updateBalanceLabel() {
    if (currentUser != null && currentUser.getBalance() != null) {
      // Hiển thị số dư từ Customer
      lblBalance.setText(String.format("%,.0f VNĐ", currentUser.getBalance()));
    } else {
      lblBalance.setText("0 VNĐ");
    }
  }

  @FXML
  public void handleSaveInfo(ActionEvent event) {
    if (currentUser == null) return;

    String fullName = txtName.getText().trim();
    String email = txtEmail.getText().trim();
    String phone = txtPhone.getText().trim();
    String address = txtAddress.getText().trim();
    String bank = currentUser.getLinkedBank();

    if (fullName.isEmpty() || email.isEmpty() || phone.isEmpty() || address.isEmpty() || bank == null || bank.trim().isEmpty()) {
      showAlert(Alert.AlertType.ERROR, "Thiếu thông tin", "Vui lòng nhập đầy đủ thông tin cá nhân và ngân hàng!");
      return;
    }

    currentUser.setFullName(fullName);
    currentUser.setEmail(email);
    currentUser.setPhone(phone);
    currentUser.setAddress(address);
    currentUser.setProfileComplete(true);

    ClientService.getInstance().sendRequest("UPDATE_PROFILE", currentUser);
    UserSession.setCurrentUser(currentUser);

    showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã cập nhật hồ sơ cá nhân!");
  }

  @FXML
  public void handleLinkBank(ActionEvent event) {
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
    alert.setTitle("Liên kết ngân hàng");
    alert.setContentText("Bạn có muốn liên kết với Vietcombank?");

    alert.showAndWait().ifPresent(response -> {
      if (response == ButtonType.OK) {
        currentUser.setLinkedBank("Vietcombank");
        updateBankStatusDisplay(); // Cập nhật lại UI
        showAlert(Alert.AlertType.INFORMATION, "Ghi nhận", "Đã ghi nhận yêu cầu liên kết. Vui lòng ấn nút 'Lưu thông tin' để hoàn tất!");
      }
    });
  }

  @FXML
  public void handleAddFunds(ActionEvent event) {
    if (currentUser.getLinkedBank() == null || currentUser.getLinkedBank().trim().isEmpty()) {
      showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng liên kết ngân hàng trước khi nạp tiền!");
      return;
    }

    TextInputDialog dialog = new TextInputDialog("100000");
    dialog.setTitle("Nạp tiền");
    dialog.setHeaderText("Nạp tiền vào tài khoản");
    dialog.setContentText("Nhập số tiền (VNĐ):");

    dialog.showAndWait().ifPresent(amountStr -> {
      try {
        BigDecimal amount = new BigDecimal(amountStr);

        // Gọi phương thức addFunds của Customer
        currentUser.addFunds(amount);

        // Cập nhật lên Server thông qua UserService
        userService.addBalance(amount);

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

  @FXML
  public void handleBack(ActionEvent event) {
    switchScene(event, "/AuctionList.fxml", "Hệ thống Đấu giá VNU");
  }

  @FXML
  public void handleGoToMyManagement(ActionEvent event) {
    switchScene(event, "/MyManagement.fxml", "Quản lý của tôi");
  }

  private void switchScene(ActionEvent event, String fxmlPath, String title) {
    try {
      Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
      Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
      stage.setScene(new Scene(root));
      stage.setTitle(title);
      stage.show();
    } catch (Exception e) {
      e.printStackTrace();
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