package com.uet.bidding.dao;

import com.uet.bidding.exception.UserException;
import com.uet.bidding.model.Customer;
import com.uet.bidding.model.Review;

import java.sql.*;
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
  public List<Review> getReviewsBySeller(int sellerId) {
    List<Review> list = new ArrayList<>();
    String sql = "SELECT * FROM reviews WHERE seller_id = ? ORDER BY created_at DESC";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, sellerId);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          int reviewerId = rs.getInt("reviewer_id");
          Customer reviewer = (Customer) userDao.findById(reviewerId);
          Review review = new Review(reviewer, rs.getInt("stars"), rs.getString("comment"));
          list.add(review);
        }
      }
    } catch (SQLException | UserException e) {
      System.err.println("Lỗi lấy danh sách review: " + e.getMessage());
    }
    return list;
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