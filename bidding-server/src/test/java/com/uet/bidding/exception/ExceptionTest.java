package com.uet.bidding.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExceptionTest {

  @Test
  public void testAuctionClosedException() {
    AuctionClosedException ex = new AuctionClosedException("Phiên đã đóng");
    assertEquals("Phiên đã đóng", ex.getMessage());
  }

  @Test
  public void testInvalidBidException() {
    InvalidBidException ex = new InvalidBidException("Giá đặt không hợp lệ");
    assertEquals("Giá đặt không hợp lệ", ex.getMessage());
  }

  @Test
  public void testUserException() {
    UserException ex = new UserException("Lỗi người dùng");
    assertEquals("Lỗi người dùng", ex.getMessage());
  }

  /**
   * Mục đích: Kiểm tra Custom Exception cho việc xác thực tài khoản (Đăng nhập/Đăng ký).
   * Kỳ vọng: Khởi tạo thành công với thông báo lỗi và không làm sập hệ thống.
   */
  @Test
  public void testAuthenticationException() {
    AuthenticationException ex = new AuthenticationException("Sai tên đăng nhập hoặc mật khẩu");
    assertEquals("Sai tên đăng nhập hoặc mật khẩu", ex.getMessage(), "Thông báo lỗi Authentication phải khớp");
  }

  /**
   * Mục đích: Kiểm tra Custom Exception liên quan đến các lỗi về sản phẩm (Item).
   * Kỳ vọng: Khởi tạo và giữ đúng thông điệp lỗi truyền vào.
   */
  @Test
  public void testItemException() {
    ItemException ex = new ItemException("Sản phẩm không hợp lệ hoặc đã tồn tại");
    assertEquals("Sản phẩm không hợp lệ hoặc đã tồn tại", ex.getMessage(), "Thông báo lỗi Item phải khớp");
  }
}