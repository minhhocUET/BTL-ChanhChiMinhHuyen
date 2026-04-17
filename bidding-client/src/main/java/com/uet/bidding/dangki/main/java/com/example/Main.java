package main.java.com.uet.bidding.dangki.main.java.com.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Tải file FXML từ thư mục resources
        Parent root = FXMLLoader.load(getClass().getResource("Login.fxml"));
        // Cài đặt tiêu đề và kích thước cửa sổ
        primaryStage.setTitle("Giao diện Đăng Nhập");
        primaryStage.setScene(new Scene(root, 350, 450));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args); // Lệnh khởi chạy ứng dụng
    }
}