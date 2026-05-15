package com.uet.bidding;

import com.uet.bidding.dao.AuctionSqlDAO;
import com.uet.bidding.exception.AuctionClosedException;
import com.uet.bidding.model.Auction;
import com.uet.bidding.model.Customer;
import com.uet.bidding.model.Item;
import com.uet.bidding.model.ItemFactory;
import com.uet.bidding.service.AuctionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

  @Test
  void testPlaceBidSuccessfully() throws Exception {
    int auctionId = 1;
    Item item = createTestItem(101);
    Customer customer = createTestCustomer("UserA");

    // SỬA: Dùng đúng Constructor (nhận vào đối tượng Item)
    Auction auction = new Auction(item, new BigDecimal("1000"),
        LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(2));
    auction.setId(auctionId);
    auction.setStatus("RUNNING");

    // Nạp vào RAM để tránh lỗi "Không tìm thấy phiên đấu giá"
    injectAuctionToRam(auction);

    // SỬA: Vì manager không có addAuction, ta giả lập hành vi trong RAM
    when(mockDao.findById(auctionId)).thenReturn(auction);
    when(mockDao.placeBid(eq(auctionId), any(Customer.class), any(BigDecimal.class))).thenReturn(true);

    // Đưa vào RAM để manager quản lý (tương đương addAuction cũ)
    manager.getAllAuctions().add(auction); // Cách này tùy vào việc getAllAuctions trả về list copy hay ref

    boolean result = manager.placeBid(auctionId, customer, new BigDecimal("1500"));

    assertTrue(result);
    // Lưu ý: Logic placeBid của bạn bây giờ gọi dao.findById để cập nhật RAM
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

    // Phải nạp vào RAM thì mới chạy tới được bước check status
    injectAuctionToRam(auction);

    // Giả lập logic ném lỗi khi đấu giá chưa RUNNING
    when(mockDao.placeBid(eq(auctionId), any(Customer.class), any(BigDecimal.class)))
        .thenThrow(new AuctionClosedException("Phiên đấu giá chưa bắt đầu!"));

    assertThrows(AuctionClosedException.class, () -> {
      manager.placeBid(auctionId, customer, new BigDecimal("1500"));
    });
  }

  @Test
  void testConcurrencySafety() throws Exception {
    int auctionId = 3;
    Item item = createTestItem(103);

    // 1. Tạo phiên đấu giá với giá khởi điểm 1000
    Auction auction = new Auction(item, new BigDecimal("1000"),
        LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(2));
    auction.setId(auctionId);
    auction.setStatus("RUNNING");

    // Nạp vào RAM để kích hoạt cơ chế Lock đa luồng
    injectAuctionToRam(auction);

    // 2. Mock hành vi của DAO
    // Khi gọi placeBid thành công, ta giả lập việc cập nhật giá trực tiếp vào object auction trong RAM
    // (Vì thực tế logic của bạn là load lại từ DB, nhưng ở đây ta mock để test sự an toàn của Lock)
    when(mockDao.placeBid(eq(auctionId), any(Customer.class), any(BigDecimal.class)))
        .thenAnswer(invocation -> {
          BigDecimal newBid = invocation.getArgument(2);
          synchronized (auction) {
            // Giả lập logic DB: Chỉ cập nhật nếu giá mới cao hơn giá cũ
            if (newBid.compareTo(auction.getCurrentPrice()) > 0) {
              auction.setCurrentPrice(newBid);
              return true;
            }
            return false;
          }
        });

    when(mockDao.findById(auctionId)).thenReturn(auction);

    // 3. Thiết lập đa luồng
    int threadCount = 20;
    ExecutorService service = Executors.newFixedThreadPool(threadCount);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch endLatch = new CountDownLatch(threadCount);

    // Giá cao nhất dự kiến sẽ là 1100 + 19 = 1119
    for (int i = 0; i < threadCount; i++) {
      final BigDecimal bidPrice = new BigDecimal(1100 + i);
      service.submit(() -> {
        try {
          startLatch.await(); // Đợi tất cả sẵn sàng để cùng "ồ" vào đặt giá
          manager.placeBid(auctionId, createTestCustomer("User"), bidPrice);
        } catch (Exception ignored) {
        } finally {
          endLatch.countDown();
        }
      });
    }

    // 4. Bắt đầu cuộc đua
    startLatch.countDown();
    boolean finished = endLatch.await(10, TimeUnit.SECONDS);
    service.shutdown();

    // 5. KIỂM TRA KẾT QUẢ CUỐI CÙNG (LOGIC CHECK)
    assertTrue(finished, "Các luồng không kết thúc kịp thời gian quy định!");

    Auction finalAuction = manager.getAuction(auctionId);
    assertNotNull(finalAuction);

    // Kiểm tra xem giá cuối cùng có phải là 1119 không (mức giá cao nhất được gửi đi)
    BigDecimal expectedMaxPrice = new BigDecimal("1119");
    assertEquals(0, expectedMaxPrice.compareTo(finalAuction.getCurrentPrice()),
        "Giá cuối cùng không khớp! Có thể đã xảy ra lỗi Race Condition.");

    // Kiểm tra xem DAO có được gọi đúng 20 lần không
    verify(mockDao, times(threadCount)).placeBid(eq(auctionId), any(Customer.class), any(BigDecimal.class));
  }
}