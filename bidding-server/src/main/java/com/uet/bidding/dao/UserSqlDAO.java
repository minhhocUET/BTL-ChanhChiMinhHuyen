package com.uet.bidding.dao;

import com.uet.bidding.exception.AuthenticationException;
import com.uet.bidding.exception.UserException;
import com.uet.bidding.model.Admin;
import com.uet.bidding.model.Customer;
import com.uet.bidding.model.User;
import org.mindrot.jbcrypt.BCrypt;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * UserSqlDAO – CRUD đầy đủ, khớp với model hierarchy:
 * <p>
 * Entity
 * └── User  (abstract)  ← username, password, isBanned
 * ├── Admin          ← getRole() = "ADMIN"
 * └── Customer       ← getRole() = "CUSTOMER"
 * + fullName, email, phone, address, balance, ...
 * + Seller  sellerProfile  (composition)
 * + Bidder  bidderProfile  (composition)
 * <p>
 * Schema DB:
 * users   → thông tin chung (cả ADMIN lẫn CUSTOMER)
 * sellers → 1-1 với CUSTOMER  (store_name, description, rating)
 * bidders → 1-1 với CUSTOMER  (user_id only)
 */
public class UserSqlDAO {

  // =========================================================
  //  PRIVATE HELPERS
  // =========================================================

  /**
   * Câu SELECT dùng chung – LEFT JOIN để ADMIN vẫn được trả về (cột seller = NULL).
   * Alias store_description tránh đụng tên với cột description của bảng khác.
   */
  private static final String BASE_SELECT =
      "SELECT u.*, " +
          "       s.store_name, " +
          "       s.description AS store_description, " +
          "       s.rating " +
          "FROM   users   u " +
          "LEFT   JOIN sellers s ON s.user_id = u.id ";

  /**
   * Tránh lỗi UNIQUE constraint khi value là "" (chuỗi rỗng).
   */
  private void setStringOrNull(PreparedStatement pstmt, int index, String value)
      throws SQLException {
    if (value == null || value.trim().isEmpty()) {
      pstmt.setNull(index, Types.VARCHAR);
    } else {
      pstmt.setString(index, value.trim());
    }
  }

  /**
   * Ánh xạ ResultSet → đúng subclass (Admin hoặc Customer).
   * <p>
   * Cột cần có trong ResultSet:
   * Từ users  : id, username, password, role, full_name, email, phone,
   * address, balance, is_profile_completed, is_banned, created_at
   * Từ sellers: store_name, store_description, rating   (NULL nếu ADMIN)
   */
  private User mapResultSetToUser(ResultSet rs) throws SQLException {
    String role = rs.getString("role");

    if ("ADMIN".equalsIgnoreCase(role)) {
      // ── Admin ────────────────────────────────────────────────
      Admin admin = new Admin();
      fillBaseFields(admin, rs);
      return admin;

    } else {
      // ── Customer ─────────────────────────────────────────────
      Customer customer = new Customer();
      fillBaseFields(customer, rs);

      customer.setFullName(rs.getString("full_name"));
      customer.setEmail(rs.getString("email"));
      customer.setPhone(rs.getString("phone"));
      customer.setAddress(rs.getString("address"));
      customer.setBalance(rs.getBigDecimal("balance"));
      customer.setProfileComplete(rs.getBoolean("is_profile_completed"));
      if (hasColumn(rs, "avatar_data")) {
        customer.getSellerProfile().setAvatarData(rs.getString("avatar_data"));
      }

      // Nạp Seller profile (constructor Customer() đã new Seller() sẵn)
      customer.getSellerProfile().setStoreName(rs.getString("store_name"));
      customer.getSellerProfile().setDescription(rs.getString("store_description"));
      double rating = rs.getDouble("rating");
      customer.getSellerProfile().setSellerRating(rs.wasNull() ? 0.0 : rating);

      // Bidder profile: danh sách auction ids được load lazy qua AuctionDAO
      // → không cần nạp ở đây

      return customer;
    }
  }

