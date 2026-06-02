package com.uet.bidding.controller;

import com.uet.bidding.model.*;
import com.uet.bidding.network.ClientService;
import com.uet.bidding.service.AutoBidService;
import com.uet.bidding.service.BidderService;
import com.uet.bidding.util.ImageUtils;
import com.uet.bidding.util.ReviewContext;
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
import java.io.IOException;
import java.util.List;
import java.util.Locale;
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
  @FXML private Button btnEnableAutoBid;
  @FXML private Button btnSellerReviews;
  @FXML private TableView<BidRow> bidHistoryTable;
  @FXML private TableColumn<BidRow, String> colBidTime;
  @FXML private TableColumn<BidRow, String> colBidderName;
  @FXML private TableColumn<BidRow, String> colBidAmount;
  @FXML private LineChart<String, Number> bidLineChart;
  @FXML private Button btnRegister;
  @FXML private Label lblRegisterHint;
  @FXML private Label lblRegisteredCount;
  @FXML private Label lblWalletBalance;

  private Auction currentAuction;
  private final NumberFormat currencyFormat = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));
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

    lblCurrentPrice.setText(currencyFormat.format(auction.getCurrentPrice()) + " VNĐ");
    refreshWalletBalanceLabel();
    updateMinBidLabel(auction, currencyFormat);

    if (auction.getHighestBidder() != null) {
      lblHighestBidder.setText("bởi: " + auction.getHighestBidder().getUsername());
    } else {
      lblHighestBidder.setText("Chưa có ai đặt giá");
    }

    ImageUtils.loadItemImage(imgProduct, item);
    if (lblRegisteredCount != null) {
      lblRegisteredCount.setText("Đã đăng ký: " + auction.getRegisteredCount() + " người");
    }
    startCountdown(auction.getEndTime());
    loadBidHistory();
    refreshRegistrationUi();
  }

  private void refreshRegistrationUi() {
    if (currentAuction == null || btnRegister == null) return;
    Customer customer = UserSession.getLoggedInCustomer();
    Item item = currentAuction.getItem();
    boolean isSeller = customer != null && item != null && customer.getId() == item.getSellerId();

    if (isSeller) {
      btnRegister.setDisable(true);
      btnRegister.setText("Bạn là người bán");
      if (lblRegisterHint != null) {
        lblRegisterHint.setText("Người bán không đăng ký phiên của chính mình.");
      }
      return;
    }
    if (customer == null) {
      btnRegister.setDisable(true);
      if (lblRegisterHint != null) {
        lblRegisterHint.setText("Đăng nhập để đăng ký tham gia đấu giá.");
      }
      return;
    }
    if (!customer.hasCompleteProfile()) {
      btnRegister.setDisable(true);
      if (lblRegisterHint != null) {
        lblRegisterHint.setText("Hoàn thiện hồ sơ trong Setting trước khi đăng ký.");
      }
      return;
    }

    new BidderService().checkRegistration(currentAuction.getId())
        .thenAccept(res -> javafx.application.Platform.runLater(() -> {
          if (!"SUCCESS".equals(res.getType())) return;
          boolean registered = Boolean.TRUE.equals(res.getData())
              || "true".equalsIgnoreCase(String.valueOf(res.getData()));
          applyRegistrationButtonState(registered);
        }));
  }

  private void applyRegistrationButtonState(boolean registered) {
    if (btnRegister == null) return;
    if (registered) {
      btnRegister.setDisable(true);
      btnRegister.setText("Đã đăng ký ✓");
      if (lblRegisterHint != null) {
        lblRegisterHint.setText("Bạn đã đăng ký tham gia phiên này.");
      }
    } else {
      btnRegister.setDisable(false);
      btnRegister.setText("Đăng ký");
      if (lblRegisterHint != null) {
        lblRegisterHint.setText("Đăng ký để tham gia phiên (bắt buộc trước khi đặt giá).");
      }
    }
  }

  @FXML
  public void handleRegister(ActionEvent event) {
    Customer customer = UserSession.getLoggedInCustomer();
    if (customer == null || !customer.hasCompleteProfile()) {
      showAlert("Chú ý", "Hoàn thiện hồ sơ và đăng nhập trước!", Alert.AlertType.WARNING);
      return;
    }
    if (currentAuction.getItem().getSellerId() == customer.getId()) {
      showAlert("Chú ý", "Người bán không thể đăng ký phiên của mình.", Alert.AlertType.WARNING);
      return;
    }

    new BidderService().registerForAuction(currentAuction.getId())
        .thenAccept(res -> javafx.application.Platform.runLater(() -> {
          if ("SUCCESS".equals(res.getType())) {
            if ("ALREADY_REGISTERED".equals(res.getData())) {
              applyRegistrationButtonState(true);
              showAlert("Thông báo", "Bạn đã đăng ký phiên này rồi.", Alert.AlertType.INFORMATION);
              return;
            }
            customer.getBidderProfile().registerForAuction(currentAuction.getId());
            applyRegistrationButtonState(true);

            // 🌟 TỐI ƯU: Đặt sẵn phương án tự tăng số lượng lên 1 làm dự phòng
            int newCount = currentAuction.getRegisteredCount() + 1;

            try {
              String json = GsonFactory.getInstance().toJson(res.getData());
              Auction updated = GsonFactory.getInstance().fromJson(json, Auction.class);
              if (updated != null && updated.getRegisteredCount() > 0) {
                newCount = updated.getRegisteredCount(); // Nếu server trả về object chuẩn thì lấy từ server
              }
            } catch (Exception ignored) {
              // Nếu ép kiểu JSON lỗi (do server trả về chuỗi thường), khối try-catch sẽ hứng lỗi
              // nhưng app không bị sập, và vẫn dùng số lượng tự tăng ở trên.
            }

            // Cập nhật số lượng vào Object hiện tại
            currentAuction.setRegisteredCount(newCount);

            // Hiển thị lên giao diện Chi tiết sản phẩm
            if (lblRegisteredCount != null) {
              lblRegisteredCount.setText("Đã đăng ký: " + newCount + " người");
            }

            // 🌟 THẦN CHÚ: Luôn luôn thông báo ra ngoài Dashboard để cập nhật số lượng ngay lập tức
            AuctionListController list = AuctionListController.getInstance();
            if (list != null) {
              list.refreshOneAuction(currentAuction);
            }

            if (MyManagementController.getInstance() != null) {
              MyManagementController.getInstance().reloadAfterRegistration();
            }
            showAlert("Thành công", "Đã đăng ký tham gia phiên đấu giá!", Alert.AlertType.INFORMATION);
          } else {
            showAlert("Lỗi", String.valueOf(res.getData()), Alert.AlertType.ERROR);
          }
        }))
        .exceptionally(ex -> {
          // 🌟 Bổ sung khối này để bắt lỗi "930003.0" nếu nó xảy ra ngầm trong luồng mạng, tránh làm treo đơ App
          javafx.application.Platform.runLater(() ->
              showAlert("Lỗi hệ thống", ex.getMessage(), Alert.AlertType.ERROR));
          return null;
        });
  }
  public void applyAuctionUpdate(Auction auction) {
    if (auction == null || currentAuction == null) return;
    if (auction.getId() != currentAuction.getId()) return;

    this.currentAuction = auction;
    Item item = auction.getItem();

    NumberFormat currencyFormat = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));
    lblCurrentPrice.setText(currencyFormat.format(auction.getCurrentPrice()) + " VNĐ");
    updateMinBidLabel(auction, currencyFormat);

    if (auction.getHighestBidder() != null) {
      lblHighestBidder.setText("bởi: " + auction.getHighestBidder().getUsername());
    }
    if (lblRegisteredCount != null) {
      lblRegisteredCount.setText("Đã đăng ký: " + auction.getRegisteredCount() + " người");
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
    refreshWalletBalanceLabel();
  }

  public void refreshWalletBalanceLabel() {
    if (lblWalletBalance == null) return;
    Customer customer = UserSession.getLoggedInCustomer();
    if (customer == null || customer.getBalance() == null) {
      lblWalletBalance.setText("—");
      return;
    }
    lblWalletBalance.setText(currencyFormat.format(customer.getBalance()) + " VNĐ");
  }

  @FXML
  public void handleEnableAutoBid(ActionEvent event) {
    Customer customer = UserSession.getLoggedInCustomer();
    if (customer == null) return;

    try {
      String clean = txtMaxAutoBid.getText().replaceAll("[^\\d.]", "");
      if (clean.isEmpty()) {
        showAlert("Lỗi", "Nhập trần giá auto-bid!", Alert.AlertType.WARNING);
        return;
      }

      BigDecimal maxLimit = new BigDecimal(clean);
      BigDecimal userBalance = customer.getBalance();

      // 🛡️ LỚP KHIÊN 1: Trần giá không được vượt quá số dư ví
      if (maxLimit.compareTo(userBalance) > 0) {
        NumberFormat fmt = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));
        showAlert("Vượt quá hạn mức tài chính",
            "Trần giá Auto-bid không được vượt quá số dư ví (" + fmt.format(userBalance) + " VNĐ).",
            Alert.AlertType.ERROR);
        return;
      }

      // 🛡️ LỚP KHIÊN 2: Trần giá phải lớn hơn hoặc bằng (Giá hiện tại + Bước giá)
      if (currentAuction != null) {
        BigDecimal currentPrice = currentAuction.getCurrentPrice();
        BigDecimal bidIncrement = currentAuction.getBidIncrement();
        BigDecimal minRequired = currentPrice.add(bidIncrement);

        if (maxLimit.compareTo(minRequired) < 0) {
          NumberFormat fmt = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));
          showAlert("Lỗi nhập liệu",
              "Trần giá phải lớn hơn hoặc bằng mức tối thiểu tiếp theo (" + fmt.format(minRequired) + " VNĐ)!",
              Alert.AlertType.ERROR);
          return;
        }
      }

      // Gửi lệnh lên Server (Backend)
      new AutoBidService().enable(currentAuction.getId(), maxLimit)
          .thenAccept(res -> javafx.application.Platform.runLater(() -> {
            if ("SUCCESS".equals(res.getType())) {
              showAlert("Thành công", "Đã đặt Auto-bid!", Alert.AlertType.INFORMATION);
              // Có thể thêm code để update UI ở đây (ví dụ: đổi màu nút, khóa ô text)
            } else {
              showAlert("Lỗi", String.valueOf(res.getData()), Alert.AlertType.ERROR);
            }
          }));
    } catch (Exception e) {
      showAlert("Lỗi", e.getMessage(), Alert.AlertType.ERROR);
    }
  }

  private void startCountdown(LocalDateTime endTime) {
    if (timeline != null) timeline.stop();

    timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
      LocalDateTime now = LocalDateTime.now();

      // --- ĐOẠN XỬ LÝ KHI THỜI GIAN KẾT THÚC ---
      if (now.isAfter(endTime) || now.isEqual(endTime)) {
        lblCountdown.setText("ĐÃ KẾT THÚC");
        lblCountdown.setStyle("-fx-text-fill: #9e9e9e;"); // Đổi màu xám cho chữ
        timeline.stop();

        // 1. Khóa toàn bộ thao tác đặt giá/đăng ký trên UI ngay lập tức
        if (btnRegister != null) btnRegister.setDisable(true);
        if (txtBidAmount != null) txtBidAmount.setDisable(true);
        if (txtMaxAutoBid != null) txtMaxAutoBid.setDisable(true);

        // 2. Gắn cờ local cho object
        currentAuction.setStatus("FINISHED");

        // 3. Tự động xóa phiên này khỏi sảnh chính (AuctionList) mà KHÔNG cần chọc lên Server
        if (AuctionListController.getInstance() != null) {
          AuctionListController.getInstance().refreshOneAuction(currentAuction);
        }

        return;
      }
      // --- KẾT THÚC ĐOẠN XỬ LÝ ---

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

    if (customer == null || !customer.hasCompleteProfile()) {
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
      BigDecimal currentPrice = currentAuction.getCurrentPrice();

      // 🎯 Lấy Bước giá (Bid Increment) từ dữ liệu phiên đấu giá
      // Lưu ý: Đảm bảo class Auction của bạn có thuộc tính bidIncrement (tương ứng cột bid_increment trong DB)
      BigDecimal stepPrice = currentAuction.getBidIncrement();
      BigDecimal minRequired = currentPrice.add(stepPrice);

      // ✅ KIỂM TRA 1: Giá đặt phải >= Giá hiện tại + Bước giá
      if (bidAmount.compareTo(minRequired) < 0) {
        NumberFormat fmt = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));
        showAlert("Giá đặt không hợp lệ",
            "Mức giá tối thiểu tiếp theo phải là: " + fmt.format(minRequired) + " VNĐ\n" +
                "(Bao gồm giá hiện tại + bước giá " + fmt.format(stepPrice) + " VNĐ)",
            Alert.AlertType.ERROR);
        return;
      }

      // ✅ KIỂM TRA 2: Số dư tài khoản phải đủ để trả mức giá đã đặt
      BigDecimal userBalance = customer.getBalance();
      if (bidAmount.compareTo(userBalance) > 0) {
        NumberFormat fmt = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));
        showAlert("Số dư không đủ",
            "Số dư hiện tại của bạn (" + fmt.format(userBalance) + " VNĐ) không đủ để đặt mức giá này.\n" +
                "Vui lòng nạp thêm tiền!",
            Alert.AlertType.ERROR);
        return;
      }

      // Nếu vượt qua các bước kiểm tra, tiến hành gửi lệnh lên Server
      new BidderService().placeBid(currentAuction.getId(), bidAmount)
          .thenAccept(response -> javafx.application.Platform.runLater(() -> {
            if ("SUCCESS".equals(response.getType())) {
              txtBidAmount.clear();
              refreshWalletBalanceLabel();
              showAlert("Thành công", "Đã gửi giá lên server!", Alert.AlertType.INFORMATION);
              loadBidHistory();
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
  public void handleViewSellerReviews(ActionEvent event) {
    if (currentAuction == null || currentAuction.getItem() == null) {
      showAlert("Lỗi", "Không xác định được người bán.", Alert.AlertType.ERROR);
      return;
    }
    Item item = currentAuction.getItem();
    int sellerId = item.getSellerId();
    String storeName = "Shop #" + sellerId;
    ReviewContext.set(currentAuction.getId(), sellerId, storeName, false);

    try {
      Parent root = FXMLLoader.load(getClass().getResource("/Review.fxml"));
      Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
      stage.setScene(new Scene(root));
      stage.setTitle("Đánh giá người bán - " + storeName);
      stage.show();
    } catch (IOException e) {
      e.printStackTrace();
      showAlert("Lỗi", "Không mở được trang đánh giá.", Alert.AlertType.ERROR);
    }
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

  private void updateMinBidLabel(Auction auction, NumberFormat currencyFormat) {
    if (lblMinBid == null || auction == null) return;
    BigDecimal currentPrice = auction.getCurrentPrice() != null ? auction.getCurrentPrice() : BigDecimal.ZERO;
    BigDecimal increment = auction.getBidIncrement() != null ? auction.getBidIncrement() : BigDecimal.ZERO;
    lblMinBid.setText("(Tối thiểu: " + currencyFormat.format(currentPrice)
        + " + " + currencyFormat.format(increment) + " VNĐ)");
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
    private final String bidderName;
    private final String amount;

    public BidRow(String time, String bidderName, String amount) {
      this.time = time;
      this.bidderName = bidderName;
      this.amount = amount;
    }

    public String getTime() { return time; }
    public String getBidderName() { return bidderName; }
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
                  String name = b.getBidderUsername();
                  if (name == null || name.isBlank()) {
                    name = "—";
                  }
                  String a = fmt.format(b.getAmount()) + " đ";
                  rows.add(new BidRow(t, name, a));
                  series.getData().add(new XYChart.Data<>(t, b.getAmount().doubleValue()));
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
                }
              } catch (Exception e) {
                e.printStackTrace();
              }
            }));
  }
}