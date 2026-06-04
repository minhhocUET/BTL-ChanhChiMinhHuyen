package com.uet.bidding.dao;

import com.uet.bidding.exception.AuthenticationException;
import com.uet.bidding.exception.UserException;
import com.uet.bidding.model.Admin;
import com.uet.bidding.model.Customer;
import com.uet.bidding.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UserSqlDAOTest – kiểm thử toàn diện UserSqlDAO bằng Mockito.
 *
 * Chiến lược: mock hoàn toàn tầng JDBC (Connection / PreparedStatement / ResultSet)
 * thông qua MockedStatic<DatabaseConnection>, không cần DB thật.
 * Mỗi nhánh logic quan trọng trong DAO đều có ít nhất một test case.
 */
@ExtendWith(MockitoExtension.class)
public class UserSqlDAOTest {

  // ── DAO cần test ─────────────────────────────────────────────────────────
  private UserSqlDAO dao;

  // ── Tầng JDBC được mock hoàn toàn ────────────────────────────────────────
  @Mock private Connection      mockConn;
  @Mock private PreparedStatement mockStmt;
  @Mock private ResultSet        mockRs;
  @Mock private ResultSetMetaData mockMeta;

  private MockedStatic<DatabaseConnection> mockedDb;

  // ── Dữ liệu mẫu ──────────────────────────────────────────────────────────
  private Customer dummyCustomer;
  private Admin    dummyAdmin;

  // =========================================================================
  //  SETUP / TEARDOWN
  // =========================================================================

  @BeforeEach
  void setUp() throws SQLException {
    dao = new UserSqlDAO();

    // Khi bất kỳ đoạn code nào gọi DatabaseConnection.getConnection()
    // → trả về mockConn thay vì kết nối thật
    mockedDb = mockStatic(DatabaseConnection.class);
    mockedDb.when(DatabaseConnection::getConnection).thenReturn(mockConn);

    // Chuỗi JDBC mặc định: conn.prepareStatement(...) → mockStmt
    lenient().when(mockConn.prepareStatement(anyString())).thenReturn(mockStmt);
    lenient().when(mockConn.prepareStatement(anyString(), anyInt())).thenReturn(mockStmt);
    lenient().when(mockStmt.executeQuery()).thenReturn(mockRs);

    // ResultSetMetaData: cần cho hasColumn() trong mapResultSetToUser()
    lenient().when(mockRs.getMetaData()).thenReturn(mockMeta);
    lenient().when(mockMeta.getColumnCount()).thenReturn(1);
    lenient().when(mockMeta.getColumnLabel(1)).thenReturn("avatar_data");

    // ── Customer mẫu ──
    dummyCustomer = new Customer();
    dummyCustomer.setId(1);
    dummyCustomer.setUsername("customer_test");
    dummyCustomer.setPassword("plain_password");
    dummyCustomer.setFullName("Nguyen Van A");
    dummyCustomer.setBalance(BigDecimal.valueOf(5000));

    // ── Admin mẫu ──
    dummyAdmin = new Admin();
    dummyAdmin.setUsername("admin_test");
    dummyAdmin.setPassword("admin_pass");
  }

  @AfterEach
  void tearDown() {
    mockedDb.close();
  }

  // =========================================================================
  //  HELPER – tránh lặp code khi mock một dòng ResultSet kiểu CUSTOMER
  // =========================================================================

  /**
   * Cấu hình mockRs để trả về thông tin một Customer đầy đủ.
   * Dùng lại trong mọi test cần đọc dữ liệu user từ DB.
   */
  private void stubCustomerResultSet() throws SQLException {
    when(mockRs.getString("role")).thenReturn("CUSTOMER");
    when(mockRs.getInt("id")).thenReturn(1);
    when(mockRs.getString("username")).thenReturn("customer_test");
    when(mockRs.getString("password")).thenReturn("hashed_pass");
    when(mockRs.getBoolean("is_banned")).thenReturn(false);
    when(mockRs.getString("full_name")).thenReturn("Nguyen Van A");
    when(mockRs.getString("email")).thenReturn("a@example.com");
    when(mockRs.getString("phone")).thenReturn("0123456789");
    when(mockRs.getString("address")).thenReturn("Ha Noi");
    when(mockRs.getBigDecimal("balance")).thenReturn(BigDecimal.valueOf(5000));
    when(mockRs.getBoolean("is_profile_completed")).thenReturn(true);
    when(mockRs.getString("avatar_data")).thenReturn(null);
    when(mockRs.getString("store_name")).thenReturn("Shop A");
    when(mockRs.getString("store_description")).thenReturn("Mo ta shop");
    when(mockRs.getDouble("rating")).thenReturn(4.5);
    when(mockRs.wasNull()).thenReturn(false);
  }

  /**
   * Cấu hình mockRs để trả về thông tin một Admin đầy đủ.
   */
  private void stubAdminResultSet() throws SQLException {
    when(mockRs.getString("role")).thenReturn("ADMIN");
    when(mockRs.getInt("id")).thenReturn(99);
    when(mockRs.getString("username")).thenReturn("admin_test");
    when(mockRs.getString("password")).thenReturn("hashed_admin_pass");
    when(mockRs.getBoolean("is_banned")).thenReturn(false);
  }

