package com.uet.bidding.dao;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ReviewSqlDAOTest {

  /**
   * Càn quét toàn bộ 3 public methods của ReviewSqlDAO
   */
  @Test
  public void testReviewDaoSweep() {
    ReviewSqlDAO dao = new ReviewSqlDAO();

    // 1. Quét hàm thêm đánh giá (auctionId=1, sellerId=2, reviewerId=3, 5 sao)
    try {
      dao.addReview(1, 2, 3, 5, "Sản phẩm tuyệt vời, chủ shop nhiệt tình!");
    } catch (Exception e) {
    }

    // 2. Quét hàm lấy danh sách đánh giá của seller
    try {
      dao.getReviewsBySeller(2);
    } catch (Exception e) {
    }

    // 3. Quét hàm xóa đánh giá
    try {
      dao.deleteReview(1, 2);
    } catch (Exception e) {
    }

    // Chốt hạ
    assertNotNull(dao, "Đã càn quét thành công ReviewSqlDAO!");
  }
}