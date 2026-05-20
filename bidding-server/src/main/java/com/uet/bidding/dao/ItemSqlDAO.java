package com.uet.bidding.dao;

import com.uet.bidding.exception.ItemException;
import com.uet.bidding.model.Art;
import com.uet.bidding.model.Electronics;
import com.uet.bidding.model.Item;
import com.uet.bidding.model.Vehicle;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ItemSqlDAO – CRUD đầy đủ cho bảng {@code items}.
 * <p>
 * ┌────────────────────────────────────────────────────────────────────┐
 * │  Schema (single table, khớp với ERD)                               │
 * │                                                                    │
 * │  items: id, seller_id, name, description, starting_price,         │
 * │          image_path, city, in_auction, item_type(ENUM),            │
 * │          author, creation_year, material,          ← Art          │
 * │          brand, warranty_months,                   ← Electronics  │
 * │          brand, model, manufacturing_year,                         │
 * │          mileage, engine_type, fuel_type,          ← Vehicle      │
 * │          created_at                                                │
 * └────────────────────────────────────────────────────────────────────┘
 * <p>
 * Model hierarchy (mirror của User/Admin/Customer):
 * Entity
 * └── Item  (abstract)  ← getType() trả về "ART" | "ELECTRONICS" | "VEHICLE"
 * ├── Art
 * ├── Electronics
 * └── Vehicle
 * <p>
 * Quy tắc:
 * - mapResultSetToItem() đọc item_type rồi new đúng subclass.
 * - addItem() dùng 1 INSERT duy nhất (single table), các cột không dùng để NULL.
 * - Xóa item → ON DELETE CASCADE tự dọn auctions liên quan.
 */
public class ItemSqlDAO {

  // =========================================================
  //  PRIVATE HELPERS
  // =========================================================

  /**
   * Câu SELECT đầy đủ tất cả cột, dùng chung cho mọi truy vấn.
   */
  private static final String BASE_SELECT = "SELECT * FROM items ";

  private void setStringOrNull(PreparedStatement stmt, int index, String value)
      throws SQLException {
    if (value == null || value.trim().isEmpty())
      stmt.setNull(index, Types.VARCHAR);
    else
      stmt.setString(index, value.trim());
  }

  private void setIntOrNull(PreparedStatement stmt, int index, Integer value)
      throws SQLException {
    if (value == null) stmt.setNull(index, Types.INTEGER);
    else stmt.setInt(index, value);
  }

  private void setDoubleOrNull(PreparedStatement stmt, int index, Double value)
      throws SQLException {
    if (value == null) stmt.setNull(index, Types.DOUBLE);
    else stmt.setDouble(index, value);
  }

  /**
   * Ánh xạ ResultSet → đúng subclass dựa vào cột {@code item_type}.
   * Cột chung (name, description, starting_price, ...) được nạp trước,
   * sau đó nạp thêm các trường riêng của từng loại.
   */
  private Item mapResultSetToItem(ResultSet rs) throws SQLException {
    // 1. Ép hoa để tránh lỗi electronics vs ELECTRONICS
    String typeFromDb = rs.getString("item_type");
    String type = (typeFromDb != null) ? typeFromDb.toUpperCase().trim() : "";

    Item item;

    try {
      switch (type) {
        case "ART" -> {
          Art art = new Art(
              rs.getInt("id"),
              rs.getString("name"),
              rs.getString("description"),
              rs.getBigDecimal("starting_price"),
              rs.getString("image_path"),
              rs.getInt("seller_id"),
              rs.getString("author"),
              rs.getInt("creation_year"),
              rs.getString("material")
          );
          item = art;
        }
        case "ELECTRONICS" -> {
          Electronics elec = new Electronics(
              rs.getInt("id"),
              rs.getString("name"),
              rs.getString("description"),
              rs.getBigDecimal("starting_price"),
              rs.getString("image_path"),
              rs.getInt("seller_id"),
              rs.getString("brand"),
              rs.getInt("warranty_months")
          );
          item = elec;
        }
        case "VEHICLE" -> {
          // manufacturingYear và mileage có thể NULL trong DB
          int mfYear = rs.getInt("manufacturing_year");
          Integer manufacturingYear = rs.wasNull() ? null : mfYear;

          double mil = rs.getDouble("mileage");
          Double mileage = rs.wasNull() ? null : mil;

          Vehicle vehicle = new Vehicle(
              rs.getInt("id"),
              rs.getString("name"),
              rs.getString("description"),
              rs.getBigDecimal("starting_price"),
              rs.getString("image_path"),
              rs.getInt("seller_id"),
              rs.getString("brand"),
              rs.getString("model"),
              manufacturingYear,
              mileage,
              rs.getString("engine_type"),
              rs.getString("fuel_type")
          );
          item = vehicle;
        }
        default -> throw new SQLException("Loại item không hợp lệ trong DB: " + type);
      }

      // 2. Gán các thuộc tính chung
      item.setInAuction(rs.getBoolean("in_auction"));
      item.setCity(rs.getString("city"));
      item.setStatus(rs.getString("status"));
      return item;
    } catch (Exception e) {
      System.err.println("❌ Lỗi khi tạo Object Item từ ResultSet: " + e.getMessage());
      return null;
    }
  }

