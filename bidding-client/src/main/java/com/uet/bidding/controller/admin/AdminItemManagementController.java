package com.uet.bidding.controller.admin;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.uet.bidding.model.Art;
import com.uet.bidding.model.Electronics;
import com.uet.bidding.model.GsonFactory;
import com.uet.bidding.model.Item;
import com.uet.bidding.model.Vehicle;
import com.uet.bidding.network.ClientService;
import com.uet.bidding.util.ImageUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;

public class AdminItemManagementController {

  private static AdminItemManagementController instance;
  private final ObservableList<Item> pendingData = FXCollections.observableArrayList();

  @FXML
  private Label lblDetailInfo;
  @FXML
  private ImageView imgPreview;
  @FXML
  private TableView<Item> tablePendingItems;
  @FXML
  private TableColumn<Item, Integer> colId;
  @FXML
  private TableColumn<Item, String> colName;
  @FXML
  private TableColumn<Item, Integer> colSellerId;
  @FXML
  private TableColumn<Item, BigDecimal> colPrice;

  public static AdminItemManagementController getInstance() {
    return instance;
  }

  @FXML
  public void initialize() {
    instance = this;

    colId.setCellValueFactory(new PropertyValueFactory<>("id"));
    colName.setCellValueFactory(new PropertyValueFactory<>("name"));
    colSellerId.setCellValueFactory(new PropertyValueFactory<>("sellerId"));
    colPrice.setCellValueFactory(new PropertyValueFactory<>("startingPrice"));

    colPrice.setCellFactory(tc -> new TableCell<Item, BigDecimal>() {
      @Override
      protected void updateItem(BigDecimal price, boolean empty) {
        super.updateItem(price, empty);
        if (empty || price == null) {
          setText(null);
        } else {
          setText(String.format("%,.0f VNĐ", price.doubleValue()));
        }
      }
    });

    tablePendingItems.setItems(pendingData);
    tablePendingItems.getSelectionModel().selectedItemProperty()
        .addListener((obs, oldVal, newVal) -> {
          if (newVal != null) {
            renderItemPreview(newVal);
          }
        });

    requestDataFromServer();
  }

  private void renderItemPreview(Item item) {
    StringBuilder sb = new StringBuilder();
    sb.append("===== CHI TIẾT SẢN PHẨM =====\n\n");
    sb.append("📍 Thành phố: ")
        .append(item.getCity() != null ? item.getCity() : "Không rõ")
        .append("\n");
    sb.append("📝 Mô tả: ")
        .append(item.getDescription() != null ? item.getDescription() : "Không có mô tả")
        .append("\n\n");

    sb.append("--- THÔNG SỐ KỸ THUẬT ---\n");

    if (item instanceof Electronics e) {
      sb.append("• Loại: Đồ điện tử\n");
      sb.append("• Hãng sản xuất: ").append(e.getBrand()).append("\n");
      sb.append("• Bảo hành: ").append(e.getWarrantyMonths()).append(" tháng\n");
    } else if (item instanceof Art a) {
      sb.append("• Loại: Tác phẩm nghệ thuật\n");
      sb.append("• Tác giả: ").append(a.getAuthor()).append("\n");
      sb.append("• Chất liệu: ").append(a.getMaterial()).append("\n");
      sb.append("• Năm sáng tác: ").append(a.getCreationYear()).append("\n");
    } else if (item instanceof Vehicle v) {
      sb.append("• Loại: Phương tiện\n");
      sb.append("• Hãng & Dòng: ").append(v.getBrand()).append(" ")
          .append(v.getModel()).append("\n");
      sb.append("• ODO: ").append(v.getMileage()).append(" km\n");
      sb.append("• Động cơ: ").append(v.getEngineType()).append(" (")
          .append(v.getFuelType()).append(")\n");
    }

    lblDetailInfo.setText(sb.toString());

    imgPreview.setImage(null);

    if (item != null) {
      ImageUtils.loadItemImage(imgPreview, item);
      imgPreview.setPreserveRatio(true);
      imgPreview.setFitWidth(290);
      imgPreview.setFitHeight(210);
    }
  }

