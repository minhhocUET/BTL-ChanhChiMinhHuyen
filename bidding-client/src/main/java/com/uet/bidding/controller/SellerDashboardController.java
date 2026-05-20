package com.uet.bidding.controller;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.uet.bidding.model.Auction;
import com.uet.bidding.model.Customer;
import com.uet.bidding.model.Electronics;
import com.uet.bidding.model.Item;
import com.uet.bidding.model.ItemFactory;
import com.uet.bidding.model.Seller;
import com.uet.bidding.network.ClientService;
import com.uet.bidding.util.CreateAuctionContext;
import com.uet.bidding.util.ImageUtils;
import com.uet.bidding.util.UserSession;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
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
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SellerDashboardController {

  @FXML private Label avatarLabel;
  @FXML private TextField storeNameField;
  @FXML private TextArea descriptionArea;
  @FXML private Label ratingLabel;
  @FXML private TabPane sellerTabPane;

  @FXML private TableView<Item> inventoryTable;

  // SỬA LỖI: Đã bổ sung thêm 2 cột bị thiếu so với giao diện của bạn
  @FXML private TableColumn<Item, Integer> invColId;
  @FXML private TableColumn<Item, String> invColName;
  @FXML private TableColumn<Item, String> invColType;
  @FXML private TableColumn<Item, String> invColCity;
  @FXML private TableColumn<Item, BigDecimal> invColPrice;
  @FXML private TableColumn<Item, String> invColStatus;

  @FXML private TableView<Auction> activeAuctionsTable;
  @FXML private TableColumn<Auction, String> activeColCity;
  @FXML private TableColumn<Auction, String> activeColType;
  @FXML private TableColumn<Auction, String> activeColName;
  @FXML private TableColumn<Auction, BigDecimal> activeColPrice;

  @FXML private TableView<Auction> finishedAuctionsTable;
  @FXML private TableColumn<Auction, String> finishedColCity;
  @FXML private TableColumn<Auction, String> finishedColType;
  @FXML private TableColumn<Auction, String> finishedColName;
  @FXML private TableColumn<Auction, BigDecimal> finishedColPrice;

  private Seller seller;
  private final ObservableList<Item> inventoryItems = FXCollections.observableArrayList();
  private final ObservableList<Auction> activeAuctions = FXCollections.observableArrayList();
  private final ObservableList<Auction> finishedAuctions = FXCollections.observableArrayList();

  @FXML
  public void initialize() {
    Customer c = UserSession.getLoggedInCustomer();
    if (c == null) {
      showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng đăng nhập lại!");
      return;
    }
    seller = c.getSellerProfile();
    setupTables();
    loadSellerData();
    reloadInventoryFromServer();
    reloadActiveAuctionsFromServer();
    reloadFinishedAuctionsFromServer();
  }

  private void setupTables() {
    // Cấu hình các cột cơ bản
    if (invColId != null) invColId.setCellValueFactory(new PropertyValueFactory<>("id"));
    if (invColName != null) invColName.setCellValueFactory(new PropertyValueFactory<>("name"));
    if (invColPrice != null) invColPrice.setCellValueFactory(new PropertyValueFactory<>("startingPrice"));
    if (invColType != null) invColType.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getType()));
    if (invColCity != null) invColCity.setCellValueFactory(cd -> new SimpleStringProperty(nullSafeCity(cd.getValue())));

    // Logic hiển thị trạng thái (Kho hàng quản lý tập trung)
    if (invColStatus != null) {
      invColStatus.setCellValueFactory(cellData -> {
        Item item = cellData.getValue();
        if (item == null) return new SimpleStringProperty("-");
        String status = item.getStatus();
        boolean inAuction = item.isInAuction();

        if ("REJECTED".equals(status)) return new SimpleStringProperty("❌ Bị từ chối");
        if ("PENDING".equals(status)) return new SimpleStringProperty("⏳ Chờ duyệt");
        if (inAuction) return new SimpleStringProperty("🔥 Đang đấu giá");
        if ("APPROVED".equals(status)) return new SimpleStringProperty("✅ Sẵn sàng");
        return new SimpleStringProperty(status);
      });
    }

    inventoryTable.setItems(inventoryItems);
    inventoryTable.setRowFactory(tv -> {
      TableRow<Item> row = new TableRow<>();
      row.setOnMouseClicked(event -> {
        if (!row.isEmpty() && event.getClickCount() == 2) {
          Item selected = row.getItem();
          // Chỉ cho phép tạo đấu giá nếu đã APPROVED và chưa tham gia đấu giá nào
          if ("APPROVED".equals(selected.getStatus()) && !selected.isInAuction()) {
            openCreateAuctionPage(selected);
          } else if (selected.isInAuction()) {
            showAlert(Alert.AlertType.WARNING, "Chú ý", "Sản phẩm đang trong phiên đấu giá!");
          } else {
            showAlert(Alert.AlertType.INFORMATION, "Thông báo", "Sản phẩm cần được Admin phê duyệt trước.");
          }
        }
      });
      return row;
    });

    // Setup các bảng đấu giá khác
    setupAuctionColumns(activeColCity, activeColType, activeColName, activeColPrice);
    activeAuctionsTable.setItems(activeAuctions);
    setupAuctionColumns(finishedColCity, finishedColType, finishedColName, finishedColPrice);
    finishedAuctionsTable.setItems(finishedAuctions);
  }

  private void setupAuctionColumns(TableColumn<Auction, String> colCity, TableColumn<Auction, String> colType,
                                   TableColumn<Auction, String> colName, TableColumn<Auction, BigDecimal> colPrice) {
    if (colCity == null) return;
    colCity.setCellValueFactory(cd -> new SimpleStringProperty(nullSafeCity(cd.getValue().getItem())));
    colType.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getItem().getType()));
    colName.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getItem().getName()));
    colPrice.setCellValueFactory(cd -> new SimpleObjectProperty<>(cd.getValue().getCurrentPrice()));
  }

  private String nullSafeCity(Item item) {
    return (item == null || item.getCity() == null) ? "-" : item.getCity();
  }

  private void loadSellerData() {
    if (storeNameField != null) storeNameField.setText(seller.getStoreName());
    if (descriptionArea != null) descriptionArea.setText(seller.getDescription());
    if (ratingLabel != null) ratingLabel.setText(String.valueOf(seller.getSellerRating()));

    if (avatarLabel != null && seller.getStoreName() != null && !seller.getStoreName().isEmpty()) {
      avatarLabel.setText(seller.getStoreName().substring(0, 1).toUpperCase());
    }
  }

  private void reloadInventoryFromServer() {
    Customer c = UserSession.getLoggedInCustomer();
    if (c == null || inventoryTable == null) return;

    ClientService.getInstance()
        .sendRequest("GET_ITEMS_BY_SELLER", c.getId())
        .thenAccept(res -> Platform.runLater(() -> {
          // In log ra console IDE để kiểm tra
          System.out.println("✅ [UI] Tín hiệu từ Server: " + res.getType());
          System.out.println("✅ [UI] Dữ liệu từ Server: " + res.getData());

          // 1. Nếu Server không trả về SUCCESS -> Request chưa được xử lý đúng trên Server
          if (res.getType() == null || !res.getType().contains("SUCCESS")) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo Server", "Server không trả về SUCCESS. Xem log console IDE!");
            inventoryItems.setAll(createDemoInventoryItem(c)); // Ép hiện demo
            return;
          }

          // 2. Nếu Server trả về đúng, tiến hành đọc JSON
          try {
            Gson gson = ClientService.getInstance().getGson();
            String json = gson.toJson(res.getData());
            List<Item> items = ItemFactory.parseItemsFromJson(json, gson);

            seller.getInventory().clear();
            seller.getInventory().addAll(items);

            if (items.isEmpty()) {
              Item demo = createDemoInventoryItem(c);
              seller.getInventory().add(demo);
              inventoryItems.setAll(demo);
            } else {
              inventoryItems.setAll(items);
            }

            inventoryTable.refresh();

          } catch (Exception e) {
            // 3. Bắt lỗi chết lâm sàng do Parse JSON và ném thẳng lên màn hình
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi đọc dữ liệu", "Lỗi Parse JSON: " + e.getMessage());
            inventoryItems.setAll(createDemoInventoryItem(c)); // Ép hiện demo
          }
        }));
  }

  private void reloadActiveAuctionsFromServer() {
    loadSellerAuctions("GET_SELLER_ACTIVE_AUCTIONS", activeAuctions);
  }

  private void reloadFinishedAuctionsFromServer() {
    loadSellerAuctions("GET_SELLER_FINISHED_AUCTIONS", finishedAuctions);
  }

  private void loadSellerAuctions(String requestType, ObservableList<Auction> target) {
    Customer c = UserSession.getLoggedInCustomer();
    if (c == null) return;

    ClientService.getInstance()
        .sendRequest(requestType, c.getId())
        .thenAccept(res -> Platform.runLater(() -> {
          if (res.getType() == null || !res.getType().contains("SUCCESS")) return;
          Gson gson = ClientService.getInstance().getGson();
          String json = gson.toJson(res.getData());
          List<Auction> list = gson.fromJson(json, new TypeToken<List<Auction>>() {}.getType());
          if (list != null) target.setAll(list);
        }));
  }

  @FXML
  private void handleSaveInfo(ActionEvent event) {
    String newStoreName = storeNameField.getText().trim();
    String newDesc = descriptionArea.getText().trim();

    if (newStoreName.isEmpty()) {
      showAlert(Alert.AlertType.WARNING, "Chú ý", "Vui lòng nhập tên cửa hàng!");
      return;
    }

    Customer customer = UserSession.getLoggedInCustomer();
    if (customer == null) {
      showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng đăng nhập lại!");
      return;
    }

    seller.setStoreName(newStoreName);
    seller.setDescription(newDesc);
    customer.getSellerProfile().setStoreName(newStoreName);
    customer.getSellerProfile().setDescription(newDesc);

    if (avatarLabel != null) {
      avatarLabel.setText(newStoreName.substring(0, 1).toUpperCase());
    }

    ClientService.getInstance().sendRequest("UPDATE_PROFILE", customer)
        .thenAccept(res -> Platform.runLater(() -> {
          if ("UPDATE_PROFILE_SUCCESS".equals(res.getType())) {
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã lưu thông tin cửa hàng lên hệ thống!");
          } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi", String.valueOf(res.getData()));
          }
        }));
  }

  @FXML
  private void handleAddProduct() {
    Customer customer = UserSession.getLoggedInCustomer();
    if (customer == null || !customer.hasCompleteProfile()) {
      showAlert(Alert.AlertType.WARNING, "Chú ý", "Vào Setting hoàn thiện hồ sơ trước khi đăng sản phẩm!");
      return;
    }

    Dialog<ButtonType> dialog = new Dialog<>();
    dialog.setTitle("Đăng sản phẩm mới");
    dialog.setHeaderText("Nhập thông tin sản phẩm");

    ComboBox<String> typeBox = new ComboBox<>();
    typeBox.setItems(FXCollections.observableArrayList("ELECTRONICS", "ART", "VEHICLE"));
    typeBox.setPromptText("--- Vui lòng chọn loại sản phẩm ---");
    typeBox.setPrefWidth(300);
    typeBox.setStyle("-fx-border-color: #3498db; -fx-border-width: 2; -fx-border-radius: 5;");

    TextField nameField = new TextField();
    TextArea descArea = new TextArea(); descArea.setPrefRowCount(3);
    TextField priceField = new TextField();
    TextField cityField = new TextField();
    Label imageLabel = new Label("Chưa chọn ảnh");
    final String[] imageBase64Holder = {null};

    Button pickImageBtn = new Button("Chọn ảnh...");
    pickImageBtn.setOnAction(e -> {
      FileChooser fc = new FileChooser();
      fc.setTitle("Chọn ảnh sản phẩm");
      fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Ảnh", "*.png", "*.jpg", "*.jpeg"));
      Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
      java.io.File file = fc.showOpenDialog(stage);
      if (file != null) {
        String base64 = ImageUtils.encodeFileToBase64(file);
        if (base64 != null) {
          imageBase64Holder[0] = base64;
          imageLabel.setText(file.getName());
        } else {
          showAlert(Alert.AlertType.ERROR, "Lỗi", "Không đọc được file ảnh!");
        }
      }
    });

    GridPane dynamicGrid = new GridPane();
    dynamicGrid.setHgap(10); dynamicGrid.setVgap(10);
    Map<String, TextField> dynamicFieldsMap = new HashMap<>();

    typeBox.setOnAction(e -> {
      dynamicGrid.getChildren().clear();
      dynamicFieldsMap.clear();
      String selectedType = typeBox.getValue();
      int row = 0;

      if ("ELECTRONICS".equals(selectedType)) {
        dynamicFieldsMap.put("brand", addDynamicRow(dynamicGrid, "Thương hiệu:", row++));
        dynamicFieldsMap.put("warrantyMonths", addDynamicRow(dynamicGrid, "Bảo hành (tháng):", row++));
      } else if ("ART".equals(selectedType)) {
        dynamicFieldsMap.put("author", addDynamicRow(dynamicGrid, "Tác giả:", row++));
        dynamicFieldsMap.put("creationYear", addDynamicRow(dynamicGrid, "Năm sáng tác:", row++));
        dynamicFieldsMap.put("material", addDynamicRow(dynamicGrid, "Chất liệu:", row++));
      } else if ("VEHICLE".equals(selectedType)) {
        dynamicFieldsMap.put("brand", addDynamicRow(dynamicGrid, "Hãng xe:", row++));
        dynamicFieldsMap.put("model", addDynamicRow(dynamicGrid, "Dòng xe:", row++));
        dynamicFieldsMap.put("manufacturingYear", addDynamicRow(dynamicGrid, "Năm sản xuất:", row++));
        dynamicFieldsMap.put("mileage", addDynamicRow(dynamicGrid, "Số KM đã đi:", row++));
        dynamicFieldsMap.put("engineType", addDynamicRow(dynamicGrid, "Loại động cơ:", row++));
        dynamicFieldsMap.put("fuelType", addDynamicRow(dynamicGrid, "Loại nhiên liệu:", row++));
      }
      dialog.getDialogPane().getScene().getWindow().sizeToScene();
    });

    GridPane mainGrid = new GridPane();
    mainGrid.setHgap(10); mainGrid.setVgap(10);
    mainGrid.addRow(0, new Label("Loại:"), typeBox);
    mainGrid.addRow(1, new Label("Tên:"), nameField);
    mainGrid.addRow(2, new Label("Mô tả:"), descArea);
    mainGrid.addRow(3, new Label("Giá khởi điểm:"), priceField);
    mainGrid.addRow(4, new Label("Thành phố:"), cityField);
    mainGrid.addRow(5, new Label("Ảnh:"), pickImageBtn);
    mainGrid.add(imageLabel, 1, 6);
    mainGrid.add(new Separator(), 0, 7, 2, 1);
    mainGrid.add(dynamicGrid, 0, 8, 2, 1);

    dialog.getDialogPane().setContent(mainGrid);
    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

    Button btOk = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
    btOk.disableProperty().bind(typeBox.valueProperty().isNull());

    btOk.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
      try {
        if (nameField.getText().trim().isEmpty() || descArea.getText().trim().isEmpty() || cityField.getText().trim().isEmpty()) {
          showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng nhập đủ Tên, Mô tả và Thành phố.");
          event.consume(); return;
        }
        new BigDecimal(priceField.getText().trim());

        if ("ELECTRONICS".equals(typeBox.getValue())) {
          String wm = dynamicFieldsMap.get("warrantyMonths").getText().trim();
          if (!wm.isEmpty()) Integer.parseInt(wm);
        } else if ("ART".equals(typeBox.getValue())) {
          String cy = dynamicFieldsMap.get("creationYear").getText().trim();
          if (!cy.isEmpty()) Integer.parseInt(cy);
        } else if ("VEHICLE".equals(typeBox.getValue())) {
          String my = dynamicFieldsMap.get("manufacturingYear").getText().trim();
          if (!my.isEmpty()) Integer.parseInt(my);
          String mil = dynamicFieldsMap.get("mileage").getText().trim();
          if (!mil.isEmpty()) Double.parseDouble(mil);
        }

      } catch (NumberFormatException ex) {
        showAlert(Alert.AlertType.ERROR, "Lỗi định dạng", "Các ô Giá tiền, Năm, Tháng bảo hành, Số KM (nếu có nhập) phải là số hợp lệ!");
        event.consume();
      }
    });

    dialog.showAndWait().ifPresent(btn -> {
      if (btn != ButtonType.OK) return;

      Map<String, Object> payload = new HashMap<>();
      payload.put("itemType", typeBox.getValue());
      payload.put("name", nameField.getText().trim());
      payload.put("description", descArea.getText().trim());
      payload.put("startingPrice", new BigDecimal(priceField.getText().trim()));
      payload.put("city", cityField.getText().trim());
      payload.put("sellerId", UserSession.getLoggedInCustomer().getId());
      // FIX LỖI SQL: Gửi các giá trị mặc định để Server không bị lỗi "in_auction"
      payload.put("status", "PENDING");
      payload.put("inAuction", false);

      if (imageBase64Holder[0] != null) {
        payload.put("imageBase64", imageBase64Holder[0]);
      }

      for (Map.Entry<String, TextField> entry : dynamicFieldsMap.entrySet()) {
        String key = entry.getKey();
        String value = entry.getValue().getText().trim();
        if (value.isEmpty()) continue;

        if (key.equals("warrantyMonths") || key.equals("creationYear") || key.equals("manufacturingYear")) {
          payload.put(key, Integer.parseInt(value));
        } else if (key.equals("mileage")) {
          payload.put(key, Double.parseDouble(value));
        } else {
          payload.put(key, value);
        }
      }

      ClientService.getInstance().sendRequest("ADD_ITEM", payload)
          .thenAccept(res -> Platform.runLater(() -> {
            if (res.getType() != null && res.getType().contains("SUCCESS")) {
              // SỬA LỖI: Gọi hàm cập nhật bảng NGAY LẬP TỨC trước khi gọi Alert
              reloadInventoryFromServer();
              showAlert(Alert.AlertType.INFORMATION, "OK", "Đã thêm sản phẩm! Vui lòng chờ Admin duyệt.");
            } else {
              showAlert(Alert.AlertType.ERROR, "Lỗi", String.valueOf(res.getData()));
            }
          }));
    });
  }

  private TextField addDynamicRow(GridPane grid, String label, int row) {
    TextField field = new TextField();
    grid.addRow(row, new Label(label), field);
    return field;
  }

  @FXML
  public void handleViewReview(ActionEvent event) {
    try {
      Parent reviewRoot = FXMLLoader.load(getClass().getResource("/SellerReview.fxml"));
      Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
      stage.setScene(new Scene(reviewRoot));
      stage.setTitle("Đánh giá của khách hàng");
      stage.show();
    } catch (IOException e) {
      e.printStackTrace();
      System.out.println("Lỗi: Không mở được trang SellerReview.fxml");
    }
  }

  @FXML
  public void handleBack(ActionEvent event) {
    try {
      Parent root = FXMLLoader.load(getClass().getResource("/AuctionList.fxml"));
      Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
      stage.setScene(new Scene(root));
      stage.setTitle("Danh sách đấu giá");
      stage.show();
    } catch (IOException e) {
      e.printStackTrace();
      System.out.println("Không thể quay lại trang AuctionList.fxml");
    }
  }

  private Item createDemoInventoryItem(Customer c) {
    Electronics demo = new Electronics(
        "MacBook Pro 14\" (Demo)",
        "Sản phẩm mẫu hiển thị khi kho trống. Thêm sản phẩm thật qua «Đăng sản phẩm mới».",
        new BigDecimal("25000000"),
        "",
        c.getId(),
        "Apple",
        12);
    demo.setId(-1);
    demo.setCity("Hà Nội");
    demo.setInAuction(false);
    return demo;
  }

  private void openCreateAuctionPage(Item item) {
    if (item == null) return;
    CreateAuctionContext.set(item);
    try {
      Parent root = FXMLLoader.load(getClass().getResource("/CreateAuctionFromItem.fxml"));
      Stage stage = (Stage) inventoryTable.getScene().getWindow();
      stage.setScene(new Scene(root));
      stage.setTitle("Tạo phiên đấu giá - " + item.getName());
      stage.show();
    } catch (IOException e) {
      e.printStackTrace();
      showAlert(Alert.AlertType.ERROR, "Lỗi", "Không mở được trang tạo phiên đấu giá.");
    }
  }

  private void showAlert(Alert.AlertType alertType, String title, String content) {
    Alert alert = new Alert(alertType);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(content);
    alert.showAndWait();
  }
}