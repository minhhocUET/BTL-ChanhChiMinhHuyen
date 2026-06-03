package com.uet.bidding.model;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class ExtraModelTest {

  /**
   * 1. Phủ kín 100% class Bid
   * Lưu ý: Bid chỉ có Constructor All-args và Getter, không có Setter.
   */
  @Test
  public void testBidClass() {
    Bidder mockBidder = new Bidder();
    LocalDateTime now = LocalDateTime.now();
    BigDecimal amount = BigDecimal.valueOf(500.0);

    Bid bid = new Bid(mockBidder, amount, now);

    assertEquals(mockBidder, bid.getBidder());
    assertEquals(amount, bid.getAmount());
    assertEquals(now, bid.getTime());
  }

  /**
   * 2. Phủ kín 100% class Review
   * Phải test cả 2 trường hợp của hàm getReviewerName() để lấy 100% Branch Coverage.
   */
  @Test
  public void testReviewClass() {
    // Test Constructor có tham số
    Customer reviewer = new Customer();
    reviewer.setFullName("Nguyen Van A");

    Review review = new Review(101, 5, reviewer, 5, "Tuyệt vời");
    review.setId(1);

    // Test Getter
    assertEquals(1, review.getId());
    assertEquals(101, review.getAuctionId());
    assertEquals(5, review.getSellerId());
    assertEquals(5, review.getStars());
    assertEquals("Tuyệt vời", review.getComment());
    assertEquals(reviewer, review.getReviewer());
    assertNotNull(review.getCreatedAt());

    // Test nhánh True của getReviewerName
    assertEquals("Nguyen Van A", review.getReviewerName());

    // Test Constructor rỗng và Setter, nhánh False của getReviewerName
    Review emptyReview = new Review();
    emptyReview.setAuctionId(102);
    emptyReview.setSellerId(6);
    emptyReview.setStars(4);
    emptyReview.setComment("Khá tốt");
    emptyReview.setReviewer(null); // Cố tình set null
    LocalDateTime time = LocalDateTime.now();
    emptyReview.setCreatedAt(time);

    assertEquals(102, emptyReview.getAuctionId());
    assertEquals(time, emptyReview.getCreatedAt());
    // Khi reviewer bị null, hàm phải trả về "Unknown"
    assertEquals("Unknown", emptyReview.getReviewerName());
  }

  /**
   * 3. Phủ kín 100% class Transaction
   * Phủ luôn cả hàm toString()
   */
  @Test
  public void testTransactionClass() {
    // Dùng null cho Enum để test không bị phụ thuộc nếu Enum của bạn thay đổi
    Transaction t = new Transaction(1, 101, BigDecimal.valueOf(1000), null, null);
    t.setId(99);

    assertEquals(99, t.getId());
    assertEquals(1, t.getUserId());
    assertEquals(101, t.getAuctionId());
    assertEquals(BigDecimal.valueOf(1000), t.getAmount());
    assertNull(t.getType());
    assertNull(t.getStatus());
    assertNotNull(t.getCreatedAt());

    // Test Constructor rỗng và Setter
    Transaction emptyT = new Transaction();
    emptyT.setUserId(2);
    emptyT.setAuctionId(102);
    emptyT.setAmount(BigDecimal.TEN);
    // Gọi hàm toString để JaCoCo tính điểm dòng lệnh đó
    assertNotNull(emptyT.toString());
    assertTrue(emptyT.toString().contains("userId=2"));
  }

  /**
   * 4. Phủ kín 100% class NetworkMessage
   */
  @Test
  public void testNetworkMessageClass() {
    NetworkMessage msg = new NetworkMessage("LOGIN", "data_chuoi");
    msg.setRequestId("REQ_123");

    assertEquals("LOGIN", msg.getType());
    assertEquals("data_chuoi", msg.getData());
    assertEquals("REQ_123", msg.getRequestId());

    // Test Setter
    msg.setType("LOGOUT");
    msg.setData(null);
    assertEquals("LOGOUT", msg.getType());

    // Quét qua hàm toString
    assertNotNull(msg.toString());
    assertTrue(msg.toString().contains("REQ_123"));
  }

  /**
   * 5. Phủ kín 100% GsonFactory (Singleton & Logic Đa hình, Chuyển đổi DateTime)
   */
  @Test
  public void testGsonFactorySingletonAndAllLogics() {
    // ---- ĐOẠN 1: KIỂM TRA SINGLETON (Giữ nguyên logic cũ của bạn) ----
    Gson gson1 = GsonFactory.getInstance();
    assertNotNull(gson1);
    Gson gson2 = GsonFactory.getInstance();
    assertSame(gson1, gson2);

    // ---- ĐOẠN 2: KIỂM TRA CÁC HÀM TIỆN ÍCH STATIC (toJson, fromJson) ----
    String sampleJson = "{\"fullName\":\"Test Static\"}";
    Customer staticCustomer = GsonFactory.fromJson(sampleJson, Customer.class);
    assertNotNull(staticCustomer);
    assertEquals("Test Static", staticCustomer.getFullName());

    String outputJson = GsonFactory.toJson(staticCustomer);
    assertTrue(outputJson.contains("Test Static"));

    java.lang.reflect.Type customerType = Customer.class;
    Customer typeCustomer = GsonFactory.fromJson(sampleJson, customerType);
    assertNotNull(typeCustomer);

    // ---- ĐOẠN 3: KIỂM TRA BỘ KHỬ TUẦN TỰ ĐA HÌNH ITEM (itemDeserializer) ----
    // Nhánh 3.1: Đọc bằng key "type" -> Lớp con ART
    String jsonArt = "{\"type\":\"ART\",\"author\":\"Picasso\"}";
    Item itemArt = GsonFactory.fromJson(jsonArt, Item.class);
    assertTrue(itemArt instanceof Art);

    // Nhánh 3.2: Đọc bằng key "item_type" -> Lớp con ELECTRONICS
    String jsonElec = "{\"item_type\":\"ELECTRONICS\",\"brand\":\"Sony\"}";
    Item itemElec = GsonFactory.fromJson(jsonElec, Item.class);
    assertTrue(itemElec instanceof Electronics);

    // Nhánh 3.3: Đọc bằng key "itemType" -> Lớp con VEHICLE
    String jsonVehicle = "{\"itemType\":\"VEHICLE\",\"brand\":\"Toyota\"}";
    Item itemVehicle = GsonFactory.fromJson(jsonVehicle, Item.class);
    assertTrue(itemVehicle instanceof Vehicle);

    // Nhánh 3.4: LỖI - Không tìm thấy thuộc tính phân loại sản phẩm
    assertThrows(JsonParseException.class, () -> {
      GsonFactory.fromJson("{\"name\":\"Vo Danh\"}", Item.class);
    });

    // Nhánh 3.5: LỖI - Loại sản phẩm không hợp lệ (nhánh default)
    assertThrows(JsonParseException.class, () -> {
      GsonFactory.fromJson("{\"type\":\"FOOD\"}", Item.class);
    });

    // ---- ĐOẠN 4: KIỂM TRA BỘ KHỬ TUẦN TỰ ĐA HÌNH USER (userDeserializer) ----
    // Nhánh 4.1: Chứa role "ADMIN" -> Trả về lớp Admin
    String jsonAdmin = "{\"username\":\"boss\",\"role\":\"ADMIN\"}";
    User userAdmin = GsonFactory.fromJson(jsonAdmin, User.class);
    assertTrue(userAdmin instanceof Admin);

    // Nhánh 4.2: Không chứa role hoặc role khác ADMIN -> Mặc định trả về Customer
    String jsonCustomer = "{\"username\":\"buyer\",\"role\":\"CUSTOMER\"}";
    User userCust = GsonFactory.fromJson(jsonCustomer, User.class);
    assertTrue(userCust instanceof Customer);

    String jsonNoRole = "{\"username\":\"guest\"}";
    User userNoRole = GsonFactory.fromJson(jsonNoRole, User.class);
    assertTrue(userNoRole instanceof Customer);

    // ---- ĐOẠN 5: KIỂM TRA XỬ LÝ LOCALDATETIME BẤT TỬ ----
    // Nhánh 5.1: Serialize LocalDateTime sang định dạng ISO chuẩn
    LocalDateTime testDateTime = LocalDateTime.of(2026, 6, 3, 15, 30, 0);
    JsonObject dateContainer = new JsonObject();
    dateContainer.add("time", GsonFactory.getInstance().toJsonTree(testDateTime));
    assertTrue(dateContainer.get("time").getAsString().contains("2026-06-03T15:30:00"));

    // Nhánh 5.2: Deserializer - Xử lý dấu cách SQL biến đổi thành chữ 'T'
    String sqlJsonTime = "\"2026-06-03 15:30:00\"";
    LocalDateTime parsedSqlTime = GsonFactory.fromJson(sqlJsonTime, LocalDateTime.class);
    assertEquals(testDateTime, parsedSqlTime);

    // Nhánh 5.3: Deserializer LỖI DỰ PHÒNG - Chuỗi dị thường lỗi nặng ở đuôi (Buộc phải nhảy vào catch để substring 19 ký tự đầu)
    String longJsonTime = "\"2026-06-03T15:30:00_LOI_DU_LIEU_SQL_O_DAY\"";
    LocalDateTime parsedLongTime = GsonFactory.fromJson(longJsonTime, LocalDateTime.class);
    assertEquals(testDateTime, parsedLongTime); // Lần này chắc chắn sẽ bằng nhau vì đã bị ép cắt mất đuôi lỗi và nano giây bằng 0

    // ---- ĐOẠN 6: KIỂM TRA XỬ LÝ LOCALDATE ----
    LocalDate testDate = LocalDate.of(2026, 6, 3);
    String jsonDate = GsonFactory.toJson(testDate);
    assertEquals("\"2026-06-03\"", jsonDate);

    LocalDate parsedDate = GsonFactory.fromJson(jsonDate, LocalDate.class);
    assertEquals(testDate, parsedDate);
  }

  // ========================================================
  // 🎯 PHỦ KÍN 100% CLASS: AutoBid (Khớp chính xác với Model)
  // ========================================================
  @Test
  public void testAutoBidClass() {
    // 1. Kiểm thử Constructor có tham số để bao phủ logic khởi tạo thời gian mặc định
    BigDecimal maxBidAmount = BigDecimal.valueOf(150000.0);
    AutoBid autoBidWithArgs = new AutoBid(101, 202, maxBidAmount, true);

    assertEquals(101, autoBidWithArgs.getAuctionId());
    assertEquals(202, autoBidWithArgs.getBidderId());
    assertEquals(maxBidAmount, autoBidWithArgs.getMaxBid());
    assertTrue(autoBidWithArgs.isActive());
    assertNotNull(autoBidWithArgs.getCreatedAt());
    assertNotNull(autoBidWithArgs.getUpdatedAt());

    // 2. Kiểm thử Constructor rỗng kết hợp với đầy đủ các hàm Setter/Getter còn lại
    AutoBid autoBidEmpty = new AutoBid();
    LocalDateTime fakeTime = LocalDateTime.now().minusDays(1);

    autoBidEmpty.setId(77);
    autoBidEmpty.setAuctionId(555);
    autoBidEmpty.setBidderId(999);
    autoBidEmpty.setMaxBid(BigDecimal.valueOf(300000.0));
    autoBidEmpty.setActive(false);
    autoBidEmpty.setCreatedAt(fakeTime);
    autoBidEmpty.setUpdatedAt(fakeTime);

    // Assert kiểm chứng toàn bộ dữ liệu vừa set
    assertEquals(77, autoBidEmpty.getId());
    assertEquals(555, autoBidEmpty.getAuctionId());
    assertEquals(999, autoBidEmpty.getBidderId());
    assertEquals(BigDecimal.valueOf(300000.0), autoBidEmpty.getMaxBid());
    assertFalse(autoBidEmpty.isActive());
    assertEquals(fakeTime, autoBidEmpty.getCreatedAt());
    assertEquals(fakeTime, autoBidEmpty.getUpdatedAt());

    // 3. Quét qua hàm toString phòng hờ nếu sau này bạn sinh tự động (auto-generate) hàm toString
    try {
      assertNotNull(autoBidEmpty.toString());
    } catch (Exception ignored) {}
  }

  // ========================================================
  // 🎯 PHỦ KÍN 100% CLASS: AuctionRegistration (Khớp chính xác với Model)
  // ========================================================
  @Test
  public void testAuctionRegistrationClass() {
    // 1. Kiểm thử Constructor có tham số để bao phủ logic khởi tạo thời gian tự động
    AuctionRegistration regWithArgs = new AuctionRegistration(105, 302);

    assertEquals(105, regWithArgs.getAuctionId());
    assertEquals(302, regWithArgs.getBidderId());
    assertNotNull(regWithArgs.getRegisteredAt());

    // 2. Kiểm thử Constructor rỗng kết hợp với đầy đủ các hàm Setter/Getter
    AuctionRegistration regEmpty = new AuctionRegistration();
    LocalDateTime fakeTime = LocalDateTime.now().minusHours(2);

    regEmpty.setAuctionId(888);
    regEmpty.setBidderId(999);
    regEmpty.setRegisteredAt(fakeTime);

    // Kiểm chứng toàn bộ dữ liệu vừa set qua getter
    assertEquals(888, regEmpty.getAuctionId());
    assertEquals(999, regEmpty.getBidderId());
    assertEquals(fakeTime, regEmpty.getRegisteredAt());

    // 3. Quét qua hàm toString phòng hờ nếu bạn có sinh tự động hàm này
    try {
      assertNotNull(regEmpty.toString());
    } catch (Exception ignored) {}
  }
}