  private void requestDataFromServer() {
    System.out.println("[Admin Item] Đang yêu cầu tải danh sách sản phẩm chờ duyệt...");

    ClientService.getInstance().sendRequest("GET_PENDING_ITEMS", "")
        .thenAccept(response -> {
          if ("GET_PENDING_ITEMS_SUCCESS".equals(response.getType())) {
            Gson gson = GsonFactory.getInstance();
            String json = gson.toJson(response.getData());

            List<Item> items = gson.fromJson(json, new TypeToken<List<Item>>() {
            }.getType());

            updatePendingItemsUI(items);
          } else {
            Platform.runLater(() ->
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tải danh sách!"));
          }
        })
        .exceptionally(ex -> {
          System.err.println("Lỗi mạng: " + ex.getMessage());
          return null;
        });
  }

  public void updatePendingItemsUI(List<Item> items) {
    if (items == null) {
      return;
    }

    Platform.runLater(() -> {
      pendingData.setAll(items);
      tablePendingItems.refresh();
      System.out.println("[Admin Item] Đã cập nhật xong "
          + items.size() + " sản phẩm lên bảng.");
    });
  }

  public void addPendingItemRealtime(Item newItem) {
    if (newItem == null) {
      return;
    }

    Platform.runLater(() -> {
      boolean isDuplicate = pendingData.stream()
          .anyMatch(item -> item.getId() == newItem.getId());

      if (!isDuplicate) {
        pendingData.add(0, newItem);
        tablePendingItems.refresh();
        System.out.println("[Real-time] Đã tự động đẩy sản phẩm mới #"
            + newItem.getId() + " lên màn hình duyệt.");
      }
    });
  }

  @FXML
  public void handleApproveAction() {
    Item selectedItem = tablePendingItems.getSelectionModel().getSelectedItem();
    if (selectedItem == null) {
      showAlert(Alert.AlertType.WARNING, "Thông báo",
          "Vui lòng chọn một sản phẩm từ bảng để phê duyệt!");
      return;
    }

    Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
    confirmAlert.setTitle("Xác nhận phê duyệt");
    confirmAlert.setHeaderText("Bạn có chắc chắn muốn phê duyệt sản phẩm này không?");
    confirmAlert.setContentText("Sản phẩm: " + selectedItem.getName()
        + " (Mã #" + selectedItem.getId() + ")");

    Optional<ButtonType> result = confirmAlert.showAndWait();

    if (result.isPresent() && result.get() == ButtonType.OK) {
      System.out.println("[Admin Item] Admin đã xác nhận phê duyệt SP #"
          + selectedItem.getId());

      ClientService.getInstance().sendRequest("APPROVE_ITEM", selectedItem.getId())
          .thenAccept(response -> {
            if ("APPROVE_SUCCESS".equals(response.getType())) {
              Platform.runLater(() -> {
                pendingData.remove(selectedItem);
                imgPreview.setImage(null);
                lblDetailInfo.setText("Chọn một sản phẩm để xem chi tiết...");
                showAlert(Alert.AlertType.INFORMATION, "Thành công",
                    "Đã duyệt sản phẩm thành công!");
              });
            } else {
              Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi",
                  String.valueOf(response.getData())));
            }
          })
          .exceptionally(ex -> {
            System.err.println("Lỗi mạng: " + ex.getMessage());
            return null;
          });
    } else {
      System.out.println("[Admin Item] Admin đã hủy lệnh phê duyệt.");
    }
  }

  @FXML
  public void handleRejectAction() {
    Item selectedItem = tablePendingItems.getSelectionModel().getSelectedItem();
    if (selectedItem == null) {
      showAlert(Alert.AlertType.WARNING, "Thông báo",
          "Vui lòng chọn một sản phẩm từ bảng để từ chối!");
      return;
    }

    TextInputDialog dialog = new TextInputDialog();
    dialog.setTitle("Từ chối bài đăng");
    dialog.setHeaderText("Sản phẩm: " + selectedItem.getName());
    dialog.setContentText("Lý do từ chối đăng bán:");

    Optional<String> result = dialog.showAndWait();
    if (result.isPresent() && !result.get().trim().isEmpty()) {
      String reason = result.get();

      ClientService.getInstance().sendRequest("REJECT_ITEM",
              selectedItem.getId() + "|" + reason)
          .thenAccept(response -> {
            if ("REJECT_SUCCESS".equals(response.getType())) {
              Platform.runLater(() -> {
                pendingData.remove(selectedItem);
                imgPreview.setImage(null);
                lblDetailInfo.setText("Chọn một sản phẩm để xem chi tiết...");
                showAlert(Alert.AlertType.INFORMATION, "Thành công",
                    "Đã từ chối sản phẩm!");
              });
            } else {
              Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi",
                  String.valueOf(response.getData())));
            }
          })
          .exceptionally(ex -> {
            System.err.println("Lỗi mạng: " + ex.getMessage());
            return null;
          });
    }
  }

  private void showAlert(Alert.AlertType type, String title, String content) {
    Alert alert = new Alert(type);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(content);
    alert.showAndWait();
  }
}