package com.uet.bidding.model;

import com.uet.bidding.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AdminTest {

  private Admin admin;
  private User normalUser;
  private Admin anotherAdmin;

  /**
   * Hàm này sẽ tự động chạy TRƯỚC MỖI hàm @Test ở bên dưới.
   * Mục đích: Reset lại dữ liệu mới tinh, tránh việc test này làm ảnh hưởng đến dữ liệu của test khác.
   */
  @BeforeEach
  public void setUp() {
    admin = new Admin(1, "superadmin", "pass123");
    anotherAdmin = new Admin(2, "subadmin", "pass456");

    // Khởi tạo một User nặc danh đóng vai trò là Bidder
    normalUser = new User(10, "bidder01", "password") {
      @Override
      public String getRole() {
        return "BIDDER";
      }
    };
  }

  /**
   * Mục đích: Kiểm tra xem các hàm khởi tạo (Constructor) có tạo ra đúng đối tượng không,
   * và hàm getRole() có trả về đúng chữ "ADMIN" không.
   * Tác dụng: Tăng Code Coverage cho các dòng Constructor cơ bản.
   */
  @Test
  public void testAdminConstructorsAndRole() {
    Admin admin1 = new Admin();
    Admin admin2 = new Admin("adminName", "adminPass");

    assertEquals("ADMIN", admin.getRole(), "Role của Admin phải luôn là 'ADMIN'");
    assertEquals("superadmin", admin.getUsername());
  }

  /**
   * Mục đích: Kịch bản CHUẨN - Admin khóa một tài khoản User thông thường.
   * Kỳ vọng: Hàm banUser trả về true và trạng thái isBanned của User chuyển thành true.
   */
  @Test
  public void testBanUserSuccess() {
    assertFalse(normalUser.isBanned(), "Ban đầu user chưa bị khóa");

    boolean result = admin.banUser(normalUser);

    assertTrue(result, "Thao tác khóa phải thành công");
    assertTrue(normalUser.isBanned(), "Trạng thái của user phải đổi thành đã bị khóa");
  }

  /**
   * Mục đích: Kịch bản LỖI - Chạy vào nhánh if (user == null).
   * Kỳ vọng: Hàm trả về false thay vì ném ra lỗi NullPointerException làm sập chương trình.
   */
  @Test
  public void testBanUserWithNull() {
    boolean result = admin.banUser(null);
    assertFalse(result, "Không thể khóa một user null");
  }

  /**
   * Mục đích: Kiểm tra logic BẢO VỆ HỆ THỐNG - Admin không được phép khóa một Admin khác.
   * Kỳ vọng: Chạy vào nhánh if(user.getRole().equals("ADMIN")), hàm trả về false.
   */
  @Test
  public void testBanAnotherAdminShouldFail() {
    boolean result = admin.banUser(anotherAdmin);

    assertFalse(result, "Hệ thống phải chặn việc Admin khóa Admin khác");
    assertFalse(anotherAdmin.isBanned(), "Tài khoản Admin kia vẫn phải an toàn");
  }

  /**
   * Mục đích: Kịch bản CHUẨN - Admin mở khóa cho một tài khoản đang bị khóa.
   * Kỳ vọng: Trạng thái isBanned của User đổi ngược lại thành false.
   */
  @Test
  public void testUnbanUser() {
    normalUser.setBanned(true); // Cố tình khóa trước

    boolean result = admin.unbanUser(normalUser);

    assertTrue(result);
    assertFalse(normalUser.isBanned(), "User phải được gỡ khóa");
  }

  /**
   * Mục đích: Kịch bản LỖI - Mở khóa cho đối tượng null.
   * Kỳ vọng: Trả về false, không sập chương trình.
   */
  @Test
  public void testUnbanUserWithNull() {
    boolean result = admin.unbanUser(null);
    assertFalse(result);
  }

  /**
   * Mục đích: Kịch bản CHUẨN - Xóa một phiên đấu giá CHƯA BẮT ĐẦU (trạng thái OPEN).
   * Phụ thuộc: Sử dụng ItemFactory.createItemFromDb để giả lập dữ liệu chuẩn.
   * Kỳ vọng: Xóa thành công, danh sách đấu giá bị giảm đi 1 phần tử.
   */
  @Test
  public void testRemoveInvalidAuctionSuccess() {
    List<Auction> auctionList = new ArrayList<>();

    Item item = ItemFactory.createItemFromDb(
        "ELECTRONICS", 99, "Laptop Test", "Mô tả",
        BigDecimal.valueOf(100), "img.png", 1, "Dell", 12
    );

    // Trạng thái OPEN, giá hiện tại = giá sàn (100)
    Auction auction = new Auction(55, item, BigDecimal.valueOf(100),
        LocalDateTime.now(), LocalDateTime.now().plusHours(2), "OPEN");

    auctionList.add(auction);

    boolean isDeleted = admin.removeInvalidAuction(55, auctionList);

    assertTrue(isDeleted, "Phải cho phép xóa phiên OPEN");
    assertEquals(0, auctionList.size(), "Danh sách phải trống rỗng sau khi xóa");
  }

  /**
   * Mục đích: Kiểm tra logic NGHIỆP VỤ - Không cho phép Admin tùy tiện xóa phiên ĐANG CHẠY mà đã có người trả giá.
   * Kỳ vọng: Chạy vào nhánh if từ chối xóa, trả về false, danh sách vẫn giữ nguyên.
   */
  @Test
  public void testRemoveAuctionFailedWhenRunningWithBids() {
    List<Auction> auctionList = new ArrayList<>();

    Item item = ItemFactory.createItemFromDb(
        "ELECTRONICS", 99, "Laptop Test", "Mô tả",
        BigDecimal.valueOf(100), "img.png", 1, "Dell", 12
    );

    // Trạng thái RUNNING, giá hiện tại (150) đã vượt giá khởi điểm (100)
    Auction auction = new Auction(55, item, BigDecimal.valueOf(150),
        LocalDateTime.now(), LocalDateTime.now().plusHours(2), "RUNNING");

    auctionList.add(auction);

    boolean isDeleted = admin.removeInvalidAuction(55, auctionList);

    assertFalse(isDeleted, "Hệ thống phải chặn xóa phiên đang có người bid");
    assertEquals(1, auctionList.size(), "Phiên đấu giá không được phép bị mất");
  }

  /**
   * Mục đích: Kịch bản LỖI - Admin cố xóa một ID không hề tồn tại trong danh sách.
   * Kỳ vọng: Trả về false, không báo lỗi văng Exception.
   */
  @Test
  public void testRemoveAuctionNotFound() {
    List<Auction> auctionList = new ArrayList<>();
    boolean isDeleted = admin.removeInvalidAuction(999, auctionList);

    assertFalse(isDeleted, "Không tìm thấy ID thì phải trả về false");
  }
}