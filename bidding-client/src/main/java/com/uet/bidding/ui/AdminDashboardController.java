package com.uet.bidding.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import java.io.IOException;

public class AdminDashboardController {

    @FXML private StackPane contentArea;

    @FXML
    public void handleManageUsers(ActionEvent event) {
        loadSubView("/AdminUserManagement.fxml");
    }

    @FXML
    public void handleManageItems(ActionEvent event) {
        loadSubView("/AdminItemManagement.fxml");
    }

    @FXML
    public void handleSystemReports(ActionEvent event) {
        loadSubView("/AdminReports.fxml");
    }

    @FXML
    public void handleLogout(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/Login.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Đăng nhập Hệ thống");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadSubView(String fxmlPath) {
        try {
            Parent node = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentArea.getChildren().setAll(node);
        } catch (IOException e) {
            System.err.println("Chưa tạo file FXML: " + fxmlPath);
            // Có thể hiện thông báo tạm thời nếu file chưa tồn tại
        }
    }
}