  // =========================================================================
  //  PHẦN 1 – ADDUSER
  // =========================================================================

  /**
   * addUser(Customer) thành công: executeUpdate + getGeneratedKeys trả về ID.
   */
  @Test
  void testAddUser_Customer_Success() throws Exception {
    when(mockStmt.executeUpdate()).thenReturn(1);
    when(mockStmt.getGeneratedKeys()).thenReturn(mockRs);
    when(mockRs.next()).thenReturn(true);
    when(mockRs.getInt(1)).thenReturn(42);

    assertDoesNotThrow(() -> dao.addUser(dummyCustomer));
    // ID phải được gán ngược lại vào object
    assertEquals(42, dummyCustomer.getId());
  }

  /**
   * addUser(Admin) thành công – đi qua nhánh else (không INSERT sellers/bidders).
   */
  @Test
  void testAddUser_Admin_Success() throws Exception {
    when(mockStmt.executeUpdate()).thenReturn(1);
    when(mockStmt.getGeneratedKeys()).thenReturn(mockRs);
    when(mockRs.next()).thenReturn(true);
    when(mockRs.getInt(1)).thenReturn(10);

    assertDoesNotThrow(() -> dao.addUser(dummyAdmin));
    assertEquals(10, dummyAdmin.getId());
  }

  /**
   * addUser() khi getGeneratedKeys không trả về dòng nào → rollback + UserException.
   */
  @Test
  void testAddUser_NoGeneratedKey_ThrowsUserException() throws Exception {
    when(mockStmt.executeUpdate()).thenReturn(1);
    when(mockStmt.getGeneratedKeys()).thenReturn(mockRs);
    when(mockRs.next()).thenReturn(false); // Không có ID tự sinh

    assertThrows(UserException.class, () -> dao.addUser(dummyCustomer));
  }

  /**
   * addUser() khi gặp SQLIntegrityConstraintViolationException (username trùng)
   * → ném UserException "Tên đăng nhập đã tồn tại".
   */
  @Test
  void testAddUser_DuplicateUsername_ThrowsUserException() throws Exception {
    when(mockConn.prepareStatement(anyString(), anyInt()))
        .thenThrow(new SQLIntegrityConstraintViolationException("Duplicate entry"));

    UserException ex = assertThrows(UserException.class, () -> dao.addUser(dummyCustomer));
    assertTrue(ex.getMessage().contains("Tên đăng nhập đã tồn tại"));
  }

  /**
   * addUser() khi gặp SQLException thông thường → ném UserException "Lỗi Database".
   */
  @Test
  void testAddUser_GenericSQLException_ThrowsUserException() throws Exception {
    when(mockConn.prepareStatement(anyString(), anyInt()))
        .thenThrow(new SQLException("Generic DB error"));

    UserException ex = assertThrows(UserException.class, () -> dao.addUser(dummyCustomer));
    assertTrue(ex.getMessage().contains("Lỗi Database"));
  }

  /**
   * addUser() với Customer có sellerProfile = null → vẫn không ném NPE,
   * đi vào nhánh setNull cho store_name và description.
   *
   * Lưu ý: Customer() constructor thường tự new Seller() sẵn, nên test này
   * kiểm tra nhánh phòng thủ "if (c.getSellerProfile() != null)".
   * Nếu không thể set sellerProfile = null từ ngoài, bỏ qua test này.
   */
  @Test
  void testAddUser_Customer_NullSellerProfile_DoesNotThrow() throws Exception {
    // Chỉ chạy nếu Customer cho phép override sellerProfile = null
    // Giả sử Customer có setSellerProfile(null) hoặc reflection
    try {
      java.lang.reflect.Field f = dummyCustomer.getClass().getDeclaredField("sellerProfile");
      f.setAccessible(true);
      f.set(dummyCustomer, null);
    } catch (NoSuchFieldException e) {
      // Nếu field không có hoặc tên khác thì bỏ qua test này
      return;
    }

    when(mockStmt.executeUpdate()).thenReturn(1);
    when(mockStmt.getGeneratedKeys()).thenReturn(mockRs);
    when(mockRs.next()).thenReturn(true);
    when(mockRs.getInt(1)).thenReturn(5);

    assertDoesNotThrow(() -> dao.addUser(dummyCustomer));
  }

  // =========================================================================
  //  PHẦN 2 – CHECKLOGIN
  // =========================================================================

  /**
   * checkLogin() thành công với Customer: username đúng, password đúng, không bị ban.
   * Dùng BCrypt.hashpw thật để tạo hash hợp lệ.
   */
  @Test
  void testCheckLogin_Customer_Success() throws Exception {
    String plain = "correctPassword";
    String hash  = org.mindrot.jbcrypt.BCrypt.hashpw(plain, org.mindrot.jbcrypt.BCrypt.gensalt());

    when(mockRs.next()).thenReturn(true);
    when(mockRs.getString("password")).thenReturn(hash);
    when(mockRs.getBoolean("is_banned")).thenReturn(false);
    stubCustomerResultSet();
    // Override password bằng hash vừa tạo (stubCustomerResultSet đã set "hashed_pass",
    // ghi đè lại ở đây)
    when(mockRs.getString("password")).thenReturn(hash);

    User result = dao.checkLogin("customer_test", plain);
    assertNotNull(result);
    assertInstanceOf(Customer.class, result);
  }

