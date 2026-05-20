package com.uet.bidding.controller;

import com.uet.bidding.model.Auction;
import com.uet.bidding.model.Customer;
import com.uet.bidding.model.Electronics;
import com.uet.bidding.model.Item;
import com.uet.bidding.network.ClientService;
import com.uet.bidding.util.UserSession;
import javafx.application.Platform;
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
import java.util.List;
import java.util.Random;
import java.util.ResourceBundle;

public class AuctionListController implements Initializable {

  @FXML
  private TableView<Auction> tableView;

  @FXML
  private TableColumn<Auction, String> colCity;

  @FXML
  private ComboBox<String> cityComboBox;
  @FXML private TableColumn<Auction, String> colItemType;
  @FXML private TableColumn<Auction, String> colProductName;
  @FXML private TableColumn<Auction, Integer> colRegistered;


  @Override
  public void initialize(URL location, ResourceBundle resources) {
    instance = this;
    colCity.setCellValueFactory(cd -> {
      Item item = cd.getValue().getItem();
      String city = (item != null && item.getCity() != null) ? item.getCity() : "-";
      return new SimpleStringProperty(city);
    });

    colItemType.setCellValueFactory(cd -> {
      Item item = cd.getValue().getItem();
      String type = (item != null) ? item.getType() : "-"; // ELECTRONICS, ART, VEHICLE
      return new SimpleStringProperty(type);
    });

    colProductName.setCellValueFactory(cd -> {
      Item item = cd.getValue().getItem();
      String name = (item != null) ? item.getName() : "-";
      return new SimpleStringProperty(name);
    });

    colRegistered.setCellValueFactory(cd ->
            new SimpleObjectProperty<>(cd.getValue().getRegisteredCount()));

    loadAuctionsFromServer();

    tableView.setRowFactory(tv -> {
      TableRow<Auction> row = new TableRow<>();
      row.setOnMouseClicked(e -> {
        if (!row.isEmpty() && e.getClickCount() >= 2) {
          openProductDetail(row.getItem(), row.getScene());
        }
      });
      return row;
    });

    cityComboBox.setItems(FXCollections.observableArrayList(
    // Danh sách tỉnh thành
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
        ));
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

                // 1. CHÈN LOGIC KIỂM TRA HỒ SƠ TẠI ĐÂY
                  Customer currentUser = UserSession.getLoggedInCustomer();
                  if (currentUser != null) {
                    // Nếu chưa hoàn thiện họ tên (hồ sơ trống)
                    if (currentUser.getFullName() == null || currentUser.getFullName().trim().isEmpty()) {

                      Alert alert = new Alert(Alert.AlertType.WARNING);
                      alert.setTitle("Yêu cầu cập nhật");
                      alert.setHeaderText(null);
                      alert.setContentText("Vui lòng hoàn thiện TẤT CẢ thông tin để có thể tham gia đấu giá hoặc đăng bán.");
                      alert.showAndWait();

                      return; // Dừng lại luôn, không cho mở trang chi tiết sản phẩm!
                    }
                  }

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
  private static AuctionListController instance;

  public static AuctionListController getInstance() {
    return instance;
  }


  public void refreshOneAuction(Auction updated) {
    if (tableView == null || updated == null) return;
    for (int i = 0; i < tableView.getItems().size(); i++) {
      if (tableView.getItems().get(i).getId() == updated.getId()) {
        tableView.getItems().set(i, updated);
        return;
      }
    }
  }

  public void addOrRefreshAuction(Auction auction) {
    if (tableView == null || auction == null) return;
    for (int i = 0; i < tableView.getItems().size(); i++) {
      if (tableView.getItems().get(i).getId() == auction.getId()) {
        tableView.getItems().set(i, auction);
        return;
      }
    }
    tableView.getItems().add(0, auction);
  }

  private void openProductDetail(Auction auction, javafx.scene.Scene scene) {
    if (auction == null || scene == null) return;
    Customer currentUser = UserSession.getLoggedInCustomer();
    if (currentUser != null && (currentUser.getFullName() == null || currentUser.getFullName().trim().isEmpty())) {
      Alert alert = new Alert(Alert.AlertType.WARNING);
      alert.setContentText("Hoàn thiện hồ sơ trong Setting trước khi xem chi tiết.");
      alert.showAndWait();
      return;
    }
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/ProductDetail.fxml"));
      Parent root = loader.load();
      ProductDetailController detailController = loader.getController();
      detailController.setAuctionData(auction);
      Stage stage = (Stage) scene.getWindow();
      stage.setScene(new Scene(root));
      stage.setTitle("Chi tiết - " + auction.getItem().getName());
      stage.show();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void loadAuctionsFromServer() {
    ClientService.getInstance().sendRequest("GET_ALL_AUCTIONS", "")
            .thenAccept(response -> Platform.runLater(() -> {
              if ("SUCCESS".equals(response.getType())) {
                String json = ClientService.getInstance().getGson().toJson(response.getData());
                List<Auction> list = ClientService.getInstance().getGson()
                        .fromJson(json, new com.google.gson.reflect.TypeToken<List<Auction>>(){}.getType());
                tableView.setItems(FXCollections.observableArrayList(list));
              }
            }));
  }
}