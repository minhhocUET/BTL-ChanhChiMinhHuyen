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
import com.uet.bidding.util.SellerAuctionContext;
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
import java.util.Optional;

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
  @FXML private TableColumn<Item, Void> invColAction; // 🔥 Thêm dòng này để tạo cột Xóa

  @FXML private TableView<Auction> activeAuctionsTable;
  @FXML private TableColumn<Auction, String> activeColCity;
  @FXML private TableColumn<Auction, String> activeColType;
  @FXML private TableColumn<Auction, String> activeColName;
  @FXML private TableColumn<Auction, BigDecimal> activeColPrice;
  @FXML private TableColumn<Auction, Integer> activeColRegistered;

  @FXML private TableView<Auction> finishedAuctionsTable;
  @FXML private TableColumn<Auction, String> finishedColCity;
  @FXML private TableColumn<Auction, String> finishedColType;
  @FXML private TableColumn<Auction, String> finishedColName;
  @FXML private TableColumn<Auction, BigDecimal> finishedColPrice;
  @FXML private TableColumn<Auction, Integer> finishedColRegistered;

  private static SellerDashboardController instance;

  private Seller seller;
  private final ObservableList<Item> inventoryItems = FXCollections.observableArrayList();
  private final ObservableList<Auction> activeAuctions = FXCollections.observableArrayList();
  private final ObservableList<Auction> finishedAuctions = FXCollections.observableArrayList();

  public static SellerDashboardController getInstance() {
    return instance;
  }

  @FXML
  public void initialize() {
    instance = this;
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

    // 🔥 CẤU HÌNH CỘT NÚT XÓA SẢN PHẨM 🔥
    if (invColAction != null) {
      invColAction.setCellFactory(param -> new TableCell<Item, Void>() {
        private final Button deleteBtn = new Button("🗑 Xóa");
        {
          // Style màu đỏ hồng hợp với theme fce4ec của bạn
          deleteBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 4 10 4 10;");
          deleteBtn.setOnAction(event -> {
            Item currentItem = getTableView().getItems().get(getIndex());
            handleDeleteProductAction(currentItem); // Gọi hàm xử lý xóa
          });
        }

        @Override
        protected void updateItem(Void item, boolean empty) {
          super.updateItem(item, empty);
          if (empty) {
            setGraphic(null);
          } else {
            setGraphic(deleteBtn);
          }
        }
      });
    }

    inventoryTable.setItems(inventoryItems);
    inventoryTable.setRowFactory(tv -> {
      TableRow<Item> row = new TableRow<>();
      row.setOnMouseClicked(event -> {
        if (!row.isEmpty() && event.getClickCount() == 2) {
          Item selected = row.getItem();
          if (selected == null) return;

          String status = selected.getStatus();

          // 1. Nếu đã duyệt (APPROVED) và chưa lên sàn -> Cho phép tạo đấu giá
          if ("APPROVED".equals(status) && !selected.isInAuction()) {
            openCreateAuctionPage(selected);
          }
          // 2. Nếu đang trong phiên đấu giá (inAuction = true)
          else if (selected.isInAuction()) {
            showAlert(Alert.AlertType.WARNING, "Chú ý", "Sản phẩm đang trong phiên đấu giá!");
          }
          // 3. 🎯 THÊM CASE NÀY: Nếu sản phẩm bị Admin từ chối (REJECTED)
          else if ("REJECTED".equals(status)) {
            showAlert(Alert.AlertType.ERROR, "Từ chối duyệt",
                "Sản phẩm này đã bị Admin từ chối phê duyệt.\nBạn có thể bấm nút XÓA để đăng lại sản phẩm mới.");
          }
          // 4. Nếu sản phẩm vẫn đang chờ duyệt (PENDING)
          else if ("PENDING".equals(status)) {
            showAlert(Alert.AlertType.INFORMATION, "Đang chờ duyệt",
                "Sản phẩm đang trong danh sách chờ Admin phê duyệt.");
          }
          // 5. Dự phòng trường hợp trạng thái khác bất thường
          else {
            showAlert(Alert.AlertType.INFORMATION, "Thông báo", "Sản phẩm không đủ điều kiện lên sàn đấu giá.");
          }
        }
      });
      return row;
    });

    // Setup các bảng đấu giá khác
    setupAuctionColumns(activeColCity, activeColType, activeColName, activeColPrice, activeColRegistered);
    activeAuctionsTable.setItems(activeAuctions);
    activeAuctionsTable.setRowFactory(this::createAuctionRow);

    setupAuctionColumns(finishedColCity, finishedColType, finishedColName, finishedColPrice, finishedColRegistered);
    finishedAuctionsTable.setItems(finishedAuctions);
    finishedAuctionsTable.setRowFactory(this::createAuctionRow);
  }

  public void applyAuctionUpdate(Auction updated) {
    if (updated == null) return;
    Platform.runLater(() -> {
      if ("FINISHED".equals(updated.getStatus())) {
        activeAuctions.removeIf(a -> a.getId() == updated.getId());
        boolean found = false;
        for (int i = 0; i < finishedAuctions.size(); i++) {
          if (finishedAuctions.get(i).getId() == updated.getId()) {
            finishedAuctions.set(i, updated);
            found = true;
            break;
          }
        }
        if (!found) {
          finishedAuctions.add(0, updated);
        }
      } else if ("RUNNING".equals(updated.getStatus())) {
        finishedAuctions.removeIf(a -> a.getId() == updated.getId());
        boolean found = false;
        for (int i = 0; i < activeAuctions.size(); i++) {
          if (activeAuctions.get(i).getId() == updated.getId()) {
            activeAuctions.set(i, updated);
            found = true;
            break;
          }
        }
        if (!found) {
          activeAuctions.add(0, updated);
        }
      }
    });
  }

  private TableRow<Auction> createAuctionRow(TableView<Auction> table) {
    TableRow<Auction> row = new TableRow<>();
    row.setOnMouseClicked(event -> {
      if (row.isEmpty() || event.getClickCount() < 2) return;
      openSellerProductDetail(row.getItem());
    });
    return row;
  }

  private void openSellerProductDetail(Auction auction) {
    if (auction == null) return;
    SellerAuctionContext.set(auction);
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/SellerProductDetail.fxml"));
      Parent root = loader.load();
      ProductDetailController ctrl = loader.getController();
      ctrl.setAuctionData(auction);
      Stage stage = (Stage) sellerTabPane.getScene().getWindow();
      stage.setScene(new Scene(root));
      stage.setTitle("Quản lý phiên - " + (auction.getItem() != null ? auction.getItem().getName() : "#" + auction.getId()));
      stage.show();
    } catch (IOException e) {
      e.printStackTrace();
      showAlert(Alert.AlertType.ERROR, "Lỗi", "Không mở được trang chi tiết phiên đấu giá.");
    }
  }

  private void setupAuctionColumns(TableColumn<Auction, String> colCity, TableColumn<Auction, String> colType,
                                   TableColumn<Auction, String> colName, TableColumn<Auction, BigDecimal> colPrice,
                                   TableColumn<Auction, Integer> colRegistered) {
    if (colCity == null) return;
    colCity.setCellValueFactory(cd -> {
      Item item = cd.getValue() != null ? cd.getValue().getItem() : null;
      return new SimpleStringProperty(nullSafeCity(item));
    });
    colType.setCellValueFactory(cd -> {
      Item item = cd.getValue() != null ? cd.getValue().getItem() : null;
      return new SimpleStringProperty(item != null ? item.getType() : "-");
    });
    colName.setCellValueFactory(cd -> {
      Item item = cd.getValue() != null ? cd.getValue().getItem() : null;
      return new SimpleStringProperty(item != null ? item.getName() : "-");
    });
    colPrice.setCellValueFactory(cd -> new SimpleObjectProperty<>(cd.getValue().getCurrentPrice()));
    if (colRegistered != null) {
      colRegistered.setCellValueFactory(cd ->
          new SimpleObjectProperty<>(cd.getValue().getRegisteredCount()));
    }
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
        // 💡 ĐÃ CẬP NHẬT: Bắt bệnh thông báo theo từng loại sản phẩm được chọn
        String selectedType = typeBox.getValue();
        String errorMessage;

        if ("VEHICLE".equals(selectedType)) {
          errorMessage = "Các ô Giá tiền, Năm, Tháng bảo hành, Số KM phải là số hợp lệ!";
        } else {
          errorMessage = "Vui lòng nhập thông tin hợp lệ!";
        }

        showAlert(Alert.AlertType.ERROR, "Lỗi định dạng", errorMessage);
        event.consume(); // Chặn không cho đóng Dialog
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
  private void handleGoToCreateAuction() {
    // 1. Kiểm tra sản phẩm được chọn
    Item selectedItem = inventoryTable.getSelectionModel().getSelectedItem();
    if (selectedItem == null) {
      showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng chọn một sản phẩm từ kho hàng!");
      return;
    }

    if (!"APPROVED".equals(selectedItem.getStatus())) {
      showAlert(Alert.AlertType.WARNING, "Chưa đủ điều kiện", "Chỉ những sản phẩm đã được Admin duyệt (APPROVED) mới có thể đưa lên sàn!");
      return;
    }

    try {
      // 2. Lưu sản phẩm được chọn vào Context ĐỂ TRUYỀN SANG MÀN HÌNH KIA
      CreateAuctionContext.set(selectedItem);

      // 3. Load file FXML tạo phiên đấu giá
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/uet/bidding/view/CreateAuctionFromItem.fxml"));
      Parent root = loader.load();

      // 4. Chuyển cảnh (Lấy Window trực tiếp từ inventoryTable thay vì event)
      Stage stage = (Stage) inventoryTable.getScene().getWindow();
      stage.setScene(new Scene(root));
      stage.show();

    } catch (Exception e) {
      e.printStackTrace();
      showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tải giao diện: " + e.getMessage());
    }
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

  /**
   * 🚀 HÀM XỬ LÝ XÓA SẢN PHẨM PHÍA SELLER
   */
  private void handleDeleteProductAction(Item item) {
    if (item == null) return;

    // 1. Chặn không cho xóa nếu sản phẩm đang trong phiên đấu giá
    if (item.isInAuction() || "APPROVED".equals(item.getStatus())) {
      showAlert(Alert.AlertType.WARNING, "Không thể xóa",
          "Sản phẩm đã được duyệt hoặc đang trong phiên đấu giá, không thể xóa bỏ!");
      return;
    }

    // 2. Tạo Dialog hỏi xác nhận "Bạn có chắc..." theo đúng yêu cầu
    Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
    confirmAlert.setTitle("Xác nhận xóa");
    confirmAlert.setHeaderText("Bạn có chắc chắn muốn xóa sản phẩm này không?");
    confirmAlert.setContentText("Sản phẩm: " + item.getName() + " (Mã #" + item.getId() + ")\nHành động này không thể hoàn tác!");

    Optional<ButtonType> result = confirmAlert.showAndWait();

    // 3. Admin/Seller bấm OK thì bắn Request lên Server
    if (result.isPresent() && result.get() == ButtonType.OK) {
      System.out.println("[Seller] Gửi yêu cầu xóa sản phẩm #" + item.getId());

      ClientService.getInstance().sendRequest("DELETE_ITEM", item.getId())
          .thenAccept(res -> Platform.runLater(() -> {
            // 🎯 Sửa 1: So sánh chính xác chuỗi Server trả về
            if (res.getType() != null && "DELETE_ITEM_SUCCESS".equals(res.getType())) {
              // Xóa thành công -> Tải lại danh sách kho hàng lập tức
              reloadInventoryFromServer();
              showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã gỡ bỏ sản phẩm khỏi hệ thống!");
            } else {
              showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể xóa sản phẩm: " + res.getData());
            }
          }))
          .exceptionally(ex -> {
            System.err.println("Lỗi kết nối khi xóa: " + ex.getMessage());
            // 🎯 Sửa 2: Hiển thị popup báo lỗi rớt mạng cho người bán biết
            Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi mạng", "Mất kết nối tới Server: " + ex.getMessage()));
            return null;
          });
    } else {
      System.out.println("[Seller] Đã hủy lệnh xóa sản phẩm #" + item.getId());
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