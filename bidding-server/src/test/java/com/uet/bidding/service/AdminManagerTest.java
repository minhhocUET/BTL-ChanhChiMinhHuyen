package com.uet.bidding.service;

import com.uet.bidding.dao.AuctionSqlDAO;
import com.uet.bidding.model.Admin;
import com.uet.bidding.model.Auction;
import com.uet.bidding.model.Item;
import com.uet.bidding.model.ItemFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AdminManagerTest {

  private AdminManager adminManager;
  private List<Auction> mockAuctions;
  private Admin admin;

  @BeforeEach
  public void setUp() {
    adminManager = AdminManager.getInstance();
    admin = new Admin(1, "adminRoot", "pass");
    mockAuctions = new ArrayList<>();

    // Khởi tạo một phiên OPEN hợp lệ để test
    Item item = ItemFactory.createArt(
        "Mona Lisa", "Tranh dầu", BigDecimal.valueOf(5000),
        "mona.png", 11, "Da Vinci", 1503, "Dầu trên gỗ"
    ); // Mock item
    item.setId(99);
    Auction auction = new Auction(101, item, BigDecimal.TEN, LocalDateTime.now(), LocalDateTime.now().plusHours(1), "OPEN");
    mockAuctions.add(auction);

    // Tạo một DAO giả để chặn kết nối DB thật
    AuctionSqlDAO fakeDao = new AuctionSqlDAO() {
      @Override
      public void deleteAuction(int id) {
        // Giả vờ xóa thành công, không kết nối DB
        System.out.println("Fake DAO deleted auction " + id);
      }
    };
    adminManager.initialize(fakeDao);
  }

  /**
   * Mục đích: Kịch bản CHUẨN - Admin có quyền xóa một phiên OPEN hợp lệ.
   */
  @Test
  public void testExecuteRemoveAuctionSuccess() {
    boolean result = adminManager.executeRemoveAuction(admin, 101, mockAuctions);
    assertTrue(result, "Phải cho phép xóa phiên OPEN hợp lệ");
    assertEquals(0, mockAuctions.size(), "Danh sách RAM phải trống sau khi xóa");
  }

  /**
   * Mục đích: Kịch bản LỖI - Không tìm thấy ID để xóa.
   */
  @Test
  public void testExecuteRemoveAuctionNotFound() {
    boolean result = adminManager.executeRemoveAuction(admin, 999, mockAuctions);
    assertFalse(result, "ID không tồn tại thì phải trả về false");
  }
}