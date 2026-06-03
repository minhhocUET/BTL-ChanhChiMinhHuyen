package com.uet.bidding.service;

import com.uet.bidding.dao.AuctionSqlDAO;
import com.uet.bidding.model.Auction;
import com.uet.bidding.model.Item;
import com.uet.bidding.model.NetworkMessage;
import com.uet.bidding.server.Server;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuctionExpirationTaskTest {

  @Mock
  private AuctionSqlDAO mockAuctionSqlDAO;

  private AuctionExpirationTask expirationTask;

  // Đối tượng mock tĩnh dùng để bắt các lệnh gọi Server.broadcast(...)
  private MockedStatic<Server> mockedServerStatic;

  @BeforeEach
  void setUp() {
    expirationTask = new AuctionExpirationTask(mockAuctionSqlDAO);
    // Khởi tạo Mock Static cho class Server trước mỗi test case
    mockedServerStatic = mockStatic(Server.class);
  }

  @AfterEach
  void tearDown() {
    // Giải phóng Mock Static để tránh ảnh hưởng tới các class test khác
    mockedServerStatic.close();
    // Đảm bảo luồng scheduler được tắt sau khi test xong
    expirationTask.stop();
  }

  /**
   * Trường hợp 1: Danh sách trống, không có phiên nào đang chạy
   */
  @Test
  void testCheckAndCloseAuctions_EmptyList() throws Exception {
    when(mockAuctionSqlDAO.getAllAuctions()).thenReturn(Collections.emptyList());

    // Vì checkAndCloseAuctions() là private, chúng ta kích hoạt thông qua việc gọi hàm start() ngầm định
    // Hoặc để kiểm tra chính xác core logic ngay lập tức mà không cần đợi scheduler 5 giây,
    // chúng ta dùng Reflection để gọi trực tiếp hàm private này.
    invokePrivateCheckAndClose();

    // Kiểm chứng: Chỉ quét chứ không làm gì thêm vì danh sách trống
    verify(mockAuctionSqlDAO, times(1)).getAllAuctions();
    verify(mockAuctionSqlDAO, never()).finishAuction(anyInt());
    mockedServerStatic.verify(() -> Server.broadcast(any(NetworkMessage.class)), never());
  }

  /**
   * Trường hợp 2: Có phiên đang chạy nhưng CHƯA HẾT HẠN (Thời gian kết thúc ở tương lai)
   */
  @Test
  void testCheckAndCloseAuctions_NotYetExpired() throws Exception {
    List<Auction> activeAuctions = new ArrayList<>();

    Auction liveAuction = new Auction();
    liveAuction.setId(10);
    // Đặt thời gian kết thúc là 1 tiếng sau (chưa hết hạn)
    liveAuction.setEndTime(LocalDateTime.now().plusHours(1));
    activeAuctions.add(liveAuction);

    when(mockAuctionSqlDAO.getAllAuctions()).thenReturn(activeAuctions);

    invokePrivateCheckAndClose();

    // Kiểm chứng: Không được đóng và không phát loa thông báo
    verify(mockAuctionSqlDAO, never()).finishAuction(10);
    mockedServerStatic.verify(() -> Server.broadcast(any(NetworkMessage.class)), never());
  }

  /**
   * Trường hợp 3: Có phiên đấu giá ĐÃ HẾT HẠN -> Phải đóng phiên thành công và phát thông báo
   */
  @Test
  void testCheckAndCloseAuctions_Success_Expired() throws Exception {
    List<Auction> activeAuctions = new ArrayList<>();

    Item mockItem = mock(Item.class);
    when(mockItem.getName()).thenReturn("Bình hoa cổ");

    Auction expiredAuction = new Auction();
    expiredAuction.setId(99);
    expiredAuction.setItem(mockItem);
    expiredAuction.setEndTime(LocalDateTime.now().minusMinutes(5));
    activeAuctions.add(expiredAuction);

    // Chuẩn bị dữ liệu trả về sau khi cập nhật thành công từ DB
    Auction updatedAuction = new Auction();
    updatedAuction.setId(99);
    updatedAuction.setItem(mockItem);

    when(mockAuctionSqlDAO.getAllAuctions()).thenReturn(activeAuctions);
    when(mockAuctionSqlDAO.findById(99)).thenReturn(updatedAuction);
    when(mockAuctionSqlDAO.getRegistrationCount(99)).thenReturn(15); // Giả lập có 15 người đăng ký

    invokePrivateCheckAndClose();

    // 1. Kiểm tra xem tầng DAO có thực thi đóng phiên không
    verify(mockAuctionSqlDAO, times(1)).finishAuction(99);
    verify(mockAuctionSqlDAO, times(1)).findById(99);
    verify(mockAuctionSqlDAO, times(1)).getRegistrationCount(99);

    // Kiểm tra xem biến số lượng người đăng ký đã được nạp lại vào đối tượng updated chưa
    assertEquals(15, updatedAuction.getRegisteredCount());

    // 2. Kiểm tra xem hệ thống có gọi phát loa Broadcast đúng 2 tin nhắn không
    ArgumentCaptor<NetworkMessage> messageCaptor = ArgumentCaptor.forClass(NetworkMessage.class);
    mockedServerStatic.verify(() -> Server.broadcast(messageCaptor.capture()), times(2));

    List<NetworkMessage> sentMessages = messageCaptor.getAllValues();

    // Tin nhắn thứ nhất: Cập nhật UI phòng đấu giá
    NetworkMessage msg1 = sentMessages.get(0);
    assertEquals("AUCTION_UPDATED", msg1.getType());
    assertNotNull(msg1.getData());

    // Tin nhắn thứ hai: Thông báo chữ chạy (Broadcast)
    NetworkMessage msg2 = sentMessages.get(1);
    assertEquals("BROADCAST", msg2.getType());
    assertEquals("🎉 Phiên đấu giá [Bình hoa cổ] đã chính thức khép lại!", msg2.getData());
  }

  /**
   * Trường hợp 4: Luồng chạy an toàn, không crash app khi tầng DAO ném ra Exception (ví dụ mất kết nối DB)
   */
  @Test
  void testCheckAndCloseAuctions_HandlesExceptionGracefully() throws Exception {
    // Giả lập việc truy vấn DB bị lỗi sập kết nối ngoại lệ
    when(mockAuctionSqlDAO.getAllAuctions()).thenThrow(new RuntimeException("Kết nối Database bị ngắt đột ngột!"));

    // Lệnh này không được ném ra lỗi làm crash luồng chính nhờ khối try-catch trong code thật của bạn
    invokePrivateCheckAndClose();

    // Kiểm tra xem tầng xử lý đóng có bị bỏ qua an toàn không
    verify(mockAuctionSqlDAO, never()).finishAuction(anyInt());
  }

  /**
   * Hàm trợ giúp (Helper) dùng Java Reflection để chọc vào gọi trực tiếp phương thức private
   * "checkAndCloseAuctions", giúp bài test chạy nhanh tức thì mà không cần block luồng 5 giây.
   */
  private void invokePrivateCheckAndClose() throws Exception {
    java.lang.reflect.Method method = AuctionExpirationTask.class.getDeclaredMethod("checkAndCloseAuctions");
    method.setAccessible(true);
    method.invoke(expirationTask);
  }
}