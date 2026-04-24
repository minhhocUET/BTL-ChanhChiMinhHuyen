package com.uet.bidding.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
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
            // Gọi thẳng hàm chuyển trang mà không cần try-catch lằng nhằng nữa
            goToMainScreen();
        } else {
            messageLabel.setText("Sai tài khoản hoặc mật khẩu!");
        }
    }

    // Hàm tổng quát dùng để kiểm tra tài khoản từ Database
    private boolean checkUserInDatabase(String username, String password) {
        // ... (Đoạn code SQL của bạn vẫn giữ nguyên ở đây để sau này dùng) ...

        // Tạm thời trả về TRUE để test việc chuyển trang sang AuctionList
        return true;
    }

    // ĐÃ SỬA: Dùng hàm của Main để chuyển sang giao diện Đấu giá
    private void goToMainScreen() {
        // Chỉ cần 1 dòng duy nhất thay vì 4 dòng như cũ
        Main.changeScene("/AuctionList.fxml", "Hệ thống Đấu giá VNU - Dashboard", 900, 600);
    }

    // ĐÃ SỬA: Nút chuyển sang trang Đăng Ký
    @FXML
    public void goToRegister(ActionEvent event) {
        // Kích thước 400x500 (bạn có thể tự chỉnh lại cho khớp form đăng ký của bạn)
        Main.changeScene("/Register.fxml", "Hệ thống Đấu giá VNU - Đăng ký", 400, 500);
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
