package com.uet.bidding.controller.admin;

import com.google.gson.Gson; // Đã thêm import Gson
import com.uet.bidding.model.Art;
import com.uet.bidding.model.Electronics;
import com.uet.bidding.model.Item;
import com.uet.bidding.model.Vehicle;
import com.uet.bidding.network.ClientService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;import javafx.scene.image.ImageView;

import java.math.BigDecimal; // Nhớ thêm import này ở trên cùng nhé
import java.util.List;
import java.util.Optional;

public class AdminItemManagementController {

  // Luôn lưu bản sao hiện tại để ClientService gọi ngược lại khi có dữ liệu từ mạng
  private static AdminItemManagementController instance;
  private final ObservableList<Item> pendingData = FXCollections.observableArrayList();

  @FXML
  private Label lblDetailInfo; // Label hiển thị thông tin chi tiết
  @FXML
  private ImageView imgPreview; // Image view để xem ảnh sản phẩm
  @FXML
  private TableView<Item> tablePendingItems;
  @FXML
  private TableColumn<Item, Integer> colId;
  @FXML
  private TableColumn<Item, String> colName;
  @FXML
  private TableColumn<Item, Integer> colSellerId;

  // 🛠️ SỬA DÒNG NÀY: Đổi Double thành BigDecimal
  @FXML
  private TableColumn<Item, BigDecimal> colPrice;

  public static AdminItemManagementController getInstance() {
    return instance;
  }

