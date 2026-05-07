package com.uet.bidding.ui;

import com.uet.bidding.model.Auction;
import com.uet.bidding.model.Item;
import com.uet.bidding.model.User;
import com.uet.bidding.util.UserSession;
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

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

public class ProductDetailController {

  @FXML
  private ImageView imgProduct;
  @FXML
  private Label lblProductName;
  @FXML
  private Label lblAuctionId;
  @FXML
  private Label lblCurrentPrice;
  @FXML
  private Label lblHighestBidder;
  @FXML
  private Label lblCountdown;
  @FXML
  private Label lblDescription;
  @FXML
  private TextField txtBidAmount;
  @FXML
  private Label lblMinBid;

  private Auction currentAuction;
  private Timeline timeline;

  public void setAuctionData(Auction auction) {
    this.currentAuction = auction;
    Item item = auction.getItem();

    lblProductName.setText(item.getName());
    lblDescription.setText(item.getDescription());
    lblAuctionId.setText("Mã phiên: #" + auction.getId());

    NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
    lblCurrentPrice.setText(currencyFormat.format(auction.getCurrentPrice()) + " VNĐ");
    lblMinBid.setText("(Tối thiểu: > " + currencyFormat.format(auction.getCurrentPrice()) + "đ)");

    if (auction.getHighestBidder() != null) {
      lblHighestBidder.setText("bởi: " + auction.getHighestBidder().getUsername());
    } else {
      lblHighestBidder.setText("Chưa có ai đặt giá");
    }

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
    // ✅ CHECK PROFILE TRƯỚC KHI ĐẶT GIÁ
    User currentUser = UserSession.getCurrentUser();
    if (currentUser == null || !currentUser.isProfileComplete()) {
      Alert alert = new Alert(Alert.AlertType.WARNING);
      alert.setTitle("⚠️ Profile chưa hoàn thành");
      alert.setHeaderText("Cần cập nhật thông tin cá nhân!");
      alert.setContentText("Vui lòng:\n" +
          "1. Click 'Avatar/My Profile' ở góc trên\n" +
          "2. Điền đầy đủ: Họ tên, SĐT, Địa chỉ, Ngân hàng\n" +
          "3. Lưu → Quay lại đấu giá");
      alert.showAndWait();
      return; // DỪNG BID
    }

    try {
      String cleanText = txtBidAmount.getText().replaceAll("[^\\d.]", "");
      if (cleanText.isEmpty()) {
        showAlert("Lỗi nhập liệu", "Vui lòng nhập số tiền hợp lệ!", Alert.AlertType.WARNING);
        return;
      }

      BigDecimal bidAmount = new BigDecimal(cleanText);
      if (bidAmount.compareTo(currentAuction.getCurrentPrice()) <= 0) {
        showAlert("Lỗi đặt giá", "Số tiền phải lớn hơn giá cao nhất hiện tại!", Alert.AlertType.ERROR);
        return;
      }

      currentAuction.setCurrentPrice(bidAmount);
      NumberFormat format = NumberFormat.getInstance(new Locale("vi", "VN"));
      lblCurrentPrice.setText(format.format(bidAmount) + " VNĐ");
      lblHighestBidder.setText("bởi: Bạn (Vừa đặt)");
      txtBidAmount.clear();
      lblMinBid.setText("(Tối thiểu: > " + format.format(bidAmount) + "đ)");

      showAlert("Thành công", "Bạn đã đặt giá thành công!", Alert.AlertType.INFORMATION);

    } catch (NumberFormatException e) {
      showAlert("Lỗi nhập liệu", "Vui lòng nhập số tiền hợp lệ!", Alert.AlertType.WARNING);
    } catch (Exception e) {
      showAlert("Lỗi hệ thống", "Đã xảy ra lỗi: " + e.getMessage(), Alert.AlertType.ERROR);
    }
  }

  @FXML
  public void handleBuyNow(ActionEvent event) {
    // ✅ CHECK PROFILE TRƯỚC KHI MUA NGAY
    User currentUser = UserSession.getCurrentUser();
    if (currentUser == null || !currentUser.isProfileComplete()) {
      Alert alert = new Alert(Alert.AlertType.WARNING);
      alert.setTitle("⚠️ Profile chưa hoàn thành");
      alert.setHeaderText("Cần cập nhật thông tin cá nhân!");
      alert.setContentText("Vui lòng:\n" +
          "1. Click 'Avatar/My Profile' ở góc trên\n" +
          "2. Điền đầy đủ thông tin cá nhân\n" +
          "3. Lưu → Quay lại mua hàng");
      alert.showAndWait();
      return; // DỪNG MUA
    }

    showAlert("Mua ngay", "Tính năng thanh toán trực tiếp đang được phát triển!", Alert.AlertType.INFORMATION);
  }

  @FXML
  public void handleBack(ActionEvent event) {
    try {
      if (timeline != null) timeline.stop();
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