  // =========================================================
  //  CREATE
  // =========================================================

  /**
   * Thêm mới một Item (Art / Electronics / Vehicle) vào database.
   * Dùng single INSERT, các cột không thuộc loại này để NULL.
   *
   * @throws ItemException nếu lỗi SQL.
   */
  public void addItem(Item item) throws ItemException {
    // Thay đổi: Dùng 19 dấu ? cho 19 cột, không hardcode chữ FALSE vào chuỗi SQL nữa
    String sql =
        "INSERT INTO items (" +
            "seller_id, name, description, starting_price, image_path, city, " +
            "in_auction, item_type, status, " +
            "author, creation_year, material, " +
            "brand, warranty_months, " +
            "model, manufacturing_year, mileage, engine_type, fuel_type) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

      // --- 9 cột cơ bản dùng chung ---
      stmt.setInt(1, item.getSellerId());
      stmt.setString(2, item.getName());
      setStringOrNull(stmt, 3, item.getDescription());
      stmt.setBigDecimal(4, item.getStartingPrice());
      setStringOrNull(stmt, 5, item.getImagePath());
      setStringOrNull(stmt, 6, item.getCity());

      // Vị trí số 7 và 9 xử lý trực tiếp trạng thái mặc định
      stmt.setBoolean(7, false); // in_auction: tinyint(1) nhận false sẽ lưu là 0
      stmt.setString(8, item.getType());
      stmt.setString(9, "PENDING"); // status: Mặc định chờ duyệt

      // --- Các cột riêng biệt (từ index 10 đến 19) ---
      if (item instanceof Art a) {
        setStringOrNull(stmt, 10, a.getAuthor());
        setIntOrNull(stmt, 11, a.getCreationYear() == 0 ? null : a.getCreationYear());
        setStringOrNull(stmt, 12, a.getMaterial());

        // Các cột không liên quan đến Art thì setNull
        stmt.setNull(13, Types.VARCHAR); // brand
        stmt.setNull(14, Types.INTEGER); // warranty_months
        stmt.setNull(15, Types.VARCHAR); // model
        stmt.setNull(16, Types.INTEGER); // manufacturing_year
        stmt.setNull(17, Types.DOUBLE);  // mileage
        stmt.setNull(18, Types.VARCHAR); // engine_type
        stmt.setNull(19, Types.VARCHAR); // fuel_type

      } else if (item instanceof Electronics e) {
        stmt.setNull(10, Types.VARCHAR);  // author
        stmt.setNull(11, Types.INTEGER);  // creation_year
        stmt.setNull(12, Types.VARCHAR);  // material

        setStringOrNull(stmt, 13, e.getBrand());
        setIntOrNull(stmt, 14, e.getWarrantyMonths());

        stmt.setNull(15, Types.VARCHAR); // model
        stmt.setNull(16, Types.INTEGER); // manufacturing_year
        stmt.setNull(17, Types.DOUBLE);  // mileage
        stmt.setNull(18, Types.VARCHAR); // engine_type
        stmt.setNull(19, Types.VARCHAR); // fuel_type

      } else if (item instanceof Vehicle v) {
        stmt.setNull(10, Types.VARCHAR);  // author
        stmt.setNull(11, Types.INTEGER);  // creation_year
        stmt.setNull(12, Types.VARCHAR);  // material

        setStringOrNull(stmt, 13, v.getBrand());
        stmt.setNull(14, Types.INTEGER);  // warranty_months

        setStringOrNull(stmt, 15, v.getModel());
        setIntOrNull(stmt, 16, v.getManufacturingYear());
        setDoubleOrNull(stmt, 17, v.getMileage());
        setStringOrNull(stmt, 18, v.getEngineType());
        setStringOrNull(stmt, 19, v.getFuelType());

      } else {
        throw new ItemException("Loại Item không được hỗ trợ: " + item.getClass().getSimpleName());
      }

      stmt.executeUpdate();

      try (ResultSet gk = stmt.getGeneratedKeys()) {
        if (!gk.next()) throw new SQLException("Không tạo được ID cho Item.");
        item.setId(gk.getInt(1));
      }

    } catch (SQLException e) {
      throw new ItemException("Lỗi SQL khi thêm item: " + e.getMessage());
    }
  }

  // =========================================================
  //  READ – danh sách
  // =========================================================

  /**
   * Toàn bộ item trong hệ thống (dùng cho Admin hoặc trang browse).
   */
  public List<Item> getAllItems() {
    return queryList(BASE_SELECT + "ORDER BY created_at DESC", null);
  }

  /**
   * Tất cả item của một seller cụ thể (dùng cho trang quản lý kho của Seller).
   *
   * @param sellerId id của seller.
   */
  public List<Item> getItemsBySeller(int sellerId) {
    return queryList(
        BASE_SELECT + "WHERE seller_id = ? ORDER BY created_at DESC",
        stmt -> stmt.setInt(1, sellerId)
    );
  }

  /**
   * Tất cả item theo loại: "ART", "ELECTRONICS", hoặc "VEHICLE".
   */
  public List<Item> getItemsByType(String type) {
    return queryList(
        BASE_SELECT + "WHERE item_type = ? ORDER BY created_at DESC",
        stmt -> stmt.setString(1, type.toUpperCase())
    );
  }

  /**
   * HÀM ĐƯỢC THÊM MỚI: Lấy danh sách sản phẩm lọc theo trạng thái duyệt (Ví dụ: "PENDING")
   */
  public List<Item> getItemsByStatus(String status) {
    return queryList(
        BASE_SELECT + "WHERE status = ? ORDER BY id ASC",
        stmt -> stmt.setString(1, status.toUpperCase())
    );
  }

  /**
   * Tất cả item hiện CHƯA được đưa vào đấu giá (in_auction = FALSE).
   * Dùng khi Seller muốn tạo phiên đấu giá mới từ kho hàng.
   *
   * @param sellerId id của seller.
   */
  public List<Item> getAvailableItemsBySeller(int sellerId) {
    return queryList(
        BASE_SELECT + "WHERE seller_id = ? AND in_auction = FALSE ORDER BY created_at DESC",
        stmt -> stmt.setInt(1, sellerId)
    );
  }

  /**
   * Hàm nội bộ thực thi SELECT trả về List.
   */
  private List<Item> queryList(String sql, StatementSetter setter) {
    List<Item> items = new ArrayList<>();
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {

      if (setter != null) setter.set(stmt);

      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) items.add(mapResultSetToItem(rs));
      }
    } catch (SQLException e) {
      System.err.println("Lỗi load danh sách item: " + e.getMessage());
    }
    return items;
  }

  // =========================================================
  //  READ – tìm kiếm theo khoá
  // =========================================================

  /**
   * Tìm Item theo id.
   *
   * @throws ItemException nếu không tìm thấy.
   */
  public Item findById(int id) throws ItemException {
    String sql = BASE_SELECT + "WHERE id = ?";

    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setInt(1, id);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) return mapResultSetToItem(rs);
      }

    } catch (SQLException e) {
      throw new ItemException("Lỗi truy vấn theo ID: " + e.getMessage());
    }
    throw new ItemException("Không tìm thấy Item với ID: " + id);
  }

  // =========================================================
  //  UPDATE – thông tin sản phẩm
  // =========================================================

  /**
   * HÀM ĐƯỢC THÊM MỚI: Cập nhật trạng thái duyệt mới cho sản phẩm (APPROVED hoặc REJECTED)
   * Trả về true nếu cập nhật thành công ít nhất 1 dòng trong DB.
   */
  public boolean updateItemStatus(int itemId, String newStatus) {
    String sql = "UPDATE items SET status = ? WHERE id = ?";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, newStatus.toUpperCase());
      pstmt.setInt(2, itemId);

      return pstmt.executeUpdate() > 0;
    } catch (SQLException e) {
      System.err.println("❌ Lỗi cập nhật trạng thái sản phẩm trong ItemSqlDAO: " + e.getMessage());
      return false;
    }
  }

  /**
   * Cập nhật thông tin sản phẩm.
   * Chỉ cập nhật các trường có thể thay đổi sau khi tạo;
   * {@code seller_id} và {@code item_type} là bất biến.
   *
   * @throws ItemException nếu item không tồn tại hoặc lỗi SQL.
   */
  public void updateItem(Item item) throws ItemException {
    String sql =
        "UPDATE items SET " +
            "    name              = ?, " +
            "    description       = ?, " +
            "    starting_price    = ?, " +
            "    image_path        = ?, " +
            "    image_data        = ?, " +
            "    city              = ?, " +
            "    author            = ?, " +
            "    creation_year     = ?, " +
            "    material          = ?, " +
            "    brand             = ?, " +
            "    warranty_months   = ?, " +
            "    model             = ?, " +
            "    manufacturing_year= ?, " +
            "    mileage           = ?, " +
            "    engine_type       = ?, " +
            "    fuel_type         = ? " +
            "WHERE id = ?";

    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setString(1, item.getName());
      setStringOrNull(stmt, 2, item.getDescription());
      stmt.setBigDecimal(3, item.getStartingPrice());
      setStringOrNull(stmt, 4, item.getImagePath());
      setStringOrNull(stmt, 5, item.getImageData());
      setStringOrNull(stmt, 6, item.getCity());

      if (item instanceof Art a) {
        setStringOrNull(stmt, 7, a.getAuthor());
        setIntOrNull(stmt, 8, a.getCreationYear() == 0 ? null : a.getCreationYear());
        setStringOrNull(stmt, 9, a.getMaterial());
        stmt.setNull(10, Types.VARCHAR);
        stmt.setNull(11, Types.INTEGER);
        stmt.setNull(12, Types.VARCHAR);
        stmt.setNull(13, Types.INTEGER);
        stmt.setNull(14, Types.DOUBLE);
        stmt.setNull(15, Types.VARCHAR);
        stmt.setNull(16, Types.VARCHAR);

      } else if (item instanceof Electronics e) {
        stmt.setNull(7, Types.VARCHAR);
        stmt.setNull(8, Types.INTEGER);
        stmt.setNull(9, Types.VARCHAR);
        setStringOrNull(stmt, 10, e.getBrand());
        setIntOrNull(stmt, 11, e.getWarrantyMonths());
        stmt.setNull(12, Types.VARCHAR);
        stmt.setNull(13, Types.INTEGER);
        stmt.setNull(14, Types.DOUBLE);
        stmt.setNull(15, Types.VARCHAR);
        stmt.setNull(16, Types.VARCHAR);

      } else if (item instanceof Vehicle v) {
        stmt.setNull(7, Types.VARCHAR);
        stmt.setNull(8, Types.INTEGER);
        stmt.setNull(9, Types.VARCHAR);
        setStringOrNull(stmt, 10, v.getBrand());
        stmt.setNull(11, Types.INTEGER);
        setStringOrNull(stmt, 12, v.getModel());
        setIntOrNull(stmt, 13, v.getManufacturingYear());
        setDoubleOrNull(stmt, 14, v.getMileage());
        setStringOrNull(stmt, 15, v.getEngineType());
        setStringOrNull(stmt, 16, v.getFuelType());
      }

      stmt.setInt(17, item.getId()); // ID đẩy về số 17 mới chuẩn

      if (stmt.executeUpdate() == 0)
        throw new ItemException("Cập nhật thất bại! Không tìm thấy Item ID: " + item.getId());

    } catch (SQLException e) {
      throw new ItemException("Lỗi SQL khi cập nhật item: " + e.getMessage());
    }
  }

  // =========================================================
  //  UPDATE – trạng thái đấu giá
  // =========================================================

  /**
   * Cập nhật cờ {@code in_auction} khi phiên đấu giá bắt đầu hoặc kết thúc.
   * Được gọi bởi AuctionDAO khi tạo / kết thúc / hủy phiên.
   *
   * @param itemId    id của item.
   * @param inAuction TRUE khi phiên bắt đầu, FALSE khi phiên kết thúc / bị hủy.
   * @throws ItemException nếu không tìm thấy item.
   */
  public void setInAuction(int itemId, boolean inAuction) throws ItemException {
    String sql = "UPDATE items SET in_auction = ? WHERE id = ?";

    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setBoolean(1, inAuction);
      stmt.setInt(2, itemId);

      if (stmt.executeUpdate() == 0)
        throw new ItemException("Không tìm thấy Item với ID: " + itemId);

    } catch (SQLException e) {
      throw new ItemException("Lỗi cập nhật trạng thái đấu giá: " + e.getMessage());
    }
  }

  // =========================================================
  //  DELETE
  // =========================================================

  /**
   * Xóa Item theo id.
   * Chỉ cho phép xóa khi item CHƯA trong đấu giá ({@code in_auction = FALSE}).
   * ON DELETE CASCADE trong DB sẽ tự dọn auctions liên quan nếu cần.
   *
   * @throws ItemException nếu item đang trong đấu giá hoặc không tồn tại.
   */
  public void deleteItem(int id) throws ItemException {
    // Kiểm tra trạng thái trước khi xóa
    String checkSql = "SELECT in_auction FROM items WHERE id = ?";
    String deleteSql = "DELETE FROM items WHERE id = ?";

    try (Connection conn = DatabaseConnection.getConnection()) {

      try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
        checkStmt.setInt(1, id);
        try (ResultSet rs = checkStmt.executeQuery()) {
          if (!rs.next())
            throw new ItemException("Không tìm thấy Item với ID: " + id);
          if (rs.getBoolean("in_auction"))
            throw new ItemException("Không thể xóa sản phẩm đang trong phiên đấu giá!");
        }
      }

      try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
        deleteStmt.setInt(1, id);
        deleteStmt.executeUpdate();
      }

    } catch (SQLException e) {
      throw new ItemException("Lỗi xóa item: " + e.getMessage());
    }
  }
  public String getImagePath(int itemId) {
    String sql = "SELECT image_path FROM items WHERE id = ?";
    try (Connection conn = DatabaseConnection.getConnection(); // Thay bằng cách lấy Conn của bạn
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setInt(1, itemId);
      try (ResultSet rs = pstmt.executeQuery()) {
        if (rs.next()) {
          return rs.getString("image_path");
        }
      }
    } catch (SQLException e) {
      System.err.println("❌ Lỗi lấy path ảnh: " + e.getMessage());
    }
    return null;
  }

  // 3. Hàm XÓA HOÀN TOÀN sản phẩm (Admin xóa hoặc Seller tự rút lại bài)
  public boolean deleteItemCompletely(int itemId) {
    // 1. Lấy path ảnh trước
    String imagePath = getImagePath(itemId);

    // 2. Xóa trong Database
    String sql = "DELETE FROM items WHERE id = ?";
    boolean dbDeleted = false;

    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setInt(1, itemId);
      dbDeleted = pstmt.executeUpdate() > 0;

    } catch (SQLException e) {
      System.err.println("❌ Lỗi xóa SP trong DB: " + e.getMessage());
      return false;
    }

    // 3. Nếu DB xóa xong thì dọn file trên ổ cứng
    if (dbDeleted && imagePath != null) {
      try {
        java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(imagePath));
        System.out.println("✅ Đã dọn dẹp file: " + imagePath);
      } catch (Exception e) {
        System.err.println("⚠️ Không xóa được file vật lý: " + e.getMessage());
      }
    }

    return dbDeleted;
  }

  // =========================================================
  //  THỐNG KÊ
  // =========================================================

  /**
   * Tổng số item trong hệ thống.
   */
  public int getTotalItemCount() {
    return countByQuery("SELECT COUNT(*) FROM items");
  }

  /**
   * Số item đang trong phiên đấu giá.
   */
  public int getInAuctionCount() {
    return countByQuery("SELECT COUNT(*) FROM items WHERE in_auction = TRUE");
  }

  /**
   * Tổng số sản phẩm đang chờ Admin duyệt (status = PENDING).
   */
  public int getPendingItemsCount() {
    return countByQuery("SELECT COUNT(*) FROM items WHERE status = 'PENDING'");
  }

  // Trong ItemSqlDAO.java
  public List<Item> getPendingItems() {
    return queryList( BASE_SELECT +  " WHERE status = 'PENDING'", null);
  }
  /**
   * Số item của một seller cụ thể.
   */
  public int getItemCountBySeller(int sellerId) {
    String sql = "SELECT COUNT(*) FROM items WHERE seller_id = ?";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {

      // 1. Phải nạp tham số vào dấu ? TRƯỚC
      stmt.setInt(1, sellerId);

      // 2. Sau đó mới thực thi câu lệnh truy vấn SAU
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return rs.getInt(1);
        }
      }
    } catch (SQLException e) {
      System.err.println("❌ Lỗi đếm sản phẩm của Seller: " + e.getMessage());
    }
    return 0;
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

  @FunctionalInterface
  private interface StatementSetter {
    void set(PreparedStatement stmt) throws SQLException;
  }
}