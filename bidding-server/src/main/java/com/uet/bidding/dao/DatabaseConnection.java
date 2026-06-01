package com.uet.bidding.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

  public static Connection getConnection() throws SQLException {
    try {
      Class.forName("com.mysql.cj.jdbc.Driver");
    } catch (ClassNotFoundException e) {
      System.err.println("Không tìm thấy MySQL JDBC Driver!");
      e.printStackTrace();
    }

    // Đọc thông tin từ Biến môi trường
    String url = System.getenv("DB_URL");
    String user = System.getenv("DB_USER");
    String password = System.getenv("DB_PASSWORD");

    // Bắt lỗi nếu quên chưa cài đặt biến môi trường
    if (url == null || user == null || password == null) {
      System.err.println("❌ THIẾU BIẾN MÔI TRƯỜNG! Vui lòng cấu hình DB_URL, DB_USER, DB_PASSWORD.");
      throw new SQLException("Thiếu cấu hình biến môi trường cho Database!");
    }

    return DriverManager.getConnection(url, user, password);
  }
}