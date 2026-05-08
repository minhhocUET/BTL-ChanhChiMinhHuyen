package com.uet.bidding.ui;

import com.uet.bidding.network.ClientService;
import com.uet.bidding.util.UserSession;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;

import java.net.URL;
import java.util.ResourceBundle;

public class MyManagementController implements Initializable {

  // --- BẢNG 1: SẢN PHẨM CỦA TÔI (Đã đổi tên khớp với FXML: myItemsTableView) ---
  @FXML private TableView<MyProductItem> myItemsTableView;
  @FXML private TableColumn<MyProductItem, Integer> colItemStt;
  @FXML private TableColumn<MyProductItem, String> colItemName, colItemDesc;
  @FXML private TableColumn<MyProductItem, Double> colItemStartPrice;
  @FXML private TableColumn<MyProductItem, Void> colItemAction;

  // --- BẢNG 2: ĐANG THAM GIA ĐẤU GIÁ ---
  @FXML private TableView<BidItem> bidTableView;
  @FXML private TableColumn<BidItem, Integer> colBidStt;
  @FXML private TableColumn<BidItem, String> colBidName, colBidStatus;
  @FXML private TableColumn<BidItem, Double> colBidCurrentPrice, colBidMyPrice;

  // --- BẢNG 3: SẢN PHẨM ĐANG GIAO BÁN ---
  @FXML private TableView<SellItem> sellTableView;
  @FXML private TableColumn<SellItem, Integer> colSellStt;
  @FXML private TableColumn<SellItem, String> colSellName;
  @FXML private TableColumn<SellItem, Double> colSellStartPrice, colSellHighestBid;
  @FXML private TableColumn<SellItem, Integer> colSellInterested;

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    setupMyProductsTable();
    setupBidTable();
    setupSellTable();

    // Tải dữ liệu từ Server
    refreshData();
  }

  // --- THIẾT LẬP CÁC BẢNG ---

  private void setupMyProductsTable() {
    colItemStt.setCellValueFactory(new PropertyValueFactory<>("stt"));
    colItemName.setCellValueFactory(new PropertyValueFactory<>("name"));
    colItemDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
    formatCurrencyColumn(colItemStartPrice, "startingPrice");
    setupActionColumn();
  }

  private void setupBidTable() {
    colBidStt.setCellValueFactory(new PropertyValueFactory<>("stt"));
    colBidName.setCellValueFactory(new PropertyValueFactory<>("name"));
    colBidStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
    formatCurrencyColumn(colBidCurrentPrice, "currentPrice");
    formatCurrencyColumn(colBidMyPrice, "myPrice");
  }

  private void setupSellTable() {
    colSellStt.setCellValueFactory(new PropertyValueFactory<>("stt"));
    colSellName.setCellValueFactory(new PropertyValueFactory<>("name"));
    colSellInterested.setCellValueFactory(new PropertyValueFactory<>("interested"));
    formatCurrencyColumn(colSellStartPrice, "startPrice");
    formatCurrencyColumn(colSellHighestBid, "highestBid");
  }

  // --- XỬ LÝ SỰ KIỆN (Bổ sung để khớp với FXML) ---

  @FXML
  private void handleAddItem(ActionEvent event) {
    System.out.println("Mở màn hình thêm sản phẩm mới...");
    // Logics: Main.changeScene("/AddItem.fxml", "Thêm sản phẩm", 600, 400);
  }

  @FXML
  private void handleRemoveItem(ActionEvent event) {
    MyProductItem selected = myItemsTableView.getSelectionModel().getSelectedItem();
    if (selected != null) {
      System.out.println("Đang xóa sản phẩm: " + selected.getName());
      // Logics: Gửi yêu cầu xóa lên Server
    } else {
      showWarning("Chú ý", "Vui lòng chọn một sản phẩm trong bảng để xóa!");
    }
  }

  @FXML
  public void handleBack(ActionEvent event) {
    Main.changeScene("/AuctionList.fxml", "Danh sách đấu giá", 1000, 700);
  }

  @FXML
  public void handleGoToMyProfile(MouseEvent event) {
    Main.changeScene("/UserProfile.fxml", "Hồ sơ cá nhân", 800, 600);
  }

  // --- TIỆN ÍCH ---

  private void formatCurrencyColumn(TableColumn column, String propertyName) {
    column.setCellValueFactory(new PropertyValueFactory<>(propertyName));
    column.setCellFactory(tc -> new TableCell<Object, Double>() {
      @Override
      protected void updateItem(Double price, boolean empty) {
        super.updateItem(price, empty);
        if (empty || price == null) setText(null);
        else setText(String.format("%,.0f VNĐ", price));
      }
    });
  }

  private void setupActionColumn() {
    colItemAction.setCellFactory(param -> new TableCell<>() {
      private final Button btnEdit = new Button("Sửa");
      {
        btnEdit.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-cursor: hand;");
        btnEdit.setOnAction(event -> {
          MyProductItem data = getTableView().getItems().get(getIndex());
          System.out.println("Chỉnh sửa: " + data.getName());
        });
      }
      @Override
      protected void updateItem(Void item, boolean empty) {
        super.updateItem(item, empty);
        setGraphic(empty ? null : btnEdit);
      }
    });
  }

  private void refreshData() {
    if (UserSession.getCurrentUser() != null) {
      ClientService.getInstance().sendRequest("GET_MY_MANAGEMENT_DATA", UserSession.getCurrentUser().getId());
    }
  }

  private void showWarning(String title, String content) {
    Alert alert = new Alert(Alert.AlertType.WARNING);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(content);
    alert.showAndWait();
  }

  // --- MODEL CLASSES ---

  public static class MyProductItem {
    private int stt;
    private String name, description;
    private double startingPrice;

    public MyProductItem(int stt, String name, String description, double startingPrice) {
      this.stt = stt; this.name = name; this.description = description; this.startingPrice = startingPrice;
    }
    public int getStt() { return stt; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getStartingPrice() { return startingPrice; }
  }

  public static class BidItem {
    private int stt;
    private String name, status;
    private double currentPrice, myPrice;

    public BidItem(int stt, String name, double currentPrice, double myPrice, String status) {
      this.stt = stt; this.name = name; this.currentPrice = currentPrice; this.myPrice = myPrice; this.status = status;
    }
    public int getStt() { return stt; }
    public String getName() { return name; }
    public double getCurrentPrice() { return currentPrice; }
    public double getMyPrice() { return myPrice; }
    public String getStatus() { return status; }
  }

  public static class SellItem {
    private int stt;
    private String name;
    private double startPrice, highestBid;
    private int interested;

    public SellItem(int stt, String name, double startPrice, double highestBid, int interested) {
      this.stt = stt; this.name = name; this.startPrice = startPrice; this.highestBid = highestBid; this.interested = interested;
    }
    public int getStt() { return stt; }
    public String getName() { return name; }
    public double getStartPrice() { return startPrice; }
    public double getHighestBid() { return highestBid; }
    public int getInterested() { return interested; }
  }
}