  /**
   * checkLogin() thành công với Admin.
   */
  @Test
  void testCheckLogin_Admin_Success() throws Exception {
    String plain = "adminPass";
    String hash  = org.mindrot.jbcrypt.BCrypt.hashpw(plain, org.mindrot.jbcrypt.BCrypt.gensalt());

    when(mockRs.next()).thenReturn(true);
    when(mockRs.getString("password")).thenReturn(hash);
    when(mockRs.getBoolean("is_banned")).thenReturn(false);
    stubAdminResultSet();
    when(mockRs.getString("password")).thenReturn(hash);

    User result = dao.checkLogin("admin_test", plain);
    assertNotNull(result);
    assertInstanceOf(Admin.class, result);
  }

  /**
   * checkLogin() khi username không tồn tại (rs.next() = false)
   * → ném AuthenticationException "Tài khoản không tồn tại".
   */
  @Test
  void testCheckLogin_UserNotFound_ThrowsAuthException() throws Exception {
    when(mockRs.next()).thenReturn(false);

    AuthenticationException ex = assertThrows(AuthenticationException.class,
        () -> dao.checkLogin("ghost", "pass"));
    assertTrue(ex.getMessage().contains("không tồn tại"));
  }

  /**
   * checkLogin() khi mật khẩu sai → ném AuthenticationException "Sai mật khẩu".
   */
  @Test
  void testCheckLogin_WrongPassword_ThrowsAuthException() throws Exception {
    String realHash = org.mindrot.jbcrypt.BCrypt.hashpw("correctPass", org.mindrot.jbcrypt.BCrypt.gensalt());
    when(mockRs.next()).thenReturn(true);
    when(mockRs.getString("password")).thenReturn(realHash);

    AuthenticationException ex = assertThrows(AuthenticationException.class,
        () -> dao.checkLogin("customer_test", "wrongPass"));
    assertTrue(ex.getMessage().contains("Sai mật khẩu"));
  }

  /**
   * checkLogin() khi tài khoản đang bị ban → ném AuthenticationException "bị khóa".
   */
  @Test
  void testCheckLogin_BannedUser_ThrowsAuthException() throws Exception {
    String plain = "pass";
    String hash  = org.mindrot.jbcrypt.BCrypt.hashpw(plain, org.mindrot.jbcrypt.BCrypt.gensalt());
    when(mockRs.next()).thenReturn(true);
    when(mockRs.getString("password")).thenReturn(hash);
    when(mockRs.getBoolean("is_banned")).thenReturn(true); // ← BAN

    AuthenticationException ex = assertThrows(AuthenticationException.class,
        () -> dao.checkLogin("customer_test", plain));
    assertTrue(ex.getMessage().contains("bị khóa"));
  }

  /**
   * checkLogin() khi DB ném SQLException → ném AuthenticationException "Lỗi hệ thống".
   */
  @Test
  void testCheckLogin_SQLException_ThrowsAuthException() throws Exception {
    when(mockConn.prepareStatement(anyString())).thenThrow(new SQLException("DB crash"));

    AuthenticationException ex = assertThrows(AuthenticationException.class,
        () -> dao.checkLogin("any", "any"));
    assertTrue(ex.getMessage().contains("Lỗi hệ thống"));
  }

  // =========================================================================
  //  PHẦN 3 – GETALLUSERS / GETUSERSBYROLLE
  // =========================================================================

  /**
   * getAllUsers() trả về list 2 phần tử (Admin + Customer).
   */
  @Test
  void testGetAllUsers_ReturnsMixedList() throws Exception {
    // Lần lặp 1 → ADMIN, lần lặp 2 → CUSTOMER, lần 3 → dừng
    when(mockRs.next()).thenReturn(true, true, false);
    when(mockRs.getString("role")).thenReturn("ADMIN", "CUSTOMER");

    // Admin fields
    lenient().when(mockRs.getInt("id")).thenReturn(99, 1);
    lenient().when(mockRs.getString("username")).thenReturn("admin_test", "cust");
    lenient().when(mockRs.getString("password")).thenReturn("h1", "h2");
    lenient().when(mockRs.getBoolean("is_banned")).thenReturn(false);

    // Customer-only fields (chỉ được đọc ở lần 2)
    lenient().when(mockRs.getString("full_name")).thenReturn("Admin Name", "Cust Name");
    lenient().when(mockRs.getString("email")).thenReturn(null, "c@c.com");
    lenient().when(mockRs.getString("phone")).thenReturn(null, "099");
    lenient().when(mockRs.getString("address")).thenReturn(null, "HN");
    lenient().when(mockRs.getBigDecimal("balance")).thenReturn(null, BigDecimal.TEN);
    lenient().when(mockRs.getBoolean("is_profile_completed")).thenReturn(false);
    lenient().when(mockRs.getString("avatar_data")).thenReturn(null);
    lenient().when(mockRs.getString("store_name")).thenReturn(null, "Shop");
    lenient().when(mockRs.getString("store_description")).thenReturn(null, "Desc");
    lenient().when(mockRs.getDouble("rating")).thenReturn(0.0, 4.0);
    lenient().when(mockRs.wasNull()).thenReturn(true);

    List<User> result = dao.getAllUsers();
    assertEquals(2, result.size());
  }

