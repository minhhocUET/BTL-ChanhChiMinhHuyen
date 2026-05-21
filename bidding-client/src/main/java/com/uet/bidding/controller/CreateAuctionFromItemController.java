package com.uet.bidding.controller;

import com.google.gson.Gson;
import com.uet.bidding.model.Auction;
import com.uet.bidding.model.Customer;
import com.uet.bidding.model.Item;
import com.uet.bidding.network.ClientService;
import com.uet.bidding.service.SellerService;
import com.uet.bidding.util.CreateAuctionContext;
import com.uet.bidding.util.ImageUtils;import com.uet.bidding.util.UserSession;
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
  @FXML private TextField txtBidIncrement; // Ô nhập bước giá
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
    ImageUtils.loadItemImage(imgItem, item);

    NumberFormat fmt = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));
    if (item.getStartingPrice() != null) {
      txtStartPrice.setText(fmt.format(item.getStartingPrice()));
      updateSuggestedIncrement(); // Gọi ngay gợi ý bước giá nếu đã có giá khởi điểm
    }

    if (item.getId() <= 0) {
      lblDemoHint.setVisible(true);
      lblDemoHint.setManaged(true);
      lblDemoHint.setText(
          "Đây là sản phẩm demo (kho trống). Dùng «Đăng sản phẩm mới» để thêm sản phẩm thật, "
              + "sau đó tạo phiên đấu giá từ dòng đó.");
    }

    // 1. Tự động tính thời gian kết thúc khi nhập số phút
    txtDuration.textProperty().addListener((obs, oldVal, newVal) -> {
      if (!newVal.matches("\\d*")) {
        txtDuration.setText(newVal.replaceAll("[^\\d]", ""));
        return;
      }
      updateEndTimeHint();
    });

    // 2. Tự động gợi ý bước giá khi nhập/sửa giá khởi điểm
    txtStartPrice.textProperty().addListener((obs, oldVal, newVal) -> {
      updateSuggestedIncrement();
    });
  }

  private void updateEndTimeHint() {
    try {
      int mins = Integer.parseInt(txtDuration.getText());
      java.time.LocalDateTime end = java.time.LocalDateTime.now().plusMinutes(mins);
      java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("HH:mm, dd/MM");
      lblDemoHint.setVisible(true);
      lblDemoHint.setManaged(true);
      lblDemoHint.setText("Dự kiến kết thúc vào: " + end.format(dtf) + " (Gia hạn nếu có đấu giá phút cuối)");
    } catch (Exception e) {
      // Nếu xóa hết chữ, hiển thị lại hint mặc định hoặc ẩn đi
    }
  }

  private void updateSuggestedIncrement() {
    try {
      String clean = txtStartPrice.getText().replaceAll("[^\\d]", "");
      if (clean.isEmpty()) return;
      BigDecimal price = new BigDecimal(clean);
      // Tính bước giá gợi ý = 5% giá khởi điểm
      BigDecimal suggest = price.multiply(new BigDecimal("0.05"));
      txtBidIncrement.setText(String.valueOf(suggest.toBigInteger()));
    } catch (Exception e) {
      // Bỏ qua nếu có lỗi parse
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
      // Chỉ giữ lại số (\d), xóa sạch mọi ký tự khác (kể cả dấu chấm, phẩy, chữ cái)
      String priceClean = txtStartPrice.getText().replaceAll("[^\\d]", "");
      String durationClean = txtDuration.getText().trim();
      String incrementClean = txtBidIncrement.getText().replaceAll("[^\\d]", "");

      if (priceClean.isEmpty() || durationClean.isEmpty()) {
        showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Nhập giá khởi điểm và thời lượng.");
        return;
      }

      BigDecimal startPrice = new BigDecimal(priceClean);
      int duration = Integer.parseInt(durationClean);
      // Nếu bỏ trống bước giá, set mặc định là 10.000 VNĐ
      BigDecimal bidIncrement = incrementClean.isEmpty() ? new BigDecimal("10000") : new BigDecimal(incrementClean);

      if (duration <= 0) {
        showAlert(Alert.AlertType.WARNING, "Lỗi", "Thời lượng phải lớn hơn 0 phút.");
        return;
      }
      if (bidIncrement.compareTo(BigDecimal.ZERO) <= 0) {
        showAlert(Alert.AlertType.WARNING, "Lỗi", "Bước giá phải lớn hơn 0.");
        return;
      }

      // TRUYỀN THÊM bidIncrement VÀO SERVICE
      new SellerService().createAuctionAsync(item.getId(), startPrice, duration, bidIncrement)
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
      Parent root = FXMLLoader.load(getClass().getResource("/SellerDashboard.fxml")); // Đảm bảo đường dẫn này đúng với dự án của bạn
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