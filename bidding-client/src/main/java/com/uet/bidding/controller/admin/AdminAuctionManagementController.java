package com.uet.bidding.controller.admin;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.uet.bidding.model.Auction;
import com.uet.bidding.network.ClientService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Map;

public class AdminAuctionManagementController {

  private static AdminAuctionManagementController instance;

  public static AdminAuctionManagementController getInstance() {
    return instance;
  }

  // --- CÁC THÀNH PHẦN BẢNG DANH SÁCH PHIÊN ---
  @FXML private TableView<Auction> tableAuctions;
  @FXML private TableColumn<Auction, String> colId;
  @FXML private TableColumn<Auction, String> colItemName;
  @FXML private TableColumn<Auction, String> colCurrentPrice;
  @FXML private TableColumn<Auction, String> colStatus;
  @FXML private TableColumn<Auction, String> colEndTime;
  @FXML private TableColumn<Auction, Auction> colAction;

  // --- CÁC THÀNH PHẦN KHUNG XEM TRƯỚC CHI TIẾT BÊN PHẢI ---
  @FXML private VBox detailPane;
  @FXML private Label lblDetailSeller;
  @FXML private Label lblDetailLeader;

  // --- BẢNG LỊCH SỬ ĐẤU GIÁ ---
  @FXML private TableView<BidRecord> tableHistory;
  @FXML private TableColumn<BidRecord, String> colTime;
  @FXML private TableColumn<BidRecord, String> colBidder;
  @FXML private TableColumn<BidRecord, String> colAmount;

  private final ObservableList<Auction> masterData = FXCollections.observableArrayList();
  private final ObservableList<BidRecord> historyData = FXCollections.observableArrayList();
  private final Gson gson = ClientService.getInstance().getGson();

