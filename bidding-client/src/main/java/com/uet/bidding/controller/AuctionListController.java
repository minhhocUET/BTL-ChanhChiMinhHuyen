package com.uet.bidding.controller;

import com.uet.bidding.model.Auction;
import com.uet.bidding.model.Customer;
import com.uet.bidding.model.Item;
import com.uet.bidding.network.ClientService;
import com.uet.bidding.util.SellerAuctionContext;
import com.uet.bidding.util.UserSession;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
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

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class AuctionListController implements Initializable {

  private static AuctionListController instance;

  @FXML private TableColumn<Auction, Void> colAction;
  @FXML private TableView<Auction> tableView;
  @FXML private TableColumn<Auction, String> colCity;
  @FXML private ComboBox<String> cityComboBox;
  @FXML private TableColumn<Auction, String> colItemType;
  @FXML private TableColumn<Auction, String> colProductName;
  @FXML private TableColumn<Auction, Integer> colRegistered;

  private List<Auction> preLoadedAuctions = null;

  public static AuctionListController getInstance() {
    return instance;
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    instance = this;
    tableView.setPlaceholder(new Label("Loading..."));

    colCity.setCellValueFactory(cd -> {
      Item item = cd.getValue().getItem();
      String city = (item != null && item.getCity() != null) ? item.getCity() : "-";
      return new SimpleStringProperty(city);
    });

    colItemType.setCellValueFactory(cd -> {
      Item item = cd.getValue().getItem();
      String type = (item != null) ? item.getType() : "-";
      return new SimpleStringProperty(type);
    });

    colProductName.setCellValueFactory(cd -> {
      Item item = cd.getValue().getItem();
      String name = (item != null) ? item.getName() : "-";
      return new SimpleStringProperty(name);
    });

    colRegistered.setCellValueFactory(cd ->
        new SimpleObjectProperty<>(cd.getValue().getRegisteredCount()));

    setupActionColumn();
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

    colProductName.prefWidthProperty().bind(
        tableView.widthProperty()
            .subtract(colCity.widthProperty())
            .subtract(colItemType.widthProperty())
            .subtract(colRegistered.widthProperty())
            .subtract(colAction.widthProperty())
            .subtract(2)
    );
  }

  private void setupActionColumn() {
    Callback<TableColumn<Auction, Void>, TableCell<Auction, Void>> cellFactory = new Callback<>() {
      @Override
      public TableCell<Auction, Void> call(final TableColumn<Auction, Void> param) {
        return new TableCell<>() {
          private final Button btn = new Button("Đăng kí tham gia");

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
              Customer currentUser = UserSession.getLoggedInCustomer();
              if (currentUser != null) {
                if (currentUser.getFullName() == null || currentUser.getFullName().trim().isEmpty()) {
                  Alert alert = new Alert(Alert.AlertType.WARNING);
                  alert.setTitle("Yêu cầu cập nhật");
                  alert.setHeaderText(null);
                  alert.setContentText("Vui lòng hoàn thiện TẤT CẢ thông tin để có thể tham gia đấu giá hoặc đăng bán.");
                  alert.showAndWait();
                  return;
                }
              }

              Auction selectedAuction = getTableView().getItems().get(getIndex());
              openAuctionDetail(selectedAuction, (Stage) ((Node) event.getSource()).getScene().getWindow());
            });
          }

          @Override
          protected void updateItem(Void item, boolean empty) {
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

  @FXML
  public void handleGoToMyManagement(MouseEvent event) {
    try {
      Parent root = FXMLLoader.load(getClass().getResource("/MyManagement.fxml"));
      Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
      stage.setScene(new Scene(root));
      stage.setTitle("Quản lý của tôi");
      stage.show();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @FXML
  public void handleGoToSellerDashboard(MouseEvent event) {
    try {
      Parent root = FXMLLoader.load(getClass().getResource("/SellerDashboard.fxml"));
      Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
      stage.setScene(new Scene(root));
      stage.setTitle("Kênh người bán - Seller Dashboard");
      stage.show();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

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
    }
  }

  private void switchScene(ActionEvent event, String fxmlPath, String title) {
    try {
      Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
      Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
      stage.setScene(new Scene(root));
      stage.setTitle(title);
      stage.show();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void openProductDetail(Auction auction, javafx.scene.Scene scene) {
    if (auction == null || scene == null) return;
    openAuctionDetail(auction, (Stage) scene.getWindow());
  }

  private boolean isOwnAuction(Auction auction) {
    Customer currentUser = UserSession.getLoggedInCustomer();
    if (currentUser == null || auction == null || auction.getItem() == null) {
      return false;
    }
    return currentUser.getId() == auction.getItem().getSellerId();
  }

  private void openAuctionDetail(Auction auction, Stage stage) {
    if (auction == null || stage == null) return;
    Customer currentUser = UserSession.getLoggedInCustomer();
    if (currentUser != null && (currentUser.getFullName() == null || currentUser.getFullName().trim().isEmpty())) {
      Alert alert = new Alert(Alert.AlertType.WARNING);
      alert.setContentText("Hoàn thiện hồ sơ trong Setting trước khi xem chi tiết.");
      alert.showAndWait();
      return;
    }
    try {
      String itemName = auction.getItem() != null ? auction.getItem().getName() : ("#" + auction.getId());
      Parent root;
      if (isOwnAuction(auction)) {
        SellerAuctionContext.set(auction);
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/SellerProductDetail.fxml"));
        root = loader.load();
        SellerProductDetailController sellerCtrl = loader.getController();
        sellerCtrl.setAuctionData(auction);
        stage.setTitle("Quản lý phiên - " + itemName);
      } else {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ProductDetail.fxml"));
        root = loader.load();
        ProductDetailController detailController = loader.getController();
        detailController.setAuctionData(auction);
        stage.setTitle("Chi tiết - " + itemName);
      }
      stage.setScene(new Scene(root));
      stage.show();
    } catch (Exception e) {
      e.printStackTrace();
      Alert alert = new Alert(Alert.AlertType.ERROR);
      alert.setTitle("Lỗi");
      alert.setHeaderText("Không thể mở trang chi tiết");
      alert.setContentText("Chi tiết lỗi: " + e.getMessage());
      alert.showAndWait();
    }
  }

  public void setPreLoadedAuctions(List<Auction> auctions) {
    this.preLoadedAuctions = auctions;
  }

  private void loadAuctionsFromServer() {
    if (preLoadedAuctions != null) {
      tableView.setItems(FXCollections.observableArrayList(preLoadedAuctions));
      preLoadedAuctions = null;
      return;
    }

    ClientService.getInstance().sendRequest("GET_ALL_AUCTIONS", "")
        .thenAccept(response -> Platform.runLater(() -> {
          if ("SUCCESS".equals(response.getType())) {
            String json = ClientService.getInstance().getGson().toJson(response.getData());
            List<Auction> list = ClientService.getInstance().getGson()
                .fromJson(json, new com.google.gson.reflect.TypeToken<List<Auction>>(){}.getType());

            if (list != null) {
              tableView.setItems(FXCollections.observableArrayList(list));
            }
          }
        }));
  }

  // =========================
  // XỬ LÝ CẬP NHẬT TRẠNG THÁI AUCTION CHUNG
  // =========================

  public void refreshData() {
    loadAuctionsFromServer();
  }

  public void handleAuctionBroadcast(Auction updated) {
    if (tableView == null || updated == null) return;
    refreshOneAuction(updated);
  }

  public void refreshOneAuction(Auction updated) {
    if (tableView == null || updated == null) return;

    if ("FINISHED".equals(updated.getStatus())) {
      tableView.getItems().removeIf(a -> a.getId() == updated.getId());
      return;
    }

    for (int i = 0; i < tableView.getItems().size(); i++) {
      if (tableView.getItems().get(i).getId() == updated.getId()) {
        tableView.getItems().set(i, updated);
        return;
      }
    }
  }

  public void addOrRefreshAuction(Auction auction) {
    if (tableView == null || auction == null) return;

    if (!"RUNNING".equals(auction.getStatus())) {
      return;
    }

    for (int i = 0; i < tableView.getItems().size(); i++) {
      if (tableView.getItems().get(i).getId() == auction.getId()) {
        tableView.getItems().set(i, auction);
        return;
      }
    }
    tableView.getItems().add(0, auction);
  }
}