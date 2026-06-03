package com.uet.bidding.service;

import com.uet.bidding.dao.AuctionSqlDAO;
import com.uet.bidding.exception.AuctionClosedException;
import com.uet.bidding.exception.InvalidBidException;
import com.uet.bidding.exception.UserException;
import com.uet.bidding.model.Auction;
import com.uet.bidding.model.Customer;
import com.uet.bidding.model.Item;
import com.uet.bidding.model.ItemFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AuctionManagerTest {
  private AuctionManager manager;
  private AuctionSqlDAO mockDao;

  @BeforeEach
  void setUp() throws Exception {
    mockDao = mock(AuctionSqlDAO.class);
    when(mockDao.getAllAuctions()).thenReturn(new ArrayList<>());

    manager = AuctionManager.getInstance();

    // Làm sạch RAM của Singleton trước mỗi lần test
    clearManagerMemory();

    manager.initialize(mockDao);
  }

  private void clearManagerMemory() throws Exception {
    Field auctionsField = AuctionManager.class.getDeclaredField("auctions");
    auctionsField.setAccessible(true);
    ((ConcurrentHashMap<?, ?>) auctionsField.get(manager)).clear();

    Field locksField = AuctionManager.class.getDeclaredField("locks");
    locksField.setAccessible(true);
    ((ConcurrentHashMap<?, ?>) locksField.get(manager)).clear();

    Field cacheField = AuctionManager.class.getDeclaredField("autoBidCache");
    cacheField.setAccessible(true);
    ((ConcurrentHashMap<?, ?>) cacheField.get(manager)).clear();
  }

  // Hàm quan trọng để tránh InvalidBidException: Nạp Auction và Lock vào RAM
  private void injectAuctionToRam(Auction auction) throws Exception {
    Field auctionsField = AuctionManager.class.getDeclaredField("auctions");
    auctionsField.setAccessible(true);
    ((ConcurrentHashMap<Integer, Auction>) auctionsField.get(manager)).put(auction.getId(), auction);

    Field locksField = AuctionManager.class.getDeclaredField("locks");
    locksField.setAccessible(true);
    ((ConcurrentHashMap<Integer, ReentrantLock>) locksField.get(manager)).put(auction.getId(), new ReentrantLock());
  }

  private Item createTestItem(int id) {
    Item item = ItemFactory.createElectronics("Test Item", "Desc", new BigDecimal("1000"), "img.jpg", 1, "Brand", 12);
    item.setId(id);
    return item;
  }

  private Customer createTestCustomer(String username) {
    Customer c = new Customer();
    c.setUsername(username);
    return c;
  }

  // ==========================================
  // THÊM: TEST HÀM INITIALIZE (NẠP CẢ AUTOBID)
  // ==========================================
  @Test
  void testInitializeWithAutoBids() throws Exception {
    int auctionId = 99;
    Auction auction = new Auction(createTestItem(101), new BigDecimal("1000"), LocalDateTime.now(), LocalDateTime.now().plusHours(1));
    auction.setId(auctionId);

    List<Auction> dbAuctions = new ArrayList<>();
    dbAuctions.add(auction);

    List<AuctionManager.RemoteAutoBid> mockAutoBids = new ArrayList<>();
    mockAutoBids.add(new AuctionManager.RemoteAutoBid(1, 10, new BigDecimal("5000")));

    // Giả lập DB trả về danh sách phiên kèm cấu hình AutoBid
    when(mockDao.getAllAuctions()).thenReturn(dbAuctions);
    when(mockDao.getAutoBidsByAuctionId(auctionId)).thenReturn(mockAutoBids);

    // Chạy khởi tạo lại
    manager.initialize(mockDao);

    assertNotNull(manager.getAuction(auctionId));
    assertEquals(1, manager.getAllAuctions().size());
  }

  // ==========================================
  // THÊM: TEST HÀM CREATEAUCTION
  // ==========================================
  @Test
  void testCreateAuction_Success() throws UserException {
    Item item = createTestItem(105);
    Auction created = new Auction(item, new BigDecimal("2000"), LocalDateTime.now(), LocalDateTime.now().plusHours(2));
    created.setId(5);

    when(mockDao.createAuction(eq(item), any(BigDecimal.class), any(LocalDateTime.class), any(LocalDateTime.class), any(BigDecimal.class)))
        .thenReturn(created);

    Auction result = manager.createAuction(item, new BigDecimal("2000"), LocalDateTime.now().plusHours(2), new BigDecimal("100"));

    assertNotNull(result);
    assertEquals(5, result.getId());
    assertEquals(result, manager.getAuction(5)); // Đã nạp vào RAM thành công
  }

  // ==========================================
  // THÊM: TEST HÀM UPDATEAUCTIONSTATUS
  // ==========================================
  @Test
  void testUpdateAuctionStatus_ToRunning() throws Exception {
    int auctionId = 6;
    Auction auction = new Auction(createTestItem(106), new BigDecimal("1000"), LocalDateTime.now(), LocalDateTime.now().plusHours(1));
    auction.setId(auctionId);
    auction.setStatus("OPEN");
    injectAuctionToRam(auction);

    // Chuyển sang trạng thái RUNNING thông thường
    manager.updateAuctionStatus(auctionId, "RUNNING");

    assertEquals("RUNNING", manager.getAuction(auctionId).getStatus());
  }

  @Test
  void testUpdateAuctionStatus_ToFinished_Success() throws Exception {
    int auctionId = 7;
    Auction auction = new Auction(createTestItem(107), new BigDecimal("1000"), LocalDateTime.now(), LocalDateTime.now().plusHours(1));
    auction.setId(auctionId);
    auction.setStatus("RUNNING");
    injectAuctionToRam(auction);

    Auction finishedAuction = new Auction(createTestItem(107), new BigDecimal("1000"), LocalDateTime.now(), LocalDateTime.now().plusHours(1));
    finishedAuction.setId(auctionId);
    finishedAuction.setStatus("FINISHED");

    when(mockDao.findById(auctionId)).thenReturn(finishedAuction);

    // Đóng phiên
    manager.updateAuctionStatus(auctionId, "FINISHED");

    verify(mockDao, times(1)).finishAuction(auctionId);
    assertEquals("FINISHED", manager.getAuction(auctionId).getStatus());
  }

  @Test
  void testUpdateAuctionStatus_ToFinished_WithException() throws Exception {
    int auctionId = 8;
    Auction auction = new Auction(createTestItem(108), new BigDecimal("1000"), LocalDateTime.now(), LocalDateTime.now().plusHours(1));
    auction.setId(auctionId);
    injectAuctionToRam(auction);

    // Giả lập finishAuction ném ngoại lệ
    doThrow(new UserException("Lỗi DB ảo")).when(mockDao).finishAuction(auctionId);

    // Lệnh này không được crash chương trình vì có try-catch báo lỗi log
    assertDoesNotThrow(() -> manager.updateAuctionStatus(auctionId, "FINISHED"));
  }

  @Test
  void testUpdateAuctionStatus_LockNotFound() {
    // Truyền một ID không tồn tại trong locks map -> hàm lập tức return an toàn
    assertDoesNotThrow(() -> manager.updateAuctionStatus(999, "RUNNING"));
  }

  // ==========================================
  // 4. KIỂM THỬ CÁC NHÁNH LỖI CỦA PLACEBID
  // ==========================================
  @Test
  void testPlaceBidSuccessfully() throws Exception {
    int auctionId = 1;
    Item item = createTestItem(101);
    Customer customer = createTestCustomer("UserA");
    customer.setId(1);

    Auction auction = new Auction(item, new BigDecimal("1000"),
        LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(2));
    auction.setId(auctionId);
    auction.setStatus("RUNNING");

    injectAuctionToRam(auction);

    when(mockDao.isBidderRegistered(eq(auctionId), eq(customer.getId()))).thenReturn(true);
    when(mockDao.findById(auctionId)).thenReturn(auction);
    when(mockDao.placeBid(eq(auctionId), any(Customer.class), any(BigDecimal.class))).thenReturn(true);

    boolean result = manager.placeBid(auctionId, customer, new BigDecimal("1500"));

    assertTrue(result);
    verify(mockDao, atLeastOnce()).placeBid(eq(auctionId), any(Customer.class), any(BigDecimal.class));
  }

  @Test
  void testPlaceBidWhenStatusNotRunning() throws Exception {
    int auctionId = 2;
    Item item = createTestItem(102);
    Customer customer = createTestCustomer("UserB");

    Auction auction = new Auction(item, new BigDecimal("1000"),
        LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(2));
    auction.setId(auctionId);
    auction.setStatus("OPEN");

    injectAuctionToRam(auction);

    assertThrows(AuctionClosedException.class, () -> {
      manager.placeBid(auctionId, customer, new BigDecimal("1500"));
    });
  }

  @Test
  void testPlaceBid_AuctionNotFound() {
    Customer customer = createTestCustomer("UserC");
    // ID không có trong RAM map
    assertThrows(InvalidBidException.class, () -> {
      manager.placeBid(999, customer, new BigDecimal("1500"));
    });
  }

  @Test
  void testPlaceBid_UserNotRegistered() throws Exception {
    int auctionId = 12;
    Customer customer = createTestCustomer("UserD");
    customer.setId(44);

    Auction auction = new Auction(createTestItem(112), new BigDecimal("1000"), LocalDateTime.now(), LocalDateTime.now().plusHours(1));
    auction.setId(auctionId);
    auction.setStatus("RUNNING");
    injectAuctionToRam(auction);

    // Giả lập user chưa đăng ký phòng này
    when(mockDao.isBidderRegistered(auctionId, customer.getId())).thenReturn(false);

    InvalidBidException ex = assertThrows(InvalidBidException.class, () -> {
      manager.placeBid(auctionId, customer, new BigDecimal("1500"));
    });
    assertEquals("Bạn chưa đăng ký tham gia phiên đấu giá này! Vui lòng ấn nút đăng ký trước.", ex.getMessage());
  }

  // ==========================================
  // THÊM: AUTOBID MANAGEMENT & SYNC LOGIC
  // ==========================================
  @Test
  void testEnableAutoBidAndSyncCache() throws Exception {
    int auctionId = 15;
    manager.enableAutoBid(auctionId, 10, new BigDecimal("3000"));

    verify(mockDao, times(1)).setAutoBid(auctionId, 10, new BigDecimal("3000"));
    verify(mockDao, times(1)).getAutoBidsByAuctionId(auctionId);
  }

  @Test
  void testSyncAutoBidCache_DaoNull() throws Exception {
    // Dùng Reflection gán thuộc tính auctionSqlDAO về null để test nhánh phòng hộ if (auctionSqlDAO == null)
    Field daoField = AuctionManager.class.getDeclaredField("auctionSqlDAO");
    daoField.setAccessible(true);
    daoField.set(manager, null);

    assertDoesNotThrow(() -> manager.syncAutoBidCache(15));
  }

  // ==========================================
  // THÊM: REFRESH AUCTION FROM DB
  // ==========================================
  @Test
  void testRefreshAuctionFromDb() throws Exception {
    int auctionId = 20;
    Auction updated = new Auction(createTestItem(120), new BigDecimal("5000"), LocalDateTime.now(), LocalDateTime.now().plusHours(1));
    updated.setId(auctionId);

    when(mockDao.findById(auctionId)).thenReturn(updated);

    manager.refreshAuctionFromDb(auctionId);

    assertEquals(updated, manager.getAuction(auctionId));
  }

  @Test
  void testConcurrencySafety() throws Exception {
    int auctionId = 3;
    Item item = createTestItem(103);

    Auction auction = new Auction(item, new BigDecimal("1000"),
        LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(2));
    auction.setId(auctionId);
    auction.setStatus("RUNNING");

    injectAuctionToRam(auction);

    when(mockDao.isBidderRegistered(eq(auctionId), anyInt())).thenReturn(true);
    lenient().when(mockDao.findById(auctionId)).thenReturn(auction);

    when(mockDao.placeBid(eq(auctionId), any(Customer.class), any(BigDecimal.class)))
        .thenAnswer(invocation -> {
          BigDecimal newBid = invocation.getArgument(2);
          synchronized (auction) {
            if (newBid.compareTo(auction.getCurrentPrice()) > 0) {
              auction.setCurrentPrice(newBid);
              return true;
            }
            return false;
          }
        });

    when(mockDao.findById(auctionId)).thenReturn(auction);

    int threadCount = 20;
    ExecutorService service = Executors.newFixedThreadPool(threadCount);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch endLatch = new CountDownLatch(threadCount);

    for (int i = 0; i < threadCount; i++) {
      final BigDecimal bidPrice = new BigDecimal(1100 + i);
      service.submit(() -> {
        try {
          startLatch.await();
          Customer threadCustomer = createTestCustomer("User" + bidPrice);
          threadCustomer.setId(bidPrice.intValue());

          manager.placeBid(auctionId, threadCustomer, bidPrice);
        } catch (Exception ignored) {
        } finally {
          endLatch.countDown();
        }
      });
    }

    startLatch.countDown();
    boolean finished = endLatch.await(10, TimeUnit.SECONDS);
    service.shutdown();

    assertTrue(finished, "Các luồng không kết thúc kịp thời gian quy định!");

    Auction finalAuction = manager.getAuction(auctionId);
    assertNotNull(finalAuction);

    BigDecimal expectedMaxPrice = new BigDecimal("1119");
    assertEquals(0, expectedMaxPrice.compareTo(finalAuction.getCurrentPrice()),
        "Giá cuối cùng không khớp! Có thể đã xảy ra lỗi Race Condition.");

    verify(mockDao, times(threadCount)).placeBid(eq(auctionId), any(Customer.class), any(BigDecimal.class));
  }
}