  @FXML
  public void initialize() {
    instance = this; // Đăng ký instance ngay khi giao diện con này được nạp

    // 1. Cấu hình ánh xạ các cột TableView với thuộc tính trong Model Item
    colId.setCellValueFactory(new PropertyValueFactory<>("id"));
    colName.setCellValueFactory(new PropertyValueFactory<>("name"));
    colSellerId.setCellValueFactory(new PropertyValueFactory<>("sellerId"));
    colPrice.setCellValueFactory(new PropertyValueFactory<>("startingPrice"));

    // 👇 SỬA LẠI ĐOẠN FORMAT GIÁ TIỀN THÀNH THẾ NÀY 👇
    colPrice.setCellFactory(tc -> new javafx.scene.control.TableCell<Item, BigDecimal>() {
      @Override
      protected void updateItem(BigDecimal price, boolean empty) {
        super.updateItem(price, empty);
        if (empty || price == null) {
          setText(null);
        } else {
          // Ép kiểu sang doubleValue() để String.format có thể hiểu được %f
          setText(String.format("%,.0f VNĐ", price.doubleValue()));
        }
      }
    });
    // 👆 SỬA LẠI ĐOẠN FORMAT GIÁ TIỀN THÀNH THẾ NÀY 👆

    tablePendingItems.setItems(pendingData);
    // Lắng nghe sự kiện click dòng trong TableView
    tablePendingItems.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
      if (newVal != null) {
        renderItemPreview(newVal);
      }
    });

    // 2. Bắn request lên Server xin danh sách sản phẩm đang chờ duyệt (PENDING)
    requestDataFromServer();
  }

  private void renderItemPreview(Item item) {
    // 1. Cập nhật thông tin chữ trước (Load cực nhanh)
    StringBuilder sb = new StringBuilder();
    sb.append("===== CHI TIẾT SẢN PHẨM =====\n\n");
    sb.append("📍 Thành phố: ").append(item.getCity() != null ? item.getCity() : "Không rõ").append("\n");
    sb.append("📝 Mô tả: ").append(item.getDescription() != null ? item.getDescription() : "Không có mô tả").append("\n\n");

    sb.append("--- THÔNG SỐ KỸ THUẬT ---\n");

    // Tối ưu: Dùng pattern matching (nếu dùng Java 17+) hoặc ép kiểu chuẩn
    if (item instanceof Electronics e) {
      sb.append("• Loại: Đồ điện tử\n");
      sb.append("• Hãng sản xuất: ").append(e.getBrand()).append("\n");
      sb.append("• Bảo hành: ").append(e.getWarrantyMonths()).append(" tháng\n");
    }
    else if (item instanceof Art a) {
      sb.append("• Loại: Tác phẩm nghệ thuật\n");
      sb.append("• Tác giả: ").append(a.getAuthor()).append("\n");
      sb.append("• Chất liệu: ").append(a.getMaterial()).append("\n");
      sb.append("• Năm sáng tác: ").append(a.getCreationYear()).append("\n");
    }
    else if (item instanceof Vehicle v) {
      sb.append("• Loại: Phương tiện\n");
      sb.append("• Hãng & Dòng: ").append(v.getBrand()).append(" ").append(v.getModel()).append("\n");
      sb.append("• ODO: ").append(v.getMileage()).append(" km\n");
      sb.append("• Động cơ: ").append(v.getEngineType()).append(" (").append(v.getFuelType()).append(")\n");
    }

    lblDetailInfo.setText(sb.toString());

    // 2. Xử lý tải ảnh (Tối ưu: Chỉ tải khi được click)
    // Xóa ảnh cũ đang hiện để tránh Admin nhìn nhầm ảnh sản phẩm trước đó
    imgPreview.setImage(null);

    if (item != null) {
      // Đảm bảo bạn đã import com.uet.bidding.util.ImageUtils;
      com.uet.bidding.util.ImageUtils.loadItemImage(imgPreview, item);

      // Chỉnh lại kích thước hiển thị cho đẹp
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
            // 🛠️ SỬA DÒNG NÀY: Dùng thẳng Gson của GsonFactory thay vì clientService.getGson()
            Gson gson = com.uet.bidding.model.GsonFactory.getInstance();
            String json = gson.toJson(response.getData());

            List<Item> items = gson.fromJson(json, new com.google.gson.reflect.TypeToken<List<Item>>() {
            }.getType());

            // Đưa vào hàm cập nhật giao diện đã viết sẵn
            updatePendingItemsUI(items);
          } else {
            Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tải danh sách!"));
          }
        })
        .exceptionally(ex -> {
          System.err.println("Lỗi mạng: " + ex.getMessage());
          return null;
        });
  }

  /**
   * Hàm này sẽ được ClientService gọi từ luồng mạng (Thread) về để cập nhật dữ liệu lên bảng
   */
  public void updatePendingItemsUI(List<Item> items) {
    if (items == null) return;

    // Đẩy về luồng giao diện chính của JavaFX để tránh crash ứng dụng
    Platform.runLater(() -> {
      pendingData.setAll(items);
      tablePendingItems.refresh();
      System.out.println("[Admin Item] Đã cập nhật xong " + items.size() + " sản phẩm lên bảng.");
    });
  }

  /**
   * Xử lý sự kiện khi bấm nút "Phê duyệt" bài đăng
   */
  @FXML
  public void handleApproveAction() {
    Item selectedItem = tablePendingItems.getSelectionModel().getSelectedItem();
    if (selectedItem == null) {
      showAlert(Alert.AlertType.WARNING, "Thông báo", "Vui lòng chọn một sản phẩm từ bảng để phê duyệt!");
      return;
    }

    // 1. Tạo hộp thoại hỏi "Bạn có chắc..."
    Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
    confirmAlert.setTitle("Xác nhận phê duyệt");
    confirmAlert.setHeaderText("Bạn có chắc chắn muốn phê duyệt sản phẩm này không?");
    confirmAlert.setContentText("Sản phẩm: " + selectedItem.getName() + " (Mã #" + selectedItem.getId() + ")");

    // 2. Hiển thị và chờ Admin bấm nút
    Optional<ButtonType> result = confirmAlert.showAndWait();

    // 3. Nếu Admin bấm OK thì mới tiến hành gửi request lên Server
    if (result.isPresent() && result.get() == ButtonType.OK) {
      System.out.println("[Admin Item] Admin đã xác nhận phê duyệt SP #" + selectedItem.getId());

      ClientService.getInstance().sendRequest("APPROVE_ITEM", selectedItem.getId())
          .thenAccept(response -> {
            if ("APPROVE_SUCCESS".equals(response.getType())) {
              Platform.runLater(() -> {
                pendingData.remove(selectedItem);// Chỉ xóa UI khi Server báo thành công
                // Làm sạch vùng Preview
                imgPreview.setImage(null);
                lblDetailInfo.setText("Chọn một sản phẩm để xem chi tiết...");
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã duyệt sản phẩm thành công!");
              });
            } else {
              Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi", String.valueOf(response.getData())));
            }
          })
          .exceptionally(ex -> {
            System.err.println("Lỗi mạng: " + ex.getMessage());
            return null;
          });
    } else {
      // Admin bấm Cancel hoặc đóng tab -> Không làm gì cả
      System.out.println("[Admin Item] Admin đã hủy lệnh phê duyệt.");
    }
  }

  /**
   * Xử lý sự kiện khi bấm nút "Từ chối" bài đăng
   */
  @FXML
  public void handleRejectAction() {
    Item selectedItem = tablePendingItems.getSelectionModel().getSelectedItem();
    if (selectedItem == null) {
      showAlert(Alert.AlertType.WARNING, "Thông báo", "Vui lòng chọn một sản phẩm từ bảng để từ chối!");
      return;
    }

    TextInputDialog dialog = new TextInputDialog();
    dialog.setTitle("Từ chối bài đăng");
    dialog.setHeaderText("Sản phẩm: " + selectedItem.getName());
    dialog.setContentText("Lý do từ chối đăng bán:");

    Optional<String> result = dialog.showAndWait();
    if (result.isPresent() && !result.get().trim().isEmpty()) {
      String reason = result.get();

      // Gửi lệnh từ chối kèm ID lên Server (định dạng "ID|Lý do")
      ClientService.getInstance().sendRequest("REJECT_ITEM", selectedItem.getId() + "|" + reason)
          .thenAccept(response -> {
            if ("REJECT_SUCCESS".equals(response.getType())) {
              Platform.runLater(() -> {
                pendingData.remove(selectedItem); // Chỉ xóa UI khi Server báo thành công
                // Làm sạch vùng Preview
                imgPreview.setImage(null);
                lblDetailInfo.setText("Chọn một sản phẩm để xem chi tiết...");
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã từ chối sản phẩm!");
              });
            } else {
              Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi", String.valueOf(response.getData())));
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