package com.uet.bidding.controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.uet.bidding.model.Auction;
import com.uet.bidding.model.Customer;
import com.uet.bidding.model.Item;
import com.uet.bidding.service.BidderService;
import com.uet.bidding.util.ReviewContext;
import com.uet.bidding.util.UserSession;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class MyManagementController {

  private static MyManagementController instance;

  @FXML private TableView<ActiveBidRow> bidTableView;
  @FXML private TableColumn<ActiveBidRow, String> colBidCity;
  @FXML private TableColumn<ActiveBidRow, String> colBidName;
  @FXML private TableColumn<ActiveBidRow, String> colBidCurrentPrice;
  @FXML private TableColumn<ActiveBidRow, String> colBidMyPrice;
  @FXML private TableColumn<ActiveBidRow, String> colBidEndTime;

  @FXML private TableView<HistoryItem> sellTableView;
  @FXML private TableColumn<HistoryItem, String> colSellCity;
  @FXML private TableColumn<HistoryItem, String> colSellName;
  @FXML private TableColumn<HistoryItem, String> colSellStartPrice;
  @FXML private TableColumn<HistoryItem, String> colSellHighestBid;
  @FXML private TableColumn<HistoryItem, Void> colReview;

  private final ObservableList<ActiveBidRow> activeRows = FXCollections.observableArrayList();
  private final ObservableList<HistoryItem> historyRows = FXCollections.observableArrayList();
  private final NumberFormat currencyFormat = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));
  private final DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
  private final BidderService bidderService = new BidderService();

  public static MyManagementController getInstance() {
    return instance;
  }

  @FXML
  public void initialize() {
    instance = this;
    setupActiveTable();
    setupHistoryTable();
    reloadActiveFromServer();
    reloadHistoryFromServer();
  }

  private void setupActiveTable() {
    colBidCity.setCellValueFactory(new PropertyValueFactory<>("city"));
    colBidName.setCellValueFactory(new PropertyValueFactory<>("name"));
    colBidCurrentPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
    colBidMyPrice.setCellValueFactory(new PropertyValueFactory<>("myPrice"));
    colBidEndTime.setCellValueFactory(new PropertyValueFactory<>("endTime"));
    bidTableView.setItems(activeRows);
    bidTableView.setRowFactory(tv -> {
      TableRow<ActiveBidRow> row = new TableRow<>();
      row.setOnMouseClicked(e -> {
        if (!row.isEmpty() && e.getClickCount() >= 2 && row.getItem().getAuction() != null) {
          openProductDetail(row.getItem().getAuction());
        }
      });
      return row;
    });
  }

  private void setupHistoryTable() {
    colSellCity.setCellValueFactory(new PropertyValueFactory<>("city"));
    colSellName.setCellValueFactory(new PropertyValueFactory<>("name"));
    colSellStartPrice.setCellValueFactory(new PropertyValueFactory<>("startPrice"));
    colSellHighestBid.setCellValueFactory(new PropertyValueFactory<>("highestBid"));
    addReviewButtonToTable();
    sellTableView.setItems(historyRows);
  }

  public void applyAuctionUpdate(Auction updated) {
    if (updated == null) return;
    Platform.runLater(() -> {
      if ("FINISHED".equals(updated.getStatus())) {
        activeRows.removeIf(r -> r.getAuctionId() == updated.getId());
        reloadHistoryFromServer();
      } else if ("RUNNING".equals(updated.getStatus())) {
        boolean inList = activeRows.stream().anyMatch(r -> r.getAuctionId() == updated.getId());
        if (inList) {
          reloadActiveFromServer();
        } else {
          Customer c = UserSession.getLoggedInCustomer();
          if (c != null && c.getBidderProfile() != null
              && c.getBidderProfile().getRegisteredAuctionIds().contains(updated.getId())) {
            reloadActiveFromServer();
          }
        }
      }
      if (AuctionListController.getInstance() != null) {
        AuctionListController.getInstance().handleAuctionBroadcast(updated);
      }
    });
  }

  public void reloadAfterRegistration() {
    reloadActiveFromServer();
  }

  private void reloadActiveFromServer() {
    bidderService.loadActiveAuctions().thenAccept(res -> Platform.runLater(() -> {
      if (!"SUCCESS".equals(res.getType())) return;
      Gson gson = com.uet.bidding.network.ClientService.getInstance().getGson();
      JsonArray arr = gson.toJsonTree(res.getData()).getAsJsonArray();
      ObservableList<ActiveBidRow> rows = FXCollections.observableArrayList();
      for (JsonElement el : arr) {
        JsonObject obj = el.getAsJsonObject();
        Auction auction = gson.fromJson(obj.get("auction"), Auction.class);
        BigDecimal myBid = null;
        if (obj.has("myHighestBid") && !obj.get("myHighestBid").isJsonNull()) {
          myBid = gson.fromJson(obj.get("myHighestBid"), BigDecimal.class);
        }
        rows.add(toActiveRow(auction, myBid));
      }
      activeRows.setAll(rows);
    }));
  }

  private void reloadHistoryFromServer() {
    bidderService.loadBidderHistory().thenAccept(res -> Platform.runLater(() -> {
      if (!"SUCCESS".equals(res.getType())) return;
      Gson gson = com.uet.bidding.network.ClientService.getInstance().getGson();
      JsonArray arr = gson.toJsonTree(res.getData()).getAsJsonArray();
      ObservableList<HistoryItem> rows = FXCollections.observableArrayList();
      for (JsonElement el : arr) {
        JsonObject obj = el.getAsJsonObject();
        Auction auction = gson.fromJson(obj.get("auction"), Auction.class);
        boolean reviewed = obj.has("reviewed") && obj.get("reviewed").getAsBoolean();
        rows.add(toHistoryItem(auction, reviewed));
      }
      historyRows.setAll(rows);
    }));
  }

  private ActiveBidRow toActiveRow(Auction auction, BigDecimal myBid) {
    if (auction == null) {
      return new ActiveBidRow(0, null, "-", "-", "-", "Chưa đặt", "-");
    }
    Item item = auction.getItem();
    String city = (item != null && item.getCity() != null) ? item.getCity() : "-";
    String name = (item != null) ? item.getName() : "-";
    String current = auction != null
        ? currencyFormat.format(auction.getCurrentPrice()) + " VNĐ" : "-";
    String myPrice = myBid != null ? currencyFormat.format(myBid) + " VNĐ" : "Chưa đặt";
    String end = (auction != null && auction.getEndTime() != null)
        ? auction.getEndTime().format(dateTimeFormat) : "-";
    return new ActiveBidRow(auction.getId(), auction, city, name, current, myPrice, end);
  }

  private HistoryItem toHistoryItem(Auction auction, boolean reviewed) {
    Item item = auction.getItem();
    String city = (item != null && item.getCity() != null) ? item.getCity() : "-";
    String name = (item != null) ? item.getName() : "-";
    BigDecimal start = item != null ? item.getStartingPrice() : auction.getCurrentPrice();
    String startStr = currencyFormat.format(start) + " VNĐ";
    String highest = currencyFormat.format(auction.getCurrentPrice()) + " VNĐ";
    int sellerId = item != null ? item.getSellerId() : 0;
    String storeName = "Shop #" + sellerId;
    Customer winner = auction.getHighestBidder();
    boolean canReview = winner != null
        && UserSession.getLoggedInCustomer() != null
        && winner.getId() == UserSession.getLoggedInCustomer().getId();
    return new HistoryItem(
        auction.getId(), sellerId, storeName, city, name, startStr, highest, reviewed, canReview);
  }

  private void addReviewButtonToTable() {
    Callback<TableColumn<HistoryItem, Void>, TableCell<HistoryItem, Void>> cellFactory =
        param -> new TableCell<>() {
          private final Button btn = new Button("⭐ Đánh giá");

          {
            btn.setStyle(
                "-fx-background-color: #e91e63; -fx-text-fill: white; -fx-font-weight: bold;"
                    + "-fx-cursor: hand; -fx-background-radius: 5;");
            btn.setOnAction(event -> {
              HistoryItem data = getTableView().getItems().get(getIndex());
              if (data == null || data.isReviewed() || !data.isCanReview()) return;
              ReviewContext.set(data.getAuctionId(), data.getSellerId(), data.getStoreName(), true);
              handleGoToReview(event);
            });
          }

          @Override
          protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
              setGraphic(null);
            } else {
              HistoryItem data = getTableView().getItems().get(getIndex());
              if (data == null) {
                setGraphic(null);
                return;
              }
              if (data.isReviewed()) {
                btn.setText("Đã đánh giá");
                btn.setDisable(true);
                btn.setStyle(
                    "-fx-background-color: #94a3b8; -fx-text-fill: white; -fx-font-weight: bold;"
                        + "-fx-background-radius: 5;");
              } else if (!data.isCanReview()) {
                btn.setText("—");
                btn.setDisable(true);
                btn.setStyle(
                    "-fx-background-color: #e2e8f0; -fx-text-fill: #64748b; -fx-font-weight: bold;"
                        + "-fx-background-radius: 5;");
              } else {
                btn.setText("⭐ Đánh giá");
                btn.setDisable(false);
                btn.setStyle(
                    "-fx-background-color: #e91e63; -fx-text-fill: white; -fx-font-weight: bold;"
                        + "-fx-cursor: hand; -fx-background-radius: 5;");
              }
              setGraphic(btn);
            }
          }
        };
    colReview.setCellFactory(cellFactory);
  }

  private void openProductDetail(Auction auction) {
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/ProductDetail.fxml"));
      Parent root = loader.load();
      ProductDetailController ctrl = loader.getController();
      ctrl.setAuctionData(auction);
      Stage stage = (Stage) bidTableView.getScene().getWindow();
      stage.setScene(new Scene(root));
      stage.setTitle("Chi tiết - " + (auction.getItem() != null ? auction.getItem().getName() : ""));
      stage.show();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void handleGoToReview(ActionEvent event) {
    try {
      Parent root = FXMLLoader.load(getClass().getResource("/Review.fxml"));
      Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
      stage.setScene(new Scene(root));
      stage.setTitle("Đánh giá Shop");
      stage.show();
    } catch (IOException e) {
      e.printStackTrace();
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
    try {
      Parent root = FXMLLoader.load(getClass().getResource("/UserProfile.fxml"));
      Stage stage = (Stage) bidTableView.getScene().getWindow();
      stage.setScene(new Scene(root));
      stage.setTitle("Hồ sơ cá nhân");
      stage.show();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static class ActiveBidRow {
    private final int auctionId;
    private final Auction auction;
    private final String city;
    private final String name;
    private final String currentPrice;
    private final String myPrice;
    private final String endTime;

    public ActiveBidRow(int auctionId, Auction auction, String city, String name,
                        String currentPrice, String myPrice, String endTime) {
      this.auctionId = auctionId;
      this.auction = auction;
      this.city = city;
      this.name = name;
      this.currentPrice = currentPrice;
      this.myPrice = myPrice;
      this.endTime = endTime;
    }

    public int getAuctionId() {
      return auctionId;
    }

    public Auction getAuction() {
      return auction;
    }

    public String getCity() {
      return city;
    }

    public String getName() {
      return name;
    }

    public String getCurrentPrice() {
      return currentPrice;
    }

    public String getMyPrice() {
      return myPrice;
    }

    public String getEndTime() {
      return endTime;
    }
  }

  public static class HistoryItem {
    private final int auctionId;
    private final int sellerId;
    private final String storeName;
    private final String city;
    private final String name;
    private final String startPrice;
    private final String highestBid;
    private final boolean reviewed;
    private final boolean canReview;

    public HistoryItem(int auctionId, int sellerId, String storeName, String city, String name,
                       String startPrice, String highestBid, boolean reviewed, boolean canReview) {
      this.auctionId = auctionId;
      this.sellerId = sellerId;
      this.storeName = storeName;
      this.city = city;
      this.name = name;
      this.startPrice = startPrice;
      this.highestBid = highestBid;
      this.reviewed = reviewed;
      this.canReview = canReview;
    }

    public int getAuctionId() {
      return auctionId;
    }

    public int getSellerId() {
      return sellerId;
    }

    public String getStoreName() {
      return storeName;
    }

    public String getCity() {
      return city;
    }

    public String getName() {
      return name;
    }

    public String getStartPrice() {
      return startPrice;
    }

    public String getHighestBid() {
      return highestBid;
    }

    public boolean isReviewed() {
      return reviewed;
    }

    public boolean isCanReview() {
      return canReview;
    }
  }
}
