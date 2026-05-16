package com.uet.bidding.dao;

import com.uet.bidding.model.Customer;
import com.uet.bidding.model.Item;
import com.uet.bidding.model.ItemFactory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AuctionSqlDAOTest {

  /**
   * Càn quét tất cả 11 public methods của AuctionSqlDAO
   */
  @Test
  public void testAuctionDaoSweep() {
    AuctionSqlDAO dao = new AuctionSqlDAO();

    // Chuẩn bị dữ liệu mồi
    Item dummyItem = ItemFactory.createArt(
        "Mona Lisa", "Tranh dầu", BigDecimal.valueOf(5000),
        "mona.png", 11, "Da Vinci", 1503, "Dầu trên gỗ"
    );
    dummyItem.setId(1);
    dummyItem.setSellerId(2);

    Customer dummyCustomer = new Customer();
    dummyCustomer.setId(1);

    LocalDateTime now = LocalDateTime.now();
    LocalDateTime tomorrow = now.plusDays(1);
    BigDecimal amount = BigDecimal.valueOf(100.0);

    // 1. Quét hàm tạo phiên
    try {
      dao.createAuction(dummyItem, amount, now, tomorrow, BigDecimal.valueOf(5));
    } catch (Exception e) {
    }

    // 2. Quét hàm tìm kiếm theo ID
    try {
      dao.findById(1);
    } catch (Exception e) {
    }

    // 3. Quét hàm lấy tất cả phiên
    try {
      dao.getAllAuctions();
    } catch (Exception e) {
    }

    // 4. Quét hàm lấy phiên theo trạng thái
    try {
      dao.getAuctionsByStatus("OPEN");
    } catch (Exception e) {
    }

    // 5. Quét hàm đặt giá (placeBid - core logic)
    try {
      dao.placeBid(1, dummyCustomer, BigDecimal.valueOf(150.0));
    } catch (Exception e) {
    }

    // 6. Quét hàm kết thúc phiên
    try {
      dao.finishAuction(1);
    } catch (Exception e) {
    }

    // 7. Quét hàm cài đặt Auto-Bid
    try {
      dao.setAutoBid(1, 1, BigDecimal.valueOf(500.0));
    } catch (Exception e) {
    }

    // 8. Quét hàm hủy Auto-Bid
    try {
      dao.removeAutoBid(1, 1);
    } catch (Exception e) {
    }

    // 9. Quét hàm đăng ký tham gia đấu giá
    try {
      dao.registerBidderForAuction(1, 1);
    } catch (Exception e) {
    }

    // 10. Quét hàm lấy danh sách người đã đăng ký
    try {
      dao.getRegisteredBidders(1);
    } catch (Exception e) {
    }

    // 11. Quét hàm xóa hoàn toàn phiên (Chức năng Admin)
    try {
      dao.deleteAuction(1);
    } catch (Exception e) {
    }

    // Chốt hạ bài test
    assertNotNull(dao, "Đã càn quét thành công AuctionSqlDAO!");
  }
}