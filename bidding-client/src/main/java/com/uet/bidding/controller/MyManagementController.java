package com.uet.bidding.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;

public class MyManagementController {

  // --- Khai báo các thành phần của Tab Lịch sử mua hàng ---
  @FXML
  private TableView<HistoryItem> sellTableView;
  @FXML
  private TableColumn<HistoryItem, Integer> colSellStt;
  @FXML
  private TableColumn<HistoryItem, String> colSellName;
  @FXML
  private TableColumn<HistoryItem, String> colSellStartPrice;
  @FXML
  private TableColumn<HistoryItem, String> colSellHighestBid;
  @FXML
  private TableColumn<HistoryItem, Void> colReview; // Cột này không chứa dữ liệu text mà chứa Nút bấm (Void)

  @FXML
  public void initialize() {
    // 1. Ánh xạ các cột với thuộc tính của Class HistoryItem
    colSellStt.setCellValueFactory(new PropertyValueFactory<>("stt"));
    colSellName.setCellValueFactory(new PropertyValueFactory<>("name"));
    colSellStartPrice.setCellValueFactory(new PropertyValueFactory<>("startPrice"));
    colSellHighestBid.setCellValueFactory(new PropertyValueFactory<>("highestBid"));

    // 2. Thêm nút "Đánh giá" vào cột colReview
    addReviewButtonToTable();

    // 3. Tạo dữ liệu giả lập (1 sản phẩm)
    ObservableList<HistoryItem> historyData = FXCollections.observableArrayList(
        new HistoryItem(1, "Balo Pink Capybara dễ thương", "150,000 VND", "320,000 VND")
    );

    // Đổ dữ liệu vào bảng
    sellTableView.setItems(historyData);
  }

  /**
   * Hàm này dùng để vẽ một nút bấm (Button) vào bên trong từng ô của cột "Đánh giá shop"
   */
  private void addReviewButtonToTable() {
    Callback<TableColumn<HistoryItem, Void>, TableCell<HistoryItem, Void>> cellFactory = new Callback<>() {
      @Override
      public TableCell<HistoryItem, Void> call(final TableColumn<HistoryItem, Void> param) {
        return new TableCell<>() {
          // Tạo nút bấm với style màu hồng
          private final Button btn = new Button("⭐ Đánh giá");

          {
            btn.setStyle("-fx-background-color: #e91e63; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5;");

            // Sự kiện khi bấm vào nút
            btn.setOnAction((ActionEvent event) -> {
              // Lấy ra thông tin sản phẩm ở dòng hiện tại (nếu cần dùng sau này)
              HistoryItem data = getTableView().getItems().get(getIndex());
              System.out.println("Đang mở đánh giá cho sản phẩm: " + data.getName());

              // Gọi hàm chuyển trang
              handleGoToReview(event);
            });
          }

          @Override
          public void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
              setGraphic(null);
            } else {
              setGraphic(btn); // Hiển thị nút nếu dòng này có dữ liệu
            }
          }
        };
      }
    };

    colReview.setCellFactory(cellFactory);
  }

  /**
   * Hàm chuyển hướng sang trang Review.fxml
   */
  private void handleGoToReview(ActionEvent event) {
    try {
      Parent root = FXMLLoader.load(getClass().getResource("/Review.fxml"));
      Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
      stage.setScene(new Scene(root));
      stage.setTitle("Đánh giá Shop");
      stage.show();
    } catch (IOException e) {
      e.printStackTrace();
      System.out.println("Lỗi: Không thể tải file Review.fxml");
    }
  }

  @FXML
  private void handleBack(ActionEvent event) {
    try {
      Parent root = FXMLLoader.load(getClass().getResource("/AuctionList.fxml"));
      Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
      stage.setScene(new Scene(root));
      stage.show();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @FXML
  private void handleGoToMyProfile() {
    System.out.println("Go to My Profile clicked");
    // Logic chuyển sang trang Profile của bạn ở đây
  }

  // =====================================================================
  // CLASS NỘI BỘ DÙNG ĐỂ CHỨA DỮ LIỆU GIẢ LẬP CHO BẢNG (MODEL)
  // =====================================================================
  public static class HistoryItem {
    private int stt;
    private String name;
    private String startPrice;
    private String highestBid;

    public HistoryItem(int stt, String name, String startPrice, String highestBid) {
      this.stt = stt;
      this.name = name;
      this.startPrice = startPrice;
      this.highestBid = highestBid;
    }

    // Getter bắt buộc phải có để TableView có thể đọc được dữ liệu
    public int getStt() { return stt; }
    public String getName() { return name; }
    public String getStartPrice() { return startPrice; }
    public String getHighestBid() { return highestBid; }
  }
}