  /**
   * getAllUsers() khi DB lỗi → trả về list rỗng (không throw).
   */
  @Test
  void testGetAllUsers_SQLException_ReturnsEmptyList() throws Exception {
    when(mockConn.prepareStatement(anyString())).thenThrow(new SQLException("DB down"));

    List<User> result = dao.getAllUsers();
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  /**
   * getUsersByRole("CUSTOMER") với 1 kết quả.
   */
  @Test
  void testGetUsersByRole_Customer_ReturnsList() throws Exception {
    when(mockRs.next()).thenReturn(true, false);
    stubCustomerResultSet();

    List<User> result = dao.getUsersByRole("CUSTOMER");
    assertEquals(1, result.size());
    assertInstanceOf(Customer.class, result.get(0));
  }

  /**
   * getUsersByRole() khi DB lỗi → list rỗng.
   */
  @Test
  void testGetUsersByRole_SQLException_ReturnsEmptyList() throws Exception {
    when(mockConn.prepareStatement(anyString())).thenThrow(new SQLException("err"));

    List<User> result = dao.getUsersByRole("CUSTOMER");
    assertTrue(result.isEmpty());
  }

  // =========================================================================
  //  PHẦN 4 – FINDBYID / FINDBYUSERNAME
  // =========================================================================

  /**
   * findById() tìm thấy Customer → trả về đúng object.
   */
  @Test
  void testFindById_Found_ReturnsCustomer() throws Exception {
    when(mockRs.next()).thenReturn(true);
    stubCustomerResultSet();

    User result = dao.findById(1);
    assertNotNull(result);
    assertInstanceOf(Customer.class, result);
    assertEquals(1, result.getId());
  }

  /**
   * findById() tìm thấy Admin → trả về đúng Admin.
   */
  @Test
  void testFindById_Found_ReturnsAdmin() throws Exception {
    when(mockRs.next()).thenReturn(true);
    stubAdminResultSet();

    User result = dao.findById(99);
    assertInstanceOf(Admin.class, result);
  }

  /**
   * findById() không tìm thấy (rs.next() = false) → throw UserException.
   */
  @Test
  void testFindById_NotFound_ThrowsUserException() throws Exception {
    when(mockRs.next()).thenReturn(false);

    assertThrows(UserException.class, () -> dao.findById(999));
  }

  /**
   * findById() khi DB lỗi → throw UserException "Lỗi truy vấn theo ID".
   */
  @Test
  void testFindById_SQLException_ThrowsUserException() throws Exception {
    when(mockConn.prepareStatement(anyString())).thenThrow(new SQLException("DB crash"));

    UserException ex = assertThrows(UserException.class, () -> dao.findById(1));
    assertTrue(ex.getMessage().contains("Lỗi truy vấn theo ID"));
  }

  /**
   * findByUsername() tìm thấy → trả về User đúng.
   */
  @Test
  void testFindByUsername_Found_ReturnsUser() throws Exception {
    when(mockRs.next()).thenReturn(true);
    stubCustomerResultSet();

    User result = dao.findByUsername("customer_test");
    assertNotNull(result);
  }

  /**
   * findByUsername() không tìm thấy → throw UserException.
   */
  @Test
  void testFindByUsername_NotFound_ThrowsUserException() throws Exception {
    when(mockRs.next()).thenReturn(false);

    assertThrows(UserException.class, () -> dao.findByUsername("ghost"));
  }

  /**
   * findByUsername() khi DB lỗi → throw UserException.
   */
  @Test
  void testFindByUsername_SQLException_ThrowsUserException() throws Exception {
    when(mockConn.prepareStatement(anyString())).thenThrow(new SQLException("err"));

    assertThrows(UserException.class, () -> dao.findByUsername("any"));
  }

  // =========================================================================
  //  PHẦN 5 – EXISTSBYUSERNAME
  // =========================================================================

  /**
   * existsByUsername() khi đã tồn tại → true.
   */
  @Test
  void testExistsByUsername_Exists_ReturnsTrue() throws Exception {
    when(mockRs.next()).thenReturn(true);

    assertTrue(dao.existsByUsername("admin_test"));
  }

  /**
   * existsByUsername() khi chưa tồn tại → false.
   */
  @Test
  void testExistsByUsername_NotExists_ReturnsFalse() throws Exception {
    when(mockRs.next()).thenReturn(false);

    assertFalse(dao.existsByUsername("nobody"));
  }

  /**
   * existsByUsername() khi DB lỗi → false (không throw).
   */
  @Test
  void testExistsByUsername_SQLException_ReturnsFalse() throws Exception {
    when(mockConn.prepareStatement(anyString())).thenThrow(new SQLException("err"));

    assertFalse(dao.existsByUsername("any"));
  }

  // =========================================================================
  //  PHẦN 6 – UPDATEPROFILE
  // =========================================================================

  /**
   * updateProfile(null) → throw UserException ngay lập tức (không cần DB).
   */
  @Test
  void testUpdateProfile_NullCustomer_ThrowsUserException() {
    assertThrows(UserException.class, () -> dao.updateProfile(null));
  }

  /**
   * updateProfile() thành công: cả 2 UPDATE đều affectedRows = 1.
   */
  @Test
  void testUpdateProfile_Success() throws Exception {
    when(mockStmt.executeUpdate()).thenReturn(1); // UPDATE users → 1 row

    assertDoesNotThrow(() -> dao.updateProfile(dummyCustomer));
  }

  /**
   * updateProfile() khi UPDATE users trả về 0 (không tìm thấy Customer)
   * → rollback + throw UserException.
   */
  @Test
  void testUpdateProfile_CustomerNotFound_ThrowsUserException() throws Exception {
    when(mockStmt.executeUpdate()).thenReturn(0); // Không có dòng nào bị ảnh hưởng

    assertThrows(UserException.class, () -> dao.updateProfile(dummyCustomer));
  }

  /**
   * updateProfile() khi gặp SQLIntegrityConstraintViolationException
   * (email/phone trùng) → throw UserException "Email hoặc Số điện thoại đã được dùng".
   */
  @Test
  void testUpdateProfile_DuplicateContact_ThrowsUserException() throws Exception {
    when(mockConn.prepareStatement(anyString()))
        .thenThrow(new SQLIntegrityConstraintViolationException("Duplicate contact"));

    UserException ex = assertThrows(UserException.class,
        () -> dao.updateProfile(dummyCustomer));
    assertTrue(ex.getMessage().contains("Email"));
  }

  /**
   * updateProfile() khi gặp generic SQLException → throw UserException "Lỗi cập nhật SQL".
   */
  @Test
  void testUpdateProfile_GenericSQLException_ThrowsUserException() throws Exception {
    when(mockConn.prepareStatement(anyString())).thenThrow(new SQLException("err"));

    UserException ex = assertThrows(UserException.class,
        () -> dao.updateProfile(dummyCustomer));
    assertTrue(ex.getMessage().contains("Lỗi cập nhật SQL"));
  }

  // =========================================================================
  //  PHẦN 7 – UPDATEAVATAR
  // =========================================================================

  /**
   * updateAvatar() với dữ liệu hợp lệ → thành công.
   */
  @Test
  void testUpdateAvatar_WithData_Success() throws Exception {
    when(mockStmt.executeUpdate()).thenReturn(1);

    assertDoesNotThrow(() -> dao.updateAvatar(1, "base64data..."));
  }

  /**
   * updateAvatar() với avatarData = null → đi qua nhánh setNull.
   */
  @Test
  void testUpdateAvatar_NullData_Success() throws Exception {
    when(mockStmt.executeUpdate()).thenReturn(1);

    assertDoesNotThrow(() -> dao.updateAvatar(1, null));
  }

  /**
   * updateAvatar() với avatarData = "" (blank) → setNull.
   */
  @Test
  void testUpdateAvatar_BlankData_Success() throws Exception {
    when(mockStmt.executeUpdate()).thenReturn(1);

    assertDoesNotThrow(() -> dao.updateAvatar(1, "   "));
  }

  /**
   * updateAvatar() khi executeUpdate trả về 0 (user không tồn tại)
   * → throw UserException.
   */
  @Test
  void testUpdateAvatar_UserNotFound_ThrowsUserException() throws Exception {
    when(mockStmt.executeUpdate()).thenReturn(0);

    assertThrows(UserException.class, () -> dao.updateAvatar(999, "data"));
  }

  /**
   * updateAvatar() khi DB lỗi → throw UserException "Lỗi cập nhật ảnh đại diện".
   */
  @Test
  void testUpdateAvatar_SQLException_ThrowsUserException() throws Exception {
    when(mockConn.prepareStatement(anyString())).thenThrow(new SQLException("err"));

    UserException ex = assertThrows(UserException.class,
        () -> dao.updateAvatar(1, "data"));
    assertTrue(ex.getMessage().contains("Lỗi cập nhật ảnh đại diện"));
  }

  // =========================================================================
  //  PHẦN 8 – UPDATEPASSWORD
  // =========================================================================

  /**
   * updatePassword() thành công: old password đúng, cập nhật hash mới.
   */
  @Test
  void testUpdatePassword_Success() throws Exception {
    String oldPlain = "oldPassword";
    String oldHash  = org.mindrot.jbcrypt.BCrypt.hashpw(oldPlain, org.mindrot.jbcrypt.BCrypt.gensalt());

    // Lần gọi prepareStatement đầu (SELECT password) → mockStmt
    // Lần gọi thứ 2 (UPDATE password) → mockStmt (lenient setup đã cover)
    when(mockRs.next()).thenReturn(true);
    when(mockRs.getString("password")).thenReturn(oldHash);
    when(mockStmt.executeUpdate()).thenReturn(1);

    assertDoesNotThrow(() -> dao.updatePassword(1, oldPlain, "newPassword"));
  }

  /**
   * updatePassword() khi user không tồn tại (rs.next() = false)
   * → throw UserException "Tài khoản không tồn tại".
   */
  @Test
  void testUpdatePassword_UserNotFound_ThrowsUserException() throws Exception {
    when(mockRs.next()).thenReturn(false);

    UserException ex = assertThrows(UserException.class,
        () -> dao.updatePassword(1, "old", "new"));
    assertTrue(ex.getMessage().contains("Tài khoản không tồn tại"));
  }

  /**
   * updatePassword() khi mật khẩu cũ sai → throw UserException "Mật khẩu cũ không chính xác".
   */
  @Test
  void testUpdatePassword_WrongOldPassword_ThrowsUserException() throws Exception {
    String realHash = org.mindrot.jbcrypt.BCrypt.hashpw("realOld", org.mindrot.jbcrypt.BCrypt.gensalt());
    when(mockRs.next()).thenReturn(true);
    when(mockRs.getString("password")).thenReturn(realHash);

    UserException ex = assertThrows(UserException.class,
        () -> dao.updatePassword(1, "wrongOld", "new"));
    assertTrue(ex.getMessage().contains("Mật khẩu cũ không chính xác"));
  }

  /**
   * updatePassword() khi DB lỗi → throw UserException "Lỗi đổi mật khẩu".
   */
  @Test
  void testUpdatePassword_SQLException_ThrowsUserException() throws Exception {
    when(mockConn.prepareStatement(anyString())).thenThrow(new SQLException("DB crash"));

    UserException ex = assertThrows(UserException.class,
        () -> dao.updatePassword(1, "old", "new"));
    assertTrue(ex.getMessage().contains("Lỗi đổi mật khẩu"));
  }

  // =========================================================================
  //  PHẦN 9 – UPDATEBALANCE
  // =========================================================================

  /**
   * updateBalance() thành công khi affectedRows = 1.
   */
  @Test
  void testUpdateBalance_Success() throws Exception {
    when(mockStmt.executeUpdate()).thenReturn(1);

    assertDoesNotThrow(() -> dao.updateBalance(1, BigDecimal.valueOf(500)));
  }

  /**
   * updateBalance() khi truyền số âm (trừ tiền) → vẫn thành công nếu DB ok.
   */
  @Test
  void testUpdateBalance_NegativeAmount_Success() throws Exception {
    when(mockStmt.executeUpdate()).thenReturn(1);

    assertDoesNotThrow(() -> dao.updateBalance(1, BigDecimal.valueOf(-200)));
  }

  /**
   * updateBalance() khi affectedRows = 0 (user không tồn tại) → throw UserException.
   */
  @Test
  void testUpdateBalance_UserNotFound_ThrowsUserException() throws Exception {
    when(mockStmt.executeUpdate()).thenReturn(0);

    assertThrows(UserException.class, () -> dao.updateBalance(999, BigDecimal.TEN));
  }

  /**
   * updateBalance() khi DB lỗi → throw UserException "Lỗi cập nhật số dư".
   */
  @Test
  void testUpdateBalance_SQLException_ThrowsUserException() throws Exception {
    when(mockConn.prepareStatement(anyString())).thenThrow(new SQLException("err"));

    UserException ex = assertThrows(UserException.class,
        () -> dao.updateBalance(1, BigDecimal.TEN));
    assertTrue(ex.getMessage().contains("Lỗi cập nhật số dư"));
  }

  // =========================================================================
  //  PHẦN 10 – GETBALANCE
  // =========================================================================

  /**
   * getBalance() trả về đúng số dư từ DB.
   */
  @Test
  void testGetBalance_Found_ReturnsBalance() throws Exception {
    when(mockRs.next()).thenReturn(true);
    when(mockRs.getBigDecimal("balance")).thenReturn(BigDecimal.valueOf(9999));

    BigDecimal balance = dao.getBalance(1);
    assertEquals(0, BigDecimal.valueOf(9999).compareTo(balance));
  }

  /**
   * getBalance() khi user không tồn tại (rs.next() = false) → throw UserException.
   */
  @Test
  void testGetBalance_NotFound_ThrowsUserException() throws Exception {
    when(mockRs.next()).thenReturn(false);

    assertThrows(UserException.class, () -> dao.getBalance(999));
  }

  /**
   * getBalance() khi DB lỗi → throw UserException "Lỗi lấy số dư".
   */
  @Test
  void testGetBalance_SQLException_ThrowsUserException() throws Exception {
    when(mockConn.prepareStatement(anyString())).thenThrow(new SQLException("err"));

    UserException ex = assertThrows(UserException.class, () -> dao.getBalance(1));
    assertTrue(ex.getMessage().contains("Lỗi lấy số dư"));
  }

  // =========================================================================
  //  PHẦN 11 – WITHDRAW
  // =========================================================================

  /**
   * withdraw() với số tiền hợp lệ và số dư đủ → thành công.
   * Cần mock getBalance (SELECT) + updateBalance (UPDATE).
   */
  @Test
  void testWithdraw_Success() throws Exception {
    // getBalance(): rs.next() = true, balance = 5000
    ResultSet balanceRs = mock(ResultSet.class);
    when(balanceRs.next()).thenReturn(true);
    when(balanceRs.getBigDecimal("balance")).thenReturn(BigDecimal.valueOf(5000));

    PreparedStatement balanceStmt = mock(PreparedStatement.class);
    PreparedStatement updateStmt  = mock(PreparedStatement.class);
    when(balanceStmt.executeQuery()).thenReturn(balanceRs);
    when(updateStmt.executeUpdate()).thenReturn(1);

    // Lần 1: SELECT balance, Lần 2: UPDATE balance
    when(mockConn.prepareStatement(anyString()))
        .thenReturn(balanceStmt)
        .thenReturn(updateStmt);

    assertDoesNotThrow(() -> dao.withdraw(1, BigDecimal.valueOf(1000)));
  }

  /**
   * withdraw() với số tiền <= 0 → throw UserException ngay, không cần DB.
   */
  @Test
  void testWithdraw_ZeroAmount_ThrowsUserException() {
    UserException ex = assertThrows(UserException.class,
        () -> dao.withdraw(1, BigDecimal.ZERO));
    assertTrue(ex.getMessage().contains("lớn hơn 0"));
  }

  /**
   * withdraw() với số tiền âm → throw UserException.
   */
  @Test
  void testWithdraw_NegativeAmount_ThrowsUserException() {
    assertThrows(UserException.class,
        () -> dao.withdraw(1, BigDecimal.valueOf(-100)));
  }

  /**
   * withdraw() khi số dư không đủ → throw UserException "Số dư không đủ".
   */
  @Test
  void testWithdraw_InsufficientBalance_ThrowsUserException() throws Exception {
    when(mockRs.next()).thenReturn(true);
    when(mockRs.getBigDecimal("balance")).thenReturn(BigDecimal.valueOf(100));

    UserException ex = assertThrows(UserException.class,
        () -> dao.withdraw(1, BigDecimal.valueOf(9999)));
    assertTrue(ex.getMessage().contains("Số dư không đủ"));
  }

  // =========================================================================
  //  PHẦN 12 – SETBANNED / UPDATEBANSTATUS
  // =========================================================================

  /**
   * setBanned(true) thành công.
   */
  @Test
  void testSetBanned_Ban_Success() throws Exception {
    when(mockStmt.executeUpdate()).thenReturn(1);

    assertDoesNotThrow(() -> dao.setBanned(1, true));
  }

  /**
   * setBanned(false) → mở khóa thành công.
   */
  @Test
  void testSetBanned_Unban_Success() throws Exception {
    when(mockStmt.executeUpdate()).thenReturn(1);

    assertDoesNotThrow(() -> dao.setBanned(1, false));
  }

  /**
   * setBanned() khi affectedRows = 0 → throw UserException "Không tìm thấy User".
   */
  @Test
  void testSetBanned_UserNotFound_ThrowsUserException() throws Exception {
    when(mockStmt.executeUpdate()).thenReturn(0);

    assertThrows(UserException.class, () -> dao.setBanned(999, true));
  }

  /**
   * setBanned() khi DB lỗi → throw UserException.
   */
  @Test
  void testSetBanned_SQLException_ThrowsUserException() throws Exception {
    when(mockConn.prepareStatement(anyString())).thenThrow(new SQLException("err"));

    assertThrows(UserException.class, () -> dao.setBanned(1, true));
  }

  /**
   * updateBanStatus() thành công → trả về true.
   */
  @Test
  void testUpdateBanStatus_Success_ReturnsTrue() throws Exception {
    when(mockStmt.executeUpdate()).thenReturn(1);

    assertTrue(dao.updateBanStatus(1, true));
  }

  /**
   * updateBanStatus() khi setBanned() ném UserException → trả về false (không throw).
   */
  @Test
  void testUpdateBanStatus_Failure_ReturnsFalse() throws Exception {
    when(mockStmt.executeUpdate()).thenReturn(0); // → setBanned ném exception

    assertFalse(dao.updateBanStatus(999, true));
  }

  // =========================================================================
  //  PHẦN 13 – RECALCULATESELLERRATING
  // =========================================================================

  /**
   * recalculateSellerRating() thành công (affectedRows = 1).
   */
  @Test
  void testRecalculateSellerRating_Success() throws Exception {
    when(mockStmt.executeUpdate()).thenReturn(1);

    assertDoesNotThrow(() -> dao.recalculateSellerRating(1));
  }

  /**
   * recalculateSellerRating() khi không tìm thấy seller (affectedRows = 0)
   * → throw UserException "Không tìm thấy Seller".
   */
  @Test
  void testRecalculateSellerRating_NotFound_ThrowsUserException() throws Exception {
    when(mockStmt.executeUpdate()).thenReturn(0);

    UserException ex = assertThrows(UserException.class,
        () -> dao.recalculateSellerRating(999));
    assertTrue(ex.getMessage().contains("Không tìm thấy Seller"));
  }

  /**
   * recalculateSellerRating() khi DB lỗi → throw UserException.
   */
  @Test
  void testRecalculateSellerRating_SQLException_ThrowsUserException() throws Exception {
    when(mockConn.prepareStatement(anyString())).thenThrow(new SQLException("err"));

    assertThrows(UserException.class, () -> dao.recalculateSellerRating(1));
  }

  // =========================================================================
  //  PHẦN 14 – DELETEUSER
  // =========================================================================

  /**
   * deleteUser() thành công (affectedRows = 1).
   */
  @Test
  void testDeleteUser_Success() throws Exception {
    when(mockStmt.executeUpdate()).thenReturn(1);

    assertDoesNotThrow(() -> dao.deleteUser(1));
  }

  /**
   * deleteUser() khi affectedRows = 0 → throw UserException "Xóa thất bại".
   */
  @Test
  void testDeleteUser_NotFound_ThrowsUserException() throws Exception {
    when(mockStmt.executeUpdate()).thenReturn(0);

    UserException ex = assertThrows(UserException.class, () -> dao.deleteUser(999));
    assertTrue(ex.getMessage().contains("Xóa thất bại"));
  }

  /**
   * deleteUser() khi DB lỗi → throw UserException "Lỗi xóa dữ liệu".
   */
  @Test
  void testDeleteUser_SQLException_ThrowsUserException() throws Exception {
    when(mockConn.prepareStatement(anyString())).thenThrow(new SQLException("err"));

    UserException ex = assertThrows(UserException.class, () -> dao.deleteUser(1));
    assertTrue(ex.getMessage().contains("Lỗi xóa dữ liệu"));
  }

  // =========================================================================
  //  PHẦN 15 – THỐNG KÊ (getTotalUserCount / getBannedUserCount / getCustomerCount)
  // =========================================================================

  /**
   * getTotalUserCount() trả về số đúng từ DB.
   */
  @Test
  void testGetTotalUserCount_ReturnsCount() throws Exception {
    when(mockRs.next()).thenReturn(true);
    when(mockRs.getInt(1)).thenReturn(50);

    assertEquals(50, dao.getTotalUserCount());
  }

  /**
   * getTotalUserCount() khi rs.next() = false → trả về 0.
   */
  @Test
  void testGetTotalUserCount_NoRow_ReturnsZero() throws Exception {
    when(mockRs.next()).thenReturn(false);

    assertEquals(0, dao.getTotalUserCount());
  }

  /**
   * getTotalUserCount() khi DB lỗi → trả về 0 (không throw).
   */
  @Test
  void testGetTotalUserCount_SQLException_ReturnsZero() throws Exception {
    when(mockConn.prepareStatement(anyString())).thenThrow(new SQLException("err"));

    assertEquals(0, dao.getTotalUserCount());
  }

  /**
   * getBannedUserCount() trả về số đúng.
   */
  @Test
  void testGetBannedUserCount_ReturnsCount() throws Exception {
    when(mockRs.next()).thenReturn(true);
    when(mockRs.getInt(1)).thenReturn(3);

    assertEquals(3, dao.getBannedUserCount());
  }

  /**
   * getBannedUserCount() khi DB lỗi → trả về 0.
   */
  @Test
  void testGetBannedUserCount_SQLException_ReturnsZero() throws Exception {
    when(mockConn.prepareStatement(anyString())).thenThrow(new SQLException("err"));

    assertEquals(0, dao.getBannedUserCount());
  }

  /**
   * getCustomerCount() trả về số đúng.
   */
  @Test
  void testGetCustomerCount_ReturnsCount() throws Exception {
    when(mockRs.next()).thenReturn(true);
    when(mockRs.getInt(1)).thenReturn(120);

    assertEquals(120, dao.getCustomerCount());
  }

  /**
   * getCustomerCount() khi DB lỗi → trả về 0.
   */
  @Test
  void testGetCustomerCount_SQLException_ReturnsZero() throws Exception {
    when(mockConn.prepareStatement(anyString())).thenThrow(new SQLException("err"));

    assertEquals(0, dao.getCustomerCount());
  }

  // =========================================================================
  //  PHẦN 16 – HELPER NHÁNH PHỤ
  // =========================================================================

  /**
   * setStringOrNull: chuỗi rỗng / null → setNull (kiểm tra gián tiếp qua updateProfile).
   * Khi customer.email = "" và customer.phone = null, không được gây NPE.
   */
  @Test
  void testUpdateProfile_EmptyAndNullFields_NoException() throws Exception {
    dummyCustomer.setEmail("");        // → setNull
    dummyCustomer.setPhone(null);      // → setNull
    dummyCustomer.setFullName("  ");   // → setNull (chỉ khoảng trắng)
    when(mockStmt.executeUpdate()).thenReturn(1);

    assertDoesNotThrow(() -> dao.updateProfile(dummyCustomer));
  }

  /**
   * hasColumn() trả về false khi cột không tồn tại trong ResultSet
   * → Customer.avatar_data được bỏ qua, không NPE.
   */
  @Test
  void testMapResultSetToUser_AvatarColumnAbsent_NoException() throws Exception {
    // Cấu hình meta để hasColumn("avatar_data") = false
    when(mockMeta.getColumnCount()).thenReturn(1);
    when(mockMeta.getColumnLabel(1)).thenReturn("other_column"); // không phải avatar_data

    when(mockRs.next()).thenReturn(true);
    stubCustomerResultSet();

    // findById gọi mapResultSetToUser → hasColumn → không tìm thấy → bỏ qua setAvatarData
    assertDoesNotThrow(() -> dao.findById(1));
  }
}