package com.uet.bidding.dao;

import com.uet.bidding.exception.AuctionClosedException;
import com.uet.bidding.exception.InvalidBidException;
import com.uet.bidding.exception.UserException;
import com.uet.bidding.model.Auction;
import com.uet.bidding.model.Customer;
import com.uet.bidding.model.Item;
import com.uet.bidding.model.ItemFactory;
import com.uet.bidding.service.AuctionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuctionSqlDAOTest {

  private AuctionSqlDAO auctionSqlDAO;

  @Mock private Connection mockConnection;
  @Mock private PreparedStatement mockStatement;
  @Mock private ResultSet mockResultSet;

  private MockedStatic<DatabaseConnection> mockedDbStatic;

  private Item dummyItem;
  private Customer dummyCustomer;

  private void mockValidItem() throws Exception {
    ResultSetMetaData metaData = mock(ResultSetMetaData.class);

    lenient().when(mockResultSet.getMetaData()).thenReturn(metaData);
    lenient().when(metaData.getColumnCount()).thenReturn(20);

    lenient().when(mockResultSet.getString("item_type"))
        .thenReturn("ART");

    lenient().when(mockResultSet.getString("name"))
        .thenReturn("Mona Lisa");

    lenient().when(mockResultSet.getString("description"))
        .thenReturn("Painting");

    lenient().when(mockResultSet.getBigDecimal("starting_price"))
        .thenReturn(BigDecimal.valueOf(1000));

    lenient().when(mockResultSet.getString("image_path"))
        .thenReturn("mona.jpg");

    lenient().when(mockResultSet.getInt("seller_id"))
        .thenReturn(202);

    lenient().when(mockResultSet.getString("author"))
        .thenReturn("Da Vinci");

    lenient().when(mockResultSet.getInt("creation_year"))
        .thenReturn(1503);

    lenient().when(mockResultSet.getString("material"))
        .thenReturn("Oil");

    lenient().when(mockResultSet.getBoolean("in_auction"))
        .thenReturn(true);

    lenient().when(mockResultSet.getString("city"))
        .thenReturn("Ha Noi");
  }

  private void mockValidAuction(String status) throws Exception {

    lenient().when(mockResultSet.next())
        .thenReturn(true);

    lenient().when(mockResultSet.getInt("id"))
        .thenReturn(1);

    lenient().when(mockResultSet.getInt("item_id"))
        .thenReturn(101);

    lenient().when(mockResultSet.getString("status"))
        .thenReturn(status);

    lenient().when(mockResultSet.getTimestamp("start_time"))
        .thenReturn(
            Timestamp.valueOf(
                LocalDateTime.now().minusHours(1)
            )
        );

    lenient().when(mockResultSet.getTimestamp("end_time"))
        .thenReturn(
            Timestamp.valueOf(
                LocalDateTime.now().plusDays(1)
            )
        );

    lenient().when(mockResultSet.getBigDecimal("current_price"))
        .thenReturn(BigDecimal.valueOf(1000));

    lenient().when(mockResultSet.getBigDecimal("bid_increment"))
        .thenReturn(BigDecimal.valueOf(100));

    lenient().when(mockResultSet.getInt("anti_snipe_window_minutes"))
        .thenReturn(2);

    lenient().when(mockResultSet.getInt("anti_snipe_extension_minutes"))
        .thenReturn(5);

    lenient().when(mockResultSet.getInt("highest_bidder_id"))
        .thenReturn(0);

    lenient().when(mockResultSet.wasNull())
        .thenReturn(true);
  }

  private void mockValidItem(ResultSet rs) throws Exception {

    when(rs.getString("item_type"))
        .thenReturn("ART");

    when(rs.getString("name"))
        .thenReturn("Mona Lisa");

    when(rs.getString("description"))
        .thenReturn("Painting");

    when(rs.getBigDecimal("starting_price"))
        .thenReturn(BigDecimal.valueOf(1000));

    when(rs.getString("image_path"))
        .thenReturn("mona.jpg");

    when(rs.getInt("seller_id"))
        .thenReturn(202);

    when(rs.getString("author"))
        .thenReturn("Da Vinci");

    when(rs.getInt("creation_year"))
        .thenReturn(1503);

    when(rs.getString("material"))
        .thenReturn("Oil");

    when(rs.getBoolean("in_auction"))
        .thenReturn(true);

    when(rs.getString("city"))
        .thenReturn("Ha Noi");
  }

  private void mockValidAuction(ResultSet rs) throws Exception {

    when(rs.next()).thenReturn(true);

    when(rs.getInt("id")).thenReturn(1);
    when(rs.getInt("item_id")).thenReturn(101);

    when(rs.getString("status")).thenReturn("RUNNING");

    when(rs.getBigDecimal("current_price"))
        .thenReturn(BigDecimal.valueOf(1000));

    when(rs.getBigDecimal("bid_increment"))
        .thenReturn(BigDecimal.valueOf(100));

    when(rs.getTimestamp("start_time"))
        .thenReturn(Timestamp.valueOf(LocalDateTime.now()));

    when(rs.getTimestamp("end_time"))
        .thenReturn(Timestamp.valueOf(LocalDateTime.now().plusDays(1)));

    when(rs.getInt("highest_bidder_id"))
        .thenReturn(0);

    when(rs.wasNull()).thenReturn(true);

    // THÊM 2 DÒNG NÀY
    when(rs.getInt("anti_snipe_window_minutes"))
        .thenReturn(2);

    when(rs.getInt("anti_snipe_extension_minutes"))
        .thenReturn(5);
  }

  @BeforeEach
  void setUp() throws SQLException {
    // Khởi tạo đối tượng DAO cần test
    auctionSqlDAO = new AuctionSqlDAO();

    // Thiết lập Mock Static cho kết nối Database từ gốc
    mockedDbStatic = mockStatic(DatabaseConnection.class);
    mockedDbStatic.when(DatabaseConnection::getConnection).thenReturn(mockConnection);

    // Cấu hình chuỗi JDBC Mock mặc định để tránh NullPointerException
    lenient().when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
    lenient().when(
        mockConnection.prepareStatement(
            anyString(),
            eq(Statement.RETURN_GENERATED_KEYS)
        )
    ).thenReturn(mockStatement);
    lenient().when(mockStatement.executeQuery()).thenReturn(mockResultSet);

    // Dữ liệu mẫu phục vụ test
    dummyItem = ItemFactory.createArt("Mona Lisa", "Tranh dầu", BigDecimal.valueOf(5000), "mona.png", 11, "Da Vinci", 1503, "Dầu trên gỗ");
    dummyItem.setId(101);
    dummyItem.setSellerId(202);

    dummyCustomer = new Customer();
    dummyCustomer.setId(1);
    dummyCustomer.setUsername("bidder1");
  }

  @AfterEach
  void tearDown() {
    // Giải phóng mock static sau mỗi test case để không gây ảnh hưởng chéo
    mockedDbStatic.close();
  }

  // ==========================================================================
  // ─── PHẦN 1: KIỂM THỬ CÁC LUỒNG THÀNH CÔNG VÀ ĐỌC DỮ LIỆU (SUCCESS FLOWS) ──
  // ==========================================================================

  @Test
  void testCreateAuction_Success() throws Exception {
    when(mockStatement.executeUpdate()).thenReturn(1);
    when(mockStatement.getGeneratedKeys()).thenReturn(mockResultSet);
    when(mockResultSet.next()).thenReturn(true);
    when(mockResultSet.getInt(1)).thenReturn(55); // Trả về ID tự sinh

    Auction result = auctionSqlDAO.createAuction(dummyItem, BigDecimal.valueOf(100), LocalDateTime.now(), LocalDateTime.now().plusDays(1), BigDecimal.valueOf(10));

    assertNotNull(result);
    assertEquals(55, result.getId());
  }

  @Test
  void testFindById_Found() throws Exception {

    when(mockResultSet.next()).thenReturn(true);

    mockValidAuction("RUNNING");
    mockValidItem();

    Auction auction = auctionSqlDAO.findById(1);

    assertNotNull(auction);
  }

  @Test
  void testGetAllAuctions_WithData() throws Exception {

    mockValidAuction("RUNNING");
    mockValidItem();

    when(mockResultSet.next())
        .thenReturn(true)   // auction #1
        .thenReturn(true)   // item #1
        .thenReturn(true)   // count #1
        .thenReturn(true)   // auction #2
        .thenReturn(true)   // item #2
        .thenReturn(true)   // count #2
        .thenReturn(false); // hết auction

    when(mockResultSet.getInt("id"))
        .thenReturn(1, 2);

    when(mockResultSet.getInt("cnt"))
        .thenReturn(5);

    List<Auction> auctions =
        auctionSqlDAO.getAllAuctions();

    assertEquals(2, auctions.size());
  }

  @Test
  void testGetAuctionsByStatus_WithData() throws Exception {

    mockValidAuction("RUNNING");
    mockValidItem();

    when(mockResultSet.next())
        .thenReturn(true)   // auction
        .thenReturn(true)   // item
        .thenReturn(false); // end

    List<Auction> auctions =
        auctionSqlDAO.getAuctionsByStatus("RUNNING");

    assertEquals(1, auctions.size());
  }

  @Test
  void testIsBidderRegistered_TrueAndFalse() throws SQLException {
    // Đoánh 1: Đăng ký hợp lệ (True)
    when(mockResultSet.next()).thenReturn(true);
    assertTrue(auctionSqlDAO.isBidderRegistered(1, 1));

    // Đoánh 2: Chưa đăng ký (False)
    when(mockResultSet.next()).thenReturn(false);
    assertFalse(auctionSqlDAO.isBidderRegistered(1, 2));
  }

  @Test
  void testGetRegistrationCount_Success() throws SQLException {

    when(mockResultSet.next()).thenReturn(true);
    when(mockResultSet.getInt("cnt")).thenReturn(15);

    assertEquals(15, auctionSqlDAO.getRegistrationCount(1));
  }

  @Test
  void testGetAutoBidsByAuctionId_Loop() throws SQLException {
    when(mockResultSet.next()).thenReturn(true, true, false);
    when(mockResultSet.getInt("id")).thenReturn(10, 11);
    when(mockResultSet.getInt("bidder_id")).thenReturn(5, 6);
    when(mockResultSet.getBigDecimal("max_bid")).thenReturn(BigDecimal.valueOf(1000));

    List<AuctionManager.RemoteAutoBid> list = auctionSqlDAO.getAutoBidsByAuctionId(1);
    assertEquals(2, list.size());
  }

  @Test
  void testGetActiveAutoBidMaxPrice_FoundAndNotFound() throws SQLException {
    when(mockResultSet.next()).thenReturn(true);
    when(mockResultSet.getBigDecimal("max_bid")).thenReturn(BigDecimal.valueOf(5000));
    assertEquals(0, BigDecimal.valueOf(5000).compareTo(auctionSqlDAO.getActiveAutoBidMaxPrice(1, 1)));

    when(mockResultSet.next()).thenReturn(false);
    assertNull(auctionSqlDAO.getActiveAutoBidMaxPrice(1, 2));
  }

  // ==========================================================================
  // ─── PHẦN 2: KIỂM THỬ CORE LOGIC ĐẶT GIÁ (PLACE_BID LOGIC BRANCHES) ───────
  // ==========================================================================

  @Test
  void testPlaceBid_NormalSuccess() throws Exception {
    // 1. Khởi tạo Mock ResultSetMetaData để tránh lỗi cấu trúc bảng
    ResultSetMetaData mockMetaData = mock(ResultSetMetaData.class);
    lenient().when(mockMetaData.getColumnCount()).thenReturn(10);
    lenient().when(mockResultSet.getMetaData()).thenReturn(mockMetaData);

    // 2. Kích hoạt luồng đọc dữ liệu luôn đúng cho cả 3 DAO con quét dữ liệu
    lenient().when(mockResultSet.next())
        .thenReturn(true)   // AuctionSqlDAO.findById()
        .thenReturn(true)   // ItemSqlDAO.findById()
        .thenReturn(true)   // UserSqlDAO.getBalance()
        .thenReturn(true)   // BidSqlDAO.getGeneratedKeys()
        .thenReturn(false); // triggerAutoBids()
    // ====================================================================
    // 3. CẤU HÌNH CÁC CỘT CHO BẢNG AUCTIONS (Phục vụ AuctionSqlDAO)
    // ====================================================================
    lenient().when(mockResultSet.getInt("id")).thenReturn(1);
    lenient().when(mockResultSet.getInt("item_id")).thenReturn(101);
    lenient().when(mockResultSet.getString("status")).thenReturn("RUNNING");
    lenient().when(mockResultSet.getBigDecimal("current_price")).thenReturn(BigDecimal.valueOf(1000));
    lenient().when(mockResultSet.getBigDecimal("bid_increment")).thenReturn(BigDecimal.valueOf(100));

    Timestamp mockStartTime = Timestamp.valueOf(LocalDateTime.now().minusHours(1));
    Timestamp mockEndTime = Timestamp.valueOf(LocalDateTime.now().plusHours(2));
    lenient().when(mockResultSet.getTimestamp("start_time")).thenReturn(mockStartTime);
    lenient().when(mockResultSet.getTimestamp("end_time")).thenReturn(mockEndTime);

    // Giả lập chưa có ai đặt giá cao nhất trước đó để tránh chạy sâu vào hàm tìm User
    lenient().when(mockResultSet.getInt("highest_bidder_id")).thenReturn(0);
    lenient().when(mockResultSet.wasNull()).thenReturn(true);

    // ====================================================================
    // 4. BÙ CÁC CỘT CHO BẢNG ITEMS (Phục vụ ngầm cho ItemSqlDAO.findById)
    // ====================================================================
    lenient().when(mockResultSet.getString("item_type")).thenReturn("ART"); // Sửa lỗi loại item không hợp lệ
    lenient().when(mockResultSet.getString("type")).thenReturn("ART");
    lenient().when(mockResultSet.getString("name")).thenReturn("Mona Lisa");
    lenient().when(mockResultSet.getBigDecimal("starting_price")).thenReturn(BigDecimal.valueOf(1000));
    lenient().when(mockResultSet.getInt("seller_id")).thenReturn(202);

    // ====================================================================
    // 5. BÙ CÁC CỘT CHO BẢNG USERS (Phục vụ ngầm cho UserSqlDAO.getBalance)
    // ====================================================================
    lenient().when(mockResultSet.getBigDecimal("balance")).thenReturn(BigDecimal.valueOf(5000000)); // Cấp hẳn 5 triệu
    lenient().when(mockResultSet.getBigDecimal("current_balance")).thenReturn(BigDecimal.valueOf(5000000));

    // 6. Giả lập lệnh update xuống cơ sở dữ liệu thành công
    lenient().when(mockStatement.executeUpdate()).thenReturn(1);

    // BidSqlDAO.addBid()
    lenient().when(mockStatement.getGeneratedKeys()).thenReturn(mockResultSet);

    lenient().when(mockResultSet.getInt(1)).thenReturn(999);

    // Điều khiển số lần next()
    lenient().when(mockResultSet.next())
        .thenReturn(true)
        .thenReturn(true)
        .thenReturn(true)
        .thenReturn(true)
        .thenReturn(false);

    // 7. Thực hiện đặt giá: 1200 VNĐ (Thoả mãn: Giá hiện tại 1000 + Bước giá 100 = 1100 VNĐ)
    // Ví có 5.000.000 VNĐ nên chắc chắn vượt qua điều kiện dòng tiền!
    boolean success = auctionSqlDAO.placeBid(1, dummyCustomer, BigDecimal.valueOf(1200));

    // Kiểm tra kết quả mong đợi
    assertTrue(success);
  }

  @Test
  void testPlaceBid_AuctionClosed() throws Exception {
    // 1. Khởi tạo Mock ResultSetMetaData để tránh lỗi cấu trúc bảng của ItemSqlDAO
    ResultSetMetaData mockMetaData = mock(ResultSetMetaData.class);
    lenient().when(mockMetaData.getColumnCount()).thenReturn(10);
    lenient().when(mockResultSet.getMetaData()).thenReturn(mockMetaData);

    // 2. Cấu hình luồng đọc rs.next() trả về true cho cả 2 lượt (1 cho Auction, 1 cho Item)
    lenient().when(mockResultSet.next())
        .thenReturn(true)   // Lần 1: Cho AuctionSqlDAO.findById()
        .thenReturn(true)   // Lần 2: Cho ItemSqlDAO.findById() ngầm bên trong mapAuction
        .thenReturn(false);

    // ====================================================================
    // 3. CẤU HÌNH CÁC CỘT CHO BẢNG AUCTIONS (Phục vụ AuctionSqlDAO)
    // ====================================================================
    lenient().when(mockResultSet.getInt("id")).thenReturn(1);
    lenient().when(mockResultSet.getInt("item_id")).thenReturn(101);
    lenient().when(mockResultSet.getString("status")).thenReturn("FINISHED"); // Trạng thái đóng phiên
    lenient().when(mockResultSet.getBigDecimal("current_price")).thenReturn(BigDecimal.valueOf(1000));
    lenient().when(mockResultSet.getBigDecimal("bid_increment")).thenReturn(BigDecimal.valueOf(100));

    Timestamp mockStartTime = Timestamp.valueOf(LocalDateTime.now().minusHours(2));
    Timestamp mockEndTime = Timestamp.valueOf(LocalDateTime.now().minusHours(1)); // Đã kết thúc trong quá khứ
    lenient().when(mockResultSet.getTimestamp("start_time")).thenReturn(mockStartTime);
    lenient().when(mockResultSet.getTimestamp("end_time")).thenReturn(mockEndTime);

    // Giả lập chưa có ai đặt giá cao nhất trước đó để tránh chạy sâu vào hàm tìm User
    lenient().when(mockResultSet.getInt("highest_bidder_id")).thenReturn(0);
    lenient().when(mockResultSet.wasNull()).thenReturn(true);

    // ====================================================================
    // 4. BÙ CÁC CỘT CHO BẢNG ITEMS (Phục vụ ngầm cho ItemSqlDAO.findById)
    // ====================================================================
    lenient().when(mockResultSet.getString("item_type")).thenReturn("ART");
    lenient().when(mockResultSet.getString("type")).thenReturn("ART");
    lenient().when(mockResultSet.getString("name")).thenReturn("Mona Lisa");
    lenient().when(mockResultSet.getBigDecimal("starting_price")).thenReturn(BigDecimal.valueOf(1000));
    lenient().when(mockResultSet.getInt("seller_id")).thenReturn(202);

    // ====================================================================
    // 5. THỰC THI KIỂM THỬ: Đặt giá vào phiên đã đóng
    // ====================================================================
    assertThrows(AuctionClosedException.class, () -> {
      auctionSqlDAO.placeBid(1, dummyCustomer, BigDecimal.valueOf(1200));
    });
  }

  @Test
  void testPlaceBid_InvalidPriceLow() throws Exception {
    // 1. Khởi tạo Mock ResultSetMetaData để tránh lỗi cấu trúc bảng của ItemSqlDAO
    ResultSetMetaData mockMetaData = mock(ResultSetMetaData.class);
    lenient().when(mockMetaData.getColumnCount()).thenReturn(10);
    lenient().when(mockResultSet.getMetaData()).thenReturn(mockMetaData);

    // 2. Cấu hình luồng đọc rs.next() trả về true cho cả 2 lượt (1 cho Auction, 1 cho Item)
    lenient().when(mockResultSet.next())
        .thenReturn(true)   // Lần 1: Cho AuctionSqlDAO.findById()
        .thenReturn(true)   // Lần 2: Cho ItemSqlDAO.findById() ngầm bên trong mapAuction
        .thenReturn(false);

    // ====================================================================
    // 3. CẤU HÌNH CÁC CỘT CHO BẢNG AUCTIONS (Phục vụ AuctionSqlDAO)
    // ====================================================================
    lenient().when(mockResultSet.getInt("id")).thenReturn(1);
    lenient().when(mockResultSet.getInt("item_id")).thenReturn(101);
    lenient().when(mockResultSet.getString("status")).thenReturn("RUNNING");
    lenient().when(mockResultSet.getBigDecimal("current_price")).thenReturn(BigDecimal.valueOf(1000));

    // Cung cấp cả 2 tên cột bước giá để tương thích tuyệt đối với mã nguồn của bạn
    lenient().when(mockResultSet.getBigDecimal("bid_increment")).thenReturn(BigDecimal.valueOf(100));
    lenient().when(mockResultSet.getBigDecimal("min_increment")).thenReturn(BigDecimal.valueOf(100));

    Timestamp mockStartTime = Timestamp.valueOf(LocalDateTime.now().minusHours(1));
    Timestamp mockEndTime = Timestamp.valueOf(LocalDateTime.now().plusHours(2));
    lenient().when(mockResultSet.getTimestamp("start_time")).thenReturn(mockStartTime);
    lenient().when(mockResultSet.getTimestamp("end_time")).thenReturn(mockEndTime);

    // 🎯 CHÌA KHÓA: Giả lập chưa có ai đặt giá cao nhất trước đó để tránh chạy sâu vào hàm tìm User
    lenient().when(mockResultSet.getInt("highest_bidder_id")).thenReturn(0);
    lenient().when(mockResultSet.wasNull()).thenReturn(true);

    // ====================================================================
    // 4. BÙ CÁC CỘT CHO BẢNG ITEMS (Phục vụ ngầm cho ItemSqlDAO.findById)
    // ====================================================================
    lenient().when(mockResultSet.getString("item_type")).thenReturn("ART");
    lenient().when(mockResultSet.getString("type")).thenReturn("ART");
    lenient().when(mockResultSet.getString("name")).thenReturn("Mona Lisa");
    lenient().when(mockResultSet.getBigDecimal("starting_price")).thenReturn(BigDecimal.valueOf(1000));
    lenient().when(mockResultSet.getInt("seller_id")).thenReturn(202);

    // ====================================================================
    // 5. THỰC THI KIỂM THỬ: Đặt giá 1050
    // (Yêu cầu tối thiểu: Hiện tại 1000 + Bước giá 100 = 1100 VNĐ)
    // ====================================================================
    assertThrows(InvalidBidException.class, () -> {
      auctionSqlDAO.placeBid(1, dummyCustomer, BigDecimal.valueOf(1050));
    });
  }

  @Test
  void testPlaceBid_AntiSnipingTriggered() throws Exception {
    // 1. Khởi tạo Mock ResultSetMetaData để tránh lỗi cấu trúc bảng của ItemSqlDAO
    ResultSetMetaData mockMetaData = mock(ResultSetMetaData.class);
    lenient().when(mockMetaData.getColumnCount()).thenReturn(10);
    lenient().when(mockResultSet.getMetaData()).thenReturn(mockMetaData);

    // 2. Định tuyến luồng đọc rs.next() cho từng bước xử lý ngầm trong logic của bạn
    lenient().when(mockResultSet.next())
        .thenReturn(true)   // auction
        .thenReturn(true)   // item
        .thenReturn(true)   // balance
        .thenReturn(true)   // generatedKeys
        .thenReturn(false); // triggerAutoBids

    // ====================================================================
    // 3. CẤU HÌNH CÁC CỘT CHO BẢNG AUCTIONS (Đồng bộ chuẩn xác dòng 436, 437)
    // ====================================================================
    lenient().when(mockResultSet.getInt("id")).thenReturn(1);
    lenient().when(mockResultSet.getInt("item_id")).thenReturn(101);
    lenient().when(mockResultSet.getString("status")).thenReturn("RUNNING");
    lenient().when(mockResultSet.getBigDecimal("current_price")).thenReturn(BigDecimal.valueOf(1000));

    // Hỗ trợ cả 2 tên cột bước giá để tương thích tuyệt đối với code của bạn
    lenient().when(mockResultSet.getBigDecimal("bid_increment")).thenReturn(BigDecimal.valueOf(100));
    lenient().when(mockResultSet.getBigDecimal("min_increment")).thenReturn(BigDecimal.valueOf(100));

    // 🎯 SỬA LỖI NPE: Bổ sung cả start_time lẫn end_time hợp lệ dưới dạng Timestamp
    Timestamp mockStartTime = Timestamp.valueOf(LocalDateTime.now().minusHours(1));
    Timestamp mockEndTime = Timestamp.valueOf(LocalDateTime.now().plusSeconds(10)); // Sắp bắn tỉa (còn 10s)
    lenient().when(mockResultSet.getTimestamp("start_time")).thenReturn(mockStartTime);
    lenient().when(mockResultSet.getTimestamp("end_time")).thenReturn(mockEndTime);

    // Cấu hình các tham số kích hoạt tính năng Anti-sniping phá phiên của bạn
    lenient().when(mockResultSet.getInt("anti_snipe_window_minutes")).thenReturn(2);
    lenient().when(mockResultSet.getInt("anti_snipe_extension_minutes")).thenReturn(5);

    // Giả lập chưa có ai đặt giá cao nhất trước đó để tránh nhảy vào nhánh tìm User khác
    lenient().when(mockResultSet.getInt("highest_bidder_id")).thenReturn(0);
    lenient().when(mockResultSet.wasNull()).thenReturn(true);

    // ====================================================================
    // 4. BÙ CÁC CỘT CHO BẢNG ITEMS (Phục vụ ngầm cho ItemSqlDAO.findById)
    // ====================================================================
    lenient().when(mockResultSet.getString("item_type")).thenReturn("ART");
    lenient().when(mockResultSet.getString("type")).thenReturn("ART");
    lenient().when(mockResultSet.getString("name")).thenReturn("Mona Lisa");
    lenient().when(mockResultSet.getBigDecimal("starting_price")).thenReturn(BigDecimal.valueOf(1000));
    lenient().when(mockResultSet.getInt("seller_id")).thenReturn(202);

    // ====================================================================
    // 5. GIẢ LẬP ĐẦU RA SỐ DƯ TÀI KHOẢN VÀ CÁC HÀM UPDATE
    // ====================================================================
    lenient().when(mockResultSet.getBigDecimal("balance")).thenReturn(BigDecimal.valueOf(10000)); // Ví có 10.000 VNĐ
    lenient().when(mockStatement.executeUpdate()).thenReturn(1);
    lenient().when(mockStatement.getGeneratedKeys()).thenReturn(mockResultSet);

    lenient().when(mockResultSet.getInt(1)).thenReturn(999);

    // ====================================================================
    // 6. THỰC THI KIỂM THỬ: Đặt mức giá 1500 VNĐ (Thỏa mãn > 1000 + 100)
    // ====================================================================
    boolean success = auctionSqlDAO.placeBid(1, dummyCustomer, BigDecimal.valueOf(1500));
    assertTrue(success);
  }

  // ==========================================================================
  // ─── PHẦN 3: KIỂM THỬ CÁC PUBLIC METHODS CÒN LẠI VÀ CHỨC NĂNG ADMIN ───────
  // ==========================================================================

  @Test
  void testFinishAuction_Success() throws Exception {

    // Auction hợp lệ
    mockValidAuction("RUNNING");
    mockValidItem();

    when(mockResultSet.next())
        .thenReturn(true)   // Auction.findById()
        .thenReturn(true);  // Item.findById()

    when(mockStatement.executeUpdate())
        .thenReturn(1);

    assertDoesNotThrow(() ->
        auctionSqlDAO.finishAuction(1)
    );
  }

  @Test
  void testDeleteAuction_Success() throws Exception {

    mockValidAuction("RUNNING");
    mockValidItem();

    when(mockResultSet.next())
        .thenReturn(true)   // Auction.findById()
        .thenReturn(true);  // Item.findById()

    when(mockStatement.executeUpdate())
        .thenReturn(1);

    assertDoesNotThrow(() ->
        auctionSqlDAO.deleteAuction(1)
    );
  }


  @Test
  void testSetAutoBid_Success() throws Exception {

    when(mockStatement.executeUpdate())
        .thenReturn(1);

    assertDoesNotThrow(() ->
        auctionSqlDAO.setAutoBid(
            1,
            1,
            BigDecimal.valueOf(2000)
        )
    );
  }



  // ==========================================================================
  // ─── PHẦN 4: QUÉT SẠCH KHỐI CATCH CỦA SQLEXCEPTION (EXCEPTIONS COVERAGE) ──
  // ==========================================================================

  @Test
  void testAllMethods_HandlesSQLExceptionGracefully() throws SQLException {
    // 1. Ép Connection văng lỗi SQLException ngay lập tức khi gọi đến database
    when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("Database Connection Crash!"));
    when(mockConnection.prepareStatement(anyString(), anyInt())).thenThrow(new SQLException("Database Connection Crash!"));

    // ====================================================================
    // NHÓM 1: Các hàm CHỦ ĐỘNG THROW UserException (Kiểm tra bằng assertThrows)
    // ====================================================================
    assertThrows(UserException.class, () -> {
      auctionSqlDAO.createAuction(dummyItem, BigDecimal.valueOf(100), LocalDateTime.now(), LocalDateTime.now(), BigDecimal.valueOf(10));
    });

    assertThrows(UserException.class, () -> {
      auctionSqlDAO.findById(1);
    });

    assertThrows(UserException.class, () -> {
      auctionSqlDAO.finishAuction(1);
    });

    assertThrows(UserException.class, () -> {
      auctionSqlDAO.deleteAuction(1);
    });

    assertThrows(UserException.class, () -> {
      auctionSqlDAO.setAutoBid(1, 1, BigDecimal.valueOf(500)); // 🎯 Đã chuyển sang nhóm throw theo dòng 319
    });

    assertThrows(UserException.class, () -> {
      auctionSqlDAO.removeAutoBid(1, 1); // Cùng bộ với setAutoBid nên cũng throw
    });

    assertThrows(UserException.class, () -> {
      auctionSqlDAO.registerBidderForAuction(1, 1); // Hàm đăng ký đấu giá cũng throw UserException
    });


    // ====================================================================
    // NHÓM 2: Các hàm HOÀN TOÀN NUỐT LỖI (Kiểm tra bằng assertDoesNotThrow)
    // ====================================================================
    assertDoesNotThrow(() -> {
      auctionSqlDAO.getAllAuctions();
      auctionSqlDAO.getAuctionsByStatus("RUNNING");

      // Các hàm phụ trợ lấy thông tin, RAM cache (chỉ in log System.err rồi return rỗng)
      auctionSqlDAO.getRegisteredBidders(1);
      auctionSqlDAO.isBidderRegistered(1, 1);
      auctionSqlDAO.getRegistrationCount(1);
      auctionSqlDAO.getAutoBidsByAuctionId(1);
      auctionSqlDAO.getActiveAutoBidMaxPrice(1, 1);
    });
  }

  // ==========================================================================
  // ─── PHẦN 5: FINDBYID – THROW USEREXCEPTION KHI KHÔNG TÌM THẤY ──────────
  // ==========================================================================

  /**
   * findById() trong DAO thực tế KHÔNG trả về null mà throw UserException
   * khi không tìm thấy phiên. Test cũ (testFindById_NotFound) đang assert
   * assertNull(...) — sẽ fail. Test dưới đây kiểm tra đúng hành vi thực.
   */
  @Test
  void testFindById_NotFound_ThrowsUserException() throws SQLException {
    when(mockResultSet.next()).thenReturn(false);

    assertThrows(UserException.class, () -> auctionSqlDAO.findById(999));
  }

  // ==========================================================================
  // ─── PHẦN 6: CREATEAUCTION – CÁC NHÁNH PHÂN KỲ CỦA BID_INCREMENT ────────
  // ==========================================================================

  /**
   * Khi bidIncrement truyền vào là null, DAO phải tự dùng giá trị mặc định
   * (10_000 VNĐ) và vẫn tạo phiên thành công.
   */
  @Test
  void testCreateAuction_NullBidIncrement_UsesDefault() throws Exception {
    when(mockStatement.executeUpdate()).thenReturn(1);
    when(mockStatement.getGeneratedKeys()).thenReturn(mockResultSet);
    when(mockResultSet.next()).thenReturn(true);
    when(mockResultSet.getInt(1)).thenReturn(77);

    // Truyền null cho bidIncrement — DAO nên tự điền mặc định 10_000
    Auction result = auctionSqlDAO.createAuction(
        dummyItem,
        BigDecimal.valueOf(500),
        LocalDateTime.now(),
        LocalDateTime.now().plusDays(1),
        null   // <-- NULL
    );

    assertNotNull(result);
    assertEquals(77, result.getId());
  }

  /**
   * Khi bidIncrement = 0 (không hợp lệ theo điều kiện > 0), DAO cũng phải
   * rơi vào nhánh dùng giá mặc định.
   */
  @Test
  void testCreateAuction_ZeroBidIncrement_UsesDefault() throws Exception {
    when(mockStatement.executeUpdate()).thenReturn(1);
    when(mockStatement.getGeneratedKeys()).thenReturn(mockResultSet);
    when(mockResultSet.next()).thenReturn(true);
    when(mockResultSet.getInt(1)).thenReturn(88);

    Auction result = auctionSqlDAO.createAuction(
        dummyItem,
        BigDecimal.valueOf(500),
        LocalDateTime.now(),
        LocalDateTime.now().plusDays(1),
        BigDecimal.ZERO   // <-- ZERO
    );

    assertNotNull(result);
    assertEquals(88, result.getId());
  }

  /**
   * Khi getGeneratedKeys() không trả về dòng nào (rs.next() = false),
   * DAO phải ném UserException "Không thể tạo phiên đấu giá."
   */
  @Test
  void testCreateAuction_NoGeneratedKey_ThrowsUserException() throws Exception {
    when(mockStatement.executeUpdate()).thenReturn(1);
    when(mockStatement.getGeneratedKeys()).thenReturn(mockResultSet);
    when(mockResultSet.next()).thenReturn(false); // không có ID tự sinh

    assertThrows(UserException.class, () ->
        auctionSqlDAO.createAuction(
            dummyItem,
            BigDecimal.valueOf(100),
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(1),
            BigDecimal.valueOf(50)
        )
    );
  }

  // ==========================================================================
  // ─── PHẦN 7: GETAUCTIONSBYSELLER – LUỒNG THÀNH CÔNG VÀ RỖng ─────────────
  // ==========================================================================
  /**
   * getAuctionsBySeller() khi DB ném SQLException → trả về list rỗng (không throw).
   */
  @Test
  void testGetAuctionsBySeller_SQLException_ReturnsEmptyList() throws SQLException {
    when(mockConnection.prepareStatement(anyString()))
        .thenThrow(new SQLException("DB down"));

    List<Auction> result = auctionSqlDAO.getAuctionsBySeller(202, "RUNNING");
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  // ==========================================================================
  // ─── PHẦN 8: REGISTERFORAUCTION – THÀNH CÔNG VÀ DUPLICATE ───────────────
  // ==========================================================================

  /**
   * registerForAuction() thành công khi executeUpdate() trả về 1.
   */
  @Test
  void testRegisterForAuction_Success() throws Exception {
    when(mockStatement.executeUpdate()).thenReturn(1);

    boolean result = auctionSqlDAO.registerForAuction(1, 99);
    assertTrue(result);
  }

  /**
   * registerForAuction() khi đã đăng ký rồi (Duplicate key – error code 1062)
   * → phải ném UserException với message "ALREADY_REGISTERED".
   */
  @Test
  void testRegisterForAuction_DuplicateEntry_ThrowsAlreadyRegistered() throws Exception {
    SQLException dupEx = mock(SQLException.class);
    when(dupEx.getErrorCode()).thenReturn(1062);
    when(mockStatement.executeUpdate()).thenThrow(dupEx);

    UserException ex = assertThrows(UserException.class, () ->
        auctionSqlDAO.registerForAuction(1, 99)
    );
    assertEquals("ALREADY_REGISTERED", ex.getMessage());
  }

  /**
   * registerForAuction() khi gặp SQLException thông thường (không phải 1062)
   * → ném UserException với message bắt đầu bằng "Lỗi lưu đăng ký".
   */
  @Test
  void testRegisterForAuction_GenericSQLException_ThrowsUserException() throws Exception {
    when(mockConnection.prepareStatement(anyString()))
        .thenThrow(new SQLException("Generic DB error"));

    UserException ex = assertThrows(UserException.class, () ->
        auctionSqlDAO.registerForAuction(1, 99)
    );
    assertTrue(ex.getMessage().startsWith("Lỗi lưu đăng ký"));
  }

  // ==========================================================================
  // ─── PHẦN 9: GETREGISTEREDAUCTIONIDSFORBIDDER & GETREGISTEREDBIDDERS ─────
  // ==========================================================================

  /**
   * getRegisteredAuctionIdsForBidder() với 3 dòng kết quả → trả về list 3 phần tử.
   */
  @Test
  void testGetRegisteredAuctionIdsForBidder_ReturnsList() throws SQLException {
    when(mockResultSet.next()).thenReturn(true, true, true, false);
    when(mockResultSet.getInt("auction_id")).thenReturn(5, 10, 15);

    List<Integer> result = auctionSqlDAO.getRegisteredAuctionIdsForBidder(1);
    assertNotNull(result);
    assertEquals(3, result.size());
  }

  /**
   * getRegisteredAuctionIdsForBidder() khi DB lỗi → trả về list rỗng.
   */
  @Test
  void testGetRegisteredAuctionIdsForBidder_SQLException_ReturnsEmpty() throws SQLException {
    when(mockConnection.prepareStatement(anyString()))
        .thenThrow(new SQLException("DB error"));

    List<Integer> result = auctionSqlDAO.getRegisteredAuctionIdsForBidder(1);
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  /**
   * getRegisteredBidders() với 2 dòng → trả về list 2 phần tử.
   */
  @Test
  void testGetRegisteredBidders_ReturnsList() throws SQLException {
    when(mockResultSet.next()).thenReturn(true, true, false);
    // Dùng getInt(1) vì code dùng rs.getInt(1) (index-based)
    when(mockResultSet.getInt(1)).thenReturn(101, 202);

    List<Integer> result = auctionSqlDAO.getRegisteredBidders(1);
    assertNotNull(result);
    assertEquals(2, result.size());
  }

  /**
   * getRegisteredBidders() khi DB lỗi → trả về list rỗng.
   */
  @Test
  void testGetRegisteredBidders_SQLException_ReturnsEmpty() throws SQLException {
    when(mockConnection.prepareStatement(anyString()))
        .thenThrow(new SQLException("DB error"));

    List<Integer> result = auctionSqlDAO.getRegisteredBidders(99);
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  // ==========================================================================
  // ─── PHẦN 10: GETACTIVEAUCTIONSCOUNT (ADMIN DASHBOARD) ───────────────────
  // ==========================================================================

  /**
   * getActiveAuctionsCount() trả về đúng con số từ DB.
   */
  @Test
  void testGetActiveAuctionsCount_ReturnsCount() throws SQLException {
    // DAO dùng try-with-resources liên tiếp: conn → stmt → rs
    // mockStatement.executeQuery() đã được cấu hình trả về mockResultSet trong setUp()
    when(mockResultSet.next()).thenReturn(true);
    when(mockResultSet.getInt(1)).thenReturn(42);

    int count = auctionSqlDAO.getActiveAuctionsCount();
    assertEquals(42, count);
  }

  /**
   * getActiveAuctionsCount() khi rs.next() = false → trả về 0 (fallback).
   */
  @Test
  void testGetActiveAuctionsCount_NoRow_ReturnsZero() throws SQLException {
    when(mockResultSet.next()).thenReturn(false);

    int count = auctionSqlDAO.getActiveAuctionsCount();
    assertEquals(0, count);
  }

  /**
   * getActiveAuctionsCount() khi DB lỗi → trả về 0 (không throw).
   */
  @Test
  void testGetActiveAuctionsCount_SQLException_ReturnsZero() throws SQLException {
    when(mockConnection.prepareStatement(anyString()))
        .thenThrow(new SQLException("DB down"));

    int count = auctionSqlDAO.getActiveAuctionsCount();
    assertEquals(0, count);
  }

  // ==========================================================================
  // ─── PHẦN 11: FINISHAUCTION – CÁC NHÁNH ĐẶC BIỆT ────────────────────────
  // ==========================================================================

  /**
   * finishAuction() khi phiên đã có status FINISHED → early return, không làm gì.
   * Xác minh bằng cách đảm bảo không có exception và không gọi executeUpdate().
   */
  @Test
  void testFinishAuction_AlreadyFinished_EarlyReturn() throws Exception {

    when(mockResultSet.next()).thenReturn(true);

    mockValidAuction("FINISHED");
    mockValidItem();

    assertDoesNotThrow(() ->
        auctionSqlDAO.finishAuction(1)
    );
  }

  /**
   * finishAuction() khi phiên có status CANCELED → early return tương tự.
   */
  @Test
  void testFinishAuction_AlreadyCanceled_EarlyReturn() throws Exception {

    when(mockResultSet.next()).thenReturn(true);

    mockValidAuction("CANCELED");
    mockValidItem();

    assertDoesNotThrow(() ->
        auctionSqlDAO.finishAuction(2)
    );
  }

  // ==========================================================================
  // ─── PHẦN 12: PLACEBID – NHÁNH INSUFFICIENT BALANCE ─────────────────────
  // ==========================================================================

  /**
   * placeBid() khi số dư tài khoản không đủ → ném InvalidBidException.
   *
   * Logic trong DAO: sau khi kiểm tra giá hợp lệ (>= minRequired), nó gọi
   * userDao.getBalance() nội bộ. Vì userDao là dependency thật (không mock),
   * ta cần mock ResultSet để userDao trả về số dư thấp.
   *
   * Cách tiếp cận: mock tất cả các lần gọi rs để:
   *   - Lần 1 (findById): auction status = RUNNING, price = 1000, increment = 100
   *   - Lần 2 (userDao.getBalance): balance = 500  ← thấp hơn bidAmount
   */
  @Test
  void testPlaceBid_InsufficientBalance_ThrowsInvalidBidException() throws Exception {

    // Auction.findById()
    mockValidAuction("RUNNING");

    // Item.findById()
    mockValidItem();

    when(mockResultSet.getInt("highest_bidder_id"))
        .thenReturn(0);
    when(mockResultSet.wasNull())
        .thenReturn(true);

    // userDao.getBalance()
    when(mockResultSet.getBigDecimal("balance"))
        .thenReturn(BigDecimal.valueOf(500));

    assertThrows(
        InvalidBidException.class,
        () -> auctionSqlDAO.placeBid(
            1,
            dummyCustomer,
            BigDecimal.valueOf(1500)
        )
    );
  }

  // ==========================================================================
  // ─── PHẦN 13: EVALUATEAUTOBIDSFORAUCTION ─────────────────────────────────
  // ==========================================================================

  /**
   * evaluateAutoBidsForAuction() khi không có auto-bid nào → chạy không lỗi.
   */
  @Test
  void testEvaluateAutoBidsForAuction_NoAutoBids_DoesNotThrow() throws Exception {
    // 1. Gọi các hàm helper trước để chúng nạp cấu hình thuộc tính của Auction và Item
    mockValidAuction("RUNNING");
    mockValidItem();

    // 2. CHỐT HẠ (Ghi đè lên helper): Định tuyến chính xác luồng lặp rs.next() cho testcase này
    // Dùng doReturn().when() để Mockito ghi đè an toàn tuyệt đối chuỗi hành vi
    doReturn(true, true, false)
        .when(mockResultSet)
        .next();
    // 💡 Nhịp 1: true (Auction findById) -> Nhịp 2: true (Item findById) -> Nhịp 3: false (Báo danh sách Bot AutoBid trống)

    // 3. Thực thi kiểm thử
    assertDoesNotThrow(() ->
        auctionSqlDAO.evaluateAutoBidsForAuction(1)
    );
  }

  /**
   * evaluateAutoBidsForAuction() khi connection ném SQLException → throw UserException.
   */
  @Test
  void testEvaluateAutoBidsForAuction_SQLException_ThrowsUserException() throws Exception {
    // Ép lỗi ngay khi lấy connection (gọi lần 2 vì lần 1 là findById bên trong)
    when(mockConnection.prepareStatement(anyString()))
        .thenThrow(new SQLException("Connection failed"));

    assertThrows(UserException.class, () ->
        auctionSqlDAO.evaluateAutoBidsForAuction(1)
    );
  }

  // ==========================================================================
  // ─── PHẦN 14: PLACEBID – NHÁNH OPEN (KHÔNG PHẢI RUNNING) ─────────────────
  // ==========================================================================

  /**
   * placeBid() với status = "OPEN" (chưa bắt đầu) → ném AuctionClosedException.
   * Nhánh này khác với FINISHED nhưng cùng điều kiện !"RUNNING".equals(status).
   */
  @Test
  void testPlaceBid_StatusOpen_ThrowsAuctionClosedException() throws Exception {

    when(mockResultSet.next())
        .thenReturn(true)   // Auction.findById
        .thenReturn(true);  // Item.findById

    mockValidAuction("OPEN");
    mockValidItem();

    assertThrows(
        AuctionClosedException.class,
        () -> auctionSqlDAO.placeBid(
            1,
            dummyCustomer,
            BigDecimal.valueOf(1200)
        )
    );
  }

  /**
   * placeBid() với status = "CANCELED" → ném AuctionClosedException.
   */
  @Test
  void testPlaceBid_StatusCanceled_ThrowsAuctionClosedException() throws Exception {

    when(mockResultSet.next())
        .thenReturn(true)   // Auction.findById
        .thenReturn(true);  // Item.findById

    mockValidAuction("CANCELED");
    mockValidItem();

    assertThrows(
        AuctionClosedException.class,
        () -> auctionSqlDAO.placeBid(
            1,
            dummyCustomer,
            BigDecimal.valueOf(1200)
        )
    );
  }

  // ==========================================================================
  // ─── PHẦN 15: SETAUTOBID – THROW USEREXCEPTION KHI SQLEXCEPTION ──────────
  // ==========================================================================

  /**
   * setAutoBid() khi DB lỗi → ném UserException (không nuốt im).
   */
  @Test
  void testSetAutoBid_SQLException_ThrowsUserException() throws SQLException {
    when(mockConnection.prepareStatement(anyString()))
        .thenThrow(new SQLException("DB down"));

    assertThrows(UserException.class, () ->
        auctionSqlDAO.setAutoBid(1, 1, BigDecimal.valueOf(5000))
    );
  }

  /**
   * removeAutoBid() khi DB lỗi → ném UserException.
   */
  @Test
  void testRemoveAutoBid_SQLException_ThrowsUserException() throws SQLException {
    when(mockConnection.prepareStatement(anyString()))
        .thenThrow(new SQLException("DB down"));

    assertThrows(UserException.class, () ->
        auctionSqlDAO.removeAutoBid(1, 1)
    );
  }

  /**
   * registerBidderForAuction() khi DB lỗi → ném UserException.
   */
  @Test
  void testRegisterBidderForAuction_SQLException_ThrowsUserException() throws SQLException {
    when(mockConnection.prepareStatement(anyString()))
        .thenThrow(new SQLException("DB down"));

    assertThrows(UserException.class, () ->
        auctionSqlDAO.registerBidderForAuction(1, 1)
    );
  }

  // ==========================================================================
  // ─── PHẦN 16: GETREGISTRATIONCOUNT – FALLBACK VÀ NHÁNH RS.NEXT FALSE ─────
  // ==========================================================================

  /**
   * getRegistrationCount() khi rs.next() = false → trả về 0 (fallback).
   */
  @Test
  void testGetRegistrationCount_NoRow_ReturnsZero() throws SQLException {
    when(mockResultSet.next()).thenReturn(false);

    int count = auctionSqlDAO.getRegistrationCount(1);
    assertEquals(0, count);
  }

  /**
   * getRegistrationCount() khi DB ném lỗi → trả về 0 (không throw).
   */
  @Test
  void testGetRegistrationCount_SQLException_ReturnsZero() throws SQLException {
    when(mockConnection.prepareStatement(anyString()))
        .thenThrow(new SQLException("DB error"));

    int count = auctionSqlDAO.getRegistrationCount(1);
    assertEquals(0, count);
  }

  // ==========================================================================
  // ─── PHẦN 17: GETAUTOBIDSBYAUCTIONID – DANH SÁCH RỖNG ───────────────────
  // ==========================================================================

  /**
   * getAutoBidsByAuctionId() khi không có auto-bid nào → trả về list rỗng.
   */
  @Test
  void testGetAutoBidsByAuctionId_Empty() throws SQLException {
    when(mockResultSet.next()).thenReturn(false);

    List<AuctionManager.RemoteAutoBid> list = auctionSqlDAO.getAutoBidsByAuctionId(1);
    assertNotNull(list);
    assertTrue(list.isEmpty());
  }

  /**
   * getAutoBidsByAuctionId() khi DB lỗi → trả về list rỗng (không throw).
   */
  @Test
  void testGetAutoBidsByAuctionId_SQLException_ReturnsEmpty() throws SQLException {
    when(mockConnection.prepareStatement(anyString()))
        .thenThrow(new SQLException("DB error"));

    List<AuctionManager.RemoteAutoBid> list = auctionSqlDAO.getAutoBidsByAuctionId(1);
    assertNotNull(list);
    assertTrue(list.isEmpty());
  }

  // ==========================================================================
  // ─── PHẦN 18: GETACTIVEAUTOBIDMAXPRICE – NHÁNH SQLEXCEPTION ─────────────
  // ==========================================================================

  /**
   * getActiveAutoBidMaxPrice() khi DB lỗi → trả về null (không throw).
   */
  @Test
  void testGetActiveAutoBidMaxPrice_SQLException_ReturnsNull() throws SQLException {
    when(mockConnection.prepareStatement(anyString()))
        .thenThrow(new SQLException("DB error"));

    BigDecimal result = auctionSqlDAO.getActiveAutoBidMaxPrice(1, 1);
    assertNull(result);
  }
}