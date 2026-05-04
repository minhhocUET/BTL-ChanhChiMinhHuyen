package com.uet.bidding.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;

public class RegisterController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label messageLabel;

    @FXML
    void handleRegister(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            messageLabel.setTextFill(Color.RED);
            messageLabel.setText("Vui lòng nhập đầy đủ thông tin!");
            return;
        }


        if (!password.equals(confirmPassword)) {
            messageLabel.setTextFill(Color.RED);
            messageLabel.setText("Mật khẩu nhập lại không khớp!");
            return;
        }

        messageLabel.setTextFill(Color.GREEN);
        messageLabel.setText("Đăng ký thành công!");

        // In ra console để kiểm tra
        System.out.println("Tài khoản mới: " + username);
        System.out.println("Mật khẩu: " + password);

    }

    @FXML
    public void goToLogin(javafx.event.ActionEvent event) throws java.io.IOException {
        javafx.scene.Parent root = javafx.fxml.FXMLLoader.load(getClass().getResource("/Login.fxml"));
        javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        stage.setScene(new javafx.scene.Scene(root, 350, 450));
        stage.show();
    }
}