  /**
   * Nạp các trường chung của lớp User (abstract) vào subclass bất kỳ.
   */
  private static boolean hasColumn(ResultSet rs, String column) throws SQLException {
    ResultSetMetaData meta = rs.getMetaData();
    for (int i = 1; i <= meta.getColumnCount(); i++) {
      if (column.equalsIgnoreCase(meta.getColumnLabel(i))) {
        return true;
      }
    }
    return false;
  }

  private void fillBaseFields(User user, ResultSet rs) throws SQLException {
    user.setId(rs.getInt("id"));
    user.setUsername(rs.getString("username"));
    user.setPassword(rs.getString("password"));
    user.setBanned(rs.getBoolean("is_banned"));
    user.setRole(rs.getString("role"));
  }

  // =========================================================
  //  CREATE
  // =========================================================
  public void addUser(User user) throws UserException {
    // Câu lệnh SQL khớp hoàn toàn 100% với cấu trúc bảng users
    String sqlUser =
        "INSERT INTO users " +
            "    (username, password, role, full_name, email, phone, address, balance, is_profile_completed, is_banned) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    try (Connection conn = DatabaseConnection.getConnection()) {
      conn.setAutoCommit(false); // Bật Transaction để bảo vệ dữ liệu đa bảng

      try {
        int newId;

        // ── BƯỚC 1: INSERT VÀO BẢNG USERS ──────────────────────────────
        try (PreparedStatement stmt = conn.prepareStatement(sqlUser, Statement.RETURN_GENERATED_KEYS)) {

          stmt.setString(1, user.getUsername());
          stmt.setString(2, user.getPassword());
          stmt.setString(3, user.getRole()); // Sử dụng tính đa hình: Tự động trả về "ADMIN" hoặc "CUSTOMER"

          if (user instanceof Customer c) {
            // Đối với Customer: Nạp thông tin cá nhân từ Object Java
            setStringOrNull(stmt, 4, c.getFullName());
            setStringOrNull(stmt, 5, c.getEmail());
            setStringOrNull(stmt, 6, c.getPhone());
            setStringOrNull(stmt, 7, c.getAddress());
            stmt.setBigDecimal(8, c.getBalance() != null ? c.getBalance() : BigDecimal.ZERO);

            // Sử dụng hàm hasCompleteProfile() có sẵn trong Model Customer để tính cờ trạng thái
            boolean isComplete = c.hasCompleteProfile();
            stmt.setBoolean(9, isComplete);
            c.setProfileComplete(isComplete); // Đồng bộ trạng thái vào Object Java
          } else {
            // Đối với Admin: Các cột cá nhân để NULL, số dư bằng 0
            stmt.setNull(4, Types.VARCHAR);
            stmt.setNull(5, Types.VARCHAR);
            stmt.setNull(6, Types.VARCHAR);
            stmt.setNull(7, Types.VARCHAR);
            stmt.setBigDecimal(8, BigDecimal.ZERO);
            stmt.setBoolean(9, true); // Admin mặc định coi như đã hoàn thiện hồ sơ
          }

          // Đồng bộ trạng thái khóa tài khoản (Mặc định ban đầu là false)
          stmt.setBoolean(10, user.isBanned());

          stmt.executeUpdate();

          // Lấy ID tự động tăng (Auto Increment) gán ngược lại cho thực thể
          try (ResultSet gk = stmt.getGeneratedKeys()) {
            if (!gk.next()) throw new SQLException("Không lấy được ID tự sinh từ bảng users.");
            newId = gk.getInt(1);
            user.setId(newId);
          }
        }

        // ── BƯỚC 2: INSERT VÀO CÁC BẢNG PHỤ NẾU LÀ CUSTOMER ──────────
        if (user instanceof Customer c) {

          // 2.1. Chèn vào bảng sellers (Khớp tên cột 'description' theo đúng DB của em)
          String sqlSeller = "INSERT INTO sellers (user_id, store_name, description, rating) VALUES (?, ?, ?, 0.00)";
          try (PreparedStatement stmtSeller = conn.prepareStatement(sqlSeller)) {
            stmtSeller.setInt(1, newId);

            // Kiểm tra an toàn chống NullPointerException nếu profile chưa được khởi tạo
            if (c.getSellerProfile() != null) {
              setStringOrNull(stmtSeller, 2, c.getSellerProfile().getStoreName());
              // Gọi hàm lấy miêu tả tương ứng của đối tượng Seller bên em (Ví dụ: getDescription)
              setStringOrNull(stmtSeller, 3, c.getSellerProfile().getDescription());
            } else {
              stmtSeller.setNull(2, Types.VARCHAR);
              stmtSeller.setNull(3, Types.VARCHAR);
            }
            stmtSeller.executeUpdate();
          }

          // 2.2. Chèn vào bảng bidders (Chỉ lưu khóa ngoại user_id tinh gọn)
          String sqlBidder = "INSERT INTO bidders (user_id) VALUES (?)";
          try (PreparedStatement stmtBidder = conn.prepareStatement(sqlBidder)) {
            stmtBidder.setInt(1, newId);
            stmtBidder.executeUpdate();
          }
        }

        conn.commit(); // Hoàn tất, ghi nhận dữ liệu xuống TiDB Cloud

      } catch (SQLException ex) {
        conn.rollback(); // Quay lui dữ liệu nếu phát sinh bất kỳ lỗi xung đột nào
        throw ex;
      } finally {
        conn.setAutoCommit(true);
      }

    } catch (SQLIntegrityConstraintViolationException e) {
      throw new UserException("Tên đăng nhập đã tồn tại trên hệ thống!");
    } catch (SQLException e) {
      throw new UserException("Lỗi Database khi thêm người dùng: " + e.getMessage());
    }
  }


  // =========================================================
  //  READ – xác thực
  // =========================================================

  /**
   * Kiểm tra đăng nhập.
   *
   * @return Admin hoặc Customer nếu hợp lệ.
   * @throws AuthenticationException nếu sai tài khoản / mật khẩu / bị khóa.
   */
  public User checkLogin(String username, String plainPassword) throws AuthenticationException {
    String sql = BASE_SELECT + "WHERE u.username = ?";

    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setString(1, username);

      try (ResultSet rs = stmt.executeQuery()) {
        if (!rs.next())
          throw new AuthenticationException("Tài khoản không tồn tại!");

        String storedHash = rs.getString("password");

        // ✅ BCrypt.checkpw: so sánh đúng plain-text với hash có salt
        // ❌ KHÔNG dùng: storedHash.equals(plainPassword)
        if (!BCrypt.checkpw(plainPassword, storedHash))
          throw new AuthenticationException("Sai mật khẩu!");

        if (rs.getBoolean("is_banned"))
          throw new AuthenticationException("Tài khoản của bạn đã bị khóa!");

        return mapResultSetToUser(rs);
      }

    } catch (SQLException e) {
      throw new AuthenticationException("Lỗi hệ thống khi đăng nhập: " + e.getMessage());
    }
  }

  // =========================================================
  //  READ – danh sách
  // =========================================================

  /**
   * Toàn bộ User (Admin + Customer), dùng cho màn hình quản lý của Admin.
   */
  public List<User> getAllUsers() {
    return queryList(BASE_SELECT + "ORDER BY u.created_at DESC", null);
  }

  /**
   * Lọc theo role: truyền "ADMIN" hoặc "CUSTOMER".
   */
  public List<User> getUsersByRole(String role) {
    return queryList(
        BASE_SELECT + "WHERE u.role = ? ORDER BY u.created_at DESC",
        stmt -> stmt.setString(1, role.toUpperCase())
    );
  }

  /**
   * Hàm nội bộ thực thi SELECT trả về danh sách.
   */
  private List<User> queryList(String sql, StatementSetter setter) {
    List<User> users = new ArrayList<>();
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {

      if (setter != null) setter.set(stmt);

      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) users.add(mapResultSetToUser(rs));
      }
    } catch (SQLException e) {
      System.err.println("Lỗi load danh sách user: " + e.getMessage());
    }
    return users;
  }

  // =========================================================
  //  READ – tìm kiếm theo khoá
  // =========================================================

  /**
   * Tìm theo id. @throws UserException nếu không tìm thấy.
   */
  public User findById(int id) throws UserException {
    String sql = BASE_SELECT + "WHERE u.id = ?";

    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setInt(1, id);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) return mapResultSetToUser(rs);
      }

    } catch (SQLException e) {
      throw new UserException("Lỗi truy vấn theo ID: " + e.getMessage());
    }
    throw new UserException("Không tìm thấy User với ID: " + id);
  }

  /**
   * Tìm theo username. @throws UserException nếu không tìm thấy.
   */
  public User findByUsername(String username) throws UserException {
    String sql = BASE_SELECT + "WHERE u.username = ?";

    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setString(1, username);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) return mapResultSetToUser(rs);
      }

    } catch (SQLException e) {
      throw new UserException("Lỗi truy vấn theo username: " + e.getMessage());
    }
    throw new UserException("Không tìm thấy User: " + username);
  }

  /**
   * Kiểm tra username đã tồn tại chưa (dùng khi validate form đăng ký).
   */
  public boolean existsByUsername(String username) {
    String sql = "SELECT 1 FROM users WHERE username = ?";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, username);
      try (ResultSet rs = stmt.executeQuery()) {
        return rs.next();
      }
    } catch (SQLException e) {
      System.err.println("Lỗi kiểm tra username: " + e.getMessage());
    }
    return false;
  }

  // =========================================================
  //  UPDATE – hồ sơ cá nhân (chỉ Customer)
  // =========================================================

  /**
   * Cập nhật hồ sơ cá nhân và thông tin cửa hàng của Customer.
   * Tự động đánh dấu is_profile_completed = TRUE.
   * Không đụng password / balance.
   *
   * @throws UserException nếu truyền vào Admin, hoặc user không tồn tại.
   */
  public void updateProfile(Customer customer) throws UserException {
    if (customer == null) throw new UserException("Customer không được null!");

    String sqlUser =
        "UPDATE users " +
            "SET    full_name             = ?, " +
            "       email                = ?, " +
            "       phone                = ?, " +
            "       address              = ?, " +
            "       is_profile_completed = TRUE " +
            "WHERE  id = ?";

    String sqlSeller =
        "UPDATE sellers " +
            "SET    store_name  = ?, " +
            "       description = ? " +
            "WHERE  user_id = ?";

    try (Connection conn = DatabaseConnection.getConnection()) {
      conn.setAutoCommit(false);

      try {
        // ── Cập nhật users ────────────────────────────────────
        try (PreparedStatement stmt = conn.prepareStatement(sqlUser)) {
          setStringOrNull(stmt, 1, customer.getFullName());
          setStringOrNull(stmt, 2, customer.getEmail());
          setStringOrNull(stmt, 3, customer.getPhone());
          setStringOrNull(stmt, 4, customer.getAddress());
          stmt.setInt(5, customer.getId());

          if (stmt.executeUpdate() == 0)
            throw new UserException("Không tìm thấy Customer ID: " + customer.getId());
        }

        // ── Cập nhật sellers ──────────────────────────────────
        try (PreparedStatement stmt = conn.prepareStatement(sqlSeller)) {
          setStringOrNull(stmt, 1, customer.getSellerProfile().getStoreName());
          setStringOrNull(stmt, 2, customer.getSellerProfile().getDescription());
          stmt.setInt(3, customer.getId());
          stmt.executeUpdate();
        }

        conn.commit();

      } catch (UserException ue) {
        conn.rollback();
        throw ue;
      } catch (SQLException ex) {
        conn.rollback();
        throw ex;
      } finally {
        conn.setAutoCommit(true);
      }

    } catch (SQLIntegrityConstraintViolationException e) {
      throw new UserException("Email hoặc Số điện thoại đã được tài khoản khác sử dụng!");
    } catch (SQLException e) {
      throw new UserException("Lỗi cập nhật SQL: " + e.getMessage());
    }
  }

  public void updateAvatar(int userId, String avatarData) throws UserException {
    String sql = "UPDATE users SET avatar_data = ? WHERE id = ?";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      if (avatarData == null || avatarData.isBlank()) {
        stmt.setNull(1, Types.LONGVARCHAR);
      } else {
        stmt.setString(1, avatarData);
      }
      stmt.setInt(2, userId);
      if (stmt.executeUpdate() == 0) {
        throw new UserException("Không tìm thấy User với ID: " + userId);
      }
    } catch (SQLException e) {
      throw new UserException("Lỗi cập nhật ảnh đại diện: " + e.getMessage());
    }
  }

  // =========================================================
  //  UPDATE – mật khẩu
  // =========================================================

  /**
   * Đổi mật khẩu có xác thực mật khẩu cũ.
   *
   * @throws UserException nếu tài khoản không tồn tại hoặc mật khẩu cũ sai.
   */
  public void updatePassword(int userId, String oldPlain, String newPlain) throws UserException {
    String checkSql = "SELECT password FROM users WHERE id = ?";
    String updateSql = "UPDATE users SET password = ? WHERE id = ?";

    try (Connection conn = DatabaseConnection.getConnection()) {

      // Bước 1: Lấy hash từ DB và xác thực bằng BCrypt
      String storedHash;
      try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
        checkStmt.setInt(1, userId);
        try (ResultSet rs = checkStmt.executeQuery()) {
          if (!rs.next()) throw new UserException("Tài khoản không tồn tại!");
          storedHash = rs.getString("password");
        }
      }

      if (!BCrypt.checkpw(oldPlain, storedHash))
        throw new UserException("Mật khẩu cũ không chính xác!");

      // Bước 2: Băm mật khẩu mới rồi lưu
      String newHash = BCrypt.hashpw(newPlain, BCrypt.gensalt(12));
      try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
        updateStmt.setString(1, newHash);
        updateStmt.setInt(2, userId);
        updateStmt.executeUpdate();
      }

    } catch (SQLException e) {
      throw new UserException("Lỗi đổi mật khẩu: " + e.getMessage());
    }
  }

  // =========================================================
  //  UPDATE – số dư
  // =========================================================

  /**
   * Cộng / trừ số dư (dùng biểu thức SQL để tránh race condition).
   * Truyền số âm để trừ tiền.
   *
   * @throws UserException nếu tài khoản không tồn tại.
   */
  public void updateBalance(int userId, BigDecimal amount) throws UserException {
    String sql = "UPDATE users SET balance = balance + ? WHERE id = ?";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setBigDecimal(1, amount);
      stmt.setInt(2, userId);
      if (stmt.executeUpdate() == 0)
        throw new UserException("Không tìm thấy tài khoản ID: " + userId);
    } catch (SQLException e) {
      throw new UserException("Lỗi cập nhật số dư: " + e.getMessage());
    }
  }

  /**
   * Lấy số dư hiện tại của user.
   */
  public BigDecimal getBalance(int userId) throws UserException {
    String sql = "SELECT balance FROM users WHERE id = ?";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, userId);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) return rs.getBigDecimal("balance");
        else throw new UserException("Không tìm thấy user ID: " + userId);
      }
    } catch (SQLException e) {
      throw new UserException("Lỗi lấy số dư: " + e.getMessage());
    }
  }

  /**
   * Rút một khoản tiền (nghiệp vụ). Tự động kiểm tra số dư đủ.
   *
   * @param amount số tiền cần rút (phải > 0)
   */
  public void withdraw(int userId, BigDecimal amount) throws UserException {
    if (amount.compareTo(BigDecimal.ZERO) <= 0)
      throw new UserException("Số tiền rút phải lớn hơn 0");
    BigDecimal current = getBalance(userId);
    if (current.compareTo(amount) < 0)
      throw new UserException("Số dư không đủ. Cần: " + amount + ", hiện có: " + current);
    updateBalance(userId, amount.negate());
  }


  // =========================================================
  //  UPDATE – khóa / mở khóa
  // =========================================================

  /**
   * HÀM ĐƯỢC THÊM MỚI: Cập nhật trạng thái khóa/mở khóa tài khoản.
   * Trả về true nếu cập nhật thành công, false nếu xảy ra lỗi hoặc không tìm thấy ID.
   * Cần thiết để tương thích trực tiếp với RequestProcessor.
   */
  public boolean updateBanStatus(int userId, boolean banned) {
    try {
      setBanned(userId, banned);
      return true;
    } catch (UserException e) {
      System.err.println("❌ Lỗi khi cập nhật trạng thái Ban cho User ID " + userId + ": " + e.getMessage());
      return false;
    }
  }

  /**
   * Khóa hoặc mở khóa tài khoản.
   * Logic Admin.banUser() / unbanUser() cập nhật object trong memory;
   * method này đồng bộ trạng thái đó xuống DB.
   *
   * @param userId id của user cần thay đổi.
   * @param banned TRUE = khóa, FALSE = mở khóa.
   */
  public void setBanned(int userId, boolean banned) throws UserException {
    String sql = "UPDATE users SET is_banned = ? WHERE id = ?";

    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setBoolean(1, banned);
      stmt.setInt(2, userId);

      if (stmt.executeUpdate() == 0)
        throw new UserException("Không tìm thấy User với ID: " + userId);

    } catch (SQLException e) {
      throw new UserException("Lỗi thay đổi trạng thái tài khoản: " + e.getMessage());
    }
  }

  // =========================================================
  //  UPDATE – seller rating
  // =========================================================

  /**
   * Tính lại rating trung bình của seller từ bảng {@code reviews}.
   * Gọi sau mỗi lần thêm / sửa / xóa review để giữ nhất quán.
   * Đồng thời cập nhật lại sellerProfile trong object Customer nếu được truyền vào.
   *
   * @throws UserException nếu không tìm thấy seller.
   */
  public void recalculateSellerRating(int sellerId) throws UserException {
    String sql =
        "UPDATE sellers s " +
            "SET    s.rating = ( " +
            "           SELECT COALESCE(AVG(r.stars), 0.00) " +
            "           FROM   reviews r " +
            "           WHERE  r.seller_id = ? " +
            "       ) " +
            "WHERE  s.user_id = ?";

    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setInt(1, sellerId);
      stmt.setInt(2, sellerId);

      if (stmt.executeUpdate() == 0)
        throw new UserException("Không tìm thấy Seller với ID: " + sellerId);

    } catch (SQLException e) {
      throw new UserException("Lỗi cập nhật seller rating: " + e.getMessage());
    }
  }

  // =========================================================
  //  DELETE
  // =========================================================

  /**
   * Xóa User theo id.
   * ON DELETE CASCADE tự dọn sellers, bidders, bids, items, ... liên quan.
   *
   * @throws UserException nếu id không tồn tại.
   */
  public void deleteUser(int id) throws UserException {
    String sql = "DELETE FROM users WHERE id = ?";

    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setInt(1, id);
      if (stmt.executeUpdate() == 0)
        throw new UserException("Xóa thất bại! Không tồn tại User với ID: " + id);

    } catch (SQLException e) {
      throw new UserException("Lỗi xóa dữ liệu: " + e.getMessage());
    }
  }

  // =========================================================
  //  THỐNG KÊ
  // =========================================================

  /**
   * Tổng số tài khoản (dùng cho dashboard Admin).
   */
  public int getTotalUserCount() {
    return countByQuery("SELECT COUNT(*) FROM users");
  }

  /**
   * Tổng số tài khoản đang bị khóa.
   */
  public int getBannedUserCount() {
    return countByQuery("SELECT COUNT(*) FROM users WHERE is_banned = TRUE");
  }

  /**
   * Tổng số CUSTOMER.
   */
  public int getCustomerCount() {
    return countByQuery("SELECT COUNT(*) FROM users WHERE role = 'CUSTOMER'");
  }

  private int countByQuery(String sql) {
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql);
         ResultSet rs = stmt.executeQuery()) {
      if (rs.next()) return rs.getInt(1);
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return 0;
  }

  // =========================================================
  //  FUNCTIONAL INTERFACE NỘI BỘ
  // =========================================================

  /**
   * Cho phép truyền lambda set tham số vào queryList() gọn hơn.
   */
  @FunctionalInterface
  private interface StatementSetter {
    void set(PreparedStatement stmt) throws SQLException;
  }
}