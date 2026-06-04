package com.uet.bidding.controller.seller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.uet.bidding.model.Customer;
import com.uet.bidding.model.NetworkMessage;
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
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SellerReviewController {

  @FXML private VBox reviewsContainer;
  @FXML private Label lblShopName;
  @FXML private Label lblShopDesc;
  @FXML private Label lblAvgRating;
  @FXML private HBox lblStarRating;
  @FXML private Label lblReviewCount;
  @FXML private javafx.scene.image.ImageView sellerReviewAvatarImageView;
  @FXML private Label sellerReviewAvatarLabel;
  @FXML private ProgressBar bar5, bar4, bar3, bar2, bar1;
  @FXML private Label lblCount5, lblCount4, lblCount3, lblCount2, lblCount1;

  @FXML
  public void initialize() {
    Customer customer = UserSession.getLoggedInCustomer();
    if (customer == null) return;

    String storeName = customer.getSellerProfile() != null
        ? customer.getSellerProfile().getStoreName() : "Cửa hàng của tôi";

    ReviewContext.set(0, customer.getId(), storeName, false);
    if(lblShopName != null) lblShopName.setText(storeName);
    // 🎯 1. CẮT TRÒN AVATAR
    if (sellerReviewAvatarImageView != null) {
      Circle clip = new Circle();
      clip.centerXProperty().bind(sellerReviewAvatarImageView.fitWidthProperty().divide(2));
      clip.centerYProperty().bind(sellerReviewAvatarImageView.fitHeightProperty().divide(2));
      clip.radiusProperty().bind(sellerReviewAvatarImageView.fitWidthProperty().divide(2));
      sellerReviewAvatarImageView.setClip(clip);
    }

    // 🎯 2. LOAD ẢNH TỪ SESSION
    if (sellerReviewAvatarLabel != null && sellerReviewAvatarImageView != null) {
      String initial = "?";
      if (storeName != null && !storeName.isEmpty()) {
        initial = storeName.substring(0, 1).toUpperCase();
      }
      sellerReviewAvatarLabel.setText(initial);

      // Lấy URL và load!
      String avatarUrl = customer.getSellerProfile().getAvatarData();
      com.uet.bidding.util.ImageUtils.loadAvatarFromUrl(sellerReviewAvatarImageView, sellerReviewAvatarLabel, avatarUrl);
    }

    loadReviews();
  }

  private void loadReviews() {
    if (reviewsContainer == null) return;
    reviewsContainer.getChildren().clear();
    reviewsContainer.setSpacing(15);

    ClientService.getInstance()
        .sendRequest("GET_REVIEWS_BY_SELLER", ReviewContext.sellerId)
        .thenAccept(this::onReviewsLoaded)
        .exceptionally(ex -> {
          Platform.runLater(() -> showAlert("Lỗi kết nối", ex.getMessage()));
          return null;
        });
  }

  private void onReviewsLoaded(NetworkMessage response) {
    Platform.runLater(() -> {
      if (!"SUCCESS".equals(response.getType())) {
        showAlert("Lỗi", String.valueOf(response.getData()));
        return;
      }

      Gson gson = ClientService.getInstance().getGson();
      try {
        JsonElement dataElement = gson.toJsonTree(response.getData());
        JsonArray arr = null;

        if (dataElement.isJsonObject()) {
          JsonObject dataObj = dataElement.getAsJsonObject();

          // 1. Hiển thị tên shop
          if (dataObj.has("storeName") && lblShopName != null) {
            lblShopName.setText(dataObj.get("storeName").getAsString());
          }

          // 📝 2. Hiển thị dòng mô tả shop động nhỏ ở dưới
          if (dataObj.has("storeDescription") && lblShopDesc != null) {
            lblShopDesc.setText(dataObj.get("storeDescription").getAsString());
            lblShopDesc.setStyle("-fx-font-size: 12px; -fx-text-fill: #747d8c; -fx-font-style: italic;");
          }
          if (dataObj.has("reviews")) arr = dataObj.getAsJsonArray("reviews");
        } else if (dataElement.isJsonArray()) {
          arr = dataElement.getAsJsonArray();
        }

        if (arr == null || arr.size() == 0) {
          Label lblEmpty = new Label("Cửa hàng của bạn chưa nhận được đánh giá nào.");
          lblEmpty.setStyle("-fx-text-fill: #94a3b8; -fx-font-style: italic;");
          reviewsContainer.getChildren().add(lblEmpty);
          return;
        }

        double sumStars = 0;
        int[] starCounts = new int[6];

        for (JsonElement el : arr) {
          JsonObject obj = el.getAsJsonObject();

          String name = "Người dùng ẩn danh";
          if (obj.has("reviewerName") && !obj.get("reviewerName").isJsonNull()) {
            name = obj.get("reviewerName").getAsString();
          } else if (obj.has("username") && !obj.get("username").isJsonNull()) {
            name = obj.get("username").getAsString();
          }

          int starsNum = obj.has("stars") ? obj.get("stars").getAsInt() : 5;
          sumStars += starsNum;
          if(starsNum >= 1 && starsNum <= 5) starCounts[starsNum]++;

          String content = obj.has("comment") && !obj.get("comment").isJsonNull()
              ? obj.get("comment").getAsString() : "(Không có bình luận)";

          String productName = "Sản phẩm đấu giá"; // Đặt mặc định trước
          if (obj.has("productName") && !obj.get("productName").isJsonNull()) {
            String pName = obj.get("productName").getAsString().trim();
            if (!pName.isEmpty()) {
              productName = pName; // Chỉ lấy nếu chuỗi thực sự có ký tự
            }
          }

          String dateStr = "-/-";
          if (obj.has("createdAt") && !obj.get("createdAt").isJsonNull()) {
            try {
              String rawDate = obj.get("createdAt").getAsString();
              if (rawDate.length() >= 19) {
                String cleanDate = rawDate.substring(0, 19).replace(" ", "T");
                // 🌟 SỬA TẠI ĐÂY: Parse ra LocalDateTime rồi cộng thêm 7 tiếng chuẩn GMT+7 Việt Nam
                LocalDateTime ldt = LocalDateTime.parse(cleanDate).plusHours(7);
                dateStr = ldt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
              } else {
                dateStr = rawDate;
              }
            } catch (Exception e) {
              dateStr = obj.get("createdAt").getAsString();
            }
          }

          addReviewCard(name, dateStr, starsNum, productName, content);
        }

        double avg = sumStars / arr.size();
        if (lblAvgRating != null) lblAvgRating.setText(String.format("%.1f", avg));
        if (lblReviewCount != null) lblReviewCount.setText("(" + arr.size() + " đánh giá)");
        if (lblStarRating != null) {
          renderDynamicStars(lblStarRating, avg);
        }
        if(bar5 != null) {
          bar5.setProgress((double) starCounts[5] / arr.size()); lblCount5.setText(String.valueOf(starCounts[5]));
          bar4.setProgress((double) starCounts[4] / arr.size()); lblCount4.setText(String.valueOf(starCounts[4]));
          bar3.setProgress((double) starCounts[3] / arr.size()); lblCount3.setText(String.valueOf(starCounts[3]));
          bar2.setProgress((double) starCounts[2] / arr.size()); lblCount2.setText(String.valueOf(starCounts[2]));
          bar1.setProgress((double) starCounts[1] / arr.size()); lblCount1.setText(String.valueOf(starCounts[1]));
        }

      } catch (Exception e) {
        e.printStackTrace();
        showAlert("Lỗi Dữ Liệu", "Không thể phân tích dữ liệu đánh giá từ server.");
      }
    });
  }

  // 🌟 ĐÃ SỬA: Sắp xếp lại luồng nạp con để bảo vệ dòng Tên sản phẩm không bị biến mất
  private void addReviewCard(String name, String date, int starsNum, String productName, String content) {
    HBox card = new HBox(15);
    card.setStyle("-fx-background-color: #ffffff; -fx-padding: 15; -fx-background-radius: 12; -fx-border-color: #f8bbd0; -fx-border-radius: 12; -fx-border-width: 1;");
    card.setAlignment(Pos.TOP_LEFT);

    // Khối Avatar tròn bên trái
    StackPane avatarPane = new StackPane();
    Circle avatarBg = new Circle(22, Color.web("#ffe4ec"));
    Label userIcon = new Label("👤");
    userIcon.setStyle("-fx-font-size: 22px; -fx-text-fill: #e91e63;");
    avatarPane.getChildren().addAll(avatarBg, userIcon);

    // Khối chứa toàn bộ nội dung chữ bên phải
    VBox contentBox = new VBox(6);
    contentBox.setMinWidth(350);
    HBox.setHgrow(contentBox, Priority.ALWAYS);

    // Dòng tiêu đề: Tên người dùng + Ngày tháng
    HBox headerBox = new HBox(10);
    headerBox.setAlignment(Pos.CENTER_LEFT);

    Label nameLabel = new Label(name);
    nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #333333;");

    Label dateLabel = new Label("(" + date + ")");
    dateLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #888888;");

    headerBox.getChildren().addAll(nameLabel, dateLabel);

    // Dòng hiển thị số sao số
    Label starsLabel = new Label("⭐ " + starsNum + ".0/5.0");
    starsLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #ffb300;");

    // Thêm tuần tự: Tiêu đề -> Số sao
    contentBox.getChildren().add(headerBox);
    contentBox.getChildren().add(starsLabel);

    // 📦 ĐÃ SỬA LỖI: Luôn kiểm tra và add trực tiếp dòng hiển thị tên sản phẩm vào đúng vị trí kế tiếp
    if (productName != null) {
      Label productLabel = new Label("📦 Sản phẩm: " + productName);
      productLabel.setStyle("-fx-font-size: 14px; -fx-font-style: italic; -fx-text-fill: #e91e63;");
      contentBox.getChildren().add(productLabel);
    }

    // Dòng nội dung bình luận chi tiết
    Label commentLabel = new Label("Đánh giá: " + content);
    commentLabel.setStyle("-fx-font-size: 15px; -fx-text-fill: #555555;");
    commentLabel.setWrapText(true);

    contentBox.getChildren().add(commentLabel);

    // Ghép ráp avatar và chữ vào thẻ card
    card.getChildren().addAll(avatarPane, contentBox);

    // Đẩy thẻ vào container chính
    if (reviewsContainer != null) {
      reviewsContainer.getChildren().add(card);
    }
  }

  private void renderDynamicStars(HBox container, double avgScore) {
    container.getChildren().clear(); // Dọn dẹp sạch cụm cũ
    container.setSpacing(3);         // Khoảng cách giữa các ngôi sao
    container.setAlignment(Pos.CENTER_LEFT);

    for (int i = 1; i <= 5; i++) {
      Label starLabel = new Label();
      starLabel.setStyle("-fx-font-size: 20px;"); // Kích thước hiển thị sao vừa vặn giao diện tổng

      if (avgScore >= i) {
        // 1. Sao vàng đậm nguyên vẹn (Điểm số bao trọn vị trí)
        starLabel.setText("★");
        starLabel.setStyle(starLabel.getStyle() + " -fx-text-fill: #ffb300;");
      } else if (avgScore > i - 1 && avgScore < i) {
        // 2. Điểm số nằm giữa khoảng lẻ (Ví dụ 3.5 thì ngôi sao thứ 4 rơi vào đây)
        // Dùng sao đặc nhưng đổi màu sang vàng chanh sáng dịu để làm nổi bật vị trí nửa sao
        starLabel.setText("★");
        starLabel.setStyle(starLabel.getStyle() + " -fx-text-fill: #ffdd67;");
      } else {
        // 3. Các ngôi sao rỗng còn lại phía sau
        starLabel.setText("☆");
        starLabel.setStyle(starLabel.getStyle() + " -fx-text-fill: #ced4da;");
      }
      container.getChildren().add(starLabel);
    }
  }

  private void showAlert(String title, String msg) {
    Alert a = new Alert(Alert.AlertType.INFORMATION);
    a.setTitle(title);
    a.setHeaderText(null);
    a.setContentText(msg);
    a.showAndWait();
  }

  @FXML
  private void handleBackToSellerDashboard(ActionEvent event) {
    try {
      Parent dashboardRoot = FXMLLoader.load(getClass().getResource("/SellerDashboard.fxml"));
      Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
      currentStage.setScene(new Scene(dashboardRoot));
      currentStage.show();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}