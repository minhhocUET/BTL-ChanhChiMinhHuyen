package com.uet.bidding.controller;

import com.uet.bidding.model.*;
import com.uet.bidding.network.ClientService;
import com.uet.bidding.server.Server;
import com.uet.bidding.service.AutoBidService;
import com.uet.bidding.service.BidderService;
import com.uet.bidding.util.UserSession;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;

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
  @FXML private Label lblAntiSnipeInfo;
  @FXML private TextField txtMaxAutoBid;
  @FXML private TableView<BidRow> bidHistoryTable;
  @FXML private TableColumn<BidRow, String> colBidTime;
  @FXML private TableColumn<BidRow, String> colBidAmount;
  @FXML private LineChart<String, Number> bidLineChart;

  private Auction currentAuction;
  private Timeline timeline;

  private static ProductDetailController instance;
  private LocalDateTime countdownEndTime;
  public static ProductDetailController getInstance() {
    return instance;
  }

  public void setAuctionData(Auction auction) {
    instance = this;
    countdownEndTime = auction.getEndTime();
    this.currentAuction = auction;
    Item item = auction.getItem();

    lblProductName.setText(item.getName());
    lblDescription.setText(item.getDescription());
    lblAuctionId.setText("Mã phiên: #" + auction.getId());

    // Đã fix lỗi deprecated của Locale
    NumberFormat currencyFormat = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));
    lblCurrentPrice.setText(currencyFormat.format(auction.getCurrentPrice()) + " VNĐ");
    lblMinBid.setText("(Tối thiểu: > " + currencyFormat.format(auction.getCurrentPrice()) + "đ)");

    if (auction.getHighestBidder() != null) {
      lblHighestBidder.setText("bởi: " + auction.getHighestBidder().getUsername());
    } else {
      lblHighestBidder.setText("Chưa có ai đặt giá");
    }

    startCountdown(auction.getEndTime());
    loadBidHistory();

  }
  public void applyAuctionUpdate(Auction auction) {
    if (auction == null || currentAuction == null) return;
    if (auction.getId() != currentAuction.getId()) return;

    this.currentAuction = auction;
    Item item = auction.getItem();

    NumberFormat currencyFormat = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));
    lblCurrentPrice.setText(currencyFormat.format(auction.getCurrentPrice()) + " VNĐ");
    lblMinBid.setText("(Tối thiểu: > " + currencyFormat.format(auction.getCurrentPrice()) + "đ)");

    if (auction.getHighestBidder() != null) {
      lblHighestBidder.setText("bởi: " + auction.getHighestBidder().getUsername());
    }

    // Anti-sniping: đổi giờ kết thúc → reset đồng hồ
    if (auction.getEndTime() != null
            && (countdownEndTime == null || !countdownEndTime.equals(auction.getEndTime()))) {
      countdownEndTime = auction.getEndTime();
      startCountdown(countdownEndTime);
      if (lblAntiSnipeInfo != null) {
        lblAntiSnipeInfo.setStyle("-fx-text-fill: #ff4757; -fx-font-weight: bold;");
        lblAntiSnipeInfo.setText("⏱ Vừa gia hạn thêm thời gian đấu giá!");
      }
    }

    loadBidHistory();
  }
  @FXML
  public void handleEnableAutoBid(ActionEvent event) {
    try {
      String clean = txtMaxAutoBid.getText().replaceAll("[^\\d.]", "");
      if (clean.isEmpty()) {
        showAlert("Lỗi", "Nhập trần giá auto-bid!", Alert.AlertType.WARNING);
        return;
      }
      BigDecimal max = new BigDecimal(clean);
      new AutoBidService().enable(currentAuction.getId(), max)
              .thenAccept(res -> javafx.application.Platform.runLater(() -> {
                if ("SUCCESS".equals(res.getType())) {
                  showAlert("OK", "Đã bật auto-bid!", Alert.AlertType.INFORMATION);
                } else {
                  showAlert("Lỗi", String.valueOf(res.getData()), Alert.AlertType.ERROR);
                }
              }));
    } catch (Exception e) {
      showAlert("Lỗi", e.getMessage(), Alert.AlertType.ERROR);
    }
  }

  @FXML
  public void handleDisableAutoBid(ActionEvent event) {
    new AutoBidService().disable(currentAuction.getId())
            .thenAccept(res -> javafx.application.Platform.runLater(() -> {
              if ("SUCCESS".equals(res.getType())) {
                showAlert("OK", "Đã tắt auto-bid!", Alert.AlertType.INFORMATION);
              } else {
                showAlert("Lỗi", String.valueOf(res.getData()), Alert.AlertType.ERROR);
              }
            }));
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
    Customer customer = UserSession.getLoggedInCustomer();

    if (customer == null || !customer.isProfileComplete()) {
      showAlert("⚠️ Thông báo",
              customer == null ? "Vui lòng đăng nhập!" : "Hoàn thiện hồ sơ trước!",
              Alert.AlertType.WARNING);
      return;
    }

    try {
      String cleanText = txtBidAmount.getText().replaceAll("[^\\d.]", "");
      if (cleanText.isEmpty()) {
        showAlert("Lỗi", "Nhập số tiền hợp lệ!", Alert.AlertType.WARNING);
        return;
      }

      BigDecimal bidAmount = new BigDecimal(cleanText);
      if (bidAmount.compareTo(currentAuction.getCurrentPrice()) <= 0) {
        showAlert("Lỗi", "Giá phải cao hơn giá hiện tại!", Alert.AlertType.ERROR);
        return;
      }

      new BidderService().placeBid(currentAuction.getId(), bidAmount)
              .thenAccept(response -> javafx.application.Platform.runLater(() -> {
                if ("SUCCESS".equals(response.getType())) {
                  txtBidAmount.clear();
                  showAlert("Thành công", "Đã gửi giá lên server!", Alert.AlertType.INFORMATION);
                  loadBidHistory(); // PHẦN 5
                } else {
                  showAlert("Lỗi", String.valueOf(response.getData()), Alert.AlertType.ERROR);
                }
              }))
              .exceptionally(ex -> {
                javafx.application.Platform.runLater(() ->
                        showAlert("Lỗi", ex.getMessage(), Alert.AlertType.ERROR));
                return null;
              });

    } catch (NumberFormatException e) {
      showAlert("Lỗi", "Số tiền không hợp lệ!", Alert.AlertType.WARNING);
    }
  }

  @FXML
  public void handleBuyNow(ActionEvent event) {
    // ✅ CHECK PROFILE TRƯỚC KHI MUA NGAY
    Customer customer = UserSession.getLoggedInCustomer();

    if (customer == null || !customer.isProfileComplete()) {
      Alert alert = new Alert(Alert.AlertType.WARNING);
      alert.setTitle("⚠️ Thông báo hệ thống");
      alert.setHeaderText(customer == null ? "Yêu cầu đăng nhập" : "Hồ sơ chưa hoàn thiện");

      // Đã bổ sung hiển thị và return để chặn flow
      String content = (customer == null)
          ? "Vui lòng đăng nhập với tài khoản khách hàng để mua ngay."
          : "Bạn cần cập nhật đầy đủ thông tin cá nhân để mua ngay sản phẩm.";
      alert.setContentText(content);
      alert.showAndWait();
      return;
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
  public static class BidRow {
    private final String time;
    private final String amount;

    public BidRow(String time, String amount) {
      this.time = time;
      this.amount = amount;
    }

    public String getTime() { return time; }
    public String getAmount() { return amount; }
  }
  private void loadBidHistory() {
    if (currentAuction == null || bidHistoryTable == null) return;

    ClientService.getInstance().sendRequest("GET_BID_HISTORY", currentAuction.getId())
            .thenAccept(response -> javafx.application.Platform.runLater(() -> {
              if (!"SUCCESS".equals(response.getType())) return;

              try {
                String json = GsonFactory.getInstance().toJson(response.getData());
                java.lang.reflect.Type listType =
                        new com.google.gson.reflect.TypeToken<List<Bid>>() {}.getType();
                List<Bid> bids = GsonFactory.getInstance().fromJson(json, listType);

                NumberFormat fmt = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));
                java.time.format.DateTimeFormatter dtf =
                        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

                ObservableList<BidRow> rows = FXCollections.observableArrayList();
                XYChart.Series<String, Number> series = new XYChart.Series<>();
                series.setName("Giá");

                // bids từ DB sort giảm dần → đảo để chart tăng theo thời gian
                java.util.Collections.reverse(bids);

                for (Bid b : bids) {
                  String t = b.getTime() != null ? b.getTime().format(dtf) : "-";
                  String a = fmt.format(b.getAmount()) + " đ";
                  rows.add(new BidRow(t, a));
                  series.getData().add(new XYChart.Data<>(t, b.getAmount().doubleValue()));
                }

                colBidTime.setCellValueFactory(new PropertyValueFactory<>("time"));
                colBidAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
                bidHistoryTable.setItems(rows);

                if (bidLineChart != null) {
                  bidLineChart.getData().clear();
                  bidLineChart.getData().add(series);
                }
              } catch (Exception e) {
                e.printStackTrace();
              }
            }));
  }
}