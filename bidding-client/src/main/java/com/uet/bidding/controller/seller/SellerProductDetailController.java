package com.uet.bidding.controller.seller;

import com.google.gson.reflect.TypeToken;
import com.uet.bidding.controller.auction.AuctionListController;
import com.uet.bidding.controller.auction.ProductDetailController;
import com.uet.bidding.model.Auction;
import com.uet.bidding.model.Bid;
import com.uet.bidding.model.GsonFactory;
import com.uet.bidding.model.Item;
import com.uet.bidding.network.ClientService;
import com.uet.bidding.util.ImageUtils;
import com.uet.bidding.util.SellerAuctionContext;
import com.uet.bidding.util.TimeManager;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class SellerProductDetailController implements Initializable {

  @FXML private ImageView imgProduct;
  @FXML private Label lblStatusBadge;
  @FXML private Label lblProductName;
  @FXML private Label lblItemMeta;
  @FXML private Label lblAuctionId;
  @FXML private Label lblCurrentPrice;
  @FXML private Label lblHighestBidder;
  @FXML private Label lblCountdown;
  @FXML private Label lblDescription;
  @FXML private Label lblRegisteredCount;
  @FXML private Label lblSellerHint;
  @FXML private Button btnEndEarly;
  @FXML private TableView<ProductDetailController.BidRow> bidHistoryTable;
  @FXML private TableColumn<ProductDetailController.BidRow, String> colBidTime;
  @FXML private TableColumn<ProductDetailController.BidRow, String> colBidderName;
  @FXML private TableColumn<ProductDetailController.BidRow, String> colBidAmount;
  @FXML private LineChart<String, Number> bidLineChart;

  private Auction currentAuction;
  private Timeline timeline;
  private LocalDateTime countdownEndTime;

  private static SellerProductDetailController instance;

  public static SellerProductDetailController getInstance() {
    return instance;
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    instance = this;
    Auction auction = SellerAuctionContext.get();
    if (auction == null) {
      showAlert("Lỗi", "Không có dữ liệu phiên đấu giá.", Alert.AlertType.ERROR);
      return;
    }
    setAuctionData(auction);
  }

  public void setAuctionData(Auction auction) {
    instance = this;
    this.currentAuction = auction;
    countdownEndTime = auction.getEndTime();
    Item item = auction.getItem();

    if (item != null) {
      lblProductName.setText(item.getName());
      lblDescription.setText(item.getDescription() != null ? item.getDescription() : "");
      lblItemMeta.setText(nullSafeCity(item) + " · " + item.getType());
      ImageUtils.loadItemImage(imgProduct, item);
    }

    lblAuctionId.setText("Mã phiên: #" + auction.getId());
    NumberFormat currencyFormat = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));
    lblCurrentPrice.setText(currencyFormat.format(auction.getCurrentPrice()) + " VNĐ");

    if (auction.getHighestBidder() != null) {
      lblHighestBidder.setText("Người dẫn đầu: " + auction.getHighestBidder().getUsername());
    } else {
      lblHighestBidder.setText("Chưa có ai đặt giá");
    }

    if (lblRegisteredCount != null) {
      lblRegisteredCount.setText("Số người đăng ký: " + auction.getRegisteredCount());
    }

    updateStatusUi(auction);
    if (auction.getEndTime() != null && "RUNNING".equals(auction.getStatus())) {
      // Đảm bảo chữ đếm ngược hiện màu đỏ/đen bình thường khi đang chạy
      if (lblCountdown != null) lblCountdown.setStyle("-fx-text-fill: #e81e63; -fx-font-weight: bold;");
      startCountdown(auction.getEndTime());
    } else if (lblCountdown != null) {
      lblCountdown.setText("ĐÃ KẾT THÚC");
      // Đổi chữ "ĐÃ KẾT THÚC" thành màu xám cho hợp lý
      lblCountdown.setStyle("-fx-text-fill: #9e9e9e; -fx-font-weight: bold;");
    }

    loadBidHistory();
  }

  public void applyAuctionUpdate(Auction auction) {
    if (auction == null || currentAuction == null || auction.getId() != currentAuction.getId()) {
      return;
    }
    setAuctionData(auction);
  }

  private void updateStatusUi(Auction auction) {
    String status = auction.getStatus() != null ? auction.getStatus() : "UNKNOWN";
    boolean running = "RUNNING".equals(status) || "OPEN".equals(status);

    if (lblStatusBadge != null) {
      lblStatusBadge.setText(status);
      // Thay đổi màu sắc của nhãn góc trái tùy theo trạng thái
      if (running) {
        lblStatusBadge.setStyle("-fx-background-color: #e81e63; -fx-background-radius: 5; -fx-text-fill: white; -fx-padding: 5 10;");
      } else {
        lblStatusBadge.setStyle("-fx-background-color: #9e9e9e; -fx-background-radius: 5; -fx-text-fill: white; -fx-padding: 5 10;");
      }
    }

    if (btnEndEarly != null) {
      btnEndEarly.setDisable(!running);
      btnEndEarly.setVisible(running);
      // DÒNG QUAN TRỌNG: Thu hồi lại không gian trống của nút khi nó bị ẩn
      btnEndEarly.setManaged(running);
    }

    if (lblSellerHint != null) {
      if (running) {
        lblSellerHint.setText("Bạn có thể dừng sớm phiên. Người mua không thể đặt giá sau khi kết thúc.");
      } else {
        lblSellerHint.setText("Phiên đã kết thúc. Chỉ xem thông tin và lịch sử đặt giá.");
      }
    }
  }

  @FXML
  public void handleEndEarly() {
    if (currentAuction == null) return;

    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
    confirm.setTitle("Xác nhận");
    confirm.setHeaderText("Dừng sớm phiên đấu giá?");
    confirm.setContentText("Phiên #" + currentAuction.getId() + " sẽ kết thúc ngay lập tức. Hành động này không thể hoàn tác.");
    if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
      return;
    }

    btnEndEarly.setDisable(true); // Tạm khóa nút tránh bấm 2 lần

    ClientService.getInstance()
        .sendRequest("SELLER_END_AUCTION", currentAuction.getId())
        .thenAccept(res -> Platform.runLater(() -> {
          if ("SUCCESS".equals(res.getType())) {

            // 1. ÉP BUỘC ĐỔI TRẠNG THÁI GIAO DIỆN NGAY LẬP TỨC
            currentAuction.setStatus("FINISHED");
            updateStatusUi(currentAuction); // Hàm này của bạn sẽ tự động ẩn nút "Dừng sớm" đi

            if (lblCountdown != null) lblCountdown.setText("ĐÃ KẾT THÚC");
            if (timeline != null) timeline.stop(); // Dừng đồng hồ

            showAlert("Thành công", "Đã dừng sớm phiên đấu giá.", Alert.AlertType.INFORMATION);

            // 2. GỌI BROADCAST ĐỂ BẢNG DANH SÁCH BÊN NGOÀI TỰ CẬP NHẬT
            if (SellerDashboardController.getInstance() != null) {
              SellerDashboardController.getInstance().applyAuctionUpdate(currentAuction);
            }
            if (AuctionListController.getInstance() != null) {
              AuctionListController.getInstance().handleAuctionBroadcast(currentAuction);
            }

          } else {
            btnEndEarly.setDisable(false); // Nếu lỗi thì mở lại nút cho người dùng bấm lại
            showAlert("Lỗi", String.valueOf(res.getData()), Alert.AlertType.ERROR);
          }
        }));
  }

  @FXML
  public void handleBack(ActionEvent event) {
    try {
      if (timeline != null) timeline.stop();
      SellerAuctionContext.clear();
      Parent root = FXMLLoader.load(getClass().getResource("/SellerDashboard.fxml"));
      Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
      stage.setScene(new Scene(root));
      stage.setTitle("Seller Dashboard");
      stage.show();
    } catch (Exception e) {
      e.printStackTrace();
      showAlert("Lỗi", "Không quay lại được Seller Dashboard.", Alert.AlertType.ERROR);
    }
  }

  private void startCountdown(LocalDateTime endTime) {
    if (timeline != null) timeline.stop();
    timeline = new Timeline(new KeyFrame(Duration.seconds(1), ev -> {
      LocalDateTime now = TimeManager.getNow();
      if (now.isAfter(endTime)) {
        lblCountdown.setText("ĐÃ KẾT THÚC");
        timeline.stop();
        return;
      }
      long days = ChronoUnit.DAYS.between(now, endTime);
      long hours = ChronoUnit.HOURS.between(now, endTime) % 24;
      long minutes = ChronoUnit.MINUTES.between(now, endTime) % 60;
      long seconds = ChronoUnit.SECONDS.between(now, endTime) % 60;
      lblCountdown.setText(String.format("%02d:%02d:%02d:%02d", days, hours, minutes, seconds));
    }));
    timeline.setCycleCount(Animation.INDEFINITE);
    timeline.play();
  }

  private void loadBidHistory() {
    if (currentAuction == null || bidHistoryTable == null) return;

    ClientService.getInstance()
        .sendRequest("GET_BID_HISTORY", currentAuction.getId())
        .thenAccept(response -> Platform.runLater(() -> {
          if (response.getType() == null || !response.getType().contains("SUCCESS")) return;
          try {
            String json = GsonFactory.getInstance().toJson(response.getData());
            List<Bid> bids = GsonFactory.getInstance().fromJson(json, new TypeToken<List<Bid>>() {}.getType());
            if (bids == null) bids = Collections.emptyList();

            NumberFormat fmt = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));
            java.time.format.DateTimeFormatter dtf =
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

            ObservableList<ProductDetailController.BidRow> rows = FXCollections.observableArrayList();
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Giá");
            Collections.reverse(bids);

            for (Bid b : bids) {
              String t = b.getTime() != null ? b.getTime().format(dtf) : "-";
              String name = b.getBidderUsername();
              if (name == null || name.isBlank()) {
                name = "—";
              }
              String a = fmt.format(b.getAmount()) + " đ";
              rows.add(new ProductDetailController.BidRow(t, name, a));

              // CHỖ SỬA 1: Khởi tạo điểm dữ liệu riêng để đính kèm thông tin hiển thị
              if (b.getAmount() != null) {
                XYChart.Data<String, Number> dataPoint = new XYChart.Data<>(t, b.getAmount().doubleValue());
                dataPoint.setExtraValue("👤 Người đặt: " + name + "\n⏱ Thời gian: " + t + "\n💰 Mức giá: " + a);
                series.getData().add(dataPoint);
              }
            }

            colBidTime.setCellValueFactory(new PropertyValueFactory<>("time"));
            if (colBidderName != null) {
              colBidderName.setCellValueFactory(new PropertyValueFactory<>("bidderName"));
            }
            colBidAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
            bidHistoryTable.setItems(rows);

            if (bidLineChart != null) {
              bidLineChart.getData().clear();
              bidLineChart.getData().add(series);

              // CHỖ SỬA 2: Lặp qua các nút hình tròn vừa tạo trên đồ thị để cấu hình Tooltip + Hover
              for (XYChart.Data<String, Number> data : series.getData()) {
                javafx.scene.Node node = data.getNode();
                if (node != null && data.getExtraValue() != null) {

                  // Khởi tạo và thiết kế giao diện Tooltip giống hệt màn hình người mua
                  Tooltip tooltip = new Tooltip(data.getExtraValue().toString());
                  tooltip.setStyle("-fx-font-size: 13px; -fx-background-color: rgba(0,0,0,0.8); -fx-text-fill: white; -fx-padding: 8px; -fx-background-radius: 6px;");
                  Tooltip.install(node, tooltip);

                  // Sự kiện khi di chuột vào điểm tròn: đổi con trỏ sang bàn tay + phóng to 1.6 lần
                  node.setOnMouseEntered(event -> {
                    node.setStyle("-fx-cursor: hand;");
                    node.setScaleX(1.6);
                    node.setScaleY(1.6);
                  });

                  // Sự kiện khi chuột rời đi: trả về kích thước 1.0 bình thường
                  node.setOnMouseExited(event -> {
                    node.setScaleX(1.0);
                    node.setScaleY(1.0);
                  });
                }
              }
            }
          } catch (Exception e) {
            e.printStackTrace();
          }
        }));
  }

  private String nullSafeCity(Item item) {
    return (item == null || item.getCity() == null) ? "-" : item.getCity();
  }


  private void showAlert(String title, String content, Alert.AlertType type) {
    Alert alert = new Alert(type);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(content);
    alert.showAndWait();
  }
}
