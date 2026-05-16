package com.uet.bidding.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class UserHierachyTest {
  /**
   * Mục đích: Kiểm tra các tính năng cơ bản của lớp trừu tượng User thông qua lớp con Customer.
   * Tác dụng: Phủ kín Coverage cho các hàm Getter/Setter, Constructor của lớp User.
   */
  @Test
  public void testAbstractUserFeatures() {
    // Sử dụng Constructor có ID của Customer để nạp dữ liệu lên lớp cha User
    Customer customer = new Customer(1, "t1win", "@1911", BigDecimal.valueOf(100.0));

    assertEquals(1, customer.getId(), "ID từ lớp cha Entity phải chính xác");
    assertEquals("t1win", customer.getUsername(), "Username từ lớp cha User phải chính xác");
    assertEquals("@1911", customer.getPassword(), "Password từ lớp cha User phải chính xác");
    assertFalse(customer.isBanned(), "Mặc định tài khoản mới không bị khóa");

    // Test các setter của lớp User để bào điểm Line Coverage
    customer.setUsername("new_username");
    customer.setPassword("new_password");
    customer.setBanned(true);
    customer.setRole("CUSTOMER");

    assertEquals("new_username", customer.getUsername());
    assertEquals("new_password", customer.getPassword());
    assertTrue(customer.isBanned());
  }

  /**
   * Mục đích: Kiểm tra các hàm nghiệp vụ tài chính và quản lý thông tin của riêng lớp Customer.
   * Tác dụng: Kiểm tra logic nạp/rút tiền (BigDecimal) và trạng thái hồ sơ cá nhân.
   */
  @Test
  public void testCustomerProfileAndFinancialLogic() {
    // Test Constructor không truyền ID
    Customer customer = new Customer("buyer01", "password", BigDecimal.valueOf(500.0));
    assertEquals("CUSTOMER", customer.getRole());
    assertEquals(BigDecimal.valueOf(500.0), customer.getBalance());

    // 1. Test logic kiểm tra thông tin đầy đủ (hasCompleteProfile)
    assertFalse(customer.hasCompleteProfile(), "Ban đầu các trường fullName, phone, address đang null");

    customer.setFullName("Nguyen Van A");
    customer.setPhone("0987654321");
    customer.setAddress("Hanoi");
    customer.setProfileComplete(true);

    assertTrue(customer.hasCompleteProfile(), "Hồ sơ đã điền đầy đủ thông tin");
    assertTrue(customer.isProfileComplete());

    // 2. Test logic nạp tiền (addFunds)
    customer.addFunds(BigDecimal.valueOf(200.0));
    assertEquals(BigDecimal.valueOf(700.0), customer.getBalance(), "500 + 200 = 700");

    // 3. Test logic rút tiền thành công (withdraw - đủ số dư)
    boolean withdrawSuccess = customer.withdraw(BigDecimal.valueOf(300.0));
    assertTrue(withdrawSuccess);
    assertEquals(BigDecimal.valueOf(400.0), customer.getBalance(), "700 - 300 = 400");

    // 4. Test logic rút tiền thất bại (withdraw - vượt quá số dư)
    boolean withdrawFail = customer.withdraw(BigDecimal.valueOf(1000.0));
    assertFalse(withdrawFail, "Không được phép rút quá số dư đang có");
    assertEquals(BigDecimal.valueOf(400.0), customer.getBalance(), "Số dư phải giữ nguyên");
  }

  /**
   * Mục đích: Kiểm tra profile Bidder được nhúng bên trong Customer và các hàm helper của nó.
   * Tác dụng: Phủ kín Coverage cho toàn bộ logic đăng ký/hoàn thành phiên đấu giá của lớp Bidder.
   */
  @Test
  public void testBidderProfileInsideCustomer() {
    Customer customer = new Customer(2, "bidder_test", "pass", BigDecimal.ZERO);

    // Lấy profile Bidder đã được khởi tạo tự động trong Constructor của Customer
    Bidder bidder = customer.getBidderProfile();
    assertNotNull(bidder, "Bidder Profile không được phép null nhờ hàm khởi tạo mặc định");

    // 1. Test tính năng đăng ký tham gia đấu giá (registerForAuction)
    bidder.registerForAuction(101);
    bidder.registerForAuction(101); // Cố tình đăng ký trùng lặp để kiểm tra nhánh if (!contains)
    bidder.registerForAuction(102);

    assertEquals(2, bidder.getRegisteredAuctionIds().size(), "Danh sách chỉ được chứa 2 ID duy nhất");
    assertTrue(bidder.getRegisteredAuctionIds().contains(101));

    // Test Setter để tăng điểm coverage dòng code
    bidder.setRegisteredAuctionIds(new ArrayList<>());
    assertEquals(0, bidder.getRegisteredAuctionIds().size());
    bidder.registerForAuction(101); // Đăng ký lại để dùng cho test tiếp theo

    // 2. Test tính năng hoàn thành đấu giá chuyển vào lịch sử (completeAuction)
    bidder.completeAuction(101);
    bidder.completeAuction(101); // Cố tình hoàn thành lại lần nữa để kiểm tra nhánh trùng lặp lịch sử

    assertFalse(bidder.getRegisteredAuctionIds().contains(101), "Phải xóa khỏi danh sách đang tham gia");
    assertTrue(bidder.getAuctionHistoryIds().contains(101), "Phải đẩy vào danh sách lịch sử");

    // Test nốt Setter của danh sách lịch sử
    bidder.setAuctionHistoryIds(new ArrayList<>());
    assertEquals(0, bidder.getAuctionHistoryIds().size());
  }

  /**
   * Mục đích: Kiểm tra profile Seller được nhúng bên trong Customer và các thuộc tính cửa hàng.
   * Tác dụng: Phủ kín Coverage cho các hàm quản lý kho hàng (inventory) và thông tin đánh giá của Seller.
   */
  @Test
  public void testSellerProfileInsideCustomer() {
    Customer customer = new Customer(3, "seller_test", "pass", BigDecimal.ZERO);

    Seller seller = customer.getSellerProfile();
    assertNotNull(seller);

    // 1. Test kiểm tra nhanh vai trò người bán (isActiveSeller)
    assertFalse(customer.isActiveSeller(), "Ban đầu chưa đặt tên shop nên chưa tính là active seller");

    seller.setStoreName("UET Bidding Shop");
    seller.setDescription("Cửa hàng công nghệ");
    seller.setSellerRating(4.8);

    assertTrue(customer.isActiveSeller(), "Đã đặt tên shop nên đã trở thành active seller");
    assertEquals("UET Bidding Shop", seller.getStoreName());
    assertEquals("Cửa hàng công nghệ", seller.getDescription());
    assertEquals(4.8, seller.getSellerRating(), 0.001);

    // 2. Test tính năng quản lý kho hàng (addItem)
    // Tạo một Item ẩn danh để làm mẫu thử mà không phụ thuộc vào Item thật
    Item mockItem = ItemFactory.createElectronics(
        "iPhone 15", "Máy mới", BigDecimal.valueOf(1000),
        "iphone.png", 10, "Apple", 12
    );

    seller.addItem(mockItem);
    assertEquals(1, seller.getInventory().size(), "Kho hàng phải tăng lên 1 sản phẩm");
    assertSame(mockItem, seller.getInventory().get(0));

    // 3. Test các danh sách trống mặc định để ăn điểm các hàm Getter còn lại
    assertNotNull(seller.getActiveAuctions());
    assertNotNull(seller.getFinishedAuctions());
    assertNull(seller.getReviews(), "Mặc định danh sách review chưa được khởi tạo trong Constructor");
  }
}
