package com.uet.bidding.dao;

import com.uet.bidding.exception.AuthenticationException;
import com.uet.bidding.exception.UserException;
import com.uet.bidding.model.Admin;
import com.uet.bidding.model.Bidder;
import com.uet.bidding.model.Seller;
import com.uet.bidding.model.User;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserSqlDAO implements IUserDAO {

  /**
   * HÀM HỖ TRỢ: Chống lỗi UNIQUE constraint bằng cách chuyển chuỗi rỗng ("") thành NULL chuẩn.
   */
  private void setStringOrNull(PreparedStatement pstmt, int index, String value) throws SQLException {
    if (value == null || value.trim().isEmpty()) {
      pstmt.setNull(index, Types.VARCHAR);
    } else {
      pstmt.setString(index, value.trim());
    }
  }

  /**
   * CREATE: Thêm mới User vào database.
   */
  @Override
  public void addUser(User user) throws UserException {
    // 1. Cập nhật câu SQL: Thêm cột `role` vào INSERT
    String sqlUser = "INSERT INTO users (username, password, fullName, email, phone, address, balance, linkedBank, role) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

    try (Connection conn = DatabaseConnection.getConnection()) {
      conn.setAutoCommit(false);

      try (PreparedStatement stmtUser = conn.prepareStatement(sqlUser, Statement.RETURN_GENERATED_KEYS)) {
        stmtUser.setString(1, user.getUsername());
        stmtUser.setString(2, user.getPassword());
        setStringOrNull(stmtUser, 3, user.getFullName());
        setStringOrNull(stmtUser, 4, user.getEmail());
        setStringOrNull(stmtUser, 5, user.getPhone());
        setStringOrNull(stmtUser, 6, user.getAddress());

        BigDecimal balance = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
        stmtUser.setBigDecimal(7, balance);
        setStringOrNull(stmtUser, 8, user.getLinkedBank());

        // 2. Tự động xác định Role dựa vào Class
        String role = (user instanceof Admin) ? "ADMIN" : "USER";
        stmtUser.setString(9, role);

        stmtUser.executeUpdate();

        int generatedUserId = -1;
        try (ResultSet gk = stmtUser.getGeneratedKeys()) {
          if (gk.next()) generatedUserId = gk.getInt(1);
        }

        if (generatedUserId == -1) throw new SQLException("Không thể tạo ID cho User");
        user.setId(generatedUserId);

        // ==========================================
        // 3. PHÂN LUỒNG LƯU BẢNG PHỤ CỰC KỲ CHUẨN XÁC
        // ==========================================
        if ("ADMIN".equals(role)) {
          // Nếu là Admin, giữ nguyên logic cũ của bạn
          Admin admin = (Admin) user;
          String sqlAdmin = "INSERT INTO admins (user_id, adminLevel, department) VALUES (?, ?, ?)";
          try (PreparedStatement stmtAdmin = conn.prepareStatement(sqlAdmin)) {
            stmtAdmin.setInt(1, generatedUserId);
            // stmtAdmin.setInt(2, admin.getAdminLevel());
            // stmtAdmin.setString(3, admin.getDepartment());
            stmtAdmin.executeUpdate();
          }
        } else {
          // NẾU LÀ USER BÌNH THƯỜNG -> TỰ ĐỘNG KẾT NẠP VÀO CẢ 2 BẢNG

          // Mở khóa chức năng Bán hàng (Seller)
          String sqlSeller = "INSERT INTO sellers (user_id, rating, taxId, shopName) VALUES (?, 5.0, NULL, NULL)";
          try (PreparedStatement stmtSeller = conn.prepareStatement(sqlSeller)) {
            stmtSeller.setInt(1, generatedUserId);
            stmtSeller.executeUpdate();
          }

          // Mở khóa chức năng Đấu giá (Bidder)
          String sqlBidder = "INSERT INTO bidders (user_id, totalBids, auctionsWon) VALUES (?, 0, 0)";
          try (PreparedStatement stmtBidder = conn.prepareStatement(sqlBidder)) {
            stmtBidder.setInt(1, generatedUserId);
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
      e.printStackTrace();
      throw new UserException("Tên đăng nhập, Email hoặc Số điện thoại đã được sử dụng!");
    } catch (SQLException e) {
      throw new UserException("Lỗi SQL: " + e.getMessage());
    }
  }

  private User mapResultSetToUser(ResultSet rs) throws SQLException {
    User user;

    if (rs.getInt("admin_id") > 0) {
      Admin admin = new Admin("chi", "ababbaba", BigDecimal.ZERO);
      // admin.setAdminLevel(rs.getInt("adminLevel"));
      // admin.setDepartment(rs.getString("department"));
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
    user.setBalance(rs.getBigDecimal("balance"));
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
    String sql = "SELECT u.*, " +
        "a.user_id AS admin_id, a.adminLevel, a.department, " +
        "s.user_id AS seller_id, s.rating, s.taxId, s.shopName, " +
        "b.user_id AS bidder_id, b.totalBids, b.auctionsWon " +
        "FROM users u " +
        "LEFT JOIN admins a ON u.id = a.user_id " +
        "LEFT JOIN sellers s ON u.id = s.user_id " +
        "LEFT JOIN bidders b ON u.id = b.user_id";

    try (Connection conn = DatabaseConnection.getConnection();
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {

      while (rs.next()) {
        users.add(mapResultSetToUser(rs));
      }
    } catch (SQLException e) {
      System.err.println("Lỗi load danh sách user: " + e.getMessage());
    }
    return users;
  }

  @Override
  public User findById(int id) throws UserException {
    String sql = "SELECT u.*, " +
        "a.user_id AS admin_id, a.adminLevel, a.department, " +
        "s.user_id AS seller_id, s.rating, s.taxId, s.shopName, " +
        "b.user_id AS bidder_id, b.totalBids, b.auctionsWon " +
        "FROM users u " +
        "LEFT JOIN admins a ON u.id = a.user_id " +
        "LEFT JOIN sellers s ON u.id = s.user_id " +
        "LEFT JOIN bidders b ON u.id = b.user_id " +
        "WHERE u.id = ?";

    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setInt(1, id);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) return mapResultSetToUser(rs);
      }
    } catch (SQLException e) {
      throw new UserException("Lỗi truy vấn ID: " + e.getMessage());
    }
    throw new UserException("Không tìm thấy User với ID: " + id);
  }

  @Override
  public void updateUser(User updatedUser) throws UserException {
    // 1. Không nên update password và balance ở đây để tránh ghi đè nhầm thành 0 hoặc null
    String sqlUser = "UPDATE users SET fullName = ?, email = ?, phone = ?, address = ?, linkedBank = ? WHERE username = ?";

    try (Connection conn = DatabaseConnection.getConnection()) {
      conn.setAutoCommit(false);

      try (PreparedStatement stmtUser = conn.prepareStatement(sqlUser)) {
        // Sử dụng hàm helper setStringOrNull bạn đã tạo
        setStringOrNull(stmtUser, 1, updatedUser.getFullName());
        setStringOrNull(stmtUser, 2, updatedUser.getEmail());
        setStringOrNull(stmtUser, 3, updatedUser.getPhone());
        setStringOrNull(stmtUser, 4, updatedUser.getAddress());
        setStringOrNull(stmtUser, 5, updatedUser.getLinkedBank());

        // Dùng Username làm điều kiện để định danh đúng người
        stmtUser.setString(6, updatedUser.getUsername());
        int rows = stmtUser.executeUpdate();
        if (rows == 0)
          throw new UserException("Cập nhật thất bại! Người dùng " + updatedUser.getUsername() + " không tồn tại.");
        if (updatedUser instanceof Admin) {
          Admin admin = (Admin) updatedUser;
          String sqlAdmin = "UPDATE admins SET adminLevel = ?, department = ? WHERE user_id = ?";
          try (PreparedStatement stmtAdmin = conn.prepareStatement(sqlAdmin)) {
            // stmtAdmin.setInt(1, admin.getAdminLevel());
            // stmtAdmin.setString(2, admin.getDepartment());
            stmtAdmin.setInt(3, admin.getId());
            stmtAdmin.executeUpdate();
          }
        } else if (updatedUser instanceof Seller) {
          Seller seller = (Seller) updatedUser;
          String sqlSeller = "UPDATE sellers SET rating = ?, taxId = ?, shopName = ? WHERE user_id = ?";
          try (PreparedStatement stmtSeller = conn.prepareStatement(sqlSeller)) {
            stmtSeller.setDouble(1, seller.getRating());
            stmtSeller.setString(2, seller.getTaxId());
            stmtSeller.setString(3, seller.getShopName());
            stmtSeller.setInt(4, seller.getId());
            stmtSeller.executeUpdate();
          }
        } else if (updatedUser instanceof Bidder) {
          Bidder bidder = (Bidder) updatedUser;
          String sqlBidder = "UPDATE bidders SET totalBids = ?, auctionsWon = ? WHERE user_id = ?";
          // ĐÃ FIX: Viết lại khối try-with-resources chuẩn xác
          try (PreparedStatement stmtBidder = conn.prepareStatement(sqlBidder)) {
            stmtBidder.setInt(1, bidder.getTotalBids());
            stmtBidder.setInt(2, bidder.getAuctionsWon());
            stmtBidder.setInt(3, bidder.getId());
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
      throw new UserException("Email hoặc Số điện thoại này đã được tài khoản khác sử dụng!");
    } catch (SQLException e) {
      throw new UserException("Lỗi cập nhật SQL: " + e.getMessage());
    }
  }

  @Override
  public void deleteUser(int id) throws UserException {
    String sql = "DELETE FROM users WHERE id = ?";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setInt(1, id);
      int rows = stmt.executeUpdate();
      if (rows == 0) throw new UserException("Xóa thất bại! ID không tồn tại.");
      System.out.println("Đã xóa User ID: " + id);

    } catch (SQLException e) {
      throw new UserException("Lỗi xóa dữ liệu: " + e.getMessage());
    }
  }
}