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

import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;

import javafx.stage.Stage;



import java.io.IOException;

import java.math.BigDecimal;

import java.nio.file.Files;

import java.util.Base64;

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

  @FXML private TableColumn<Item, String> invColCity;

  @FXML private TableColumn<Item, String> invColType;

  @FXML private TableColumn<Item, String> invColName;

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

    invColCity.setCellValueFactory(cd ->

        new SimpleStringProperty(nullSafeCity(cd.getValue())));

    invColType.setCellValueFactory(cd ->

        new SimpleStringProperty(cd.getValue() != null ? cd.getValue().getType() : "-"));

    invColName.setCellValueFactory(cd ->

        new SimpleStringProperty(cd.getValue() != null ? cd.getValue().getName() : "-"));


    inventoryTable.setItems(inventoryItems);
    inventoryTable.setRowFactory(tv -> {
      TableRow<Item> row = new TableRow<>();
      row.setOnMouseClicked(e -> {
        if (!row.isEmpty() && e.getClickCount() >= 1) {
          openCreateAuctionPage(row.getItem());
        }
      });
      return row;
    });

    setupAuctionColumns(activeColCity, activeColType, activeColName, activeColPrice);

    activeAuctionsTable.setItems(activeAuctions);



    setupAuctionColumns(finishedColCity, finishedColType, finishedColName, finishedColPrice);

    finishedAuctionsTable.setItems(finishedAuctions);

  }



  private void setupAuctionColumns(TableColumn<Auction, String> colCity,

                                   TableColumn<Auction, String> colType,

                                   TableColumn<Auction, String> colName,

                                   TableColumn<Auction, BigDecimal> colPrice) {

    colCity.setCellValueFactory(cd -> {

      Item item = cd.getValue().getItem();

      return new SimpleStringProperty(nullSafeCity(item));

    });

    colType.setCellValueFactory(cd -> {

      Item item = cd.getValue().getItem();

      return new SimpleStringProperty(item != null ? item.getType() : "-");

    });

    colName.setCellValueFactory(cd -> {

      Item item = cd.getValue().getItem();

      return new SimpleStringProperty(item != null ? item.getName() : "-");

    });

    colPrice.setCellValueFactory(cd ->

        new SimpleObjectProperty<>(cd.getValue().getCurrentPrice()));

  }



  private String nullSafeCity(Item item) {

    if (item == null || item.getCity() == null || item.getCity().isBlank()) return "-";

    return item.getCity();

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

          if (!"SUCCESS".equals(res.getType())) return;

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

          if (!"SUCCESS".equals(res.getType())) return;

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

            showAlert(Alert.AlertType.INFORMATION, "Thành công",

                "Đã lưu thông tin cửa hàng lên hệ thống!");

          } else {

            showAlert(Alert.AlertType.ERROR, "Lỗi", String.valueOf(res.getData()));

          }

        }));

  }



  @FXML

  private void handleAddProduct() {

    Customer customer = UserSession.getLoggedInCustomer();

    if (customer == null || !customer.hasCompleteProfile()) {

      showAlert(Alert.AlertType.WARNING, "Chú ý",

          "Vào Setting hoàn thiện hồ sơ trước khi đăng sản phẩm!");

      return;

    }



    Dialog<ButtonType> dialog = new Dialog<>();

    dialog.setTitle("Đăng sản phẩm mới");

    dialog.setHeaderText("Nhập thông tin sản phẩm");



    ComboBox<String> typeBox = new ComboBox<>(

        FXCollections.observableArrayList("ELECTRONICS", "ART", "VEHICLE"));

    typeBox.setValue("ELECTRONICS");

    TextField nameField = new TextField();

    TextArea descArea = new TextArea();

    descArea.setPrefRowCount(3);

    TextField priceField = new TextField();

    TextField cityField = new TextField();

    Label imageLabel = new Label("Chưa chọn ảnh");

    final String[] imageBase64Holder = {null};



    Button pickImageBtn = new Button("Chọn ảnh...");

    pickImageBtn.setOnAction(e -> {

      FileChooser fc = new FileChooser();

      fc.setTitle("Chọn ảnh sản phẩm");

      fc.getExtensionFilters().add(

          new FileChooser.ExtensionFilter("Ảnh", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"));

      Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();

      java.io.File file = fc.showOpenDialog(stage);

      if (file != null) {

        try {

          byte[] bytes = Files.readAllBytes(file.toPath());

          imageBase64Holder[0] = Base64.getEncoder().encodeToString(bytes);

          imageLabel.setText(file.getName());

        } catch (IOException ex) {

          showAlert(Alert.AlertType.ERROR, "Lỗi", "Không đọc được file ảnh: " + ex.getMessage());

        }

      }

    });



    GridPane grid = new GridPane();

    grid.setHgap(10);

    grid.setVgap(10);

    grid.addRow(0, new Label("Loại:"), typeBox);

    grid.addRow(1, new Label("Tên:"), nameField);

    grid.addRow(2, new Label("Mô tả:"), descArea);

    grid.addRow(3, new Label("Giá khởi điểm:"), priceField);

    grid.addRow(4, new Label("Thành phố:"), cityField);

    grid.addRow(5, new Label("Ảnh:"), pickImageBtn);

    grid.add(imageLabel, 1, 6);

    dialog.getDialogPane().setContent(grid);

    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);



    dialog.showAndWait().ifPresent(btn -> {

      if (btn != ButtonType.OK) return;

      try {

        Map<String, Object> payload = new HashMap<>();

        payload.put("itemType", typeBox.getValue());

        payload.put("name", nameField.getText().trim());

        payload.put("description", descArea.getText().trim());

        payload.put("startingPrice", new BigDecimal(priceField.getText().trim()));

        payload.put("city", cityField.getText().trim());

        if (imageBase64Holder[0] != null) {

          payload.put("imageBase64", imageBase64Holder[0]);

        }



        ClientService.getInstance().sendRequest("ADD_ITEM", payload)

            .thenAccept(res -> Platform.runLater(() -> {

              if ("SUCCESS".equals(res.getType())) {

                showAlert(Alert.AlertType.INFORMATION, "OK", "Đã thêm sản phẩm!");

                reloadInventoryFromServer();

              } else {

                showAlert(Alert.AlertType.ERROR, "Lỗi", String.valueOf(res.getData()));

              }

            }));

      } catch (NumberFormatException e) {

        showAlert(Alert.AlertType.ERROR, "Lỗi", "Giá khởi điểm không hợp lệ!");

      }

    });

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

