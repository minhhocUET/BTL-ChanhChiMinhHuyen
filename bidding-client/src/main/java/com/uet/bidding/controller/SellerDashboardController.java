package com.uet.bidding.controller;

import com.uet.bidding.model.Item;
import com.uet.bidding.model.Seller;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

public class SellerDashboardController {

  @FXML
  private Label storeNameLabel;

  @FXML
  private TextArea descriptionArea;

  @FXML
  private Label ratingLabel;

  @FXML
  private FlowPane productPane;

  private Seller seller;

  @FXML
  public void initialize() {

    // TEST DATA
    seller = new Seller();
    seller.setStoreName("Pink Shop");
    seller.setDescription("Chuyên bán đồ cute và phụ kiện màu hồng.");
    seller.setSellerRating(4.8);

    loadSellerData();
  }

  private void loadSellerData() {

    storeNameLabel.setText(seller.getStoreName());
    descriptionArea.setText(seller.getDescription());
    ratingLabel.setText(String.valueOf(seller.getSellerRating()));

    // HIỂN THỊ ITEM
    for (Item item : seller.getInventory()) {
      addProductCard(item);
    }
  }

  private void addProductCard(Item item) {

    VBox card = new VBox(10);
    card.setAlignment(Pos.CENTER);

    card.setPrefWidth(170);
    card.setPrefHeight(220);

    card.setStyle("""
        -fx-background-color: #ffe4ec;
        -fx-background-radius: 20;
        -fx-border-radius: 20;
        -fx-border-color: #ffb3cc;
        -fx-padding: 15;
        """);

    Label name = new Label(item.getName());
    name.setStyle("""
        -fx-font-size: 18px;
        -fx-font-weight: bold;
        -fx-text-fill: #880e4f;
        """);

    card.getChildren().add(name);

    productPane.getChildren().add(card);
  }

  @FXML
  private void handleAddProduct() {

    System.out.println("Add Product clicked");

    // sau này có thể mở cửa sổ AddProduct.fxml
  }

  @FXML
  private void handleViewReview() {

    try {

      FXMLLoader loader = new FXMLLoader(
          getClass().getResource("/Review.fxml"));

      Stage stage = new Stage();

      stage.setTitle("Review");

      stage.setScene(new Scene(loader.load()));

      stage.show();

    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
