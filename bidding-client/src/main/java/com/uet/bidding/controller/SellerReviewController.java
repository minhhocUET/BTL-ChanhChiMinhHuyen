package com.uet.bidding.controller;

import com.google.gson.reflect.TypeToken;
import com.uet.bidding.model.Customer;
import com.uet.bidding.model.GsonFactory;
import com.uet.bidding.model.NetworkMessage;
import com.uet.bidding.model.Review;
import com.uet.bidding.network.ClientService;
import com.uet.bidding.util.ReviewContext;
import com.uet.bidding.util.UserSession;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class SellerReviewController {

  @FXML
  private VBox reviewsContainer;

  @FXML
  public void initialize() {
    Customer customer = UserSession.getLoggedInCustomer();
    if (customer == null) {
      return;
    }

    String storeName = customer.getSellerProfile() != null
        ? customer.getSellerProfile().getStoreName()
        : "Cửa hàng";

    // auctionId = 0 vì màn này chỉ XEM review, không gửi mới
    ReviewContext.set(0, customer.getId(), storeName, false);

    loadReviews();
  }

  private void loadReviews() {
    if (reviewsContainer == null) return;
    reviewsContainer.getChildren().clear();

    ClientService.getInstance()
        .sendRequest("GET_REVIEWS_BY_SELLER", ReviewContext.sellerId)
        .thenAccept(this::onReviewsLoaded)
        .exceptionally(ex -> {
          Platform.runLater(() -> showAlert("Lỗi", ex.getMessage()));
          return null;
        });
  }

  private void onReviewsLoaded(NetworkMessage response) {
    Platform.runLater(() -> {
      if (!"SUCCESS".equals(response.getType())) {
        showAlert("Lỗi", String.valueOf(response.getData()));
        return;
      }

      String json = GsonFactory.getInstance().toJson(response.getData());
      List<Review> reviews = GsonFactory.getInstance().fromJson(json,
          new TypeToken<List<Review>>() {
          }.getType());

      for (Review r : reviews) {
        String date = r.getCreatedAt() != null
            ? r.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            : "";
        String stars = "⭐".repeat(Math.max(0, r.getStars()));
        addReviewCard(r.getReviewerName(), date, stars, r.getComment());
      }
    });
  }

  private void showAlert(String title, String msg) {
    Alert a = new Alert(Alert.AlertType.INFORMATION);
    a.setTitle(title);
    a.setContentText(msg);
    a.showAndWait();
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