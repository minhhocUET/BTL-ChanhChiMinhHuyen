package com.uet.bidding.ui; // Thêm package cho khớp với thư mục của bạn

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;

public class Main extends Application {

    // Biến toàn cục lưu trữ Cửa sổ (Window) chính
    private static Stage window;

    @Override
    public void start(Stage primaryStage) {
        window = primaryStage;

        // 1. Mặc định mở Giao diện đăng nhập đầu tiên khi chạy
        // Dùng luôn hàm changeScene cho đồng nhất code
        changeScene("/Login.fxml", "Hệ thống Đấu giá VNU - Đăng nhập", 400, 500);
        window.show();
    }

    // 2. Hàm dùng chung để chuyển đổi màn hình (có bắt lỗi an toàn)
    public static void changeScene(String fxmlFile, String title, int width, int height) {
        try {
            // Tìm file FXML
            URL fxmlLocation = Main.class.getResource(fxmlFile);

            // Nếu không tìm thấy file, in ra cảnh báo đỏ để dễ sửa
            if (fxmlLocation == null) {
                System.err.println("LỖI NGHIÊM TRỌNG: Không tìm thấy file FXML -> " + fxmlFile);
                System.err.println("Hãy chắc chắn file " + fxmlFile + " nằm trong thư mục src/main/resources/");
                return;
            }

            // Load file và set Scene
            Parent pane = FXMLLoader.load(fxmlLocation);
            window.setTitle(title);
            window.setScene(new Scene(pane, width, height));
            window.centerOnScreen(); // Tự động căn giữa màn hình

        } catch (IOException e) {
            System.err.println("LỖI: Có vấn đề bên trong file FXML hoặc Controller của: " + fxmlFile);
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
