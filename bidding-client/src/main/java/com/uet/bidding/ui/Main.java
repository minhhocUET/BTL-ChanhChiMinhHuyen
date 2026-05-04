package com.uet.bidding.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class Main extends Application {

    // Biến toàn cục lưu trữ Cửa sổ (Window) chính
    private static Stage window;

    @Override
    public void start(Stage primaryStage) throws Exception {
        window = primaryStage;

        // 1. Mặc định mở Giao diện đăng nhập đầu tiên
        Parent root = FXMLLoader.load(getClass().getResource("/Login.fxml"));
        window.setTitle("Hệ thống Đấu giá VNU - Đăng nhập");
        window.setScene(new Scene(root, 400, 500)); // Chỉnh kích thước cho khớp với form đăng nhập
        window.show();
    }

    // 2. Hàm dùng chung để chuyển đổi màn hình
    public static void changeScene(String fxmlFile, String title, int width, int height) throws IOException {
        Parent pane = FXMLLoader.load(Main.class.getResource(fxmlFile));
        window.setTitle(title);
        window.setScene(new Scene(pane, width, height));
        window.centerOnScreen(); // Tự động căn giữa màn hình cho đẹp
    }

    public static void main(String[] args) {
        launch(args);
    }
}