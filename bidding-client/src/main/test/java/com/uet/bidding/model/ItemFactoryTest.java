package com.uet.bidding.model;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ItemFactoryTest {

  private final Gson gson = GsonFactory.getInstance(); // Tận dụng GsonFactory sẵn có của bạn

  /**
   * Mục đích: Kiểm tra hàm tạo sản phẩm Điện tử (createElectronics) dành cho Client.
   */
  @Test
  public void testCreateElectronics() {
    Item item = ItemFactory.createElectronics(
        "iPhone 15", "Máy mới", BigDecimal.valueOf(1000),
        "iphone.png", 10, "Apple", 12
    );

    assertNotNull(item, "Sản phẩm điện tử tạo ra không được null");
    assertTrue(item instanceof Electronics, "Sản phẩm phải thuộc lớp con Electronics");
    assertEquals("iPhone 15", item.getName());
    assertEquals(BigDecimal.valueOf(1000), item.getStartingPrice());
  }

  /**
   * Mục đích: Kiểm tra hàm tạo sản phẩm Nghệ thuật (createArt) dành cho Client.
   */
  @Test
  public void testCreateArt() {
    Item item = ItemFactory.createArt(
        "Mona Lisa", "Tranh dầu", BigDecimal.valueOf(5000),
        "mona.png", 11, "Da Vinci", 1503, "Dầu trên gỗ"
    );

    assertNotNull(item);
    assertTrue(item instanceof Art, "Sản phẩm phải thuộc lớp con Art");
    assertEquals("Mona Lisa", item.getName());
  }

  /**
   * Mục đích: Kiểm tra hàm tạo Xe cộ (createVehicle) khớp với Constructor 11 tham số.
   */
  @Test
  public void testCreateVehicle() {
    Item item = ItemFactory.createVehicle(
        "Civic", "Xe cũ", BigDecimal.valueOf(20000), "civic.png", 12,
        "Honda", "2022", 2022, 15000.0, "VTEC", "Xăng"
    );

    assertNotNull(item);
    assertTrue(item instanceof Vehicle, "Sản phẩm phải thuộc lớp con Vehicle");
    assertEquals("Civic", item.getName());
  }

  /**
   * Mục đích: Kiểm tra nhánh "ELECTRONICS" trong hàm switch-case nạp từ Database (createItemFromDb).
   */
  @Test
  public void testCreateItemFromDbElectronics() {
    Item item = ItemFactory.createItemFromDb(
        "ELECTRONICS", 101, "Sony TV", "4K TV",
        BigDecimal.valueOf(800), "tv.png", 1, "Sony", 24
    );

    assertNotNull(item);
    assertEquals(101, item.getId(), "ID nạp từ DB phải chính xác");
    assertTrue(item instanceof Electronics);
  }

  /**
   * Mục đích: Kiểm tra nhánh "ART" trong hàm switch-case nạp từ Database.
   */
  @Test
  public void testCreateItemFromDbArt() {
    Item item = ItemFactory.createItemFromDb(
        "ART", 102, "Bình gốm", "Gốm cổ",
        BigDecimal.valueOf(1500), "binh.png", 2, "Ẩn danh", 1800, "Đất sét"
    );

    assertNotNull(item);
    assertEquals(102, item.getId());
    assertTrue(item instanceof Art);
  }

  /**
   * Mục đích: Kiểm tra nhánh "VEHICLE" trong hàm switch-case nạp từ Database.
   */
  @Test
  public void testCreateItemFromDbVehicle() {
    Item item = ItemFactory.createItemFromDb(
        "vehicle", 103, "VinFast VF8", "Xe điện",
        BigDecimal.valueOf(40000), "vf8.png", 3, "VinFast", "VF8", 2023, 5000.0, "Electric", "Pin"
    );

    assertNotNull(item);
    assertEquals(103, item.getId());
    assertTrue(item instanceof Vehicle);
  }

  /**
   * Mục đích: Kiểm tra xử lý chuỗi chữ thường/chữ hoa của tham số type (type.toUpperCase()).
   */
  @Test
  public void testCreateItemFromDbCaseInsensitive() {
    Item item = ItemFactory.createItemFromDb(
        "electronics", 200, "Tai nghe", "Mô tả",
        BigDecimal.valueOf(50), "airpods.png", 5, "Apple", 12
    );
    assertNotNull(item);
    assertTrue(item instanceof Electronics);
  }

  /**
   * Mục đích: Kịch bản LỖI - Kiểm tra nhánh bảo vệ khi type truyền vào bị null.
   */
  @Test
  public void testCreateItemFromDbNullType() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      ItemFactory.createItemFromDb(null, 1, "Tên", "Mô tả", BigDecimal.TEN, "img.png", 1);
    });

    assertEquals("Type cannot be null", exception.getMessage());
  }

  /**
   * Mục đích: Kịch bản LỖI - Người dùng truyền vào một loại sản phẩm lạ lẫm không nằm trong hệ thống (nhánh default).
   */
  @Test
  public void testCreateItemFromDbUnknownType() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      ItemFactory.createItemFromDb("FURNITURE", 1, "Bàn gỗ", "Mô tả", BigDecimal.TEN, "img.png", 1);
    });

    assertTrue(exception.getMessage().contains("Loại sản phẩm không xác định"));
  }

  // =========================================================================
  // 🔥 PHẦN THÊM MỚI: QUÉT SẠCH TOÀN BỘ LOGIC JSON PARSING (ĂN TRỌN 52 DÒNG THIẾU)
  // =========================================================================

  /**
   * 1. Test phân tích cú pháp chuỗi JSON rỗng/lỗi hoặc trả về mảng null
   */
  @Test
  public void testParseItemsFromJson_NullOrEmptyArray() {
    List<Item> result = ItemFactory.parseItemsFromJson("[]", gson);
    assertTrue(result.isEmpty());

    // Nếu el không phải JsonObject (ví dụ mảng chứa chuỗi nguyên bản thay vì object)
    List<Item> mixedResult = ItemFactory.parseItemsFromJson("[\"not_an_object\"]", gson);
    assertTrue(mixedResult.isEmpty());
  }

  /**
   * 2. Test parse thành công ELECTRONICS từ JSON với đầy đủ các trường bổ sung nâng cao
   * Kiểm tra bọc lót: `itemType`, `city`, `imageData`, `status`, `rejectionReason`, `inAuction` (dạng camelCase)
   */
  @Test
  public void testParseItemsFromJson_ElectronicsFullFields() {
    String json = "[" +
        "{" +
        "  \"itemType\": \"ELECTRONICS\"," +
        "  \"id\": 501," +
        "  \"name\": \"Samsung S24\"," +
        "  \"description\": \"Chính hãng\"," +
        "  \"startingPrice\": 1200.50," +
        "  \"imagePath\": \"s24.jpg\"," +
        "  \"sellerId\": 99," +
        "  \"brand\": \"Samsung\"," +
        "  \"warrantyMonths\": 12," +
        "  \"city\": \"Hanoi\"," +
        "  \"imageData\": \"base64_string_here\"," +
        "  \"status\": \"APPROVED\"," +
        "  \"rejectionReason\": \"None\"," +
        "  \"inAuction\": true" +
        "}" +
        "]";

    List<Item> items = ItemFactory.parseItemsFromJson(json, gson);
    assertEquals(1, items.size());
    Item item = items.get(0);

    assertTrue(item instanceof Electronics);
    assertEquals(501, item.getId());
    assertEquals("Samsung S24", item.getName());
    assertEquals(new BigDecimal("1200.50"), item.getStartingPrice());
    assertEquals("Hanoi", item.getCity());
    assertEquals("base64_string_here", item.getImageData());
    assertEquals("APPROVED", item.getStatus());
    assertEquals("None", item.getRejectionReason());
    assertTrue(item.isInAuction());
  }

  /**
   * 3. Test parse thành công ART & VEHICLE từ JSON với cơ chế dự phòng biến "type"
   * Đồng thời test fallback gán giá trị mặc định khi JSON khuyết các trường cơ bản (has và isJsonNull)
   */
  @Test
  public void testParseItemsFromJson_ArtAndVehicleFallbackNulls() {
    // Chuỗi JSON chứa 1 object ART dùng key "type" thay vì "itemType" và cố tình truyền null ở một số trường
    // Chuỗi JSON chứa thêm 1 object VEHICLE sử dụng cấu trúc "in_auction" dạng snake_case
    String json = "[" +
        "{" +
        "  \"type\": \"ART\"," +
        "  \"id\": null," +
        "  \"name\": null," +
        "  \"description\": null," +
        "  \"startingPrice\": null," +
        "  \"imagePath\": null," +
        "  \"sellerId\": null," +
        "  \"author\": null," +
        "  \"creationYear\": null," +
        "  \"material\": null" +
        "}," +
        "{" +
        "  \"itemType\": \"VEHICLE\"," +
        "  \"brand\": null," +
        "  \"model\": null," +
        "  \"manufacturingYear\": null," +
        "  \"mileage\": null," +
        "  \"engineType\": null," +
        "  \"fuelType\": null," +
        "  \"in_auction\": false" + // Thử nghiệm nhánh đọc "in_auction" rẽ nhánh của bạn
        "}" +
        "]";

    List<Item> items = ItemFactory.parseItemsFromJson(json, gson);
    assertEquals(2, items.size());

    // Khảo sát phần tử 1: ART nạp null -> Hệ thống phải tự gán giá trị mặc định an toàn
    Item artItem = items.get(0);
    assertTrue(artItem instanceof Art);
    assertEquals(0, artItem.getId());
    assertEquals("", artItem.getName());
    assertEquals(BigDecimal.ZERO, artItem.getStartingPrice());
    assertEquals("PENDING", artItem.getStatus()); // Mặc định do JSON thiếu trường status

    // Khảo sát phần tử 2: VEHICLE
    Item vehicleItem = items.get(1);
    assertTrue(vehicleItem instanceof Vehicle);
    assertFalse(vehicleItem.isInAuction());
  }

  /**
   * 4. Test cơ chế TỰ ĐOÁN KIỂU DỮ LIỆU (Fallback Detection) khi JSON hoàn toàn không gửi kèm "itemType" hay "type"
   */
  @Test
  public void testDetectItemType_FallbackMechanism() {
    String json = "[" +
        "  { \"warrantyMonths\": 24, \"name\": \"TV\" }," +                         // Phải tự đoán ra ELECTRONICS
        "  { \"author\": \"Picasso\", \"name\": \"Tranh cổ\" }," +                  // Phải tự đoán ra ART
        "  { \"engineType\": \"V8\", \"name\": \"Siêu xe Mustang\" }" +             // Phải tự đoán ra VEHICLE
        "]";

    List<Item> items = ItemFactory.parseItemsFromJson(json, gson);
    assertEquals(3, items.size());
    assertTrue(items.get(0) instanceof Electronics);
    assertTrue(items.get(1) instanceof Art);
    assertTrue(items.get(2) instanceof Vehicle);
  }

  /**
   * 5. Kịch bản LỖI - JSON không gửi type định danh và cũng không có bất kỳ trường đặc trưng nào để đoán kiểu
   */
  @Test
  public void testDetectItemType_CannotDetermine() {
    String json = "[{ \"weight\": \"10kg\", \"color\": \"Red\" }]"; // Không có manh mối nào để đoán loại Item

    assertThrows(IllegalArgumentException.class, () -> {
      ItemFactory.parseItemsFromJson(json, gson);
    });
  }

  /**
   * 6. Kịch bản LỖI - Định danh loại sản phẩm hợp lệ ở bước đoán nhưng switch-case khởi tạo gặp lỗi loại sản phẩm không xác định
   */
  @Test
  public void testParseItemFromJsonObject_UnknownTypeSwitch() {
    // Ép kiểu thủ công qua JsonObject để gọi nội hàm kiểm tra ngoại lệ rẽ nhánh
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty("itemType", "UNKNOWN_PRODUCT_TYPE");

    assertThrows(IllegalArgumentException.class, () -> {
      // Vì parseItemFromJsonObject là hàm private, ta đẩy qua hàm public bọc nó để test gián tiếp
      JsonArray array = new JsonArray();
      array.add(jsonObject);
      ItemFactory.parseItemsFromJson(gson.toJson(array), gson);
    });
  }
}