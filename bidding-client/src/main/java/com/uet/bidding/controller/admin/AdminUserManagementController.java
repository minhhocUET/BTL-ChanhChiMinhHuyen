package com.uet.bidding.controller.admin;

import com.uet.bidding.model.User;
import com.uet.bidding.network.ClientService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;

import java.util.List;

public class AdminUserManagementController {
  private static AdminUserManagementController instance;
  private final ObservableList<User> userObservableList = FXCollections.observableArrayList();
  // Đã sửa lại khớp chính xác 100% với fx:id trong FXML mới
  @FXML
  private TableView<User> userTable;
  @FXML
  private TableColumn<User, Integer> colUserId;
  @FXML
  private TableColumn<User, String> colUsername;
  @FXML
  private TableColumn<User, String> colRole;
  @FXML
  private TableColumn<User, String> colStatus;
  @FXML
  private TableColumn<User, Void> colUserAction; // Cột xử lý nút bấm động

  public static AdminUserManagementController getInstance() {
    return instance;
  }

  @FXML
  public void initialize() {
    instance = this;

    // 1. Ánh xạ dữ liệu cho các cột cơ bản
    colUserId.setCellValueFactory(new PropertyValueFactory<>("id"));
    colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
    colRole.setCellValueFactory(new PropertyValueFactory<>("role"));

    colStatus.setCellValueFactory(cellData -> {
      boolean isBanned = cellData.getValue().isBanned();
      return new SimpleStringProperty(isBanned ? "❌ Đã khóa" : "✅ Hoạt động");
    });

    // 2. Tự động dựng nút bấm Khóa/Mở khóa cho từng dòng trong bảng
    setupActionColumn();

    userTable.setItems(userObservableList);

    // 💡 BỔ SUNG DÒNG NÀY: Khóa chết chiều cao tất cả các hàng là 45px (Không cho phép tự phình to)
    userTable.setFixedCellSize(45.0);

    // 3. Tải danh sách người dùng từ Server khi vừa mở màn hình
    loadUsersFromServer();
  }

  public void loadUsersFromServer() {
    ClientService.getInstance().sendRequest("GET_ALL_USERS", "")
        .thenAccept(msg -> {
          if ("ERROR".equals(msg.getType())) {
            Platform.runLater(() -> showAlert("Thất bại", String.valueOf(msg.getData()), Alert.AlertType.ERROR));
            return;
          }

          try {
            // 1. Nhận mảng dữ liệu thô từ Server (dạng List các Map)
            List<?> rawList = (List<?>) msg.getData();
            List<User> platformUsers = new java.util.ArrayList<>();

            if (rawList != null) {
              for (Object rawData : rawList) {
                // 2. DÙNG CHÍNH HÀM PARSE CÓ SẴN CỦA BẠN: Tự động nhận diện chuẩn Admin / Customer
                User user = ClientService.getInstance().parseUser(rawData);
                if (user != null) {
                  platformUsers.add(user);
                }
              }
            }

            // 3. Đổ danh sách chuẩn (đã phân loại Admin/Customer) lên TableView
            updateUsersUI(platformUsers);

          } catch (Exception e) {
            System.err.println("❌ Lỗi giải mã danh sách người dùng tại Controller: " + e.getMessage());
            e.printStackTrace();
          }
        });
  }

  /**
   * Nhận phản hồi danh sách người dùng từ luồng ClientService và nạp lên giao diện
   */
  public void updateUsersUI(List<User> users) {
    Platform.runLater(() -> {
      userObservableList.setAll(users);
      userTable.refresh();
    });
  }

  /**
   * Sinh nút bấm động (Khóa / Mở khóa) cho từng dòng dựa vào trạng thái tài khoản
   */
  private void setupActionColumn() {
    Callback<TableColumn<User, Void>, TableCell<User, Void>> cellFactory = new Callback<>() {
      @Override
      public TableCell<User, Void> call(final TableColumn<User, Void> param) {
        return new TableCell<>() {
          private final Button actionBtn = new Button();

          {
            actionBtn.setPrefHeight(30.0);
            actionBtn.setStyle("-fx-cursor: hand; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 4; -fx-font-size: 12px;");
          }

          @Override
          protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
              setGraphic(null);
            } else {
              User user = getTableView().getItems().get(getIndex());

              // Bảo vệ: Admin không thể tự tác động lên tài khoản Admin khác/chính mình tại đây
              if ("ADMIN".equalsIgnoreCase(user.getRole())) {
                setGraphic(null);
                return;
              }

              // Đổi giao diện nút tùy theo trạng thái bị Ban hay chưa
              if (user.isBanned()) {
                actionBtn.setText("Mở khóa");
                actionBtn.setStyle(actionBtn.getStyle() + "-fx-background-color: #2ed573;"); // Màu xanh lá
                actionBtn.setOnAction(event -> handleToggleBan(user, false));
              } else {
                actionBtn.setText("Khóa");
                actionBtn.setStyle(actionBtn.getStyle() + "-fx-background-color: #ff4757;"); // Màu đỏ
                actionBtn.setOnAction(event -> handleToggleBan(user, true));
              }
              setGraphic(actionBtn);
            }
          }
        };
      }
    };

    colUserAction.setCellFactory(cellFactory);
  }

  /**
   * Hợp nhất logic Ban/Unban cũ thành một hàm xử lý tập trung, an toàn qua luồng mạng
   */
  private void handleToggleBan(User user, boolean shouldBan) {
    String actionType = shouldBan ? "BAN_USER" : "UNBAN_USER";
    String confirmMsg = shouldBan ? "Bạn có chắc chắn muốn khóa tài khoản [" + user.getUsername() + "]?"
        : "Bạn có chắc chắn muốn mở khóa tài khoản [" + user.getUsername() + "]?";

    // Tạo thông báo xác nhận hành động trước khi gửi gói tin đi
    Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION, confirmMsg, ButtonType.YES, ButtonType.NO);
    confirmAlert.setHeaderText(null);
    confirmAlert.setTitle("Xác nhận thao tác");

    confirmAlert.showAndWait().ifPresent(response -> {
      if (response == ButtonType.YES) {
        ClientService.getInstance().sendRequest(actionType, user.getId())
            .thenAccept(msg -> {
              if ("SUCCESS".equals(msg.getType())) {
                Platform.runLater(() -> {
                  showAlert("Thành công", (shouldBan ? "Đã khóa " : "Đã mở khóa ") + "tài khoản thành công!", Alert.AlertType.INFORMATION);
                  loadUsersFromServer(); // Làm mới lại bảng dữ liệu sau khi cập nhật thành công
                });
              } else {
                Platform.runLater(() -> showAlert("Thất bại", String.valueOf(msg.getData()), Alert.AlertType.ERROR));
              }
            });
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

  // ─── PHƯƠNG THỨC GETTER BỔ SUNG PHỤC VỤ UNIT TEST ────────────────

  public TableView<User> getUserTable() {
    return userTable;
  }

  public ObservableList<User> getUserObservableList() {
    return userObservableList;
  }

  public TableColumn<User, Integer> getColUserId() {
    return colUserId;
  }

  public TableColumn<User, String> getColUsername() {
    return colUsername;
  }

  public TableColumn<User, String> getColRole() {
    return colRole;
  }

  public TableColumn<User, String> getColStatus() {
    return colStatus;
  }

  public TableColumn<User, Void> getColUserAction() {
    return colUserAction;
  }
}