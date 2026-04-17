package com.uet.bidding;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class HelloWorld extends Application {
    @Override
    public void start(Stage stage) {
        Label label = new Label("Xin chào! Client Đấu Giá đang chạy!");
        label.setFont(new Font(30));
        Scene scene = new Scene(label, 500, 300);

        stage.setTitle("Bidding Application - Client");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}