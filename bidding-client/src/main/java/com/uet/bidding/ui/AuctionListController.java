package com.uet.bidding.ui;

import com.uet.bidding.model.AuctionItem;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

public class AuctionListController implements Initializable {

  @FXML
  private TableView<AuctionItem> tableView;
  @FXML
  private TableColumn<AuctionItem, Integer> colStt;
  @FXML
  private TableColumn<AuctionItem, String> colCity;
  @FXML
  private TableColumn<AuctionItem, String> colProduct;
  @FXML
  private TableColumn<AuctionItem, Integer> colInterested;
  @FXML
  private TableColumn<AuctionItem, Void> colAction;

  @FXML
  private ComboBox<String> cityComboBox;

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    // 1. Cấu hình các cột lấy dữ liệu từ Model
    colStt.setCellValueFactory(new PropertyValueFactory<>("stt"));
    colCity.setCellValueFactory(new PropertyValueFactory<>("city"));
    colProduct.setCellValueFactory(new PropertyValueFactory<>("productType"));
    colInterested.setCellValueFactory(new PropertyValueFactory<>("interestedCount"));

    // 2. Tạo cột chứa nút bấm
    setupActionColumn();

    // 3. TẠO DỮ LIỆU ĐỂ ĐỔ VÀO BẢNG
    ObservableList<AuctionItem> dataList = FXCollections.observableArrayList(
        new AuctionItem(1, "Hà Nội", "Laptop Dell XPS 15", 125),
        new AuctionItem(2, "Đà Nẵng", "Đồng hồ Apple Watch S9", 45),
        new AuctionItem(3, "TP. HCM", "Xe đạp điện VinFast", 89),
        new AuctionItem(4, "Cần Thơ", "Máy ảnh Canon EOS R5", 12)
    );

    // 4. Đổ dữ liệu vào bảng
    tableView.setItems(dataList);

    // 5. KHỞI TẠO DANH SÁCH 63 TỈNH THÀNH CHO COMBOBOX
    ObservableList<String> cities = FXCollections.observableArrayList(
        "An Giang", "Bà Rịa - Vũng Tàu", "Bắc Giang", "Bắc Kạn", "Bạc Liêu", "Bắc Ninh", "Bến Tre",
        "Bình Định", "Bình Dương", "Bình Phước", "Bình Thuận", "Cà Mau", "Cần Thơ", "Cao Bằng",
        "Đà Nẵng", "Đắk Lắk", "Đắk Nông", "Điện Biên", "Đồng Nai", "Đồng Tháp", "Gia Lai",
        "Hà Giang", "Hà Nam", "Hà Nội", "Hà Tĩnh", "Hải Dương", "Hải Phòng", "Hậu Giang",
        "Hòa Bình", "Hưng Yên", "Khánh Hòa", "Kiên Giang", "Kon Tum", "Lai Châu", "Lâm Đồng",
        "Lạng Sơn", "Lào Cai", "Long An", "Nam Định", "Nghệ An", "Ninh Bình", "Ninh Thuận",
        "Phú Thọ", "Phú Yên", "Quảng Bình", "Quảng Nam", "Quảng Ngãi", "Quảng Ninh", "Quảng Trị",
        "Sóc Trăng", "Sơn La", "Tây Ninh", "Thái Bình", "Thái Nguyên", "Thanh Hóa", "Thừa Thiên Huế",
        "Tiền Giang", "TP Hồ Chí Minh", "Trà Vinh", "Tuyên Quang", "Vĩnh Long", "Vĩnh Phúc", "Yên Bái"
    );
    cityComboBox.setItems(cities);
  }

  private void setupActionColumn() {
    Callback<TableColumn<AuctionItem, Void>, TableCell<AuctionItem, Void>> cellFactory = new Callback<>() {
      @Override
      public TableCell<AuctionItem, Void> call(final TableColumn<AuctionItem, Void> param) {
        return new TableCell<>() {
          private final Button btn = new Button("Đăng kí đấu giá");

          {
            btn.setStyle("-fx-background-color: white; -fx-border-color: black; " +
                "-fx-border-radius: 20; -fx-background-radius: 20; " +
                "-fx-text-fill: #e84393; -fx-font-weight: bold; -fx-cursor: hand;");

            btn.setOnAction(event -> {
              AuctionItem selectedItem = getTableView().getItems().get(getIndex());

              try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/ProductDetail.fxml"));
                Parent root = loader.load();
                ProductDetailController detailController = loader.getController();

                com.uet.bidding.model.Electronics fakeProduct = new com.uet.bidding.model.Electronics(
                    selectedItem.getStt(),
                    selectedItem.getProductType(),
                    "Mô tả chi tiết: " + selectedItem.getProductType() + " chính hãng, bảo hành đầy đủ.",
                    new BigDecimal("1500000"),
                    "/images/default.jpg",
                    1,
                    "Thương hiệu VNU",
                    12
                );

                com.uet.bidding.model.Auction fakeAuction = new com.uet.bidding.model.Auction(
                    fakeProduct,
                    fakeProduct.getStartingPrice(),
                    LocalDateTime.now(),
                    LocalDateTime.now().plusDays(3)
                );
                fakeAuction.setId(selectedItem.getStt() + 1000);

                detailController.setAuctionData(fakeAuction);

                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.setTitle("Chi tiết sản phẩm - " + selectedItem.getProductType());
                stage.show();

              } catch (Exception e) {
                e.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Lỗi");
                alert.setHeaderText("Không thể mở trang chi tiết");
                alert.setContentText("Chi tiết lỗi: " + e.getMessage());
                alert.showAndWait();
              }
            });
          }

          @Override
          protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) setGraphic(null);
            else setGraphic(btn);
          }
        };
      }
    };
    colAction.setCellFactory(cellFactory);
  }

  // ==========================================
  // CÁC HÀM XỬ LÝ SỰ KIỆN NÚT BẤM / CHUYỂN TRANG
  // ==========================================

  @FXML
  public void handleSearch(ActionEvent event) {
    String selectedCity = cityComboBox.getValue();
    if (selectedCity != null && !selectedCity.trim().isEmpty()) {
      System.out.println("Bạn đang muốn tìm kiếm tại: " + selectedCity);
    } else {
      System.out.println("Vui lòng chọn một tỉnh/thành phố trước khi tìm kiếm!");
    }
  }

  @FXML
  public void handleLogout(ActionEvent event) {
    switchScene(event, "/Login.fxml", "Hệ thống Đấu giá VNU - Đăng nhập");
  }

  // ĐÃ SỬA LẠI ĐƯỜNG DẪN Ở ĐÂY
  @FXML
  public void handleGoToMyManagement(ActionEvent event) {
    switchScene(event, "/MyManagement.fxml", "Quản lý của tôi");
  }

  /**
   * Xử lý sự kiện khi click vào cụm Avatar / "My Profile"
   */
  @FXML
  public void handleGoToMyProfile(MouseEvent event) {
    try {
      Parent root = FXMLLoader.load(getClass().getResource("/UserProfile.fxml"));
      Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
      stage.setScene(new Scene(root));
      stage.setTitle("Hồ sơ cá nhân");
      stage.show();
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("Lỗi khi chuyển sang trang UserProfile.fxml");
    }
  }

  // Hàm tiện ích để chuyển trang (dùng cho các nút bấm)
  private void switchScene(ActionEvent event, String fxmlPath, String title) {
    try {
      Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
      Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
      stage.setScene(new Scene(root));
      stage.setTitle(title);
      stage.show();
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("Lỗi khi chuyển sang trang: " + fxmlPath);
    }
  }
}