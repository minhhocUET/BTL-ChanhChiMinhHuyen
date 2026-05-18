package com.uet.bidding.controller;

import com.uet.bidding.model.Item;
import com.uet.bidding.model.Seller;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

public class SellerDashboardController {

  @FXML
  private Label avatarLabel;

  @FXML
  private TextField storeNameField;

  @FXML
  private TextArea descriptionArea;

  @FXML
  private Label ratingLabel;

  @FXML
  private FlowPane productPane;

  private Seller seller;

  @FXML
  public void initialize() {
    seller = new Seller();
    seller.setStoreName("Pink Shop");
    seller.setDescription("Chuyên bán đồ cute và phụ kiện màu hồng.");
    seller.setSellerRating(4.8);

    loadSellerData();
  }

  private void loadSellerData() {
    if (storeNameField != null) storeNameField.setText(seller.getStoreName());
    if (descriptionArea != null) descriptionArea.setText(seller.getDescription());
    if (ratingLabel != null) ratingLabel.setText(String.valueOf(seller.getSellerRating()));

    if (avatarLabel != null && seller.getStoreName() != null && !seller.getStoreName().isEmpty()) {
      avatarLabel.setText(seller.getStoreName().substring(0, 1).toUpperCase());
    }

    if (productPane != null && seller.getInventory() != null) {
      for (Item item : seller.getInventory()) {
        addProductCard(item);
      }
    }
  }

  private void addProductCard(Item item) {
    if (item == null) return;
    VBox card = new VBox(10);
    card.setAlignment(Pos.CENTER);
    card.setPrefWidth(170);
    card.setPrefHeight(220);
    card.setStyle("-fx-background-color: #ffe4ec; -fx-background-radius: 20; -fx-border-radius: 20; -fx-border-color: #ffb3cc; -fx-padding: 15;");

    Label name = new Label(item.getName());
    name.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #880e4f;");

    card.getChildren().add(name);
    productPane.getChildren().add(card);
  }

  @FXML
  private void handleSaveInfo(ActionEvent event) {
    String newStoreName = storeNameField.getText().trim();
    String newDesc = descriptionArea.getText().trim();

    if (newStoreName.isEmpty()) {
      showAlert(Alert.AlertType.WARNING, "Chú ý", "Vui lòng nhập tên cửa hàng!");
      return;
    }

    seller.setStoreName(newStoreName);
    seller.setDescription(newDesc);

    if (avatarLabel != null) {
      avatarLabel.setText(newStoreName.substring(0, 1).toUpperCase());
    }

    System.out.println(">>> ĐANG BẮN TÍN HIỆU ĐỒNG BỘ QUA SOCKET LÊN SERVER...");

    showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã lưu và gửi yêu cầu đồng bộ thông tin cửa hàng lên hệ thống!");
  }

  @FXML
  private void handleAddProduct() {
    System.out.println("Add Product clicked");
  }

  @FXML
  public void handleViewReview(ActionEvent event) {
    try {
      Parent reviewRoot = FXMLLoader.load(getClass().getResource("/SellerReview.fxml"));
      Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
      stage.setScene(new Scene(reviewRoot));
      stage.setTitle("Đánh giá của khách hàng");
      stage.show();
    } catch (IOException e) {
      e.printStackTrace();
      System.out.println("Lỗi: Không mở được trang SellerReview.fxml");
    }
  }

  @FXML
  public void handleBack(ActionEvent event) {
    try {
      Parent root = FXMLLoader.load(getClass().getResource("/AuctionList.fxml"));
      Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
      stage.setScene(new Scene(root));
      stage.setTitle("Danh sách đấu giá");
      stage.show();
    } catch (IOException e) {
      e.printStackTrace();
      System.out.println("Không thể quay lại trang AuctionList.fxml");
    }
  }

  // Hàm bị thiếu đã được khôi phục
  private void showAlert(Alert.AlertType alertType, String title, String content) {
    Alert alert = new Alert(alertType);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(content);
    alert.showAndWait();
  }
}