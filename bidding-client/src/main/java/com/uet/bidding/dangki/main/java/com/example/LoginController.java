package main.java.com.uet.bidding.dangki.main.java.com.example;

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

        // 4. Gọi một hàm tổng quát để kiểm tra thông tin (Logic thực sự nằm ở đây)
        boolean isLoginSuccess = checkUserInDatabase(username, password);

        // 5. Xử lý kết quả trả về
        if (isLoginSuccess) {
            messageLabel.setText("Đăng nhập thành công!");

            // Code chuyển sang màn hình chính của ứng dụng
            // goToMainScreen(event);

        } else {
            messageLabel.setText("Sai tài khoản hoặc mật khẩu!");
        }
    }
    // Hàm tổng quát dùng để kiểm tra tài khoản từ Database
    private boolean checkUserInDatabase(String username, String password) {
        // Nếu bạn đang dùng MySQL hoặc SQL Server, code sẽ có dạng như sau:
    /*
    String dbUrl = "jdbc:mysql://localhost:3306/ten_database_cua_ban";
    String dbUser = "root";
    String dbPass = "mat_khau_db";

    // Câu lệnh SQL để tìm tài khoản
    String sql = "SELECT * FROM users WHERE username = ? AND password = ?";

    try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

        // Truyền tham số vào câu SQL
        pstmt.setString(1, username);
        pstmt.setString(2, password);

        // Thực thi và kiểm tra xem có kết quả không
        ResultSet rs = pstmt.executeQuery();

        if (rs.next()) {
            return true; // Tìm thấy tài khoản -> Đăng nhập thành công
        }

    } catch (SQLException e) {
        e.printStackTrace();
        // Nếu có lỗi mạng hoặc database, in ra console
    }
    return false; // Không tìm thấy hoặc có lỗi
    */

        // Tạm thời trả về false để không bị báo lỗi thiếu return khi bạn chưa mở comment
        return false;
    }

    // Nút chuyển sang trang Đăng Ký
    @FXML
    public void goToRegister(ActionEvent event) throws IOException {
        // 1. Tải file giao diện Đăng Ký
        Parent root = FXMLLoader.load(getClass().getResource("Register.fxml"));

        // 2. Lấy ra cái Cửa sổ (Stage) hiện tại đang hiển thị
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        // 3. Đắp giao diện Đăng Ký lên cửa sổ đó
        stage.setScene(new Scene(root, 350, 450));
        stage.show();
    }
}