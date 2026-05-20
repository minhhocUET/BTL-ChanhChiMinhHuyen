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
    String type = rs.getString("item_type");
    Item item;

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

    item.setInAuction(rs.getBoolean("in_auction"));
    item.setCity(rs.getString("city"));
    try {
      item.setImageData(rs.getString("image_data"));
    } catch (SQLException ignored) {
      // column may not exist until migration is applied
    }

    return item;
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
    String sql =
        "INSERT INTO items " +
            "    (seller_id, name, description, starting_price, image_path, image_data, city, " +
            "     in_auction, item_type, " +
            "     author, creation_year, material, " +
            "     brand, warranty_months, " +
            "     model, manufacturing_year, mileage, engine_type, fuel_type) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, " +
            "        FALSE, ?, " +
            "        ?, ?, ?, " +
            "        ?, ?, " +
            "        ?, ?, ?, ?, ?)";

    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

      // ── Cột chung ─────────────────────────────────────────────
      stmt.setInt(1, item.getSellerId());
      stmt.setString(2, item.getName());
      setStringOrNull(stmt, 3, item.getDescription());
      stmt.setBigDecimal(4, item.getStartingPrice());
      setStringOrNull(stmt, 5, null); // image_path unused
      setStringOrNull(stmt, 6, item.getImageData());
      setStringOrNull(stmt, 7, item.getCity());
      stmt.setString(8, item.getType()); // "ART" | "ELECTRONICS" | "VEHICLE"

      // ── Cột riêng từng loại ───────────────────────────────────
      if (item instanceof Art a) {
        setStringOrNull(stmt, 9, a.getAuthor());
        setIntOrNull(stmt, 10, a.getCreationYear() == 0 ? null : a.getCreationYear());
        setStringOrNull(stmt, 11, a.getMaterial());
        stmt.setNull(12, Types.VARCHAR); // brand
        stmt.setNull(13, Types.INTEGER); // warranty_months
        stmt.setNull(14, Types.VARCHAR); // model
        stmt.setNull(15, Types.INTEGER); // manufacturing_year
        stmt.setNull(16, Types.DOUBLE);  // mileage
        stmt.setNull(17, Types.VARCHAR); // engine_type
        stmt.setNull(18, Types.VARCHAR); // fuel_type

      } else if (item instanceof Electronics e) {
        stmt.setNull(9, Types.VARCHAR);  // author
        stmt.setNull(10, Types.INTEGER);  // creation_year
        stmt.setNull(11, Types.VARCHAR); // material
        setStringOrNull(stmt, 12, e.getBrand());
        setIntOrNull(stmt, 13, e.getWarrantyMonths());
        stmt.setNull(14, Types.VARCHAR); // model
        stmt.setNull(15, Types.INTEGER); // manufacturing_year
        stmt.setNull(16, Types.DOUBLE);  // mileage
        stmt.setNull(17, Types.VARCHAR); // engine_type
        stmt.setNull(18, Types.VARCHAR); // fuel_type

      } else if (item instanceof Vehicle v) {
        stmt.setNull(9, Types.VARCHAR);  // author
        stmt.setNull(10, Types.INTEGER);  // creation_year
        stmt.setNull(11, Types.VARCHAR); // material
        setStringOrNull(stmt, 12, v.getBrand());
        stmt.setNull(13, Types.INTEGER); // warranty_months
        setStringOrNull(stmt, 14, v.getModel());
        setIntOrNull(stmt, 15, v.getManufacturingYear());
        setDoubleOrNull(stmt, 16, v.getMileage());
        setStringOrNull(stmt, 17, v.getEngineType());
        setStringOrNull(stmt, 18, v.getFuelType());

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
      setStringOrNull(stmt, 4, null);
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

      stmt.setInt(17, item.getId());

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
   * Số item của một seller cụ thể.
   */
  public int getItemCountBySeller(int sellerId) {
    String sql = "SELECT COUNT(*) FROM items WHERE seller_id = ?";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, sellerId);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) return rs.getInt(1);
      }
    } catch (SQLException e) {
      e.printStackTrace();
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