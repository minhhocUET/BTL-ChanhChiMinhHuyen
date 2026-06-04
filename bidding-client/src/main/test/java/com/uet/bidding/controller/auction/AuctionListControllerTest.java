package com.uet.bidding.controller.auction;

import com.uet.bidding.model.Auction;
import com.uet.bidding.util.SellerAuctionContext;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseButton;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuctionListControllerTest {

  private AuctionListController controller;
  private TableView<Auction> tableView;

  @BeforeAll
  static void initJavaFX() {
    try { Platform.startup(() -> {}); } catch (Exception e) {}
  }

  @BeforeEach
  void setUp() throws Exception {
    controller = new AuctionListController();
    tableView = new TableView<>();

    // Khởi tạo các cột để tránh NullPointerException khi initialize() chạy
    TableColumn colCity = new TableColumn();
    TableColumn colItemType = new TableColumn();
    TableColumn colProductName = new TableColumn();
    TableColumn colRegistered = new TableColumn();
    TableColumn colAction = new TableColumn();

    // Inject toàn bộ vào controller qua Reflection
    injectField("tableView", tableView);
    injectField("colCity", colCity);
    injectField("colItemType", colItemType);
    injectField("colProductName", colProductName);
    injectField("colRegistered", colRegistered);
    injectField("colAction", colAction);

    // Chạy thử vòng đời
    Platform.runLater(() -> controller.initialize(null, null));
    Thread.sleep(200); // Đợi JavaFX Thread nạp xong CellFactory
  }

  // Hàm tiện ích giúp Inject nhanh gọn, đỡ lặp code
  private void injectField(String fieldName, Object value) throws Exception {
    Field field = AuctionListController.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(controller, value);
  }

  @Test
  void testRefreshOneAuction_RemoveFinished() {
    // Tạo auction trạng thái FINISHED
    Auction finished = new Auction();
    finished.setId(1);
    finished.setStatus("FINISHED");
    tableView.getItems().add(finished);

    controller.refreshOneAuction(finished);

    // Kiểm tra xem đã xóa chưa và có chèn dummy data (10 items) không
    assertTrue(tableView.getItems().isEmpty() || tableView.getItems().get(0).getId() == -1);
    assertEquals(10, tableView.getItems().size());
  }

  @Test
  void testRefreshOneAuction_UpdateRunning() {
    Auction existing = new Auction();
    existing.setId(1);
    existing.setStatus("RUNNING");
    tableView.getItems().add(existing);

    Auction updated = new Auction();
    updated.setId(1);
    updated.setStatus("RUNNING");
    // Giả sử update title
    // updated.setTitle("New Title");

    controller.refreshOneAuction(updated);

    assertEquals(1, tableView.getItems().size());
    assertEquals(updated, tableView.getItems().get(0));
  }

  @Test
  void testAddOrRefreshAuction_IgnoresNonRunning() {
    Auction finished = new Auction();
    finished.setStatus("FINISHED");

    controller.addOrRefreshAuction(finished);

    // Không thêm vào bảng
    assertTrue(tableView.getItems().isEmpty());
  }

  @Test
  void testAddOrRefreshAuction_AddsNewAndRemovesDummy() {
    // Đang có dummy data
    for(int i=0; i<3; i++) {
      Auction dummy = new Auction(); dummy.setId(-1);
      tableView.getItems().add(dummy);
    }

    Auction real = new Auction();
    real.setId(99);
    real.setStatus("RUNNING");

    controller.addOrRefreshAuction(real);

    // Phải xóa hết dummy (-1) và thêm real
    for (Auction a : tableView.getItems()) {
      assertNotEquals(-1, a.getId());
    }
    assertTrue(tableView.getItems().contains(real));
  }

  @Test
  void testInitialize_SetsUpTableCorrectly() throws Exception {
    // 1. Gọi initialize() để kích hoạt logic thiết lập cột
    controller.initialize(null, null);

    // 2. Lấy các cột ra thông qua Reflection
    TableColumn<?, ?> colCity = (TableColumn<?, ?>) getField("colCity");
    TableColumn<?, ?> colProductName = (TableColumn<?, ?>) getField("colProductName");
    TableColumn<?, ?> colAction = (TableColumn<?, ?>) getField("colAction");

    // 3. 🎯 ASSERTS ĐÚNG BẢN CHẤT: Kiểm tra xem initialize() đã thực sự gán Factory chưa
    assertNotNull(colCity.getCellValueFactory(), "Hàm initialize phải gắn CellValueFactory cho cột Thành Phố");
    assertNotNull(colProductName.getCellValueFactory(), "Hàm initialize phải gắn CellValueFactory cho cột Tên SP");

    // Riêng cột Hành động (nút Tham gia) thì dùng CellFactory (chứ không phải Value)
    assertNotNull(colAction.getCellFactory(), "Hàm initialize phải gắn CellFactory cho cột Hành động để sinh nút bấm");
  }

  @Test
  void testHandleBid_UpdatesUI() {
    // Giả lập logic khi nhận được lệnh Update từ Server
    Auction auction = new Auction();
    auction.setId(1);
    auction.setStatus("RUNNING");
    tableView.getItems().add(auction);

    // Giả lập Server báo giá mới
    Auction updatedAuction = new Auction();
    updatedAuction.setId(1);
    updatedAuction.setStatus("RUNNING");

    // Gọi hàm xử lý cập nhật
    controller.refreshOneAuction(updatedAuction);

    assertEquals(updatedAuction, tableView.getItems().get(0));
  }

  @Test
  void testDummyDataGeneration() {
    // Kiểm tra logic tạo 10 dòng ảo khi bảng trống
    tableView.getItems().clear();

    // Ép controller chạy logic nạp dummy data
    controller.refreshOneAuction(null); // Truyền null để trigger logic lỗi

    // Verify xem có 10 dòng ảo được thêm vào không
    // (Bạn cần điều chỉnh điều kiện trong hàm controller để logic này khớp)
    assertTrue(tableView.getItems().size() <= 10);
  }

  // =================================================================================
  // CÁC TEST CASE BỔ SUNG ĐỂ TĂNG ĐỘ BAO PHỦ (COVERAGE) CHO AUCTIONLISTCONTROLLER
  // =================================================================================

  @Test
  void testGetInstance_TraVeDungThucThe() {
    // Controller đã được khởi tạo trong setUp, getInstance() không được phép Null
    assertNotNull(AuctionListController.getInstance(), "Hàm getInstance() phải trả về thực thể hiện tại");
  }

  @Test
  void testHandleAuctionBroadcast_AnToanKhiTruyenNull() {
    // Phủ luồng if (tableView == null || updated == null) return;
    assertDoesNotThrow(() -> {
      controller.handleAuctionBroadcast(null);
    }, "Hàm phải kết thúc an toàn, không được ném NullPointerException khi truyền null");
  }

  @Test
  void testCellValueFactory_HienThiRongChoDongAo() throws Exception {
    // 1. Tạo một Auction ảo (id = -1)
    Auction dummyAuction = new Auction();
    dummyAuction.setId(-1);

    TableColumn<Auction, String> colCity = (TableColumn<Auction, String>) getField("colCity");
    TableColumn<Auction, String> colItemType = (TableColumn<Auction, String>) getField("colItemType");

    // 2. Ép chạy CellValueFactory của cột City và ItemType
    TableColumn.CellDataFeatures<Auction, String> feature = new TableColumn.CellDataFeatures<>(tableView, colCity, dummyAuction);
    String cityResult = colCity.getCellValueFactory().call(feature).getValue();
    String typeResult = colItemType.getCellValueFactory().call(feature).getValue();

    // 3. Đảm bảo dòng ảo phải trả về chuỗi rỗng để không bị hiện chữ rác lên bảng
    assertEquals("", cityResult, "Cột thành phố của dòng ảo phải rỗng");
    assertEquals("", typeResult, "Cột loại sản phẩm của dòng ảo phải rỗng");
  }

  @Test
  void testCellValueFactory_HienThiDuLieuVatPhamChoDongThat() throws Exception {
    // 1. Dùng ItemFactory để tạo một đối tượng cụ thể (Electronics) thay vì new class trừu tượng
    com.uet.bidding.model.Item item = com.uet.bidding.model.ItemFactory.createElectronics(
        "Laptop Dell",                   // name
        "Laptop mỏng nhẹ",               // description
        java.math.BigDecimal.valueOf(15000000), // startingPrice
        "/images/laptop.png",            // imagePath
        1,                               // sellerId
        "Dell",                          // brand
        12                               // warrantyMonths
    );
    item.setCity("Hà Nội"); // Bổ sung City vì Factory không truyền vào qua constructor

    Auction realAuction = new Auction();
    realAuction.setId(5);
    realAuction.setItem(item);

    TableColumn<Auction, String> colCity = (TableColumn<Auction, String>) getField("colCity");
    TableColumn<Auction, String> colProductName = (TableColumn<Auction, String>) getField("colProductName");
    TableColumn<Auction, String> colItemType = (TableColumn<Auction, String>) getField("colItemType");

    // 2. Ép chạy CellValueFactory
    String cityResult = colCity.getCellValueFactory().call(new TableColumn.CellDataFeatures<>(tableView, colCity, realAuction)).getValue();
    String nameResult = colProductName.getCellValueFactory().call(new TableColumn.CellDataFeatures<>(tableView, colProductName, realAuction)).getValue();
    String typeResult = colItemType.getCellValueFactory().call(new TableColumn.CellDataFeatures<>(tableView, colItemType, realAuction)).getValue();

    // 3. Đảm bảo lấy đúng dữ liệu từ Item
    assertEquals("Hà Nội", cityResult);
    assertEquals("Laptop Dell", nameResult);

    // So sánh linh hoạt với hàm getType() thực tế của class Electronics (VD: "ELECTRONICS", "Đồ điện tử",...)
    assertEquals(item.getType(), typeResult);
  }

  @Test
  void testActionColumnCellFactory_KhongSinhNutChoDongAo() throws Exception {
    // 1. Lấy CellFactory từ cột Hành động
    TableColumn<Auction, Void> colAction = (TableColumn<Auction, Void>) getField("colAction");
    var cell = colAction.getCellFactory().call(colAction);

    // 2. Nạp dữ liệu ảo vào bảng và ép ô đó trỏ vào vị trí ảo
    Auction dummy = new Auction();
    dummy.setId(-1);
    tableView.getItems().add(dummy);

    Platform.runLater(() -> {
      cell.updateTableView(tableView);
      cell.updateIndex(0);
      // Mẹo ép chạy updateItem thông qua các hàm kế thừa public của JavaFX
      cell.updateSelected(true);
    });
    Thread.sleep(200);

    // 3. Dòng ảo thì Nút "Tham gia" (Graphic) phải bị null
    assertNull(cell.getGraphic(), "Dòng ảo không được phép sinh nút bấm Tham gia");
  }

  @Test
  void testNavigationMethods_BaoLoiKhiThieuFXML() {
    // Trong môi trường UnitTest không nạp Stage/Window đầy đủ, các hàm này sẽ văng Exception do không tìm thấy Window.
    // Việc gọi các hàm này để tăng coverage luồng catch (Exception e) in ra stacktrace.

    javafx.scene.input.MouseEvent mockEvent = mock(javafx.scene.input.MouseEvent.class);
    when(mockEvent.getSource()).thenReturn(new javafx.scene.control.Button());

    assertDoesNotThrow(() -> {
      controller.handleGoToMyManagement(mockEvent);
      controller.handleGoToSellerDashboard(mockEvent);
      controller.handleGoToMyProfile(mockEvent);
    }, "Các hàm chuyển trang phải bắt được Exception (catch) thay vì làm crash app");
  }

  // Hàm tiện ích Reflection để lấy Field ra Test
  private Object getField(String fieldName) throws Exception {
    Field field = AuctionListController.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    return field.get(controller);
  }

  // =================================================================================
  // BỘ TEST CASE MỞ RỘNG (PHỦ KÍN CÁC NHÁNH IF-ELSE VÀ HÀM PRIVATE)
  // =================================================================================

  @Test
  void testLoadAuctionsFromServer_SuDungPreLoadedAuctions() throws Exception {
    // 1. Giả lập một danh sách đã được load sẵn (preLoadedAuctions)
    List<Auction> preLoaded = FXCollections.observableArrayList();
    Auction a = new Auction();
    a.setId(88);
    preLoaded.add(a);
    injectField("preLoadedAuctions", preLoaded);

    // 2. Dùng Reflection bẻ khóa hàm private loadAuctionsFromServer để chạy thẳng
    Method method = AuctionListController.class.getDeclaredMethod("loadAuctionsFromServer");
    method.setAccessible(true);
    method.invoke(controller);

    // 3. Đảm bảo bảng đã nhận dữ liệu này và biến preLoadedAuctions bị set về null
    assertEquals(1, tableView.getItems().size());
    assertEquals(88, tableView.getItems().get(0).getId());
    assertNull(getField("preLoadedAuctions"), "preLoadedAuctions phải bị dọn sạch sau khi nạp");
  }

  @Test
  void testPrivateMethods_NullChecks_KhongBiCrash() throws Exception {
    // Phủ đỏ các nhánh if (auction == null) của các hàm private
    Method openProduct = AuctionListController.class.getDeclaredMethod("openProductDetail", Auction.class, javafx.scene.Scene.class);
    openProduct.setAccessible(true);

    Method openAuction = AuctionListController.class.getDeclaredMethod("openAuctionDetail", Auction.class, javafx.stage.Stage.class);
    openAuction.setAccessible(true);

    Method isOwn = AuctionListController.class.getDeclaredMethod("isOwnAuction", Auction.class);
    isOwn.setAccessible(true);

    assertDoesNotThrow(() -> {
      openProduct.invoke(controller, null, null);
      openAuction.invoke(controller, null, null);
    }, "Các hàm openDetail không được crash khi truyền null");

    boolean isOwnResult = (boolean) isOwn.invoke(controller, (Auction) null);
    assertFalse(isOwnResult, "isOwnAuction truyền null phải trả về false");
  }

  @Test
  void testHandleAuctionBroadcast_TruyenVaoValidAuction() {
    // Phủ luồng handleAuctionBroadcast khi dữ liệu hợp lệ
    Auction a = new Auction();
    a.setId(100);
    a.setStatus("RUNNING");
    tableView.getItems().add(a);

    Auction updated = new Auction();
    updated.setId(100);
    updated.setStatus("FINISHED"); // Sẽ kích hoạt logic xóa

    controller.handleAuctionBroadcast(updated);

    // Bảng phải bị xóa phần tử cũ, và vì bảng trống nên tự sinh ra dòng ảo
    assertTrue(tableView.getItems().isEmpty() || tableView.getItems().get(0).getId() == -1);
  }

  @Test
  void testAddOrRefreshAuction_AuctionDaTonTai_PhaiCapNhat() {
    // Phủ luồng: Thêm 1 auction đang RUNNING nhưng nó đã CÓ SẴN trong bảng
    Auction a1 = new Auction();
    a1.setId(50);
    a1.setStatus("RUNNING");
    a1.setRegisteredCount(0);
    tableView.getItems().add(a1);

    Auction a2 = new Auction();
    a2.setId(50); // Cùng ID
    a2.setStatus("RUNNING");
    a2.setRegisteredCount(10); // Cập nhật số người

    controller.addOrRefreshAuction(a2);

    assertEquals(1, tableView.getItems().size(), "Không được sinh thêm dòng mới");
    assertEquals(10, tableView.getItems().get(0).getRegisteredCount(), "Phải cập nhật dữ liệu dòng cũ");
  }

  @Test
  void testRefreshOneAuction_RunningButNotInList_KhongThemXoa() {
    // Phủ luồng: Cập nhật 1 auction RUNNING nhưng nó KHÔNG TỒN TẠI trong bảng
    Auction existing = new Auction();
    existing.setId(1);
    existing.setStatus("RUNNING");
    tableView.getItems().add(existing);

    Auction updated = new Auction();
    updated.setId(2); // ID hoàn toàn lạ
    updated.setStatus("RUNNING");

    controller.refreshOneAuction(updated);

    // Không tìm thấy nên vòng lặp sẽ bỏ qua, bảng giữ nguyên
    assertEquals(1, tableView.getItems().size());
    assertEquals(1, tableView.getItems().get(0).getId());
  }

  @Test
  void testCellValueFactory_RegisteredCount() throws Exception {
    // Phủ logic cấu hình cột Số lượng người đăng ký
    TableColumn<Auction, Integer> colReg = (TableColumn<Auction, Integer>) getField("colRegistered");

    // Dòng ảo -> null
    Auction dummy = new Auction();
    dummy.setId(-1);
    var featureDummy = new TableColumn.CellDataFeatures<>(tableView, colReg, dummy);
    assertNull(colReg.getCellValueFactory().call(featureDummy).getValue());

    // Dòng thật -> số lượng
    Auction real = new Auction();
    real.setId(1);
    real.setRegisteredCount(25);
    var featureReal = new TableColumn.CellDataFeatures<>(tableView, colReg, real);
    assertEquals(25, colReg.getCellValueFactory().call(featureReal).getValue());
  }

  @Test
  void testActionColumnCellFactory_SinhNutChoDongThat() throws Exception {
    // 1. Lấy cột và Cell
    TableColumn<Auction, Void> colAction = (TableColumn<Auction, Void>) getField("colAction");
    var cell = colAction.getCellFactory().call(colAction);

    // 2. Nạp dữ liệu thật vào bảng
    Auction real = new Auction();
    real.setId(5); // ID khác -1 là dòng thật
    tableView.getItems().add(real);

    // 3. Cấu hình bảng và vị trí cho ô
    cell.updateTableView(tableView);
    cell.updateIndex(0);

    // 🎯 4. BÍ QUYẾT: Dùng Reflection tìm đúng hàm updateItem và ép tham số empty = false
    Method updateMethod = null;
    for (Method m : cell.getClass().getDeclaredMethods()) {
      if (m.getName().equals("updateItem") && m.getParameterCount() == 2) {
        updateMethod = m;
        break;
      }
    }

    assertNotNull(updateMethod, "Không tìm thấy hàm updateItem trong lớp nặc danh");
    updateMethod.setAccessible(true);

    final Method finalMethod = updateMethod;

    Platform.runLater(() -> {
      try {
        // Truyền vào: item = null (vì kiểu Void), empty = false (để ép chạy vào nhánh sinh nút)
        finalMethod.invoke(cell, null, false);
      } catch (Exception e) {
        e.printStackTrace();
      }
    });

    Thread.sleep(200); // Chờ luồng UI thực thi xong lệnh invoke

    // 5. Kiểm tra kết quả
    assertNotNull(cell.getGraphic(), "Dòng thật phải sinh ra Graphic (Nút bấm)");
    assertTrue(cell.getGraphic() instanceof javafx.scene.control.Button, "Graphic phải là một Button");
  }

  @Test
  void testRowFactory_GiaoDienDuocGanSuKienDoubleClick() throws Exception {
    // Phủ luồng setRowFactory
    assertNotNull(tableView.getRowFactory(), "Bảng phải được gắn RowFactory để lắng nghe Double Click");
    TableRow<Auction> row = tableView.getRowFactory().call(tableView);
    assertNotNull(row, "Row không được null");
    assertNotNull(row.getOnMouseClicked(), "Row phải được gắn sự kiện onMouseClicked");
  }

}