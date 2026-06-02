package com.uet.bidding.controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.uet.bidding.model.GsonFactory;
import com.uet.bidding.model.NetworkMessage;
import com.uet.bidding.model.Review;
import com.uet.bidding.network.ClientService;
import com.uet.bidding.service.BidderService;
import com.uet.bidding.util.ImageUtils;
import com.uet.bidding.util.ReviewContext;
import com.uet.bidding.util.UserSession;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReviewController {
  @FXML private ImageView reviewAvatarImageView;
  @FXML private Label reviewAvatarLabel;
  @FXML
  private Label lblBigAvgRating; // Khai báo điều khiển ô điểm lớn bên phải
  @FXML
  private VBox reviewsContainer;
  @FXML
  private Label lblShopName;
  @FXML
  private Label lblShopDescription;
  @FXML
  private Label lblAvgRating;
  @FXML
  private Label lblReviewCount;
  @FXML
  private VBox addReviewBox;
  @FXML
  private ComboBox<Integer> cmbStars;
  @FXML
  private TextArea txtReviewComment;
  @FXML
  private Button btnBackToManagement;

  private final BidderService bidderService = new BidderService();

  @FXML
  public void initialize() {
    // 1. Cấu hình ComboBox chọn điểm sao (1 - 5)
    cmbStars.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5));
    cmbStars.getSelectionModel().select(4);

    // 2. Lấy thông tin cơ bản của Shop
    if (ReviewContext.storeName != null) {
      lblShopName.setText(ReviewContext.storeName);
    } else {
      lblShopName.setText("Đánh giá cửa hàng #" + ReviewContext.sellerId);
    }
    // 🎯 THIẾT LẬP MẶT NẠ CẮT TRÒN CHO AVATAR (Chuẩn động - không fix cứng)
    if (reviewAvatarImageView != null) {
      Circle clip = new Circle();
      // Tự động bám theo kích thước của ImageView trên FXML
      clip.centerXProperty().bind(reviewAvatarImageView.fitWidthProperty().divide(2));
      clip.centerYProperty().bind(reviewAvatarImageView.fitHeightProperty().divide(2));
      clip.radiusProperty().bind(reviewAvatarImageView.fitWidthProperty().divide(2));

      reviewAvatarImageView.setClip(clip);
    }

    // 🎯 LẤY LINK ẢNH ĐỂ HIỂN THỊ
    if (reviewAvatarLabel != null) {
      String initial = "?";
      if (lblShopName.getText() != null && !lblShopName.getText().isEmpty()) {
        initial = lblShopName.getText().substring(0, 1).toUpperCase();
      }
      reviewAvatarLabel.setText(initial);

      // Tìm URL ảnh đại diện của Seller này:
      String avatarUrl = null;

      // Nếu Seller đang xem đánh giá của chính mình (Lấy từ Session)
      if (UserSession.getLoggedInCustomer() != null
          && UserSession.getLoggedInCustomer().getId() == ReviewContext.sellerId) {
        avatarUrl = UserSession.getLoggedInCustomer().getSellerProfile().getAvatarData();
      }
      // (Tùy chọn) Nếu sau này bạn mở trang này cho Khách hàng xem shop người khác,
      // hãy đảm bảo bạn truyền link ảnh vào biến ReviewContext.avatarUrl trước khi mở form nhé!
      // else if (ReviewContext.avatarUrl != null) {
      //    avatarUrl = ReviewContext.avatarUrl;
      // }

      // Gọi hàm load ảnh với URL thật (không truyền null nữa)
      ImageUtils.loadAvatarFromUrl(reviewAvatarImageView, reviewAvatarLabel, avatarUrl);
    }

    // 3. ĐỒNG BỘ ĐỘNG: Ẩn hoàn toàn form nhập và các nút bấm tạo mới nếu showAddForm = false
    addReviewBox.setVisible(ReviewContext.showAddForm);
    addReviewBox.setManaged(ReviewContext.showAddForm);

    // 🌟 ĐÃ SỬA: Nút "Quay lại" (btnBackToManagement) LÚC NÀO CŨNG PHẢI HIỂN THỊ
    // Dù người dùng có được phép viết đánh giá hay không, họ vẫn phải thấy nút này để bấm quay về
    if (btnBackToManagement != null) {
      btnBackToManagement.setVisible(true);
      btnBackToManagement.setManaged(true);
    }

    // 4. Tự động tải danh sách cũ từ Server lên màn hình để đọc
    loadReviews();
  }

  private void loadReviews() {
    // Xóa sạch các card cũ để chuẩn bị nạp mới
    reviewsContainer.getChildren().clear();
    reviewsContainer.setSpacing(15);

    // Gọi hàm từ BidderService gửi tín hiệu lên Server
    bidderService.loadReviewsForSeller(ReviewContext.sellerId).thenAccept(res -> {
      Platform.runLater(() -> {
        if (!"SUCCESS".equals(res.getType())) {
          showAlert("Lỗi", String.valueOf(res.getData()));
          return;
        }

        Gson gson = com.uet.bidding.network.ClientService.getInstance().getGson();

        // Chuyển dữ liệu nhận về thành JsonObject tổng thể (thành phần bọc ngoài)
        JsonObject dataObj = gson.toJsonTree(res.getData()).getAsJsonObject();

        // Cập nhật Tên Shop thật và Mô tả Shop động lên UI Client
        if (dataObj.has("storeName")) {
          String storeName = dataObj.get("storeName").getAsString();
          lblShopName.setText(storeName);
        }

        if (dataObj.has("storeDescription")) {
          String desc = dataObj.get("storeDescription").getAsString();
          System.out.println(">>> [DEBUG CLIENT] Mô tả nhận từ Server: " + desc); // Kiểm tra log tại tab Run của IntelliJ

          if (lblShopDescription != null) {
            if (desc == null || desc.trim().isEmpty() || "-".equals(desc)) {
              lblShopDescription.setText("Cửa hàng hiện chưa có mô tả nào.");
            } else {
              lblShopDescription.setText(desc);
            }
          }
        } else {
          System.out.println(">>> [DEBUG CLIENT] Server HOÀN TOÀN KHÔNG gửi trường storeDescription về!");
        }
        // 🎯 TẢI VÀ ĐỒNG BỘ ẢNH AVATAR CLOUDINARY
        String avatarUrl = null;
        if (dataObj.has("avatarData") && !dataObj.get("avatarData").isJsonNull()) {
          avatarUrl = dataObj.get("avatarData").getAsString();
        } else if (dataObj.has("avatar") && !dataObj.get("avatar").isJsonNull()) {
          // Phòng hờ nếu key trả về từ server là "avatar"
          avatarUrl = dataObj.get("avatar").getAsString();
        }

        // Cập nhật lại chữ cái dự phòng cho chắc chắn
        if (reviewAvatarLabel != null) {
          String initial = "?";
          if (lblShopName.getText() != null && !lblShopName.getText().isEmpty()) {
            initial = lblShopName.getText().substring(0, 1).toUpperCase();
          }
          reviewAvatarLabel.setText(initial);
        }

        // Kích hoạt hàm Load URL từ ImageUtils để tự động hiện ảnh bo tròn (nếu có URL) hoặc hiện chữ cái (nếu url null)
        ImageUtils.loadAvatarFromUrl(reviewAvatarImageView, reviewAvatarLabel, avatarUrl);

        // Bóc tách mảng danh sách bài review nằm bên trong Object tổng thể
        JsonArray arr = dataObj.getAsJsonArray("reviews");

        double sum = 0;

        if (arr == null || arr.size() == 0) {
          Label lblEmpty = new Label("Chưa có đánh giá nào cho shop này.");
          lblEmpty.setStyle("-fx-text-fill: #94a3b8; -fx-font-style: italic;");
          reviewsContainer.getChildren().add(lblEmpty);

          lblAvgRating.setText("0.0");
          if (lblBigAvgRating != null) lblBigAvgRating.setText("0.0");
          lblReviewCount.setText("(0 đánh giá)");
          return;
        }

        // Vòng lặp duyệt qua từng bài review để sinh card giao diện phẳng
        for (JsonElement el : arr) {
          JsonObject obj = el.getAsJsonObject();

          String name = "Người dùng ẩn danh";
          if (obj.has("reviewerName") && !obj.get("reviewerName").isJsonNull()) {
            name = obj.get("reviewerName").getAsString();
          } else if (obj.has("username") && !obj.get("username").isJsonNull()) {
            name = obj.get("username").getAsString();
          }

          int stars = obj.has("stars") ? obj.get("stars").getAsInt() : 5;
          sum += stars;

          String comment = obj.has("comment") && !obj.get("comment").isJsonNull()
              ? obj.get("comment").getAsString()
              : "(Không có bình luận)";

          String productName = obj.has("productName") && !obj.get("productName").isJsonNull()
              ? obj.get("productName").getAsString()
              : "Sản phẩm đấu giá";

          String dateStr = "-/-";
          if (obj.has("createdAt") && !obj.get("createdAt").isJsonNull()) {
            try {
              String rawDate = obj.get("createdAt").getAsString();
              if (rawDate.length() >= 19) {
                String cleanDate = rawDate.substring(0, 19).replace(" ", "T");
                LocalDateTime ldt = LocalDateTime.parse(cleanDate);
                dateStr = ldt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
              } else {
                dateStr = rawDate;
              }
            } catch (Exception e) {
              dateStr = obj.get("createdAt").getAsString();
            }
          }

          // Khởi tạo Layout HBox (Card vẽ động cho từng bài đánh giá trên UI)
          HBox card = new HBox(12);
          card.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 12; "
              + "-fx-border-color: #f8bbd0; -fx-border-width: 1; -fx-border-radius: 12;");
          card.setAlignment(Pos.TOP_LEFT);

          VBox avatarBox = new VBox();
          Label avatarLabel = new Label("👤");
          avatarLabel.setStyle("-fx-font-size: 16; -fx-background-color: #ffe4e6; -fx-text-fill: #e91e63; -fx-padding: 8; -fx-background-radius: 50;");
          avatarBox.getChildren().add(avatarLabel);

          VBox contentBox = new VBox(4);
          HBox.setHgrow(contentBox, Priority.ALWAYS);

          HBox topRow = new HBox();
          topRow.setAlignment(Pos.CENTER_LEFT);
          Label nameLabel = new Label(name);
          nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #333333; -fx-font-size: 14;");

          Region spacer = new Region();
          HBox.setHgrow(spacer, Priority.ALWAYS);

          Label dateLabel = new Label(dateStr);
          dateLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 12;");
          topRow.getChildren().addAll(nameLabel, spacer, dateLabel);

          String starRatingText = String.format("⭐ %.1f/5.0", (double) stars);
          Label starsLabel = new Label(starRatingText);
          starsLabel.setStyle("-fx-text-fill: #ffb300; -fx-font-weight: bold; -fx-font-size: 13;");

          Label productLabel = new Label("📦 Sản phẩm: " + productName);
          productLabel.setStyle("-fx-text-fill: #e91e63; -fx-font-size: 12; -fx-font-style: italic;");

          Label commentLabel = new Label("Đánh giá: " + comment);
          commentLabel.setStyle("-fx-text-fill: #555555; -fx-font-size: 14; -fx-wrap-text: true;");
          commentLabel.setWrapText(true);

          contentBox.getChildren().addAll(topRow, starsLabel, productLabel, commentLabel);
          card.getChildren().addAll(avatarBox, contentBox);

          reviewsContainer.getChildren().add(card);
        }

        double avg = sum / arr.size();
        String avgStr = String.format("%.1f", avg);
        lblAvgRating.setText(avgStr);
        if (lblBigAvgRating != null) lblBigAvgRating.setText(avgStr);
        lblReviewCount.setText("(" + arr.size() + " đánh giá)");
      });
    }).exceptionally(ex -> {
      Platform.runLater(() -> showAlert("Lỗi kết nối", "Không thể tải đánh giá: " + ex.getMessage()));
      return null;
    });
  }

  private void onReviewsLoaded(NetworkMessage response) {
    Platform.runLater(() -> {
      if (!"SUCCESS".equals(response.getType())) {
        showAlert("Lỗi", String.valueOf(response.getData()));
        return;
      }
      String json = GsonFactory.getInstance().toJson(response.getData());
      List<Review> reviews = GsonFactory.getInstance().fromJson(json,
          new TypeToken<List<Review>>() {
          }.getType());

      if (reviews == null) reviews = new java.util.ArrayList<>();

      double sum = 0;
      for (Review r : reviews) {
        sum += r.getStars();
        String date = r.getCreatedAt() != null
            ? r.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            : "";
        addReviewCard(r.getReviewerName(), date, starsToEmoji(r.getStars()), r.getComment());
      }

      double avg = reviews.isEmpty() ? 0 : sum / reviews.size();

      // CẬP NHẬT ĐỒNG THỜI CẢ 2 Ô ĐIỂM TRÊN GIAO DIỆN
      String avgStr = String.format("%.1f", avg);
      lblAvgRating.setText(avgStr);
      if (lblBigAvgRating != null) {
        lblBigAvgRating.setText(avgStr); // Ô điểm to bên phải sẽ nhảy số theo đúng DB
      }
      lblReviewCount.setText("(" + reviews.size() + " đánh giá)");
    });
  }

  @FXML
  private void handleSubmitReview(ActionEvent event) {
    Integer stars = cmbStars.getValue();
    String comment = txtReviewComment.getText().trim();

    if (stars == null) {
      showAlert(Alert.AlertType.WARNING, "Thông báo", "Vui lòng chọn số sao đánh giá!");
      return;
    }

    Map<String, Object> payload = new HashMap<>();
    payload.put("auctionId", ReviewContext.auctionId);
    payload.put("sellerId", ReviewContext.sellerId);
    payload.put("stars", stars);
    payload.put("comment", comment);

    ClientService.getInstance()
        .sendRequest("ADD_REVIEW", payload)
        .thenAccept(res -> Platform.runLater(() -> {
          if ("SUCCESS".equals(res.getType())) {
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Cảm ơn bạn đã gửi đánh giá thành công!");

            // Đóng form nhập đánh giá lại hoàn toàn sau khi viết xong
            addReviewBox.setVisible(false);
            addReviewBox.setManaged(false);
            txtReviewComment.clear();

            // Refresh cập nhật danh sách lập tức
            loadReviews();
          } else {
            showAlert(Alert.AlertType.ERROR, "Thất bại", String.valueOf(res.getData()));
          }
        }))
        .exceptionally(ex -> {
          Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi hệ thống", ex.getMessage()));
          return null;
        });
  }

  private String starsToEmoji(int stars) {
    return "⭐".repeat(Math.max(0, stars));
  }

  // 🌟 HÀM SỬA ĐỔI CHÍNH: Lắng nghe sự kiện để quay lại màn hình MyManagementController
  @FXML
  private void handleBackToManagement(ActionEvent event) {
    try {
      // Nạp tệp giao diện quản lý từ thư mục tài nguyên của bạn
      Parent root = FXMLLoader.load(getClass().getResource("/MyManagement.fxml"));

      // Khai thác Stage hiện tại từ nút bấm được kích hoạt
      Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

      // Chuyển scene mượt mà về trang quản lý lịch sử và sảnh đấu giá của bidder
      stage.setScene(new Scene(root));
      stage.setTitle("Quản lý của tôi - Hệ thống đấu giá");
      stage.show();
      System.out.println(">>> [Quay lại] Đã quay trở về màn hình MyManagement.fxml thành công!");
    } catch (IOException e) {
      e.printStackTrace();
      System.err.println("❌ Lỗi: Không thể tải /MyManagement.fxml. Kiểm tra lại đường dẫn tệp!");
    }
  }

  private void addReviewCard(String name, String date, String stars, String content) {
    VBox card = new VBox(8);
    card.setStyle("-fx-background-color: #ffffff; -fx-padding: 15; -fx-background-radius: 14; -fx-border-color: #f8bbd0; -fx-border-radius: 14; -fx-border-width: 1;");

    HBox topRow = new HBox(12);
    topRow.setAlignment(Pos.CENTER_LEFT);

    StackPane avatarPane = new StackPane();
    Circle avatarBg = new Circle(18, Color.web("#ffe4ec"));
    avatarBg.setStroke(Color.web("#ffb3cc"));
    Label userIcon = new Label("👤");
    userIcon.setStyle("-fx-font-size: 16px;");
    avatarPane.getChildren().addAll(avatarBg, userIcon);

    Label nameLabel = new Label(name);
    nameLabel.setFont(Font.font("System", FontWeight.BOLD, 15));
    nameLabel.setTextFill(Color.web("#333333"));

    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    Label dateLabel = new Label(date);
    dateLabel.setTextFill(Color.web("#888888"));
    dateLabel.setFont(Font.font("System", 12));

    topRow.getChildren().addAll(avatarPane, nameLabel, spacer, dateLabel);

    Label starsLabel = new Label(stars);
    starsLabel.setTextFill(Color.web("#ffb300"));
    starsLabel.setStyle("-fx-font-size: 14px;");

    Label contentLabel = new Label(content == null || content.isEmpty() ? "(Không có bình luận)" : content);
    contentLabel.setTextFill(Color.web("#555555"));
    contentLabel.setFont(Font.font("System", 14));
    contentLabel.setWrapText(true);

    card.getChildren().addAll(topRow, starsLabel, contentLabel);

    if (reviewsContainer != null) {
      reviewsContainer.getChildren().add(card);
    }
  }

  @FXML
  private void handleBackToAuctionList(ActionEvent event) {
    try {
      Parent auctionListRoot = FXMLLoader.load(getClass().getResource("/AuctionList.fxml"));
      Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
      currentStage.setScene(new Scene(auctionListRoot));
      currentStage.setTitle("Danh sách đấu giá");
      currentStage.show();
      System.out.println(">>> Đã chuyển hướng mượt mà sang AuctionList.fxml thành công!");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * HÀM SỬA LỖI: Hỗ trợ cả 2 tham số (để tương thích loadReviews)
   */
  private void showAlert(String title, String msg) {
    showAlert(Alert.AlertType.INFORMATION, title, msg);
  }

  /**
   * HÀM SỬA LỖI: Hỗ trợ 3 tham số kèm theo icon động (cho handleSubmitReview)
   */
  private void showAlert(Alert.AlertType type, String title, String msg) {
    Alert a = new Alert(type);
    a.setTitle(title);
    a.setHeaderText(null);
    a.setContentText(msg);
    a.showAndWait();
  }
}
