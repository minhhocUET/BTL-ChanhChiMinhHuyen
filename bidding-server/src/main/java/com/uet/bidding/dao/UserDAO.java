package com.uet.bidding.dao;

import com.uet.bidding.model.Bidder;
import com.uet.bidding.model.Seller;
import com.uet.bidding.model.User;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {
    private final Connection conn;

    public UserDAO(Connection conn) {
        this.conn = conn;
    }

    // Thêm user mới vào DB
    public void addUser(User user) throws SQLException {
        String sql = "INSERT INTO users (username, password, balance, role) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPassword());
            stmt.setBigDecimal(3, user.getBalance());
            stmt.setString(4, user.getRole()); // "BIDDER" hoặc "SELLER"
            stmt.executeUpdate();
        }
    }

    // Kiểm tra đăng nhập, trả về đối tượng User cụ thể
    public User checkLogin(String username, String password) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, password);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id");
                    BigDecimal balance = rs.getBigDecimal("balance");
                    String role = rs.getString("role");

                    if ("BIDDER".equalsIgnoreCase(role)) {
                        return new Bidder(id, username, password, balance);
                    } else if ("SELLER".equalsIgnoreCase(role)) {
                        return new Seller(id, username, password, balance);
                    }
                }
            }
        }
        return null;
    }

    // Lấy toàn bộ danh sách user
    public List<User> getAllUsers() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String username = rs.getString("username");
                String password = rs.getString("password");
                BigDecimal balance = rs.getBigDecimal("balance");
                String role = rs.getString("role");

                if ("BIDDER".equalsIgnoreCase(role)) {
                    users.add(new Bidder(id, username, password, balance));
                } else if ("SELLER".equalsIgnoreCase(role)) {
                    users.add(new Seller(id, username, password, balance));
                }
            }
        }
        return users;
    }

    // Tìm user theo ID
    public User findById(int id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String username = rs.getString("username");
                    String password = rs.getString("password");
                    BigDecimal balance = rs.getBigDecimal("balance");
                    String role = rs.getString("role");

                    if ("BIDDER".equalsIgnoreCase(role)) {
                        return new Bidder(id, username, password, balance);
                    } else if ("SELLER".equalsIgnoreCase(role)) {
                        return new Seller(id, username, password, balance);
                    }
                }
            }
        }
        return null;
    }
}
