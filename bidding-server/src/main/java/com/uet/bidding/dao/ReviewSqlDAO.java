package com.uet.bidding.dao;

import com.uet.bidding.exception.UserException;
import com.uet.bidding.model.Customer;
import com.uet.bidding.model.Review;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * ReviewSqlDAO – quản lý đánh giá (bảng reviews).
 * Tự động cập nhật rating trung bình của seller sau mỗi lần thêm/sửa/xóa review.
 */
public class ReviewSqlDAO {

  private final UserSqlDAO userDao = new UserSqlDAO();

  /**
   * Thêm đánh giá cho một seller sau khi phiên đấu giá kết thúc.
   *
   * @param auctionId  id phiên (ràng buộc UNIQUE trong bảng reviews)
   * @param sellerId   id người bán
   * @param reviewerId id người đánh giá (thường là người thắng đấu giá)
   * @param stars      số sao (1-5)
   * @param comment    nội dung
   */
  public void addReview(int auctionId, int sellerId, int reviewerId, int stars, String comment)
      throws UserException {
    String sql = "INSERT INTO reviews (auction_id, seller_id, reviewer_id, stars, comment) VALUES (?, ?, ?, ?, ?)";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, auctionId);
      stmt.setInt(2, sellerId);
      stmt.setInt(3, reviewerId);
      stmt.setInt(4, stars);
      stmt.setString(5, comment);
      stmt.executeUpdate();
      // Cập nhật lại rating trung bình cho seller
      userDao.recalculateSellerRating(sellerId);
    } catch (SQLException e) {
      throw new UserException("Lỗi khi thêm review: " + e.getMessage());
    }
  }

  /**
   * Lấy tất cả đánh giá của một seller.
   *
   * @param sellerId id người bán
   * @return List<Review>, mỗi review chứa đối tượng Customer (người đánh giá)
   */
  /**
   * 🌟 ĐÃ SỬA: Thay đổi u.username thành u.full_name để lấy tên thật khách hàng
   */
  public List<Review> getReviewsBySeller(int sellerId) {
    List<Review> list = new ArrayList<>();

    // 🚀 ĐÃ ĐỔI: u.username AS reviewerName ➡️ CHUYỂN THÀNH u.full_name AS reviewerName
    // (Nếu DB của bạn đặt tên cột này là 'name' hoặc 'display_name' thì bạn thay chữ full_name thành tên cột đó nhé)
    String sql = "SELECT r.*, u.full_name AS reviewerName, i.name AS productName " +
        "FROM reviews r " +
        "JOIN users u ON r.reviewer_id = u.id " +
        "JOIN auctions a ON r.auction_id = a.id " +
        "JOIN items i ON a.item_id = i.id " +
        "WHERE r.seller_id = ? " +
        "ORDER BY r.created_at DESC";

    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setInt(1, sellerId);

      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          Review review = new Review();
          review.setId(rs.getInt("id"));
          review.setAuctionId(rs.getInt("auction_id"));
          review.setSellerId(rs.getInt("seller_id"));
          review.setStars(rs.getInt("stars"));
          review.setComment(rs.getString("comment"));

          review.setReviewerName(rs.getString("reviewerName"));
          review.setProductName(rs.getString("productName"));

          // Giữ nguyên logic lấy giờ thô, chúng ta sẽ ép múi giờ ở DatabaseConnection
          java.sql.Timestamp ts = rs.getTimestamp("created_at");
          if (ts != null) {
            review.setCreatedAt(ts.toLocalDateTime());
          }

          list.add(review);
        }
      }
    } catch (SQLException e) {
      System.err.println("Lỗi lấy danh sách review: " + e.getMessage());
    }
    return list;
  }

  public boolean hasReviewForAuction(int auctionId, int reviewerId) {
    String sql = "SELECT 1 FROM reviews WHERE auction_id = ? AND reviewer_id = ? LIMIT 1";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, auctionId);
      stmt.setInt(2, reviewerId);
      try (ResultSet rs = stmt.executeQuery()) {
        return rs.next();
      }
    } catch (SQLException e) {
      System.err.println("Lỗi kiểm tra review: " + e.getMessage());
    }
    return false;
  }

  /**
   * Xóa đánh giá (dùng cho Admin nếu cần). Sau khi xóa, tự động cập nhật lại rating.
   */
  public void deleteReview(int reviewId, int sellerId) throws UserException {
    String sql = "DELETE FROM reviews WHERE id = ?";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, reviewId);
      stmt.executeUpdate();
      userDao.recalculateSellerRating(sellerId);
    } catch (SQLException e) {
      throw new UserException("Lỗi xóa review: " + e.getMessage());
    }
  }
}