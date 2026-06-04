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
  }

  /**
   * Test trường hợp đặt giá vào phiên đã kết thúc
   */
  @Test
  void testBidOnClosedAuction() throws Exception {
    int auctionId = 555;

    // 1. Tạo một món đồ Xe cộ bằng Factory
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

    // 3. Nạp vào RAM
    injectAuctionToRam(auction);

    Customer customer = new Customer();
    customer.setUsername("UserLate");

    // 4. Kiểm tra xem ngoại lệ có văng ra đúng như dự kiến không
    assertThrows(AuctionClosedException.class, () -> {
      manager.placeBid(auctionId, customer, new BigDecimal("60000"));
    });

    // Xác nhận rằng DAO chưa bao giờ được gọi (vì bị chặn từ RAM)
    verify(mockDao, never()).placeBid(anyInt(), any(), any());
  }

  /**
   * Test tính toàn vẹn dữ liệu khi tạo phiên mới
   */
  @Test
  void testAuctionDataIntegrity() throws UserException {
    // 1. Tạo đồ điện tử
    Item item = ItemFactory.createElectronics(
        "iPhone 15", "New", new BigDecimal("1000"), "ip15.jpg", 1, "Apple", 12
    );
    item.setId(108);

    // 🎯 CHUẨN BỊ ĐẦY ĐỦ THAM SỐ NHƯ BẢN FIX DAO MỚI NHẤT
    BigDecimal startPrice = new BigDecimal("1000");
    BigDecimal bidIncrement = new BigDecimal("50");
    LocalDateTime endTime = LocalDateTime.now().plusHours(2);

    // 2. Giả lập đối tượng DAO sẽ trả về
    Auction mockAuction = new Auction(item, startPrice, LocalDateTime.now(), endTime);
    mockAuction.setId(888);
    mockAuction.setStatus("RUNNING");
    mockAuction.setBidIncrement(bidIncrement);

    // 🎯 SỬA CHỮ KÝ MOCKITO: Khi dùng any() thì các tham số khác phải bọc trong eq()
    when(mockDao.createAuction(eq(item), eq(startPrice), any(LocalDateTime.class), eq(endTime), eq(bidIncrement)))
        .thenReturn(mockAuction);

    // 3. 🎯 GỌI HÀM VỚI CHỮ KÝ MỚI NHẤT
    Auction created = manager.createAuction(item, startPrice, endTime, bidIncrement);

    // 4. Kiểm tra kết quả
    assertNotNull(created);
    assertEquals(888, created.getId());
    assertEquals(0, startPrice.compareTo(created.getCurrentPrice()));
    assertEquals("RUNNING", created.getStatus());
    assertEquals("ELECTRONICS", created.getItem().getType());
    assertEquals(0, bidIncrement.compareTo(created.getBidIncrement()));
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

    Customer customer = new Customer();
    customer.setId(1); // 🌟 1. BẮT BUỘC: Đặt ID cho customer để truyền vào hàm check của DAO
    customer.setUsername("UserA");
    BigDecimal bidAmount = new BigDecimal("600");

    // 🌟 2. THÊM MOCK NÀY: Giả lập DAO xác nhận user này ĐÃ đăng ký tham gia phiên đấu giá
    when(mockDao.isBidderRegistered(eq(auctionId), eq(customer.getId()))).thenReturn(true);

    // Mock DAO xử lý đặt giá thành công
    when(mockDao.placeBid(eq(auctionId), any(Customer.class), eq(bidAmount))).thenReturn(true);

    // Mock DAO trả về auction đã cập nhật giá sau khi đặt thành công
    Auction updatedAuction = new Auction(item, bidAmount, auction.getStartTime(), auction.getEndTime());
    updatedAuction.setId(auctionId);
    updatedAuction.setStatus("RUNNING");

    // 🌟 3. THÊM MOCK NÀY: Vì inside placeBid() gọi hàm này để refresh lại RAM sau khi đặt giá thành công
    when(mockDao.findById(auctionId)).thenReturn(updatedAuction);

    // Thực thi
    boolean result = manager.placeBid(auctionId, customer, bidAmount);

    // Kiểm tra
    assertTrue(result);
    // So sánh giá tiền trên RAM sau khi quản lý đã update thành công
    assertEquals(0, bidAmount.compareTo(manager.getAuction(auctionId).getCurrentPrice()));
    verify(mockDao, times(1)).placeBid(eq(auctionId), any(), eq(bidAmount));
  }
  /**
   * 1. Test bao phủ toàn bộ các Constructor còn lại và các hàm Getter/Setter khuyết
   */
  @Test
  void testAuctionConstructorsAndAllGettersSetters() {
    Item item = ItemFactory.createElectronics("Sony PS5", "Game Console", new BigDecimal("500"), "ps5.png", 1, "Sony", 12);

    // Test Constructor 1 (Dùng khi load từ DB lên)
    LocalDateTime start = LocalDateTime.now().minusDays(1);
    LocalDateTime end = LocalDateTime.now().plusDays(1);
    Auction auctionDb = new Auction(123, item, new BigDecimal("600"), start, end, "RUNNING");

    assertEquals(123, auctionDb.getId());
    assertEquals("RUNNING", auctionDb.getStatus());
    assertEquals(start, auctionDb.getStartTime());

    // Test Constructor 2 (Seller tạo nhanh qua số phút)
    Auction auctionSeller = new Auction(item, new BigDecimal("500"), 120); // 120 phút
    assertEquals("OPEN", auctionSeller.getStatus());
    assertNotNull(auctionSeller.getEndTime());

    // Quét sạch toàn bộ các Getter/Setter để JaCoCo tính điểm tuyệt đối
    Auction a = new Auction();
    a.setId(999);
    a.setItem(item);
    a.setCurrentPrice(BigDecimal.TEN);
    a.setStatus("OPEN");
    a.setRegisteredCount(50);
    a.setEndTime(end);
    a.setBidIncrement(BigDecimal.ONE);
    a.setAntiSnipeWindowMinutes(5);
    a.setAntiSnipeExtensionMinutes(10);

    Customer highestUser = new Customer();
    highestUser.setUsername("LeadUser");
    a.setHighestBidder(highestUser);

    // Assert kiểm chứng dữ liệu vừa ghi nhận
    assertEquals(999, a.getId());
    assertEquals(item, a.getItem());
    assertEquals(BigDecimal.TEN, a.getCurrentPrice());
    assertEquals("OPEN", a.getStatus());
    assertEquals(50, a.getRegisteredCount());
    assertEquals(end, a.getEndTime());
    assertEquals(BigDecimal.ONE, a.getBidIncrement());
    assertEquals(5, a.getAntiSnipeWindowMinutes());
    assertEquals(10, a.getAntiSnipeExtensionMinutes());
    assertEquals(highestUser, a.getHighestBidder());
  }

  /**
   * 2. Test hàm trạng thái isActive và refreshStatus tự chuyển vùng khi hết giờ
   */
  @Test
  void testAuctionStatusAndRefresh() {
    Auction a = new Auction();

    // Nhánh isActive
    a.setStatus("RUNNING");
    assertTrue(a.isActive());
    a.setStatus("OPEN");
    assertTrue(a.isActive());
    a.setStatus("FINISHED");
    assertFalse(a.isActive());

    // Nhánh refreshStatus: Vẫn còn trong giờ -> Giữ nguyên RUNNING
    Item item = ItemFactory.createElectronics("M", "D", BigDecimal.ONE, "i.png", 1, "B", 1);
    Auction aRefresh = new Auction(item, BigDecimal.TEN, LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1));
    aRefresh.setStatus("RUNNING");
    aRefresh.refreshStatus();
    assertEquals("RUNNING", aRefresh.getStatus());

    // Nhánh refreshStatus: Quá giờ kết thúc -> Tự động hóa nhảy sang FINISHED
    Auction aExpired = new Auction(item, BigDecimal.TEN, LocalDateTime.now().minusHours(5), LocalDateTime.now().minusHours(2));
    aExpired.setStatus("RUNNING");
    aExpired.refreshStatus();
    assertEquals("FINISHED", aExpired.getStatus());

    // Thử nghiệm với trạng thái ban đầu là OPEN nhưng quá giờ
    Auction aOpenExpired = new Auction(item, BigDecimal.TEN, LocalDateTime.now().minusHours(5), LocalDateTime.now().minusHours(2));
    aOpenExpired.setStatus("OPEN");
    aOpenExpired.refreshStatus();
    assertEquals("FINISHED", aOpenExpired.getStatus());
  }

  /**
   * 3. Test trực diện hàm đặt giá nội tại placeNewBid của Auction (Ném các ngoại lệ)
   */
  @Test
  void testPlaceNewBidInternalExceptions() {
    Item item = ItemFactory.createElectronics("Item", "Desc", BigDecimal.TEN, "i.png", 1, "B", 1);
    Auction a = new Auction(item, new BigDecimal("1000"), LocalDateTime.now(), LocalDateTime.now().plusHours(1));
    Customer customer = new Customer();
    customer.setUsername("BidderA");

    // Lỗi 1: Đặt giá khi phiên đấu giá không ở trạng thái RUNNING (Đang OPEN)
    a.setStatus("OPEN");
    assertThrows(AuctionClosedException.class, () -> {
      a.placeNewBid(customer, new BigDecimal("1200"));
    });

    // Chuyển sang RUNNING để test lỗi tiếp theo
    a.setStatus("RUNNING");

    // Lỗi 2: Đặt số tiền thấp hơn hoặc bằng giá hiện tại
    assertThrows(com.uet.bidding.exception.InvalidBidException.class, () -> {
      a.placeNewBid(customer, new BigDecimal("900")); // Thấp hơn 1000
    });
    assertThrows(com.uet.bidding.exception.InvalidBidException.class, () -> {
      a.placeNewBid(customer, new BigDecimal("1000")); // Bằng 1000
    });
  }

  /**
   * 4. Test kịch bản đặt giá nội tại THÀNH CÔNG kết hợp cơ chế kích hoạt thông báo Observer Pattern
   */
  @Test
  void testPlaceNewBidInternalSuccessWithObservers() throws Exception {
    Item item = ItemFactory.createElectronics("Gaming Phone", "Desc", BigDecimal.TEN, "i.png", 1, "B", 1);
    Auction a = new Auction(item, new BigDecimal("1000"), LocalDateTime.now(), LocalDateTime.now().plusHours(1));
    a.setStatus("RUNNING");

    Customer customer = new Customer();
    customer.setUsername("ProBidder");
    // Giả lập profile bidder để không bị NullPointerException khi add vào bidHistory

    // Tạo một Observer giả lập bằng Mockito để nghe ngóng tình hình thay đổi giá
    AuctionObserver mockObserver = mock(AuctionObserver.class);

    // Ép object nhận diện danh sách observer (vì observers khai báo private)
    Field observersField = Auction.class.getDeclaredField("observers");
    observersField.setAccessible(true);
    java.util.List<AuctionObserver> obsList = (java.util.List<AuctionObserver>) observersField.get(a);
    obsList.add(mockObserver);

    // Kích hoạt đặt giá hợp lệ
    boolean isSuccess = a.placeNewBid(customer, new BigDecimal("1500"));

    assertTrue(isSuccess);
    assertEquals(new BigDecimal("1500"), a.getCurrentPrice());
    assertEquals(customer, a.getHighestBidder());

    // Kiểm chứng xem Observer có nhận được cuộc gọi updatePrice chuẩn xác không
    verify(mockObserver, times(1)).updatePrice(eq("Sản phẩm: Gaming Phone"), eq(1500.0), eq("ProBidder"));
  }

  /**
   * 5. Quét qua hàm phòng hộ readResolve dành cho Serialization ( transient bảo vệ )
   */
  @Test
  void testReadResolveSerializationSafe() throws Exception {
    Auction a = new Auction();

    // Dùng Reflection cố tình phá hoại gán list observers về null (giống hệt hiện tượng xảy ra khi De-serialize biến transient)
    Field observersField = Auction.class.getDeclaredField("observers");
    observersField.setAccessible(true);
    observersField.set(a, null);

    // Gọi hàm private readResolve thông qua Reflection để xem nó tự chữa lành vùng nhớ không
    java.lang.reflect.Method readResolveMethod = Auction.class.getDeclaredMethod("readResolve");
    readResolveMethod.setAccessible(true);
    Auction repairedAuction = (Auction) readResolveMethod.invoke(a);

    // Kiểm chứng danh sách observer đã được khởi tạo mới thành công, không còn bị null nữa
    java.util.List<?> listAfter = (java.util.List<?>) observersField.get(repairedAuction);
    assertNotNull(listAfter);
  }
}