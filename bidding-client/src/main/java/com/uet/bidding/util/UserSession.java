package com.uet.bidding.util;

import com.uet.bidding.model.User;

public class UserSession {
  // Lưu trữ toàn bộ đối tượng User đang đăng nhập
  private static User currentUser;

  public static User getCurrentUser() {
    return currentUser;
  }

  public static void setCurrentUser(User user) {
    currentUser = user;
  }

  // Khi đăng xuất thì gọi hàm này
  public static void cleanSession() {
    currentUser = null;
  }
}