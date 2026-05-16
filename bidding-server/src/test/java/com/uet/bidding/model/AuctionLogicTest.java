package com.uet.bidding.model;

import com.uet.bidding.dao.AuctionSqlDAO;
import com.uet.bidding.exception.AuctionClosedException;
import com.uet.bidding.exception.UserException;
import com.uet.bidding.service.AuctionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AuctionLogicTest {
  private AuctionManager manager;
  private AuctionSqlDAO mockDao;

  @BeforeEach
  void setUp() throws Exception {
    mockDao = mock(AuctionSqlDAO.class);
    // Khởi tạo sạch cho mỗi lần test
    when(mockDao.getAllAuctions()).thenReturn(new ArrayList<>());

    manager = AuctionManager.getInstance();

    // --- DÙNG REFLECTION ĐỂ LÀM SẠCH RAM CỦA SINGLETON ---
    clearManagerMemory();

    manager.initialize(mockDao);
  }

  /**
   * Hàm phụ trợ để chọc vào field private và xóa dữ liệu cũ
   */
  private void clearManagerMemory() throws Exception {
    Field auctionsField = AuctionManager.class.getDeclaredField("auctions");
    auctionsField.setAccessible(true);
    ((ConcurrentHashMap<?, ?>) auctionsField.get(manager)).clear();

    Field locksField = AuctionManager.class.getDeclaredField("locks");
    locksField.setAccessible(true);
    ((ConcurrentHashMap<?, ?>) locksField.get(manager)).clear();
  }

  /**
   * Hàm phụ trợ để nạp Auction vào RAM của Manager (giả lập cache)
   */
  private void injectAuctionToRam(Auction auction) throws Exception {
    Field auctionsField = AuctionManager.class.getDeclaredField("auctions");
    auctionsField.setAccessible(true);
    ((ConcurrentHashMap<Integer, Auction>) auctionsField.get(manager)).put(auction.getId(), auction);

    // Lock sẽ được Manager tự tạo nhờ hàm computeIfAbsent/putIfAbsent bạn đã viết
  }

  /**
   * Test trường hợp đặt giá vào phiên đã kết thúc
   */
  @Test
  void testBidOnClosedAuction() throws Exception {
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

    // 2. Nạp vào RAM của Manager để qua bước check 'auction == null'
    injectAuctionToRam(auction);

    Customer customer = new Customer();
    customer.setUsername("UserLate");

    // 4. Kiểm tra xem ngoại lệ có văng ra đúng như dự kiến không
    assertThrows(AuctionClosedException.class, () -> {
      manager.placeBid(auctionId, customer, new BigDecimal("60000"));
    });

    // Xác nhận rằng DAO chưa bao giờ được gọi (vì đã bị chặn từ RAM)
    verify(mockDao, never()).placeBid(anyInt(), any(), any());
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

  @Test
  void testBidOnRunningAuctionSuccessfully() throws Exception {
    int auctionId = 777;
    Item item = ItemFactory.createElectronics("iPad", "Pro", new BigDecimal("500"), "ipad.jpg", 1, "Apple", 12);
    item.setId(200);

    Auction auction = new Auction(item, new BigDecimal("500"), LocalDateTime.now().minusMinutes(10), LocalDateTime.now().plusHours(1));
    auction.setId(auctionId);
    auction.setStatus("RUNNING");

    injectAuctionToRam(auction);

    // Mock DAO xử lý đặt giá thành công
    Customer customer = new Customer();
    customer.setUsername("UserA");
    BigDecimal bidAmount = new BigDecimal("600");

    when(mockDao.placeBid(eq(auctionId), any(Customer.class), eq(bidAmount))).thenReturn(true);
    // Mock DAO trả về auction đã cập nhật giá sau khi đặt thành công
    Auction updatedAuction = new Auction(item, bidAmount, auction.getStartTime(), auction.getEndTime());
    updatedAuction.setId(auctionId);
    updatedAuction.setStatus("RUNNING");
    when(mockDao.findById(auctionId)).thenReturn(updatedAuction);

    // Thực thi
    boolean result = manager.placeBid(auctionId, customer, bidAmount);

    // Kiểm tra
    assertTrue(result);
    assertEquals(0, bidAmount.compareTo(manager.getAuction(auctionId).getCurrentPrice()));
    verify(mockDao, times(1)).placeBid(eq(auctionId), any(), eq(bidAmount));
  }
}