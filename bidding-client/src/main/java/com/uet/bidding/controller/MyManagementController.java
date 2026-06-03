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
import javafx.geometry.Pos;
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

    // 🌟 CĂN GIỮA TEXT CHO BẢNG ĐANG THAM GIA ĐẤU GIÁ
    centerColumn(colBidCity);
    centerColumn(colBidName);
    centerColumn(colBidCurrentPrice);
    centerColumn(colBidMyPrice);
    centerColumn(colBidEndTime);

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

    // 🌟 CĂN GIỮA TEXT CHO BẢNG LỊCH SỬ MUA HÀNG
    centerColumn(colSellCity);
    centerColumn(colSellName);
    centerColumn(colSellStartPrice);
    centerColumn(colSellHighestBid);

    addReviewButtonToTable();

    sellTableView.setItems(historyRows);

    // 🌟 🚀 BỔ SUNG: BẮT SỰ KIỆN DOUBLE CLICK VÀO DÒNG LỊCH SỬ ĐỂ XEM ĐÁNH GIÁ
    sellTableView.setRowFactory(tv -> {
      TableRow<HistoryItem> row = new TableRow<>();
      row.setOnMouseClicked(e -> {
        if (!row.isEmpty() && e.getClickCount() >= 2) {
          HistoryItem data = row.getItem();
          if (data != null) {
            // Nếu đã đánh giá: canReview = false (chỉ xem). Nếu chưa đánh giá: canReview = true (được viết)
            boolean activeFormInput = !data.isReviewed();

            // Đóng gói thông tin nạp vào Context trung gian
            ReviewContext.set(data.getAuctionId(), data.getSellerId(), data.getStoreName(), activeFormInput);

            // Chuyển scene mượt mà sang màn hình Review.fxml
            try {
              Parent root = FXMLLoader.load(getClass().getResource("/Review.fxml"));
              Stage stage = (Stage) sellTableView.getScene().getWindow();
              stage.setScene(new Scene(root));
              stage.setTitle("Xem Đánh giá Shop - " + data.getStoreName());
              stage.show();
              System.out.println(">>> [Double-click] Đã chuyển hướng sang xem đánh giá của phiên: " + data.getAuctionId());
            } catch (IOException ex) {
              ex.printStackTrace();
              System.err.println("❌ Lỗi tải giao diện Review khi Double-click!");
            }
          }
        }
      });
      return row;
    });
  }

  /**
   * Hàm Helper tiện ích dùng để đẩy text của các cột thường ra chính giữa dòng
   */
  private <T> void centerColumn(TableColumn<T, String> column) {
    column.setCellFactory(tc -> new TableCell<>() {
      @Override
      protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
          setText(null);
        } else {
          setText(item);
          setAlignment(Pos.CENTER); // Thực hiện căn giữa nội dung chữ
        }
      }
    });
  }

  public void applyAuctionUpdate(Auction updated) {
    if (updated == null) return;
    Platform.runLater(() -> {

      // Kiểm tra trạng thái trực tiếp bằng String getStatus()
      boolean isFinished = "FINISHED".equals(updated.getStatus());
      boolean isRunning = "RUNNING".equals(updated.getStatus());

      // Xử lý logic hiển thị
      if (isFinished) {
        // Gỡ khỏi danh sách Active và tải lại danh sách Lịch sử
        activeRows.removeIf(r -> r.getAuctionId() == updated.getId());
        reloadHistoryFromServer();
      } else if (isRunning) {
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

      // Chuyển tiếp tín hiệu sang Sảnh chính
      if (AuctionListController.getInstance() != null) {
        AuctionListController.getInstance().handleAuctionBroadcast(updated);
      }
    });
  }

  public void reloadAfterRegistration() {
    reloadActiveFromServer();
  }

  private void reloadActiveFromServer() {
    // 1. Hiển thị chữ "Đang tải dữ liệu..." lên giao diện TRƯỚC KHI gọi Server
    Platform.runLater(() -> {
      Label loadingLabel = new Label("Loading...");
      loadingLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 14; -fx-font-style: italic;");
      bidTableView.setPlaceholder(loadingLabel);
    });

    bidderService.loadActiveAuctions().thenAccept(res -> {
      if (!"SUCCESS".equals(res.getType())) return;

      // 2. 🌟 XỬ LÝ NẶNG (Parse JSON) Ở LUỒNG NỀN (Ngoài Platform.runLater)
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

      // 3. 🌟 CHỈ ĐẨY LÊN GIAO DIỆN KHI ĐÃ CHUẨN BỊ XONG DỮ LIỆU
      Platform.runLater(() -> {
        activeRows.setAll(rows);

        // Nếu không có dữ liệu, đổi chữ Loading thành Không có dữ liệu
        if (rows.isEmpty()) {
          Label emptyLabel = new Label("Không có phiên đấu giá nào.");
          emptyLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-style: italic;");
          bidTableView.setPlaceholder(emptyLabel);
        }
      });
    }).exceptionally(ex -> {
      ex.printStackTrace();
      Platform.runLater(() -> {
        Label errorLabel = new Label("Lỗi khi tải dữ liệu.");
        errorLabel.setStyle("-fx-text-fill: red;");
        bidTableView.setPlaceholder(errorLabel);
      });
      return null;
    });
  }

  private void reloadHistoryFromServer() {
    // 1. Hiển thị chữ "Loading..." lên bảng Lịch sử trước khi gửi Request
    Platform.runLater(() -> {
      Label loadingLabel = new Label("Loading...");
      loadingLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 14; -fx-font-style: italic;");
      sellTableView.setPlaceholder(loadingLabel);
    });

    bidderService.loadBidderHistory().thenAccept(res -> {
      if (res == null || !"SUCCESS".equals(res.getType())) {
        System.err.println("❌ Server phản hồi thất bại hoặc gói tin bị rỗng!");
        return;
      }

      Gson gson = com.uet.bidding.network.ClientService.getInstance().getGson();
      JsonElement dataTree = gson.toJsonTree(res.getData());
      JsonArray arr = null;

      // 🌟 GIẢI PHÁP AN TOÀN: Tự động nhận diện cấu trúc JSON từ Server trả về
      if (dataTree.isJsonArray()) {
        // Trường hợp 1: Server trả về mảng List thuần túy trực tiếp
        arr = dataTree.getAsJsonArray();
      } else if (dataTree.isJsonObject()) {
        // Trường hợp 2: Server trả về Object bọc ngoài (chứa key "reviews" hoặc "history")
        JsonObject dataObj = dataTree.getAsJsonObject();
        if (dataObj.has("history")) {
          arr = dataObj.getAsJsonArray("history");
        } else if (dataObj.has("reviews")) {
          arr = dataObj.getAsJsonArray("reviews");
        } else {
          // Nếu không trúng key nào, lấy thử Array đầu tiên tìm thấy trong Object
          for (java.util.Map.Entry<String, JsonElement> entry : dataObj.entrySet()) {
            if (entry.getValue().isJsonArray()) {
              arr = entry.getValue().getAsJsonArray();
              break;
            }
          }
        }
      }

      // Nếu sau khi kiểm tra động mà vẫn không tìm ra mảng dữ liệu nào
      if (arr == null) {
        System.err.println("⚠️ Định dạng dữ liệu từ Server không tương thích với Client: " + dataTree);
        arr = new JsonArray(); // Gán mảng rỗng để không bị lỗi NullPointerException bên dưới
      }

      ObservableList<HistoryItem> rows = FXCollections.observableArrayList();

      for (JsonElement el : arr) {
        try {
          JsonObject obj = el.getAsJsonObject();

          // Kiểm tra tính hợp lệ của thuộc tính "auction" bên trong từng phần tử
          if (!obj.has("auction") || obj.get("auction").isJsonNull()) continue;

          Auction auction = gson.fromJson(obj.get("auction"), Auction.class);
          boolean reviewed = obj.has("reviewed") && obj.get("reviewed").getAsBoolean();
          rows.add(toHistoryItem(auction, reviewed));
        } catch (Exception e) {
          System.err.println("❌ Lỗi phân tích một dòng lịch sử: " + e.getMessage());
        }
      }

      // 3. Đẩy dữ liệu sạch lên giao diện
      Platform.runLater(() -> {
        historyRows.setAll(rows);

        // Cập nhật lại placeholder tương ứng dựa vào kết quả mảng
        if (rows.isEmpty()) {
          Label emptyLabel = new Label("Bạn chưa mua sản phẩm nào.");
          emptyLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-style: italic;");
          sellTableView.setPlaceholder(emptyLabel);
        }
      });
    }).exceptionally(ex -> {
      // In chi tiết lỗi ra console (StackTrace) giúp bạn bắt mạch chính xác lỗi gì
      System.err.println("❌ Gặp ngoại lệ nghiêm trọng tại luồng nền reloadHistoryFromServer:");
      ex.printStackTrace();

      Platform.runLater(() -> {
        Label errorLabel = new Label("Lỗi hệ thống khi tải dữ liệu.");
        errorLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        sellTableView.setPlaceholder(errorLabel);
      });
      return null;
    });
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

    // ÉP BUỘC TRUE ĐỂ NÚT BẤM HIỂN THỊ TRÊN MÀN HÌNH MÀ KHÔNG BỊ DẤU "—" ĐÈ LÊN
    boolean canReview = true;

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

              // Đóng gói dữ liệu vào Context trung gian trước khi chuyển Scene
              ReviewContext.set(data.getAuctionId(), data.getSellerId(), data.getStoreName(), true);

              // Thực hiện chuyển trang sang màn hình Review.fxml
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
                btn.setStyle("-fx-background-color: #94a3b8; -fx-text-fill: white; -fx-background-radius: 5;");
              } else if (!data.isCanReview()) {
                btn.setText("—");
                btn.setDisable(true);
                btn.setStyle("-fx-background-color: #e2e8f0; -fx-text-fill: #64748b; -fx-background-radius: 5;");
              } else {
                btn.setText("⭐ Đánh giá");
                btn.setDisable(false);
                btn.setStyle("-fx-background-color: #e91e63; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5;");
              }
              // 🌟 CĂN GIỮA KHỐI NÚT BẤM HOẶC DẤU GẠCH TRONG Ô Ô CỦA CỘT colReview
              setGraphic(btn);
              setAlignment(Pos.CENTER);
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
      // 1. Tải file giao diện màn hình đánh giá từ thư mục resources
      Parent root = FXMLLoader.load(getClass().getResource("/Review.fxml"));

      // 2. Lấy Stage (cửa sổ ứng dụng hiện tại) từ sự kiện bấm nút của chuột
      Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

      // 3. Thay đổi giao diện bên trong Stage sang màn hình Review
      stage.setScene(new Scene(root));
      stage.setTitle("Đánh giá Shop - Hệ thống đấu giá");
      stage.show();

      System.out.println(">>> Chuyển trang thành công sang màn hình Review.fxml!");
    } catch (IOException e) {
      e.printStackTrace();
      System.err.println("❌ Lỗi: Không thể nạp file /Review.fxml. Hãy kiểm tra lại đường dẫn và fx:controller!");
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
