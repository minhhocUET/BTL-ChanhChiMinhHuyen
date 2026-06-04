package com.uet.bidding.controller.admin;

import java.util.ArrayList;
import java.util.List;
import com.uet.bidding.model.User;
import com.uet.bidding.network.ClientService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;

public class AdminUserManagementController {

  private static AdminUserManagementController instance;
  private final ObservableList<User> userObservableList = FXCollections.observableArrayList();

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
  private TableColumn<User, Void> colUserAction;

  public static AdminUserManagementController getInstance() {
    return instance;
  }

  @FXML
  public void initialize() {
    instance = this;

    colUserId.setCellValueFactory(new PropertyValueFactory<>("id"));
    colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
    colRole.setCellValueFactory(new PropertyValueFactory<>("role"));

    colStatus.setCellValueFactory(cellData -> {
      boolean isBanned = cellData.getValue().isBanned();
      return new SimpleStringProperty(isBanned ? "❌ Đã khóa" : "✅ Hoạt động");
    });

    setupActionColumn();

    userTable.setItems(userObservableList);
    userTable.setFixedCellSize(45.0);

    loadUsersFromServer();
  }

  public void loadUsersFromServer() {
    ClientService.getInstance().sendRequest("GET_ALL_USERS", "")
        .thenAccept(msg -> {
          if ("ERROR".equals(msg.getType())) {
            Platform.runLater(() -> showAlert("Thất bại",
                String.valueOf(msg.getData()), Alert.AlertType.ERROR));
            return;
          }

          try {
            List<?> rawList = (List<?>) msg.getData();
            List<User> platformUsers = new ArrayList<>();

            if (rawList != null) {
              for (Object rawData : rawList) {
                User user = ClientService.getInstance().parseUser(rawData);
                if (user != null) {
                  platformUsers.add(user);
                }
              }
            }

            updateUsersUI(platformUsers);

          } catch (Exception e) {
            System.err.println("❌ Lỗi giải mã danh sách người dùng tại Controller: "
                + e.getMessage());
            e.printStackTrace();
          }
        });
  }

  public void updateUsersUI(List<User> users) {
    Platform.runLater(() -> {
      userObservableList.setAll(users);
      userTable.refresh();
    });
  }

  private void setupActionColumn() {
    Callback<TableColumn<User, Void>, TableCell<User, Void>> cellFactory = new Callback<>() {
      @Override
      public TableCell<User, Void> call(final TableColumn<User, Void> param) {
        return new TableCell<>() {
          private final Button actionBtn = new Button();

          {
            actionBtn.setPrefHeight(30.0);
            actionBtn.setStyle("-fx-cursor: hand; -fx-text-fill: white; "
                + "-fx-font-weight: bold; -fx-background-radius: 4; "
                + "-fx-font-size: 12px;");
          }

          @Override
          protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
              setGraphic(null);
            } else {
              User user = getTableView().getItems().get(getIndex());

              if ("ADMIN".equalsIgnoreCase(user.getRole())) {
                setGraphic(null);
                return;
              }

              if (user.isBanned()) {
                actionBtn.setText("Mở khóa");
                actionBtn.setStyle(actionBtn.getStyle()
                    + "-fx-background-color: #2ed573;");
                actionBtn.setOnAction(event -> handleToggleBan(user, false));
              } else {
                actionBtn.setText("Khóa");
                actionBtn.setStyle(actionBtn.getStyle()
                    + "-fx-background-color: #ff4757;");
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

  private void handleToggleBan(User user, boolean shouldBan) {
    String actionType = shouldBan ? "BAN_USER" : "UNBAN_USER";
    String confirmMsg = shouldBan
        ? "Bạn có chắc chắn muốn khóa tài khoản [" + user.getUsername() + "]?"
        : "Bạn có chắc chắn muốn mở khóa tài khoản [" + user.getUsername() + "]?";

    Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION, confirmMsg,
        ButtonType.YES, ButtonType.NO);
    confirmAlert.setHeaderText(null);
    confirmAlert.setTitle("Xác nhận thao tác");

    confirmAlert.showAndWait().ifPresent(response -> {
      if (response == ButtonType.YES) {
        ClientService.getInstance().sendRequest(actionType, user.getId())
            .thenAccept(msg -> {
              if ("SUCCESS".equals(msg.getType())) {
                Platform.runLater(() -> {
                  String successMsg = (shouldBan ? "Đã khóa " : "Đã mở khóa ")
                      + "tài khoản thành công!";
                  showAlert("Thành công", successMsg, Alert.AlertType.INFORMATION);
                  loadUsersFromServer();
                });
              } else {
                Platform.runLater(() -> showAlert("Thất bại",
                    String.valueOf(msg.getData()), Alert.AlertType.ERROR));
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