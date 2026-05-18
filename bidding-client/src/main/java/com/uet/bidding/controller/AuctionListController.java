package com.uet.bidding.controller;

import com.uet.bidding.model.Auction;
import com.uet.bidding.model.Electronics;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
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
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.ResourceBundle;

public class AuctionListController implements Initializable {

  @FXML
  private TableView<Auction> tableView;

  @FXML
  private TableColumn<Auction, Integer> colStt;

  @FXML
  private TableColumn<Auction, String> colCity;

  @FXML
  private TableColumn<Auction, String> colProduct;

  @FXML
  private TableColumn<Auction, Integer> colInterested;

  @FXML
  private TableColumn<Auction, Void> colAction;

  @FXML
  private ComboBox<String> cityComboBox;

  @Override
  public void initialize(URL location, ResourceBundle resources) {

    // STT
    colStt.setCellValueFactory(cellData ->
        new SimpleObjectProperty<>(cellData.getValue().getId())
    );

    // Tên sản phẩm
    colProduct.setCellValueFactory(cellData ->
        new SimpleStringProperty(
            cellData.getValue().getItem().getName()
        )
    );

    // Thành phố
    colCity.setCellValueFactory(cellData ->
        new SimpleStringProperty("Hà Nội")
    );

    // Số người quan tâm
    colInterested.setCellValueFactory(cellData ->
        new SimpleObjectProperty<>(
            new Random().nextInt(100) + 10
        )
    );

    // Cột nút bấm
    setupActionColumn();

    // Danh sách dữ liệu fake
    ObservableList<Auction> dataList =
        FXCollections.observableArrayList();

    String[] products = {
        "Laptop Dell XPS 15",
        "Đồng hồ Apple Watch S9",
        "Xe đạp điện VinFast",
        "Máy ảnh Canon EOS R5"
    };

    for (int i = 0; i < products.length; i++) {

      Electronics fakeProduct = new Electronics(
          i + 1,
          products[i],
          "Mô tả chi tiết: " + products[i]
              + " chính hãng, bảo hành đầy đủ.",
          new BigDecimal("1500000"),
          "/images/default.jpg",
          1,
          "Thương hiệu VNU",
          12
      );

      Auction fakeAuction = new Auction(
          fakeProduct,
          fakeProduct.getStartingPrice(),
          LocalDateTime.now(),
          LocalDateTime.now().plusDays(3)
      );

      fakeAuction.setId(i + 1);

      dataList.add(fakeAuction);
    }

    // Đổ dữ liệu vào bảng
    tableView.setItems(dataList);

    // Tự co cột
    tableView.setColumnResizePolicy(
        TableView.CONSTRAINED_RESIZE_POLICY
    );

    // Danh sách tỉnh thành
    ObservableList<String> cities =
        FXCollections.observableArrayList(
            "An Giang", "Bà Rịa - Vũng Tàu", "Bắc Giang", "Bắc Kạn", "Bạc Liêu",
            "Bắc Ninh", "Bến Tre", "Bình Định", "Bình Dương", "Bình Phước",
            "Bình Thuận", "Cà Mau", "Cần Thơ", "Cao Bằng", "Đà Nẵng",
            "Đắk Lắk", "Đắk Nông", "Điện Biên", "Đồng Nai", "Đồng Tháp",
            "Gia Lai", "Hà Giang", "Hà Nam", "Hà Nội", "Hà Tĩnh",
            "Hải Dương", "Hải Phòng", "Hậu Giang", "Hòa Bình", "Hưng Yên",
            "Khánh Hòa", "Kiên Giang", "Kon Tum", "Lai Châu", "Lâm Đồng",
            "Lạng Sơn", "Lào Cai", "Long An", "Nam Định", "Nghệ An",
            "Ninh Bình", "Ninh Thuận", "Phú Thọ", "Phú Yên", "Quảng Bình",
            "Quảng Nam", "Quảng Ngãi", "Quảng Ninh", "Quảng Trị", "Sóc Trăng",
            "Sơn La", "Tây Ninh", "Thái Bình", "Thái Nguyên", "Thanh Hóa",
            "Thừa Thiên Huế", "Tiền Giang", "TP Hồ Chí Minh", "Trà Vinh",
            "Tuyên Quang", "Vĩnh Long", "Vĩnh Phúc", "Yên Bái"
        );

    cityComboBox.setItems(cities);
  }

