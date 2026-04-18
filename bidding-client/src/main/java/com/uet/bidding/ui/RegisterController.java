package com.uet.bidding.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;

public class RegisterController {

    // Liên kết với các thành phần bên Scene Builder qua fx:id
    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label messageLabel;

    // Phương thức xử lý khi bấm nút Đăng ký
    @FXML
    void handleRegister(ActionEvent event) {
        // Lấy dữ liệu từ giao diện
        String username = usernameField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // 1. Kiểm tra không được để trống
        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            messageLabel.setTextFill(Color.RED);
            messageLabel.setText("Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        // 2. Kiểm tra mật khẩu và nhập lại mật khẩu có khớp nhau không
        if (!password.equals(confirmPassword)) {
            messageLabel.setTextFill(Color.RED);
            messageLabel.setText("Mật khẩu nhập lại không khớp!");
            return;
        }

        // 3. Nếu mọi thứ hợp lệ (Xử lý lưu vào Database ở đây)
        messageLabel.setTextFill(Color.GREEN);
        messageLabel.setText("Đăng ký thành công!");

        // In ra console để kiểm tra
        System.out.println("Tài khoản mới: " + username);
        System.out.println("Mật khẩu: " + password);

        // (Tùy chọn) Xóa trắng các ô nhập liệu sau khi đăng ký thành công
        // usernameField.clear();
        // passwordField.clear();
        // confirmPasswordField.clear();
    }
    @FXML
    public void goToLogin(javafx.event.ActionEvent event) throws java.io.IOException {
        javafx.scene.Parent root = javafx.fxml.FXMLLoader.load(getClass().getResource("/Login.fxml"));
        javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        stage.setScene(new javafx.scene.Scene(root, 350, 450));
        stage.show();
    }
}
