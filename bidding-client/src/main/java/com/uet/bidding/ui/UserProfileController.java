package com.uet.bidding.ui;

import com.uet.bidding.exception.UserException;
import com.uet.bidding.model.User;
import com.uet.bidding.service.UserService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

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

  // Tích hợp Service để thao tác với Database
  private UserService userService = new UserService();

  @Override
  public void initialize(URL location, ResourceBundle resources) {
  }

  public void setUserData(User user) {
    this.currentUser = user;

    // Nếu là tài khoản mới đăng ký (dùng email ảo), nhắc nhở họ
    if (user.getEmail().endsWith("@temp.uet.bidding.vn")) {
      txtName.setPromptText("Vui lòng nhập họ tên thật");
      txtEmail.setText(""); // Xóa email ảo đi để họ phải nhập mới
      txtEmail.setPromptText("Vui lòng nhập email thật");
      showAlert(Alert.AlertType.WARNING, "Cập nhật hồ sơ", "Vui lòng cập nhật Họ Tên và Email thật để sử dụng hệ thống!");
    } else {
      txtName.setText(user.getFullName());
      txtEmail.setText(user.getEmail());
    }

    txtPhone.setText(user.getPhone() != null ? user.getPhone() : "");
    txtAddress.setText(user.getAddress() != null ? user.getAddress() : "");

    if (user.getLinkedBank() != null && !user.getLinkedBank().equals("Chưa liên kết")) {
      lblBankStatus.setText("Trạng thái: Đã liên kết (" + user.getLinkedBank() + ")");
      lblBankStatus.setStyle("-fx-text-fill: #059669;");
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
      String newName = txtName.getText().trim();
      String newEmail = txtEmail.getText().trim();

      // Chặn không cho lưu nếu vẫn dùng thông tin ảo hoặc bỏ trống
      if (newName.isEmpty() || newName.startsWith("User ")) {
        showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng nhập Họ tên thật hợp lệ!");
        return;
      }
      if (newEmail.isEmpty() || newEmail.endsWith("@temp.uet.bidding.vn")) {
        showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng nhập Email thật hợp lệ!");
        return;
      }

      // Gán dữ liệu vào Object
      currentUser.setFullName(newName);
      currentUser.setEmail(newEmail);
      currentUser.setPhone(txtPhone.getText().trim());
      currentUser.setAddress(txtAddress.getText().trim());

      try {
        // Gọi Service để LƯU XUỐNG DATABASE (TiDB)
        userService.updateUser(currentUser);
        showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã cập nhật thông tin xuống Cơ sở dữ liệu!");
      } catch (UserException e) {
        showAlert(Alert.AlertType.ERROR, "Lỗi cập nhật", e.getMessage());
      }
    }
  }

  @FXML
  public void handleAddFunds(ActionEvent event) {
    TextInputDialog dialog = new TextInputDialog("1000000");
    dialog.setTitle("Nạp tiền");
    dialog.setHeaderText("Nạp tiền vào tài khoản");
    dialog.setContentText("Nhập số tiền (VNĐ):");

    dialog.showAndWait().ifPresent(amountStr -> {
      try {
        // Chuẩn hóa dùng BigDecimal
        BigDecimal amount = new BigDecimal(amountStr);

        // Gọi Service để nạp tiền (Service sẽ lo cộng dồn và lưu DB)
        userService.addBalance(currentUser, amount);

        // Cập nhật lại giao diện
        updateBalanceLabel();
        showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã nạp thành công: " + String.format("%,.0f VNĐ", amount));

      } catch (NumberFormatException e) {
        showAlert(Alert.AlertType.ERROR, "Lỗi", "Số tiền nhập vào không phải là số hợp lệ!");
      } catch (UserException e) {
        showAlert(Alert.AlertType.ERROR, "Lỗi nạp tiền", e.getMessage());
      }
    });
  }

  @FXML
  public void handleLinkBank(ActionEvent event) {
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
    alert.setTitle("Liên kết ngân hàng");
    alert.setContentText("Bạn có muốn liên kết với Vietcombank?");

    alert.showAndWait().ifPresent(response -> {
      if (response == ButtonType.OK) {
        currentUser.setLinkedBank("Vietcombank");
        try {
          // Lưu trạng thái liên kết ngân hàng xuống Database
          userService.updateUser(currentUser);
          lblBankStatus.setText("Trạng thái: Đã liên kết (Vietcombank)");
          lblBankStatus.setStyle("-fx-text-fill: #059669;");
          showAlert(Alert.AlertType.INFORMATION, "Thành công", "Liên kết ngân hàng thành công!");
        } catch (UserException e) {
          showAlert(Alert.AlertType.ERROR, "Lỗi hệ thống", "Không thể lưu trạng thái liên kết: " + e.getMessage());
        }
      }
    });
  }

  @FXML
  public void handleBack(ActionEvent event) {
    // Bắt buộc phải điền đủ thông tin mới cho thoát ra Dashboard
    if (currentUser.getEmail() == null || currentUser.getEmail().endsWith("@temp.uet.bidding.vn")) {
      showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Bạn phải Lưu thông tin (Họ tên & Email) trước khi sử dụng hệ thống!");
      return;
    }
    Main.changeScene("/AuctionList.fxml", "Hệ thống Đấu giá VNU - Dashboard", 900, 600);
  }

  private void showAlert(Alert.AlertType type, String title, String content) {
    Alert alert = new Alert(type);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(content);
    alert.showAndWait();
  }
}