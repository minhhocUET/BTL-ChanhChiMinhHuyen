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
      showAlert(Alert.AlertType.WARNING, "Cập nhật hồ sơ", "Vui lòng điền ĐẦY ĐỦ tất cả thông tin cá nhân và liên kết ngân hàng trước khi sử dụng hệ thống!");
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
      // 1. Gán toàn bộ dữ liệu từ form vào Object (Bao gồm cả việc họ để rỗng)
      currentUser.setFullName(txtName.getText().trim());
      currentUser.setEmail(txtEmail.getText().trim());
      currentUser.setPhone(txtPhone.getText().trim());
      currentUser.setAddress(txtAddress.getText().trim());

      try {
        // 2. Gọi Service để LƯU XUỐNG DATABASE
        // Không cần `if-else` ở Controller nữa, vì Service đã bao thầu toàn bộ bài test khắt khe nhất!
        userService.updateUser(currentUser);
        showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã cập nhật hồ sơ vào hệ thống! Bạn có thể vào Trang chủ.");

      } catch (UserException e) {
        // Nếu bắt được bất kỳ lỗi nào từ Service (thiếu tên, sđt chứa chữ cái...), hiển thị đỏ chót!
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
        // Gán trạng thái vào User hiện tại và đổi màu UI
        currentUser.setLinkedBank("Vietcombank");
        lblBankStatus.setText("Trạng thái: Đã liên kết (Vietcombank)");
        lblBankStatus.setStyle("-fx-text-fill: #059669;");

        // CẢNH BÁO UX: Khuyên người dùng ấn Lưu để ghi nhận đồng loạt xuống DB
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

  @FXML
  public void handleBack(ActionEvent event) {
    // NGƯỜI GÁC CỔNG: Kiểm tra KHẮT KHE xem các trường đã có dữ liệu thật trong Object chưa
    boolean isInvalid = (currentUser.getFullName() == null || currentUser.getFullName().isEmpty() ||
        currentUser.getEmail() == null || currentUser.getEmail().isEmpty() ||
        currentUser.getPhone() == null || currentUser.getPhone().isEmpty() ||
        currentUser.getAddress() == null || currentUser.getAddress().isEmpty() ||
        currentUser.getLinkedBank() == null || currentUser.getLinkedBank().isEmpty());

    if (isInvalid) {
      showAlert(Alert.AlertType.WARNING, "Cảnh báo bảo mật", "Bạn phải điền ĐẦY ĐỦ thông tin và ấn LƯU trước khi vào hệ thống đấu giá!");
      return; // Chặn đứng lệnh chuyển trang
    }

    // Nếu pass qua được trạm gác trên -> Cho phép vào thẳng Dashboard
    Main.changeScene("/AuctionList.fxml", "Hệ thống Đấu giá VNU - Dashboard", 900, 600);
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