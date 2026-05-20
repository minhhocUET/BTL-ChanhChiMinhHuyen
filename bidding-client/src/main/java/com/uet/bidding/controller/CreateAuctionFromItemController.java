package com.uet.bidding.controller;

import com.google.gson.Gson;
import com.uet.bidding.model.Auction;
import com.uet.bidding.model.Customer;
import com.uet.bidding.model.Item;
import com.uet.bidding.network.ClientService;
import com.uet.bidding.service.SellerService;
import com.uet.bidding.util.CreateAuctionContext;
import com.uet.bidding.util.ImageUtil;
import com.uet.bidding.util.UserSession;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public class CreateAuctionFromItemController {

  @FXML private ImageView imgItem;
  @FXML private Label lblName;
  @FXML private Label lblCity;
  @FXML private Label lblType;
  @FXML private Label lblDescription;
  @FXML private TextField txtStartPrice;
  @FXML private TextField txtDuration;
  @FXML private TextField txtBidIncrement;
  @FXML private Label lblDemoHint;

  private Item item;

  @FXML
  public void initialize() {
    item = CreateAuctionContext.get();
    if (item == null) {
      showAlert(Alert.AlertType.ERROR, "Lỗi", "Không có sản phẩm được chọn.");
      return;
    }
    lblName.setText(item.getName());
    lblCity.setText("Thành phố: " + (item.getCity() != null && !item.getCity().isBlank() ? item.getCity() : "-"));
    lblType.setText("Loại: " + item.getType());
    lblDescription.setText(item.getDescription() != null ? item.getDescription() : "");
    ImageUtil.loadItemImage(imgItem, item);

    NumberFormat fmt = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));
    if (item.getStartingPrice() != null) {
      txtStartPrice.setText(fmt.format(item.getStartingPrice()));
    }

    if (item.getId() <= 0) {
      lblDemoHint.setVisible(true);
      lblDemoHint.setManaged(true);
      lblDemoHint.setText(
          "Đây là sản phẩm demo (kho trống). Dùng «Đăng sản phẩm mới» để thêm sản phẩm thật, "
              + "sau đó tạo phiên đấu giá từ dòng đó.");
    }
  }

  @FXML
  private void handleCreateAuction(ActionEvent event) {
    if (item == null) return;
    if (item.getId() <= 0) {
      showAlert(Alert.AlertType.WARNING, "Sản phẩm demo",
          "Không thể tạo phiên trên server với sản phẩm mẫu. Hãy thêm sản phẩm thật trước.");
      return;
    }
    if (item.isInAuction()) {
      showAlert(Alert.AlertType.WARNING, "Chú ý", "Sản phẩm đang trong phiên đấu giá khác.");
      return;
    }

    try {
      String priceClean = txtStartPrice.getText().replaceAll("[^\\d.]", "");
      String durationClean = txtDuration.getText().trim();
      if (priceClean.isEmpty() || durationClean.isEmpty()) {
        showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Nhập giá khởi điểm và thời lượng.");
        return;
      }
      BigDecimal startPrice = new BigDecimal(priceClean);
      int duration = Integer.parseInt(durationClean);
      if (duration <= 0) {
        showAlert(Alert.AlertType.WARNING, "Lỗi", "Thời lượng phải lớn hơn 0 phút.");
        return;
      }

      new SellerService().createAuctionAsync(item.getId(), startPrice, duration)
          .thenAccept(res -> Platform.runLater(() -> {
            if (!"SUCCESS".equals(res.getType())) {
              showAlert(Alert.AlertType.ERROR, "Lỗi", String.valueOf(res.getData()));
              return;
            }
            Gson gson = ClientService.getInstance().getGson();
            Auction created = gson.fromJson(gson.toJson(res.getData()), Auction.class);
            if (created != null) {
              item.setInAuction(true);
              Customer c = UserSession.getLoggedInCustomer();
              if (c != null) {
                c.getSellerProfile().getActiveAuctions().add(created);
              }
            }
            showAlert(Alert.AlertType.INFORMATION, "Thành công",
                "Phiên đấu giá đã được tạo và hiển thị trên sàn!");
            openSellerDashboard(event.getSource());
          }));
    } catch (NumberFormatException e) {
      showAlert(Alert.AlertType.ERROR, "Lỗi", "Giá hoặc thời lượng không hợp lệ.");
    }
  }

  @FXML
  private void handleBack(ActionEvent event) {
    openSellerDashboard(event.getSource());
  }

  private void openSellerDashboard(Object source) {
    CreateAuctionContext.clear();
    try {
      Parent root = FXMLLoader.load(getClass().getResource("/SellerDashboard.fxml"));
      Stage stage = (Stage) ((Node) source).getScene().getWindow();
      stage.setScene(new Scene(root));
      stage.setTitle("Kênh người bán - Seller Dashboard");
      stage.show();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void showAlert(Alert.AlertType type, String title, String content) {
    Alert alert = new Alert(type);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(content);
    alert.showAndWait();
  }
}
