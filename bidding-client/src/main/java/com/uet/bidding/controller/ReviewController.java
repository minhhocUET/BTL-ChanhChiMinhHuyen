package com.uet.bidding.controller;

import com.google.gson.reflect.TypeToken;
import com.uet.bidding.model.GsonFactory;
import com.uet.bidding.model.NetworkMessage;
import com.uet.bidding.model.Review;
import com.uet.bidding.network.ClientService;
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
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReviewController {
  @FXML
  private VBox reviewsContainer;
  @FXML
  private Label lblShopName;
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
  private Button btnAddReview;

  @FXML
  public void initialize() {
    cmbStars.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5));
    cmbStars.getSelectionModel().select(4);
    lblShopName.setText(ReviewContext.storeName != null ? ReviewContext.storeName : "Shop");
    addReviewBox.setVisible(ReviewContext.showAddForm);
    addReviewBox.setManaged(ReviewContext.showAddForm);
    btnAddReview.setVisible(!ReviewContext.showAddForm);
    btnAddReview.setManaged(!ReviewContext.showAddForm);
    loadReviews();
  }

  private void loadReviews() {
    reviewsContainer.getChildren().clear();
    ClientService.getInstance()
        .sendRequest("GET_REVIEWS_BY_SELLER", ReviewContext.sellerId)
        .thenAccept(this::onReviewsLoaded)
        .exceptionally(ex -> {
          Platform.runLater(() -> showAlert("Lỗi", ex.getMessage()));
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
      double sum = 0;
      for (Review r : reviews) {
        sum += r.getStars();
        String date = r.getCreatedAt() != null
            ? r.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            : "";
        addReviewCard(r.getReviewerName(), date, starsToEmoji(r.getStars()), r.getComment());
      }
      double avg = reviews.isEmpty() ? 0 : sum / reviews.size();
      lblAvgRating.setText(String.format("%.1f", avg));
      lblReviewCount.setText("(" + reviews.size() + " đánh giá)");
    });
  }

  @FXML
  private void handleOpenAddReview() {
    addReviewBox.setVisible(true);
    addReviewBox.setManaged(true);
    btnAddReview.setVisible(false);
    btnAddReview.setManaged(false);
  }

  @FXML
  private void handleSubmitReview() {
    if (UserSession.getLoggedInCustomer() == null) {
      showAlert("Lỗi", "Đăng nhập trước!");
      return;
    }
    int stars = cmbStars.getValue();
    String comment = txtReviewComment.getText().trim();
    if (comment.isEmpty()) {
      showAlert("Lỗi", "Nhập nội dung đánh giá!");
      return;
    }
    Map<String, Object> payload = new HashMap<>();
    payload.put("auctionId", ReviewContext.auctionId);
    payload.put("sellerId", ReviewContext.sellerId);
    payload.put("stars", stars);
    payload.put("comment", comment);
    ClientService.getInstance().sendRequest("ADD_REVIEW", payload)
        .thenAccept(res -> Platform.runLater(() -> {
          if ("SUCCESS".equals(res.getType())) {
            showAlert("OK", "Đã gửi đánh giá!");
            txtReviewComment.clear();
            addReviewBox.setVisible(false);
            addReviewBox.setManaged(false);
            loadReviews();
          } else {
            showAlert("Lỗi", String.valueOf(res.getData()));
          }
        }));
  }

  private String starsToEmoji(int stars) {
    return "⭐".repeat(Math.max(0, stars));
  }

  /**
   * Hàm sinh tự động các ô Đánh giá (Review Card) theo phong cách thiết kế gọn gàng, bo góc
   */
  private void addReviewCard(String name, String date, String stars, String content) {
    // Khung card bao ngoài
    VBox card = new VBox(8);
    card.setStyle("-fx-background-color: #ffffff; -fx-padding: 15; -fx-background-radius: 14; -fx-border-color: #f8bbd0; -fx-border-radius: 14; -fx-border-width: 1;");

    // HBox phần đầu chứa Avatar, Tên và Ngày tháng
    HBox topRow = new HBox(12);
    topRow.setAlignment(Pos.CENTER_LEFT);

    // Tạo avatar dạng hình tròn hồng chứa icon user
    StackPane avatarPane = new StackPane();
    Circle avatarBg = new Circle(18, Color.web("#ffe4ec"));
    avatarBg.setStroke(Color.web("#ffb3cc"));
    Label userIcon = new Label("👤");
    userIcon.setStyle("-fx-font-size: 16px;");
    avatarPane.getChildren().addAll(avatarBg, userIcon);

    // Tên user
    Label nameLabel = new Label(name);
    nameLabel.setFont(Font.font("System", FontWeight.BOLD, 15));
    nameLabel.setTextFill(Color.web("#333333"));

    // Đẩy ngày tháng dạt về phía bên phải rìa màn hình
    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    // Ngày tháng đánh giá
    Label dateLabel = new Label(date);
    dateLabel.setTextFill(Color.web("#888888"));
    dateLabel.setFont(Font.font("System", 12));

    topRow.getChildren().addAll(avatarPane, nameLabel, spacer, dateLabel);

    // Dòng hiển thị số sao vàng
    Label starsLabel = new Label(stars);
    starsLabel.setTextFill(Color.web("#ffb300"));
    starsLabel.setStyle("-fx-font-size: 14px;");

    // Nội dung văn bản phản hồi của khách hàng
    Label contentLabel = new Label(content);
    contentLabel.setTextFill(Color.web("#555555"));
    contentLabel.setFont(Font.font("System", 14));
    contentLabel.setWrapText(true); // Tự động xuống dòng khi text quá dài

    // Gắn tất cả các lớp vào card chung
    card.getChildren().addAll(topRow, starsLabel, contentLabel);

    // Thêm card vừa tạo vào container lớn trên giao diện
    if (reviewsContainer != null) {
      reviewsContainer.getChildren().add(card);
    }
  }

  /**
   * HÀM XỬ LÝ CHUYỂN TRANG SANG AUCTIONLIST KHI ẤN "DANH SÁCH ĐẤU GIÁ"
   */
  @FXML
  private void handleBackToAuctionList(ActionEvent event) {
    try {
      // Nạp file giao diện danh sách đấu giá từ tài nguyên hệ thống
      Parent auctionListRoot = FXMLLoader.load(getClass().getResource("/AuctionList.fxml"));

      // Lấy Stage (Cửa sổ hiện tại) từ sự kiện bấm nút của chuột
      Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();

      // Thay đổi Scene bên trong Stage sang giao diện mới công khai
      currentStage.setScene(new Scene(auctionListRoot));
      currentStage.setTitle("Danh sách đấu giá");
      currentStage.show();

      System.out.println(">>> Đã chuyển hướng mượt mà sang AuctionList.fxml thành công!");
    } catch (IOException e) {
      e.printStackTrace();
      System.out.println("❌ Lỗi nghiêm trọng: Không thể tìm thấy hoặc tải file AuctionList.fxml!");
    }
  }

  private void showAlert(String title, String msg) {
    Alert a = new Alert(Alert.AlertType.INFORMATION);
    a.setTitle(title);
    a.setContentText(msg);
    a.showAndWait();
  }
}