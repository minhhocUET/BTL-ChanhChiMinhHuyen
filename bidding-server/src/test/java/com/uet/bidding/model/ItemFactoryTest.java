package com.uet.bidding.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class ItemFactoryTest {

  /**
   * Mục đích: Kiểm tra hàm tạo sản phẩm Điện tử (createElectronics) dành cho Client.
   * Kỳ vọng: Đối tượng trả về không null, thuộc đúng lớp con Electronics và lưu đúng các thuộc tính đặc trưng (Brand, Warranty).
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
   * Kỳ vọng: Đối tượng thuộc lớp con Art và chứa đúng thông tin Tác giả, Năm sáng tác, Chất liệu.
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
   * Kỳ vọng: Đối tượng thuộc lớp con Vehicle và lưu chính xác các thông số xe.
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
   * Kỳ vọng: Ép kiểu thành công mảng extra args sang String (Brand) và Integer (Warranty).
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
   * Kỳ vọng: Hàm phân tách đúng các tham số phụ (extra) đặc trưng của đồ mỹ thuật.
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
   * Kỳ vọng: Đọc m mượt mà cấu trúc varargs phức tạp (gồm String, Integer, Double) để dựng đối tượng Vehicle.
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
   * Kỳ vọng: Truyền vào "electronics" (chữ thường) thì hệ thống vẫn nhận diện đúng và không bị lỗi.
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
   * Kỳ vọng: Ném ra lỗi `IllegalArgumentException` kèm thông điệp "Type cannot be null".
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
   * Kỳ vọng: Ném ra lỗi `IllegalArgumentException` kèm thông báo loại sản phẩm không xác định.
   */
  @Test
  public void testCreateItemFromDbUnknownType() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      ItemFactory.createItemFromDb("FURNITURE", 1, "Bàn gỗ", "Mô tả", BigDecimal.TEN, "img.png", 1);
    });

    assertTrue(exception.getMessage().contains("Loại sản phẩm không xác định"));
  }
}