package com.uet.bidding.ui;

import com.uet.bidding.exception.UserException;
import com.uet.bidding.model.User;
import com.uet.bidding.service.UserService;
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

  @FXML private TextField txtName;
  @FXML private TextField txtEmail;
  @FXML private TextField txtPhone;
  @FXML private TextArea txtAddress;
  @FXML private Label lblBalance;
  @FXML private Label lblBankStatus;

  private User currentUser;
  private UserService userService = new UserService();

  @Override
  public void initialize(URL location, ResourceBundle resources) {
  }

  public void setUserData(User user) {
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

    // 3. Hiển thị trạng thái ngân hàng
    if (user.getLinkedBank() != null && !user.getLinkedBank().trim().isEmpty()) {
      lblBankStatus.setText("Trạng thái: Đã liên kết (" + user.getLinkedBank() + ")");
      lblBankStatus.setStyle("-fx-text-fill: #059669;"); // Màu xanh lá
    } else {
      lblBankStatus.setText("Trạng thái: Chưa liên kết");
      lblBankStatus.setStyle("-fx-text-fill: red;");
    }

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
    if (currentUser != null) {
      // 1. Gán toàn bộ dữ liệu từ form vào Object
      currentUser.setFullName(txtName.getText().trim());
      currentUser.setEmail(txtEmail.getText().trim());
      currentUser.setPhone(txtPhone.getText().trim());
      currentUser.setAddress(txtAddress.getText().trim());

      try {
        // 2. Gọi Service để LƯU XUỐNG DATABASE
        userService.updateUser(currentUser);
        showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã cập nhật hồ sơ vào hệ thống!");

      } catch (UserException e) {
        // Nếu bắt được lỗi từ Service (thiếu tên, sđt chứa chữ cái...)
        showAlert(Alert.AlertType.ERROR, "Lỗi cập nhật", e.getMessage());
      }
    }
  }

  @FXML
  public void handleLinkBank(ActionEvent event) {
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
    alert.setTitle("Liên kết ngân hàng");
    alert.setContentText("Bạn có muốn liên kết với Vietcombank?");

    alert.showAndWait().ifPresent(response -> {
      if (response == ButtonType.OK) {
        currentUser.setLinkedBank("Vietcombank");
        lblBankStatus.setText("Trạng thái: Đã liên kết (Vietcombank)");
        lblBankStatus.setStyle("-fx-text-fill: #059669;");

        showAlert(Alert.AlertType.INFORMATION, "Ghi nhận", "Đã ghi nhận yêu cầu liên kết. Vui lòng ấn nút 'Lưu thông tin' để hoàn tất!");
      }
    });
  }

  @FXML
  public void handleAddFunds(ActionEvent event) {
    TextInputDialog dialog = new TextInputDialog("1000000");
    dialog.setTitle("Nạp tiền");
    dialog.setHeaderText("Nạp tiền vào tài khoản");
    dialog.setContentText("Nhập số tiền (VNĐ):");

    dialog.showAndWait().ifPresent(amountStr -> {
      try {
        BigDecimal amount = new BigDecimal(amountStr);
        userService.addBalance(currentUser, amount);
        updateBalanceLabel();
        showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã nạp thành công: " + String.format("%,.0f VNĐ", amount));
      } catch (NumberFormatException e) {
        showAlert(Alert.AlertType.ERROR, "Lỗi", "Số tiền nhập vào không phải là số hợp lệ!");
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
    try {
      // Load trực tiếp file AuctionList.fxml
      Parent root = FXMLLoader.load(getClass().getResource("/AuctionList.fxml"));
      Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
      stage.setScene(new Scene(root));
      stage.setTitle("Hệ thống Đấu giá VNU - Dashboard");
      stage.show();
    } catch (Exception e) {
      e.printStackTrace();
      showAlert(Alert.AlertType.ERROR, "Lỗi điều hướng", "Không thể mở trang Danh sách đấu giá: " + e.getMessage());
    }
  }

  // HÀM MỚI THÊM: Xử lý nút chuyển sang trang Quản lý của tôi
  @FXML
  public void handleGoToMyManagement(ActionEvent event) {
    try {
      Parent root = FXMLLoader.load(getClass().getResource("/MyManagement.fxml"));
      Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
      stage.setScene(new Scene(root));
      stage.setTitle("Quản lý của tôi");
      stage.show();
    } catch (Exception e) {
      e.printStackTrace();
      showAlert(Alert.AlertType.ERROR, "Lỗi điều hướng", "Không thể mở trang Quản lý của tôi: " + e.getMessage());
    }
  }

  // Hàm tiện ích hiển thị Popup
  private void showAlert(Alert.AlertType type, String title, String content) {
    Alert alert = new Alert(type);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(content);
    alert.showAndWait();
  }
}