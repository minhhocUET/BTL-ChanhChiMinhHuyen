package com.uet.bidding.dao;

import com.uet.bidding.exception.AuthenticationException;
import com.uet.bidding.exception.UserException;
import com.uet.bidding.model.*;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserSqlDAO implements IUserDAO {

  @Override
  public void addUser(User user) throws UserException {
    String sqlUser = "INSERT INTO users (username, password, fullName, email, phone, address, balance, linkedBank) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    try (Connection conn = DatabaseConnection.getConnection()) {
      conn.setAutoCommit(false);

      try (PreparedStatement stmtUser = conn.prepareStatement(sqlUser, Statement.RETURN_GENERATED_KEYS)) {
        stmtUser.setString(1, user.getUsername());
        stmtUser.setString(2, user.getPassword());
        stmtUser.setString(3, user.getFullName());
        stmtUser.setString(4, user.getEmail());
        stmtUser.setString(5, user.getPhone());
        stmtUser.setString(6, user.getAddress());

        // CHỐT CHẶN AN TOÀN: Tránh lỗi NullPointerException khi Insert
        BigDecimal balance = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
        stmtUser.setBigDecimal(7, balance);

        stmtUser.setString(8, user.getLinkedBank());

        stmtUser.executeUpdate();

        int generatedUserId = -1;
        try (ResultSet gk = stmtUser.getGeneratedKeys()) {
          if (gk.next()) generatedUserId = gk.getInt(1);
        }

        if (generatedUserId == -1) throw new SQLException("Không thể lấy ID tự sinh!");
        user.setId(generatedUserId);
        System.out.println("[SQL] Đã INSERT User thành công. ID cấp phát: " + generatedUserId);

        if (user instanceof Admin) {
          Admin admin = (Admin) user;
          String sqlAdmin = "INSERT INTO admins (user_id, adminLevel, department) VALUES (?, ?, ?)";
          try (PreparedStatement stmtAdmin = conn.prepareStatement(sqlAdmin)) {
            stmtAdmin.setInt(1, generatedUserId);
            stmtAdmin.setInt(2, admin.getAdminLevel());
            stmtAdmin.setString(3, admin.getDepartment());
            stmtAdmin.executeUpdate();
          }
        } else if (user instanceof Seller) {
          Seller seller = (Seller) user;
          String sqlSeller = "INSERT INTO sellers (user_id, rating, taxId, shopName) VALUES (?, ?, ?, ?)";
          try (PreparedStatement stmtSeller = conn.prepareStatement(sqlSeller)) {
            stmtSeller.setInt(1, generatedUserId);
            stmtSeller.setDouble(2, seller.getRating());
            stmtSeller.setString(3, seller.getTaxId());
            stmtSeller.setString(4, seller.getShopName());
            stmtSeller.executeUpdate();
          }
        } else if (user instanceof Bidder) {
          Bidder bidder = (Bidder) user;
          String sqlBidder = "INSERT INTO bidders (user_id, totalBids, auctionsWon) VALUES (?, ?, ?)";
          try (PreparedStatement stmtBidder = conn.prepareStatement(sqlBidder)) {
            stmtBidder.setInt(1, generatedUserId);
            stmtBidder.setInt(2, bidder.getTotalBids());
            stmtBidder.setInt(3, bidder.getAuctionsWon());
            stmtBidder.executeUpdate();
          }
        }

        conn.commit();
      } catch (SQLException ex) {
        conn.rollback();
        throw ex;
      } finally {
        conn.setAutoCommit(true);
      }

    } catch (SQLIntegrityConstraintViolationException e) {
      throw new UserException("Thông tin đăng nhập, Email hoặc SĐT đã tồn tại!");
    } catch (SQLException e) {
      throw new UserException("Lỗi SQL: " + e.getMessage());
    }
  }

  private User mapResultSetToUser(ResultSet rs) throws SQLException {
    User user;

    if (rs.getInt("admin_id") > 0) {
      Admin admin = new Admin();
      admin.setAdminLevel(rs.getInt("adminLevel"));
      admin.setDepartment(rs.getString("department"));
      user = admin;
    } else if (rs.getInt("seller_id") > 0) {
      Seller seller = new Seller();
      seller.setRating(rs.getDouble("rating"));
      seller.setTaxId(rs.getString("taxId"));
      seller.setShopName(rs.getString("shopName"));
      user = seller;
    } else {
      Bidder bidder = new Bidder();
      bidder.setTotalBids(rs.getInt("totalBids"));
      bidder.setAuctionsWon(rs.getInt("auctionsWon"));
      user = bidder;
    }

    user.setId(rs.getInt("id"));
    user.setUsername(rs.getString("username"));
    user.setPassword(rs.getString("password"));
    user.setFullName(rs.getString("fullName"));
    user.setEmail(rs.getString("email"));
    user.setPhone(rs.getString("phone"));
    user.setAddress(rs.getString("address"));

    // CHỐT CHẶN AN TOÀN: Đọc BigDecimal từ DB
    BigDecimal dbBalance = rs.getBigDecimal("balance");
    user.setBalance(dbBalance != null ? dbBalance : BigDecimal.ZERO);

    user.setLinkedBank(rs.getString("linkedBank"));

    return user;
  }

  @Override
  public User checkLogin(String username, String password) throws AuthenticationException {
    String sql = "SELECT u.*, " +
        "a.user_id AS admin_id, a.adminLevel, a.department, " +
        "s.user_id AS seller_id, s.rating, s.taxId, s.shopName, " +
        "b.user_id AS bidder_id, b.totalBids, b.auctionsWon " +
        "FROM users u " +
        "LEFT JOIN admins a ON u.id = a.user_id " +
        "LEFT JOIN sellers s ON u.id = s.user_id " +
        "LEFT JOIN bidders b ON u.id = b.user_id " +
        "WHERE u.username = ?";

    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setString(1, username);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          if (rs.getString("password").equals(password)) {
            System.out.println("[SQL] Login OK. ID: " + rs.getInt("id") + " | User: " + username);
            return mapResultSetToUser(rs);
          } else {
            throw new AuthenticationException("Sai mật khẩu!");
          }
        }
      }
      throw new AuthenticationException("Tài khoản không tồn tại!");

    } catch (SQLException e) {
      throw new AuthenticationException("Lỗi hệ thống khi đăng nhập: " + e.getMessage());
    }
  }

  @Override
  public List<User> getAllUsers() {
    List<User> users = new ArrayList<>();
    // ... (Giữ nguyên logic của bạn)
    return users;
  }

  @Override
  public User findById(int id) throws UserException {
    // ... (Giữ nguyên logic của bạn)
    return null;
  }

  @Override
  public void updateUser(User updatedUser) throws UserException {
    System.out.println("[SQL] Bắt đầu gọi UPDATE cho ID: " + updatedUser.getId());
    String sqlUser = "UPDATE users SET password = ?, fullName = ?, email = ?, phone = ?, address = ?, balance = ?, linkedBank = ? WHERE id = ?";

    try (Connection conn = DatabaseConnection.getConnection()) {
      conn.setAutoCommit(false);

      try (PreparedStatement stmtUser = conn.prepareStatement(sqlUser)) {
        stmtUser.setString(1, updatedUser.getPassword());
        stmtUser.setString(2, updatedUser.getFullName());
        stmtUser.setString(3, updatedUser.getEmail());
        stmtUser.setString(4, updatedUser.getPhone());
        stmtUser.setString(5, updatedUser.getAddress());

        // CHỐT CHẶN AN TOÀN
        BigDecimal balance = updatedUser.getBalance() != null ? updatedUser.getBalance() : BigDecimal.ZERO;
        stmtUser.setBigDecimal(6, balance);

        stmtUser.setString(7, updatedUser.getLinkedBank());
        stmtUser.setInt(8, updatedUser.getId());

        int rows = stmtUser.executeUpdate();
        System.out.println("[SQL] Số dòng dữ liệu User bị thay đổi: " + rows);

        if (rows == 0) throw new UserException("Cập nhật thất bại! Không tìm thấy ID: " + updatedUser.getId());

        if (updatedUser instanceof Admin) {
          Admin admin = (Admin) updatedUser;
          String sqlAdmin = "UPDATE admins SET adminLevel = ?, department = ? WHERE user_id = ?";
          try (PreparedStatement stmtAdmin = conn.prepareStatement(sqlAdmin)) {
            stmtAdmin.setInt(1, admin.getAdminLevel());
            stmtAdmin.setString(2, admin.getDepartment());
            stmtAdmin.setInt(3, admin.getId());
            stmtAdmin.executeUpdate();
          }
        } else if (updatedUser instanceof Seller) {
          Seller seller = (Seller) updatedUser;
          String sqlSeller = "UPDATE sellers SET rating = ?, taxId = ?, shopName = ? WHERE user_id = ?";
          try (PreparedStatement stmtSeller =prepareStatement(conn, sqlSeller, seller.getRating(), seller.getTaxId(), seller.getShopName(), seller.getId())) {
            stmtSeller.executeUpdate();
          }
        } else if (updatedUser instanceof Bidder) {
          Bidder bidder = (Bidder) updatedUser;
          String sqlBidder = "UPDATE bidders SET totalBids = ?, auctionsWon = ? WHERE user_id = ?";
          try (PreparedStatement stmtBidder = prepareStatement(conn, sqlBidder, bidder.getTotalBids(), bidder.getAuctionsWon(), bidder.getId())) {
            stmtBidder.executeUpdate();
          }
        }

        conn.commit();
        System.out.println("[SQL] COMMIT THÀNH CÔNG DỮ LIỆU CẬP NHẬT!");
      } catch (SQLException ex) {
        conn.rollback();
        throw ex;
      } finally {
        conn.setAutoCommit(true);
      }

    } catch (SQLException e) {
      System.err.println("[SQL] Lỗi UPDATE: " + e.getMessage());
      throw new UserException("Lỗi cập nhật SQL: " + e.getMessage());
    }
  }

  // Hàm Helper để code trên ngắn gọn hơn
  private PreparedStatement prepareStatement(Connection conn, String sql, Object... params) throws SQLException {
    PreparedStatement stmt = conn.prepareStatement(sql);
    for (int i = 0; i < params.length; i++) {
      stmt.setObject(i + 1, params[i]);
    }
    return stmt;
  }

  @Override
  public void deleteUser(int id) throws UserException {
    // ... (Giữ nguyên logic của bạn)
  }
}