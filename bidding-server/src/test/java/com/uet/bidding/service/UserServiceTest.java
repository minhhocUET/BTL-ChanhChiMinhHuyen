package com.uet.bidding.service;

import com.uet.bidding.exception.AuthenticationException;
import com.uet.bidding.exception.UserException;
import com.uet.bidding.model.Admin;
import com.uet.bidding.model.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class UserServiceTest {

  private UserService userService;
  private Customer validCustomer;

  @BeforeEach
  public void setUp() {
    userService = new UserService();
    validCustomer = new Customer(1, "t1win", "@1911", BigDecimal.ZERO);
  }

  /**
   * Mục đích: Kiểm tra chặn đăng ký với tên quá ngắn.
   */
  @Test
  public void testRegisterCustomerInvalidUsername() {
    assertThrows(UserException.class, () -> {
      userService.registerCustomer("abc", "123456");
    });
  }

  /**
   * Mục đích: Kiểm tra bắt lỗi nạp tiền với số âm.
   */
  @Test
  public void testAddBalanceNegative() {
    assertThrows(UserException.class, () -> {
      userService.addBalance(validCustomer, BigDecimal.valueOf(-100));
    });
  }

  /**
   * Mục đích: Kiểm tra bắt lỗi nạp tiền sai vai trò (VD: Admin không có ví tiền).
   */
  @Test
  public void testAddBalanceInvalidRole() {
    Admin admin = new Admin(9, "admin", "pass");
    assertThrows(UserException.class, () -> {
      userService.addBalance(admin, BigDecimal.valueOf(100));
    });
  }

  /**
   * Mục đích: Kiểm tra rào cản - Chưa điền đủ hồ sơ thì không được mở Shop bán hàng.
   */
  @Test
  public void testRegisterSellerProfileWithoutCompleteProfile() {
    validCustomer.setProfileComplete(false);
    assertThrows(UserException.class, () -> {
      userService.registerSellerProfile(validCustomer, "MyShop", "Mô tả");
    });
  }

  /**
   * Mục đích: Kiểm tra login khi tài khoản/mật khẩu bị bỏ trống.
   */
  @Test
  public void testLoginWithEmptyFields() {
    assertThrows(AuthenticationException.class, () -> {
      userService.login("", null);
    });
  }
}