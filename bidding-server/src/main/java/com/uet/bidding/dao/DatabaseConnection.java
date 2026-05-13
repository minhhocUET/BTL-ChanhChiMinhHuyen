package com.uet.bidding.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
  // 1. Lưu ý: Hãy đổi chữ 'sys' thành tên database chứa các bảng ERD của bạn (ví dụ: bidding_db)
  private static final String URL = "jdbc:mysql://gateway01.ap-southeast-1.prod.aws.tidbcloud.com:4000/AuctionDB?sslMode=VERIFY_IDENTITY";

  // 2. Giữ nguyên tài khoản và mật khẩu
  private static final String USER = "PqCPN4Nmo9KQw8B.root";
  private static final String PASSWORD = "gxryxCrn9Xa7Hysb";

  public static Connection getConnection() throws SQLException {
    try {
      // Đảm bảo đã load Driver (Dành cho các bản MySQL Connector cũ, bản mới có thể bỏ qua)
      Class.forName("com.mysql.cj.jdbc.Driver");
    } catch (ClassNotFoundException e) {
      System.err.println("Không tìm thấy MySQL JDBC Driver!");
      e.printStackTrace();
    }

    // 3. TRUYỀN 3 THAM SỐ VÀO HÀM GET CONNECTION
    return DriverManager.getConnection(URL, USER, PASSWORD);
  }
}