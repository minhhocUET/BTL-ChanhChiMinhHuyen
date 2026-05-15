package com.uet.bidding;

import com.uet.bidding.dao.AuctionSqlDAO;
import com.uet.bidding.exception.AuctionClosedException;
import com.uet.bidding.exception.InvalidBidException;
import com.uet.bidding.exception.UserException;
import com.uet.bidding.model.Auction;
import com.uet.bidding.model.Customer;
import com.uet.bidding.model.Item;
import com.uet.bidding.model.ItemFactory; // Dùng Factory mới
import com.uet.bidding.service.AuctionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AuctionLogicTest {
  private AuctionManager manager;
  private AuctionSqlDAO mockDao;

  @BeforeEach
  void setUp() {
    mockDao = mock(AuctionSqlDAO.class);
    // Khởi tạo sạch cho mỗi lần test
    when(mockDao.getAllAuctions()).thenReturn(new ArrayList<>());

    manager = AuctionManager.getInstance();
    // manager.reset(); // Bỏ comment nếu bạn đã thêm hàm reset() vào AuctionManager
    manager.initialize(mockDao);
  }

  /**
   * Test trường hợp đặt giá vào phiên đã kết thúc
   */
  @Test
  void testBidOnClosedAuction() throws UserException, AuctionClosedException, InvalidBidException {
    int auctionId = 555;

    // 1. Tạo một món đồ Xe cộ bằng Factory mới (rất gọn)
    Item item = ItemFactory.createVehicle(
        "Tesla Model S", "Electric Car", new BigDecimal("50000"), "tesla.jpg", 1,
        "Tesla", "Model S", 2023, 0.0, "Electric", "Battery"
    );
    item.setId(105);

    // 2. Tạo phiên đấu giá giả lập đã FINISHED
    Auction auction = new Auction(item, new BigDecimal("50000"),
        LocalDateTime.now().minusHours(5), LocalDateTime.now().minusHours(1));
    auction.setId(auctionId);
    auction.setStatus("FINISHED");

    // 3. Mock hành vi của DAO: Trả về auction và ném lỗi khi placeBid
    when(mockDao.findById(auctionId)).thenReturn(auction);
    when(mockDao.placeBid(eq(auctionId), any(Customer.class), any(BigDecimal.class)))
        .thenThrow(new AuctionClosedException("Phiên đấu giá đã kết thúc!"));

    Customer customer = new Customer();
    customer.setUsername("UserLate");

    // 4. Kiểm tra xem ngoại lệ có văng ra đúng như dự kiến không
    assertThrows(AuctionClosedException.class, () -> {
      manager.placeBid(auctionId, customer, new BigDecimal("60000"));
    });
  }

  /**
   * Test tính toàn vẹn dữ liệu khi tạo phiên mới
   */
  @Test
  void testAuctionDataIntegrity() throws UserException {
    // 1. Tạo đồ điện tử bằng Factory
    Item item = ItemFactory.createElectronics(
        "iPhone 15", "New", new BigDecimal("1000"), "ip15.jpg", 1, "Apple", 12
    );
    item.setId(108);

    LocalDateTime endTime = LocalDateTime.now().plusHours(2);

    // 2. Giả lập khi Manager gọi DAO để tạo auction
    Auction mockAuction = new Auction(item, new BigDecimal("1000"), LocalDateTime.now(), endTime);
    mockAuction.setId(888);
    mockAuction.setStatus("RUNNING");

    when(mockDao.createAuction(eq(item), any(), any(), eq(endTime), any()))
        .thenReturn(mockAuction);

    // 3. Thực thi hành động
    Auction created = manager.createAuction(item, endTime);

    // 4. Kiểm tra kết quả
    assertNotNull(created);
    assertEquals(888, created.getId());
    assertEquals(0, new BigDecimal("1000").compareTo(created.getCurrentPrice()));
    assertEquals("RUNNING", created.getStatus());
    assertEquals("ELECTRONICS", created.getItem().getType());
  }
}