package com.uet.bidding.ui;

import com.uet.bidding.model.Auction;
import com.uet.bidding.model.Item;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
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
import javafx.util.Duration;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

public class ProductDetailController {

  @FXML private ImageView imgProduct;
  @FXML private Label lblProductName;
  @FXML private Label lblAuctionId;
  @FXML private Label lblCurrentPrice;
  @FXML private Label lblHighestBidder;
  @FXML private Label lblCountdown;
  @FXML private Label lblDescription;
  @FXML private TextField txtBidAmount;
  @FXML private Label lblMinBid;

  private Auction currentAuction;
  private Timeline timeline; // Dùng để làm đồng hồ đếm ngược

  // Hàm này sẽ được gọi từ màn hình Danh sách để truyền dữ liệu qua đây
  public void setAuctionData(Auction auction) {
    this.currentAuction = auction;
    Item item = auction.getItem();

    // 1. Nạp dữ liệu lên giao diện
    lblProductName.setText(item.getName());
    lblDescription.setText(item.getDescription());
    lblAuctionId.setText("Mã phiên: #" + auction.getAuctionId());

    // Format tiền tệ kiểu Việt Nam (1.000.000 VNĐ)
    NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
    lblCurrentPrice.setText(currencyFormat.format(auction.getCurrentHighestBid()) + " VNĐ");

    // Mức giá tối thiểu phải lớn hơn giá hiện tại
    lblMinBid.setText("(Tối thiểu: > " + currencyFormat.format(auction.getCurrentHighestBid()) + "đ)");

    if (auction.getHighestBidder() != null) {
      lblHighestBidder.setText("bởi: " + auction.getHighestBidder().toString()); // Thay bằng getName() tuỳ model của bạn
    } else {
      lblHighestBidder.setText("Chưa có ai đặt giá");
    }

    // 2. Khởi động đồng hồ đếm ngược
    startCountdown(auction.getEndTime());
  }

  private void startCountdown(LocalDateTime endTime) {
    if (timeline != null) timeline.stop();

    timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
      LocalDateTime now = LocalDateTime.now();
      if (now.isAfter(endTime)) {
        lblCountdown.setText("ĐÃ KẾT THÚC");
        timeline.stop();
        return;
      }

      long days = ChronoUnit.DAYS.between(now, endTime);
      long hours = ChronoUnit.HOURS.between(now, endTime) % 24;
      long minutes = ChronoUnit.MINUTES.between(now, endTime) % 60;
      long seconds = ChronoUnit.SECONDS.between(now, endTime) % 60;

      String timeString = String.format("%02d:%02d:%02d:%02d", days, hours, minutes, seconds);
      lblCountdown.setText(timeString);
    }));
    timeline.setCycleCount(Animation.INDEFINITE);
    timeline.play();
  }

  @FXML
  public void handlePlaceBid(ActionEvent event) {
    try {
      double bidAmount = Double.parseDouble(txtBidAmount.getText().replaceAll("[^\\d.]", ""));

      if (bidAmount <= currentAuction.getCurrentHighestBid()) {
        showAlert("Lỗi đặt giá", "Số tiền phải lớn hơn giá cao nhất hiện tại!", Alert.AlertType.ERROR);
        return;
      }

      // GỌI MODEL ĐỂ XỬ LÝ LOGIC (Giả lập cập nhật giá)
      currentAuction.setCurrentHighestBid(bidAmount);

      // Cập nhật lại giao diện
      NumberFormat format = NumberFormat.getInstance(new Locale("vi", "VN"));
      lblCurrentPrice.setText(format.format(bidAmount) + " VNĐ");
      lblHighestBidder.setText("bởi: Bạn (Vừa đặt)");
      txtBidAmount.clear();
      lblMinBid.setText("(Tối thiểu: > " + format.format(bidAmount) + "đ)");

      showAlert("Thành công", "Bạn đã đặt giá thành công!", Alert.AlertType.INFORMATION);

    } catch (NumberFormatException e) {
      showAlert("Lỗi nhập liệu", "Vui lòng nhập số tiền hợp lệ!", Alert.AlertType.WARNING);
    }
  }

  @FXML
  public void handleBuyNow(ActionEvent event) {
    showAlert("Mua ngay", "Tính năng thanh toán trực tiếp đang được phát triển!", Alert.AlertType.INFORMATION);
  }

  @FXML
  public void handleBack(ActionEvent event) {
    try {
      if (timeline != null) timeline.stop(); // Dừng đồng hồ khi thoát

      Parent root = FXMLLoader.load(getClass().getResource("/AuctionList.fxml"));
      Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
      stage.setScene(new Scene(root));
      stage.setTitle("Danh sách Đấu giá VNU");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void showAlert(String title, String content, Alert.AlertType type) {
    Alert alert = new Alert(type);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(content);
    alert.showAndWait();
  }
}