  @FXML
  public void initialize() {
    instance = this;

    // 1. CẤU HÌNH BẢNG CHÍNH
    colId.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().getId())));
    colItemName.setCellValueFactory(cellData -> new SimpleStringProperty(
        cellData.getValue().getItem() != null ? cellData.getValue().getItem().getName() : "Không rõ"
    ));
    colCurrentPrice.setCellValueFactory(cellData -> {
      if (cellData.getValue().getCurrentPrice() != null) {
        return new SimpleStringProperty(String.format("%,.0f đ", cellData.getValue().getCurrentPrice().doubleValue()));
      }
      return new SimpleStringProperty("0 đ");
    });

    colStatus.setCellValueFactory(cellData -> new SimpleStringProperty(
        cellData.getValue().getStatus() != null ? cellData.getValue().getStatus().toString() : ""
    ));

    colStatus.setCellFactory(column -> new TableCell<>() {
      @Override
      protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
          setText(null);
          setStyle("");
        } else {
          setText(item);
          switch (item.toUpperCase()) {
            case "RUNNING" -> setStyle("-fx-text-fill: #2ed573; -fx-font-weight: bold;");
            case "CANCELED" -> setStyle("-fx-text-fill: #ff4757; -fx-font-weight: bold;");
            case "FINISHED", "PAID" -> setStyle("-fx-text-fill: #1b3394; -fx-font-weight: bold;");
            default -> setStyle("-fx-text-fill: #7f8c8d;");
          }
        }
      }
    });

    colEndTime.setCellValueFactory(cellData -> new SimpleStringProperty(
        cellData.getValue().getEndTime() != null ? cellData.getValue().getEndTime().toString() : ""
    ));

    // CỘT THAO TÁC ĐỘNG
    colAction.setCellFactory(param -> new TableCell<>() {
      private final MenuButton btnAction = new MenuButton("Thao tác");
      {
        btnAction.setStyle("-fx-background-color: #ecf0f1; -fx-background-radius: 3; -fx-cursor: hand;");
      }

      @Override
      protected void updateItem(Auction item, boolean empty) {
        super.updateItem(item, empty);
        if (empty) {
          setGraphic(null);
          return;
        }

        Auction auction = getTableView().getItems().get(getIndex());
        if (auction == null) {
          setGraphic(null);
          return;
        }

        btnAction.getItems().clear();
        String status = auction.getStatus() != null ? auction.getStatus().toString().toUpperCase() : "";

        if ("RUNNING".equals(status)) {
          MenuItem menuCancel = new MenuItem("❌ Hủy phiên");
          MenuItem menuDelete = new MenuItem("🗑️ Xóa phiên");

          menuCancel.setOnAction(e -> handleCancelAuctionAction(auction));
          menuDelete.setOnAction(e -> handleDeleteAuctionAction(auction));

          btnAction.getItems().addAll(menuCancel, menuDelete);
          setGraphic(btnAction);
        } else {
          MenuItem menuDelete = new MenuItem("🗑️ Xóa phiên");
          menuDelete.setOnAction(e -> handleDeleteAuctionAction(auction));

          btnAction.getItems().addAll(menuDelete);
          setGraphic(btnAction);
        }
      }
    });

    tableAuctions.setItems(masterData);

    tableAuctions.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
      if (newSelection != null) {
        handleViewHistory(newSelection);
      }
    });

    // 2. CẤU HÌNH BẢNG PHỤ (LỊCH SỬ)
    colTime.setCellValueFactory(new PropertyValueFactory<>("time"));
    colBidder.setCellValueFactory(new PropertyValueFactory<>("bidder"));
    colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
    tableHistory.setItems(historyData);

    // Kéo dữ liệu ban đầu
    loadAuctionsFromServer();
  }

  public void loadAuctionsFromServer() {
    // Gửi request lấy tất cả các phiên (RUNNING, FINISHED, CANCELED) dành riêng cho Admin
    ClientService.getInstance().sendRequest("GET_ALL_AUCTIONS_FOR_ADMIN", "")
        .thenAccept(msg -> Platform.runLater(() -> {
          if ("SUCCESS".equals(msg.getType())) {
            try {
              String json = gson.toJson(msg.getData());
              List<Auction> list = gson.fromJson(json, new TypeToken<List<Auction>>(){}.getType());
              masterData.setAll(list);
            } catch (Exception e) {
              e.printStackTrace();
              showAlert("Lỗi", "Không thể phân tích danh sách phiên đấu giá.", Alert.AlertType.ERROR);
            }
          } else {
            showAlert("Lỗi hệ thống", String.valueOf(msg.getData()), Alert.AlertType.ERROR);
          }
        }));
  }

  private void handleViewHistory(Auction auction) {
    if (detailPane == null) return;

    // Ép qua JSON để đọc động mã người bán (Fix triệt để vụ thiếu hàm getSeller)
    String sellerId = "?";
    try {
      JsonObject jsonAuction = gson.toJsonTree(auction).getAsJsonObject();
      if (jsonAuction.has("sellerId")) sellerId = jsonAuction.get("sellerId").getAsString();
      else if (jsonAuction.has("ownerId")) sellerId = jsonAuction.get("ownerId").getAsString();
      else if (jsonAuction.has("userId")) sellerId = jsonAuction.get("userId").getAsString();
      else if (jsonAuction.has("item")) {
        JsonObject jsonItem = jsonAuction.getAsJsonObject("item");
        if (jsonItem != null) {
          if (jsonItem.has("ownerId")) sellerId = jsonItem.get("ownerId").getAsString();
          else if (jsonItem.has("sellerId")) sellerId = jsonItem.get("sellerId").getAsString();
        }
      }
    } catch (Exception e) {
      System.err.println("Không bóc được mã người bán từ JSON: " + e.getMessage());
    }

    final String finalSellerId = sellerId;

    Platform.runLater(() -> {
      detailPane.setVisible(true);
      lblDetailSeller.setText("👤 Mã Người bán (Seller ID): " + finalSellerId);
      lblDetailLeader.setText("⏳ Đang tải thông tin...");
      historyData.clear();
      tableHistory.setPlaceholder(new Label("Đang kéo lịch sử đấu giá từ máy chủ..."));
    });

    ClientService.getInstance().sendRequest("GET_BID_HISTORY", auction.getId())
        .thenAccept(msg -> Platform.runLater(() -> {
          if ("SUCCESS".equals(msg.getType())) {
            updateDetailHistory(msg.getData());
          } else {
            tableHistory.setPlaceholder(new Label("Lỗi mạng: Không phản hồi từ máy chủ!"));
            lblDetailLeader.setText("👑 Người trả cao nhất: Lỗi kết nối");
          }
        }));
  }

  private void updateDetailHistory(Object bidData) {
    try {
      String json = gson.toJson(bidData);
      com.google.gson.JsonArray bidsArray = gson.fromJson(json, com.google.gson.JsonArray.class);
      historyData.clear();

      if (bidsArray == null || bidsArray.isEmpty()) {
        tableHistory.setPlaceholder(new Label("⚠️ Phiên này chưa có ai tham gia đặt giá."));
        lblDetailLeader.setText("👑 Người trả cao nhất: Chưa có");
        return;
      }

      // Quét từng lượt đặt giá trong mảng
      for (com.google.gson.JsonElement elem : bidsArray) {
        com.google.gson.JsonObject b = elem.getAsJsonObject();

        // 1. Lấy THỜI GIAN (Xử lý trường hợp key là bidTime hoặc time)
        String timeStr = "N/A";
        if (b.has("bidTime") && !b.get("bidTime").isJsonNull()) {
          timeStr = b.get("bidTime").getAsString().replace("T", " ");
        } else if (b.has("time") && !b.get("time").isJsonNull()) {
          timeStr = b.get("time").getAsString().replace("T", " ");
        }

        // 2. Lấy TÊN NGƯỜI ĐẶT (Quét sâu vào object Customer nếu có)
        String bidderName = "Khách ẩn danh";
        if (b.has("customer") && !b.get("customer").isJsonNull()) {
          com.google.gson.JsonObject customerObj = b.getAsJsonObject("customer");
          if (customerObj.has("username") && !customerObj.get("username").isJsonNull()) {
            bidderName = customerObj.get("username").getAsString();
          }
        } else if (b.has("bidderUsername") && !b.get("bidderUsername").isJsonNull()) {
          bidderName = b.get("bidderUsername").getAsString();
        }

        // 3. Lấy SỐ TIỀN
        double amountVal = 0;
        if (b.has("amount") && !b.get("amount").isJsonNull()) {
          amountVal = b.get("amount").getAsDouble();
        }
        String amountStr = String.format("%,.0f đ", amountVal);

        historyData.add(new BidRecord(timeStr, bidderName, amountStr));
      }

      // Cập nhật thẻ "Người dẫn đầu" bên trên
      if (!historyData.isEmpty()) {
        BidRecord topRecord = historyData.get(0);
        lblDetailLeader.setText("👑 Người trả cao nhất: " + topRecord.getBidder() + " (" + topRecord.getAmount() + ")");
      }

    } catch (Exception e) {
      e.printStackTrace();
      tableHistory.setPlaceholder(new Label("Lỗi phân tích dữ liệu lịch sử!"));
      lblDetailLeader.setText("👑 Người trả cao nhất: Lỗi cấu trúc JSON");
    }
  }

  private void handleCancelAuctionAction(Auction auction) {
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Sếp có chắc chắn muốn HỦY phiên #" + auction.getId() + "?", ButtonType.YES, ButtonType.NO);
    alert.setTitle("Xác nhận hủy phiên");
    alert.setHeaderText(null);
    alert.showAndWait().ifPresent(response -> {
      if (response == ButtonType.YES) {
        JsonObject req = new JsonObject();
        req.addProperty("auctionId", auction.getId());
        req.addProperty("status", "CANCELED");

        ClientService.getInstance().sendRequest("ADMIN_UPDATE_AUCTION_STATUS", req)
            .thenAccept(msg -> Platform.runLater(() -> {
              if ("SUCCESS".equals(msg.getType())) {
                showAlert("Thành công", "Đã chuyển trạng thái phiên sang: CANCELED", Alert.AlertType.INFORMATION);
                loadAuctionsFromServer();
              } else {
                showAlert("Thất bại", "Không thể xử lý: " + msg.getData(), Alert.AlertType.ERROR);
              }
            }));
      }
    });
  }

  private void handleDeleteAuctionAction(Auction auction) {
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "CẢNH BÁO: Sếp chắc chắn muốn XÓA hoàn toàn phiên #" + auction.getId() + "?", ButtonType.YES, ButtonType.NO);
    alert.setTitle("Xác nhận xóa phiên");
    alert.setHeaderText(null);
    alert.showAndWait().ifPresent(response -> {
      if (response == ButtonType.YES) {
        ClientService.getInstance().sendRequest("ADMIN_DELETE_AUCTION", auction.getId())
            .thenAccept(msg -> Platform.runLater(() -> {
              if ("SUCCESS".equals(msg.getType())) {
                showAlert("Thành công", "Đã xóa phiên đấu giá thành công!", Alert.AlertType.INFORMATION);
                loadAuctionsFromServer();
              } else {
                showAlert("Thất bại", "Lỗi xóa dữ liệu từ Server: " + msg.getData(), Alert.AlertType.ERROR);
              }
            }));
      }
    });
  }

  private void showAlert(String title, String content, Alert.AlertType type) {
    Alert alert = new Alert(type);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(content);
    alert.showAndWait();
  }

  public static class BidRecord {
    private final SimpleStringProperty time;
    private final SimpleStringProperty bidder;
    private final SimpleStringProperty amount;

    public BidRecord(String time, String bidder, String amount) {
      this.time = new SimpleStringProperty(time);
      this.bidder = new SimpleStringProperty(bidder);
      this.amount = new SimpleStringProperty(amount);
    }

    public String getTime() { return time.get(); }
    public String getBidder() { return bidder.get(); }
    public String getAmount() { return amount.get(); }
  }
}