  private void setupActionColumn() {

    Callback<TableColumn<Auction, Void>,
        TableCell<Auction, Void>> cellFactory =
        new Callback<>() {

          @Override
          public TableCell<Auction, Void> call(
              final TableColumn<Auction, Void> param
          ) {

            return new TableCell<>() {

              private final Button btn =
                  new Button("Đăng kí đấu giá");

              {

                btn.setStyle(
                    "-fx-background-color: white;"
                        + "-fx-border-color: black;"
                        + "-fx-border-radius: 20;"
                        + "-fx-background-radius: 20;"
                        + "-fx-text-fill: #e84393;"
                        + "-fx-font-weight: bold;"
                        + "-fx-cursor: hand;"
                );

                btn.setOnAction(event -> {

                  Auction selectedAuction =
                      getTableView()
                          .getItems()
                          .get(getIndex());

                  try {

                    FXMLLoader loader =
                        new FXMLLoader(
                            getClass().getResource(
                                "/ProductDetail.fxml"
                            )
                        );

                    Parent root = loader.load();

                    ProductDetailController detailController =
                        loader.getController();

                    detailController.setAuctionData(
                        selectedAuction
                    );

                    Stage stage =
                        (Stage) ((Node) event.getSource())
                            .getScene()
                            .getWindow();

                    stage.setScene(new Scene(root));

                    stage.setTitle(
                        "Chi tiết sản phẩm - "
                            + selectedAuction
                            .getItem()
                            .getName()
                    );

                    stage.show();

                  } catch (Exception e) {

                    e.printStackTrace();

                    Alert alert =
                        new Alert(Alert.AlertType.ERROR);

                    alert.setTitle("Lỗi");

                    alert.setHeaderText(
                        "Không thể mở trang chi tiết"
                    );

                    alert.setContentText(
                        "Chi tiết lỗi: "
                            + e.getMessage()
                    );

                    alert.showAndWait();
                  }
                });
              }

              @Override
              protected void updateItem(
                  Void item,
                  boolean empty
              ) {

                super.updateItem(item, empty);

                if (empty) {
                  setGraphic(null);
                } else {
                  setGraphic(btn);
                }
              }
            };
          }
        };

    colAction.setCellFactory(cellFactory);
  }

  // =========================
  // SEARCH
  // =========================

  @FXML
  public void handleSearch(ActionEvent event) {
    String selectedCity = cityComboBox.getValue();
    if (selectedCity != null && !selectedCity.trim().isEmpty()) {
      System.out.println("Bạn đang muốn tìm kiếm tại: " + selectedCity);
    } else {
      System.out.println("Vui lòng chọn một tỉnh/thành phố trước khi tìm kiếm!");
    }
  }

  // =========================
  // LOGOUT
  // =========================

  @FXML
  public void handleLogout(ActionEvent event) {
    switchScene(
        event,
        "/Login.fxml",
        "Hệ thống Đấu giá VNU - Đăng nhập"
    );
  }

  // =========================
  // BIDDER -> MY MANAGEMENT
  // =========================

  @FXML
  public void handleGoToMyManagement(MouseEvent event) {
    try {
      Parent root = FXMLLoader.load(
          getClass().getResource("/MyManagement.fxml")
      );
      Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
      stage.setScene(new Scene(root));
      stage.setTitle("Quản lý của tôi");
      stage.show();
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("Lỗi khi chuyển sang MyManagement.fxml");
    }
  }

  // =========================
  // SELLER -> SELLER DASHBOARD
  // =========================

  @FXML
  public void handleGoToSellerDashboard(MouseEvent event) {
    try {
      Parent root = FXMLLoader.load(
          getClass().getResource("/SellerDashboard.fxml")
      );
      Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
      stage.setScene(new Scene(root));
      stage.setTitle("Kênh người bán - Seller Dashboard");
      stage.show();
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("Lỗi khi chuyển sang SellerDashboard.fxml");
    }
  }

  // =========================
  // USER PROFILE
  // =========================

  @FXML
  public void handleGoToMyProfile(MouseEvent event) {
    try {
      Parent root = FXMLLoader.load(
          getClass().getResource("/UserProfile.fxml")
      );
      Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
      stage.setScene(new Scene(root));
      stage.setTitle("Hồ sơ cá nhân");
      stage.show();
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("Lỗi khi chuyển sang UserProfile.fxml");
    }
  }

  // =========================
  // HÀM CHUYỂN SCENE CHUNG
  // =========================

  private void switchScene(
      ActionEvent event,
      String fxmlPath,
      String title
  ) {
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