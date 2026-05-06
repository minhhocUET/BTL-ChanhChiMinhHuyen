package com.uet.bidding.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
  // 1. Lưu ý: Hãy đổi chữ 'sys' thành tên database chứa các bảng ERD của bạn (ví dụ: bidding_db)
  private static final String URL = "jdbc:mysql://gateway01.ap-southeast-1.prod.aws.tidbcloud.com:4000/bidding_system?sslMode=VERIFY_IDENTITY";

  // 2. Giữ nguyên tài khoản và mật khẩu
  private static final String USER = "PqCPN4Nmo9KQw8B.root";
  private static final String PASSWORD = "gxryxCrn9Xa7Hysb";

  public static Connection getConnection() throws SQLException {
    try {
      // 3. Khai báo rõ việc sử dụng Driver của MySQL (vì TiDB dùng chung Driver này)
      Class.forName("com.mysql.cj.jdbc.Driver");

      // 4. Thực hiện kết nối
      return DriverManager.getConnection(URL, USER, PASSWORD);

    } catch (ClassNotFoundException e) {
      throw new SQLException("LỖI: Chưa thêm thư viện mysql-connector-j vào project!", e);
    }
  }
}