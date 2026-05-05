package com.uet.bidding.ui;

import com.uet.bidding.model.User;
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

  // Biến lưu trữ người dùng đang đăng nhập
  private User currentUser;

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    // Không set dữ liệu cứng ở đây nữa
  }

  /**
   * Hàm này dùng để LoginController truyền dữ liệu User sang
   */
  public void setUserData(User user) {
    this.currentUser = user;

    // Đổ dữ liệu từ object User vào giao diện
    txtName.setText(user.getFullName() != null ? user.getFullName() : "");
    txtEmail.setText(user.getEmail() != null ? user.getEmail() : "");
    txtPhone.setText(user.getPhone() != null ? user.getPhone() : "");
    txtAddress.setText(user.getAddress() != null ? user.getAddress() : "");

    if (user.getLinkedBank() != null && !user.getLinkedBank().equals("Chưa liên kết")) {
      lblBankStatus.setText("Trạng thái: Đã liên kết (" + user.getLinkedBank() + ")");
      lblBankStatus.setStyle("-fx-text-fill: #059669;");
    }

    updateBalanceLabel();
  }

  private void updateBalanceLabel() {
    if (currentUser != null) {
      lblBalance.setText(String.format("%,.0f VNĐ", currentUser.getBalance()));
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
        double amount = Double.parseDouble(amountStr);
        if (amount > 0) {
          // Cập nhật vào Model
          currentUser.addFunds(BigDecimal.valueOf(amount));
          // Cập nhật giao diện
          updateBalanceLabel();
          showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã nạp: " + String.format("%,.0f VNĐ", amount));
        }
      } catch (NumberFormatException e) {
        showAlert(Alert.AlertType.ERROR, "Lỗi", "Số tiền không hợp lệ!");
      }
    });
  }

  @FXML
  public void handleSaveInfo(ActionEvent event) {
    if (currentUser != null) {
      // Cập nhật dữ liệu từ giao diện ngược lại vào object Model
      currentUser.setFullName(txtName.getText());
      currentUser.setEmail(txtEmail.getText());
      currentUser.setPhone(txtPhone.getText());
      currentUser.setAddress(txtAddress.getText());

      System.out.println("Đã cập nhật Model cho User: " + currentUser.getUsername());
      showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã lưu thông tin tài khoản!");
    }
  }

  @FXML
  public void handleBack(ActionEvent event) {
    // Sử dụng hàm tiện ích của Main để quay lại danh sách đấu giá
    Main.changeScene("/AuctionList.fxml", "Hệ thống Đấu giá VNU - Dashboard", 900, 600);
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
      }
    });
  }

  private void showAlert(Alert.AlertType type, String title, String content) {
    Alert alert = new Alert(type);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(content);
    alert.showAndWait();
  }
}