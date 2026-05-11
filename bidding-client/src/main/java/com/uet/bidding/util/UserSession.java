package com.uet.bidding.util;

import com.uet.bidding.model.Admin;
import com.uet.bidding.model.Customer;
import com.uet.bidding.model.User;

public class UserSession {
  // Biến static lưu trữ người dùng đang đăng nhập (có thể là Admin hoặc Customer)
  private static User currentUser;

  // --- CÁC HÀM CƠ BẢN ---
  public static void setCurrentUser(User user) {
    currentUser = user;
  }

  public static User getCurrentUser() {
    return currentUser;
  }

  public static boolean isLoggedIn() {
    return currentUser != null;
  }

  /**
   * Xóa sạch session khi đăng xuất
   */
  public static void clear() {
    currentUser = null;
  }

  // --- CÁC HÀM HELPER (Cực kỳ hữu ích để code UI nhanh hơn) ---
  public static Customer getLoggedInCustomer() {
    // Nếu currentUser là Customer, Java tự ép kiểu sang biến 'c' và trả về luôn
    if (currentUser instanceof Customer c) {
      return c;
    }
    return null; // Trả về null nếu là Admin hoặc chưa đăng nhập
  }

  public static Admin getLoggedInAdmin() {
    if (currentUser instanceof Admin a) {
      return a;
    }
    return null;
  }
}