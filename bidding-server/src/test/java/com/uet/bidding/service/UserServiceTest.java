package com.uet.bidding.service;

import com.uet.bidding.dao.UserSqlDAO;
import com.uet.bidding.exception.AuthenticationException;
import com.uet.bidding.exception.UserException;
import com.uet.bidding.model.Admin;
import com.uet.bidding.model.Customer;
import com.uet.bidding.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

  @Mock
  private UserSqlDAO mockUserDAO; // Tạo DAO giả lập để không bị gọi vào DB thật

  @InjectMocks
  private UserService userService; // Mockito sẽ tự động tiêm mockUserDAO vào đây

  private Customer validCustomer;

  @BeforeEach
  public void setUp() {
    validCustomer = new Customer(1, "t1win", "@1911", BigDecimal.ZERO);
  }

  // ==========================================
  // 1. KIỂM THỬ HÀM: registerCustomer
  // ==========================================

  @Test
  public void testRegisterCustomer_Success() throws UserException {
    // Thực thi đăng ký hợp lệ
    userService.registerCustomer("biddingUser", "securePassword123");

    // Kiểm tra xem tầng DAO có thực sự được gọi để lưu user xuống không
    verify(mockUserDAO, times(1)).addUser(any(Customer.class));
  }

  @Test
  public void testRegisterCustomer_InvalidUsername() {
    // Tên ngắn hơn 5 ký tự
    UserException exception = assertThrows(UserException.class, () -> {
      userService.registerCustomer("abc", "123456");
    });
    assertEquals("Tên đăng nhập phải có ít nhất 5 ký tự!", exception.getMessage());
  }

  @Test
  public void testRegisterCustomer_InvalidPassword() {
    // Mật khẩu ngắn hơn 6 ký tự
    UserException exception = assertThrows(UserException.class, () -> {
      userService.registerCustomer("validUser", "123");
    });
    assertEquals("Mật khẩu phải từ 6 ký tự trở lên!", exception.getMessage());
  }

  // ==========================================
  // 2. KIỂM THỬ HÀM: login
  // ==========================================

  @Test
  public void testLogin_Success() throws AuthenticationException {
    when(mockUserDAO.checkLogin("t1win", "@1911")).thenReturn(validCustomer);

    User loggedInUser = userService.login("t1win", "@1911");

    assertNotNull(loggedInUser);
    assertEquals("t1win", loggedInUser.getUsername());
  }

  @Test
  public void testLogin_WithEmptyFields() {
    // Test cả trường hợp null và rỗng
    assertThrows(AuthenticationException.class, () -> userService.login("", "pass"));
    assertThrows(AuthenticationException.class, () -> userService.login("user", null));
    assertThrows(AuthenticationException.class, () -> userService.login(null, ""));
  }

  @Test
  public void testLogin_UserBanned() throws Exception {
    validCustomer.setBanned(true); // Giả lập tài khoản bị admin khóa
    when(mockUserDAO.checkLogin("t1win", "@1911")).thenReturn(validCustomer);

    AuthenticationException exception = assertThrows(AuthenticationException.class, () -> {
      userService.login("t1win", "@1911");
    });
    assertEquals("Tài khoản của bạn đã bị khóa bởi quản trị viên!", exception.getMessage());
  }

  // ==========================================
  // 3. KIỂM THỬ HÀM: addBalance
  // ==========================================

  @Test
  public void testAddBalance_Success() throws UserException {
    userService.addBalance(validCustomer, BigDecimal.valueOf(500000));

    assertEquals(new BigDecimal("500000"), validCustomer.getBalance());
    verify(mockUserDAO, times(1)).updateProfile(validCustomer);
  }

  @Test
  public void testAddBalance_InvalidRole() {
    Admin admin = new Admin(9, "admin", "pass");
    UserException exception = assertThrows(UserException.class, () -> {
      userService.addBalance(admin, BigDecimal.valueOf(100));
    });
    assertEquals("Tài khoản này không hỗ trợ chức năng nạp tiền!", exception.getMessage());
  }

  @Test
  public void testAddBalance_NegativeOrNullAmount() {
    // Số tiền âm hoặc bằng 0
    assertThrows(UserException.class, () -> userService.addBalance(validCustomer, BigDecimal.valueOf(-100)));
    assertThrows(UserException.class, () -> userService.addBalance(validCustomer, BigDecimal.ZERO));
    assertThrows(UserException.class, () -> userService.addBalance(validCustomer, null));
  }

  // ==========================================
  // 4. KIỂM THỬ HÀM: updateCustomerProfile
  // ==========================================

  @Test
  public void testUpdateCustomerProfile_Success() throws UserException {
    validCustomer.setFullName("Nguyen Van A");
    validCustomer.setEmail("vna@gmail.com");
    validCustomer.setPhone("0912345678");
    validCustomer.setAddress("Hanoi, Vietnam");

    userService.updateCustomerProfile(validCustomer);

    assertTrue(validCustomer.isProfileComplete());
    verify(mockUserDAO, times(1)).updateProfile(validCustomer);
  }

  @Test
  public void testUpdateCustomerProfile_InvalidRole() {
    Admin admin = new Admin(9, "admin", "pass");
    assertThrows(UserException.class, () -> userService.updateCustomerProfile(admin));
  }

  @Test
  public void testUpdateCustomerProfile_ValidationFields() {
    // 1. Kiểm tra họ tên trống
    validCustomer.setFullName("");
    assertThrows(UserException.class, () -> userService.updateCustomerProfile(validCustomer));

    // 2. Kiểm tra email thiếu ký tự '@' hoặc rỗng
    validCustomer.setFullName("Valid Name");
    validCustomer.setEmail("invalid-email.com");
    assertThrows(UserException.class, () -> userService.updateCustomerProfile(validCustomer));

    // 3. Kiểm tra số điện thoại chứa chữ hoặc rỗng
    validCustomer.setEmail("test@gmail.com");
    validCustomer.setPhone("0987abc123");
    assertThrows(UserException.class, () -> userService.updateCustomerProfile(validCustomer));

    // 4. Kiểm tra địa chỉ rỗng
    validCustomer.setPhone("0987654321");
    validCustomer.setAddress("   ");
    assertThrows(UserException.class, () -> userService.updateCustomerProfile(validCustomer));
  }

  // ==========================================
  // 5. KIỂM THỬ HÀM: registerSellerProfile
  // ==========================================

  @Test
  public void testRegisterSellerProfile_Success() throws UserException {
    validCustomer.setProfileComplete(true); // Đã xong thông tin cá nhân

    userService.registerSellerProfile(validCustomer, "Shop UET", "Chuyên đồ điện tử");

    assertNotNull(validCustomer.getSellerProfile());
    assertEquals("Shop UET", validCustomer.getSellerProfile().getStoreName());
    verify(mockUserDAO, times(1)).updateProfile(validCustomer);
  }

  @Test
  public void testRegisterSellerProfile_InvalidRole() {
    Admin admin = new Admin(9, "admin", "pass");
    assertThrows(UserException.class, () -> userService.registerSellerProfile(admin, "Shop", "Desc"));
  }

  @Test
  public void testRegisterSellerProfile_WithoutCompleteProfile() {
    validCustomer.setProfileComplete(false);
    UserException exception = assertThrows(UserException.class, () -> {
      userService.registerSellerProfile(validCustomer, "MyShop", "Mô tả");
    });
    assertTrue(exception.getMessage().contains("Vui lòng cập nhật đầy đủ thông tin cá nhân"));
  }

  @Test
  public void testRegisterSellerProfile_EmptyStoreName() {
    validCustomer.setProfileComplete(true);
    assertThrows(UserException.class, () -> {
      userService.registerSellerProfile(validCustomer, "", "Mô tả");
    });
  }

  // ==========================================
  // 6. KIỂM THỬ HÀM: joinAuction
  // ==========================================

  @Test
  public void testJoinAuction_Success() throws UserException {
    validCustomer.setProfileComplete(true);
    int targetAuctionId = 105;

    userService.joinAuction(validCustomer, targetAuctionId);

    // Kiểm tra xem ID phiên đấu giá đã nằm trong danh sách đăng ký của bidder chưa
    assertTrue(validCustomer.getBidderProfile().isRegistered(targetAuctionId));
    verify(mockUserDAO, times(1)).updateProfile(validCustomer);
  }

  @Test
  public void testJoinAuction_InvalidRole() {
    Admin admin = new Admin(9, "admin", "pass");
    assertThrows(UserException.class, () -> userService.joinAuction(admin, 101));
  }

  @Test
  public void testJoinAuction_WithoutCompleteProfile() {
    validCustomer.setProfileComplete(false);
    UserException exception = assertThrows(UserException.class, () -> {
      userService.joinAuction(validCustomer, 101);
    });
    assertEquals("Vui lòng cập nhật thông tin cá nhân trước khi tham gia đấu giá!", exception.getMessage());
  }
}