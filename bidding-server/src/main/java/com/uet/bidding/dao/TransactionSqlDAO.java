package com.uet.bidding.dao;

import com.uet.bidding.model.Transaction;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO cho bảng transactions – ghi nhận mọi biến động số dư tài khoản
 */
public class TransactionSqlDAO {

  /**
   * Thêm mới một transaction.
   *
   * @param transaction đối tượng Transaction chưa có id (sẽ được set sau khi insert)
   * @throws SQLException nếu lỗi
   */
  public void addTransaction(Transaction transaction) throws SQLException {
    String sql = "INSERT INTO transactions (user_id, auction_id, amount, type, status, created_at) " +
        "VALUES (?, ?, ?, ?, ?, ?)";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

      stmt.setInt(1, transaction.getUserId());
      if (transaction.getAuctionId() != null) {
        stmt.setInt(2, transaction.getAuctionId());
      } else {
        stmt.setNull(2, Types.INTEGER);
      }
      stmt.setBigDecimal(3, transaction.getAmount());
      stmt.setString(4, transaction.getType());
      stmt.setString(5, transaction.getStatus());
      stmt.setTimestamp(6, Timestamp.valueOf(transaction.getCreatedAt()));

      stmt.executeUpdate();

      try (ResultSet rs = stmt.getGeneratedKeys()) {
        if (rs.next()) {
          transaction.setId(rs.getInt(1));
        }
      }
    }
  }

  /**
   * Lấy tất cả transaction của một user, sắp xếp mới nhất lên đầu.
   */
  public List<Transaction> getTransactionsByUserId(int userId) throws SQLException {
    List<Transaction> list = new ArrayList<>();
    String sql = "SELECT * FROM transactions WHERE user_id = ? ORDER BY created_at DESC";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, userId);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          list.add(mapResultSet(rs));
        }
      }
    }
    return list;
  }

  /**
   * Lấy transaction theo id.
   */
  public Transaction findById(int id) throws SQLException {
    String sql = "SELECT * FROM transactions WHERE id = ?";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, id);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return mapResultSet(rs);
        }
      }
    }
    return null;
  }

  /**
   * Cập nhật status của transaction (ví dụ từ PENDING -> SUCCESS/FAILED)
   */
  public void updateStatus(int transactionId, String newStatus) throws SQLException {
    String sql = "UPDATE transactions SET status = ? WHERE id = ?";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, newStatus);
      stmt.setInt(2, transactionId);
      stmt.executeUpdate();
    }
  }

  // =========================================================
  //  PRIVATE HELPER
  // =========================================================

  private Transaction mapResultSet(ResultSet rs) throws SQLException {
    Transaction tx = new Transaction();
    tx.setId(rs.getInt("id"));
    tx.setUserId(rs.getInt("user_id"));
    int auctionId = rs.getInt("auction_id");
    if (!rs.wasNull()) tx.setAuctionId(auctionId);
    tx.setAmount(rs.getBigDecimal("amount"));
    tx.setType(rs.getString("type"));
    tx.setStatus(rs.getString("status"));
    Timestamp ts = rs.getTimestamp("created_at");
    if (ts != null) tx.setCreatedAt(ts.toLocalDateTime());
    return tx;
  }
}