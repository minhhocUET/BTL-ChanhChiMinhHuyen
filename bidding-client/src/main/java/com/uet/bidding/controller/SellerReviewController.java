package com.uet.bidding.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.IOException;

public class SellerReviewController {

  @FXML
  private VBox reviewsContainer;

  @FXML
  public void initialize() {
    // Tự động thêm các bình luận khớp với hình ảnh thiết kế
    addReviewCard("Nguyễn Hoài An", "12/05/2026", "⭐⭐⭐⭐⭐", "Sản phẩm rất đẹp, đóng gói cẩn thận, shop tư vấn nhiệt tình. Sẽ ủng hộ tiếp!");
    addReviewCard("Trần Minh Khoa", "10/05/2026", "⭐⭐⭐⭐★", "Chất lượng ổn, giao hàng nhanh. Màu sắc đúng như hình.");
    addReviewCard("Lê Thu Trang", "08/05/2026", "⭐⭐⭐⭐⭐", "Shop dễ thương, sản phẩm xinh xỉu luôn! Rất hài lòng.");
    addReviewCard("Phạm Đức Nhật", "05/05/2026", "⭐⭐⭐⭐★", "Sản phẩm tốt, giá hợp lý. Sẽ quay lại mua lần sau.");
  }

  private void addReviewCard(String name, String date, String stars, String content) {
    VBox card = new VBox(8);
    card.setStyle("-fx-background-color: #ffffff; -fx-padding: 15; -fx-background-radius: 12; -fx-border-color: #f8bbd0; -fx-border-radius: 12; -fx-border-width: 1;");

    HBox topRow = new HBox(12);
    topRow.setAlignment(Pos.CENTER_LEFT);

    // Avatar người dùng (Vòng tròn hồng)
    StackPane avatarPane = new StackPane();
    Circle avatarBg = new Circle(20, Color.web("#e91e63"));
    Label userIcon = new Label("👤");
    userIcon.setTextFill(Color.WHITE);
    userIcon.setStyle("-fx-font-size: 18px;");
    avatarPane.getChildren().addAll(avatarBg, userIcon);

    // Cột chứa Tên và Sao
    VBox nameStarBox = new VBox(2);
    Label nameLabel = new Label(name);
    nameLabel.setFont(Font.font("System", FontWeight.BOLD, 15));
    nameLabel.setTextFill(Color.web("#333333"));

    Label starsLabel = new Label(stars);
    starsLabel.setTextFill(Color.web("#e91e63"));
    starsLabel.setStyle("-fx-font-size: 14px;");
    nameStarBox.getChildren().addAll(nameLabel, starsLabel);

    // Ngày tháng
    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);
    Label dateLabel = new Label(date);
    dateLabel.setTextFill(Color.web("#d32f2f"));
    dateLabel.setFont(Font.font("System", 12));

    topRow.getChildren().addAll(avatarPane, nameStarBox, spacer, dateLabel);

    // Nội dung review
    Label contentLabel = new Label(content);
    contentLabel.setTextFill(Color.web("#555555"));
    contentLabel.setFont(Font.font("System", 14));
    contentLabel.setWrapText(true);

    card.getChildren().addAll(topRow, contentLabel);

    if (reviewsContainer != null) {
      reviewsContainer.getChildren().add(card);
    }
  }

  @FXML
  private void handleBackToSellerDashboard(ActionEvent event) {
    try {
      Parent dashboardRoot = FXMLLoader.load(getClass().getResource("/SellerDashboard.fxml"));
      Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
      currentStage.setScene(new Scene(dashboardRoot));
      currentStage.setTitle("Kênh người bán - Seller Dashboard");
      currentStage.show();
    } catch (IOException e) {
      e.printStackTrace();
      System.out.println("Lỗi: Không thể tải file SellerDashboard.fxml");
    }
  }
}