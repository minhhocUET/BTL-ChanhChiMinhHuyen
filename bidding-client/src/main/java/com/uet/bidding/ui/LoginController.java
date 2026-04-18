package com.uet.bidding.ui;

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
import java.net.Socket;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    @FXML
    public void handleLogin(ActionEvent event) {
        // 1. Lấy thông tin từ giao diện
        String username = usernameField.getText();
        String password = passwordField.getText();

        // 2. Kiểm tra dữ liệu rỗng
        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        // 3. Thông báo đang xử lý để người dùng chờ
        messageLabel.setText("Đang kiểm tra thông tin...");

        // 4. Gọi hàm kiểm tra database
        boolean isLoginSuccess = checkUserInDatabase(username, password);

        // 5. Xử lý kết quả trả về
        if (isLoginSuccess) {
            messageLabel.setText("Đăng nhập thành công!");

            // Gọi hàm chuyển sang màn hình chính của ứng dụng
            try {
                goToMainScreen(event);
            } catch (IOException e) {
                e.printStackTrace();
                messageLabel.setText("Lỗi: Không thể tải giao diện đấu giá!");
            }
        } else {
            messageLabel.setText("Sai tài khoản hoặc mật khẩu!");
        }
    }

    // Hàm tổng quát dùng để kiểm tra tài khoản từ Database
    private boolean checkUserInDatabase(String username, String password) {
        // ... (Đoạn code SQL của bạn vẫn giữ nguyên ở đây để sau này dùng) ...

        // SỬA Ở ĐÂY: Tạm thời trả về TRUE để test việc chuyển trang sang AuctionList
        return true;
    }

    // HÀM MỚI: CHUYỂN SANG GIAO DIỆN ĐẤU GIÁ
    private void goToMainScreen(ActionEvent event) throws IOException {
        // 1. Tải file giao diện Đấu giá
        Parent root = FXMLLoader.load(getClass().getResource("/AuctionList.fxml"));

        // 2. Lấy ra cái Cửa sổ (Stage) hiện tại đang hiển thị
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        // 3. Đắp giao diện Đấu giá lên cửa sổ đó (để kích thước to hơn cho đẹp)
        stage.setScene(new Scene(root, 900, 600));
        stage.setTitle("Hệ thống Đấu giá VNU - Dashboard");
        stage.centerOnScreen(); // Căn giữa màn hình
        stage.show();
    }

    // Nút chuyển sang trang Đăng Ký
    @FXML
    public void goToRegister(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/Register.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 350, 450));
        stage.show();
    }

    public void xuLyDangNhap() {
        try {
            Socket socket = new Socket("localhost", 8080);
            System.out.println("🟢 Đã kết nối tới Server thành công!");
        } catch (IOException e) {
            System.out.println("🔴 Lỗi: Không tìm thấy Server. Hãy chắc chắn Server đang chạy!");
        }
    }
}
