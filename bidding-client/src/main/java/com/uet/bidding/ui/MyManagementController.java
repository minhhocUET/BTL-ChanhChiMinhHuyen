package com.uet.bidding.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class MyManagementController implements Initializable {

  // --- BẢNG ĐANG THAM GIA ĐẤU GIÁ ---
  @FXML private TableView<BidItem> bidTableView;
  @FXML private TableColumn<BidItem, Integer> colBidStt;
  @FXML private TableColumn<BidItem, String> colBidName;
  @FXML private TableColumn<BidItem, String> colBidCurrentPrice;
  @FXML private TableColumn<BidItem, String> colBidMyPrice;
  @FXML private TableColumn<BidItem, String> colBidStatus;

  // --- BẢNG ĐANG GIAO BÁN ---
  @FXML private TableView<SellItem> sellTableView;
  @FXML private TableColumn<SellItem, Integer> colSellStt;
  @FXML private TableColumn<SellItem, String> colSellName;
  @FXML private TableColumn<SellItem, String> colSellStartPrice;
  @FXML private TableColumn<SellItem, String> colSellHighestBid;
  @FXML private TableColumn<SellItem, Integer> colSellInterested;

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    setupBidTable();
    setupSellTable();
  }

  private void setupBidTable() {
    colBidStt.setCellValueFactory(new PropertyValueFactory<>("stt"));
    colBidName.setCellValueFactory(new PropertyValueFactory<>("name"));
    colBidCurrentPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
    colBidMyPrice.setCellValueFactory(new PropertyValueFactory<>("myPrice"));
    colBidStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

    ObservableList<BidItem> data = FXCollections.observableArrayList(
        new BidItem(1, "Laptop Dell XPS 15", "15.000.000 VNĐ", "14.500.000 VNĐ", "Đang bị vượt giá"),
        new BidItem(2, "Đồng hồ Apple Watch S9", "8.500.000 VNĐ", "8.500.000 VNĐ", "Đang dẫn đầu")
    );
    bidTableView.setItems(data);
  }

  private void setupSellTable() {
    colSellStt.setCellValueFactory(new PropertyValueFactory<>("stt"));
    colSellName.setCellValueFactory(new PropertyValueFactory<>("name"));
    colSellStartPrice.setCellValueFactory(new PropertyValueFactory<>("startPrice"));
    colSellHighestBid.setCellValueFactory(new PropertyValueFactory<>("highestBid"));
    colSellInterested.setCellValueFactory(new PropertyValueFactory<>("interested"));

    ObservableList<SellItem> data = FXCollections.observableArrayList(
        new SellItem(1, "Bàn phím cơ Logitech", "1.200.000 VNĐ", "1.500.000 VNĐ", 12),
        new SellItem(2, "Màn hình LG 24 inch", "2.000.000 VNĐ", "Chưa có người trả", 5)
    );
    sellTableView.setItems(data);
  }

  // ==========================================
  // CÁC HÀM CHUYỂN TRANG
  // ==========================================

  @FXML
  public void handleBack(ActionEvent event) {
    switchScene(event, "/AuctionList.fxml", "Danh sách đấu giá");
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

  // ==========================================
  // CÁC LỚP DỮ LIỆU NỘI BỘ (Chỉ dùng cho UI này)
  // ==========================================

  public static class BidItem {
    private int stt;
    private String name;
    private String currentPrice;
    private String myPrice;
    private String status;

    public BidItem(int stt, String name, String currentPrice, String myPrice, String status) {
      this.stt = stt; this.name = name; this.currentPrice = currentPrice;
      this.myPrice = myPrice; this.status = status;
    }

    public int getStt() { return stt; }
    public String getName() { return name; }
    public String getCurrentPrice() { return currentPrice; }
    public String getMyPrice() { return myPrice; }
    public String getStatus() { return status; }
  }

  public static class SellItem {
    private int stt;
    private String name;
    private String startPrice;
    private String highestBid;
    private int interested;

    public SellItem(int stt, String name, String startPrice, String highestBid, int interested) {
      this.stt = stt; this.name = name; this.startPrice = startPrice;
      this.highestBid = highestBid; this.interested = interested;
    }

    public int getStt() { return stt; }
    public String getName() { return name; }
    public String getStartPrice() { return startPrice; }
    public String getHighestBid() { return highestBid; }
    public int getInterested() { return interested; }
  }
}
