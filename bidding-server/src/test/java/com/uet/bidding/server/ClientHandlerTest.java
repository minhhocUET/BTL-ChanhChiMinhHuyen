package com.uet.bidding.server;

import com.google.gson.Gson;
import com.uet.bidding.dao.AuctionSqlDAO;
import com.uet.bidding.dao.ItemSqlDAO;
import com.uet.bidding.dao.UserSqlDAO;
import com.uet.bidding.model.GsonFactory;
import com.uet.bidding.model.NetworkMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ClientHandlerTest {

  @Mock private Socket mockSocket;
  @Mock private UserSqlDAO userSqlDAO;
  @Mock private ItemSqlDAO itemSqlDAO;
  @Mock private AuctionSqlDAO auctionSqlDAO;

  @Test
  void testClientHandlerRun_AndSendResponse() throws IOException {
    // 1. Tạo gói tin giả lập Client gửi lên dưới dạng chuỗi JSON thô kết thúc bằng ký tự xuống dòng (\n)
    NetworkMessage clientMsg = new NetworkMessage("GET_ALL_AUCTIONS", "");
    clientMsg.setRequestId("req-abc");
    String jsonInput = GsonFactory.getInstance().toJson(clientMsg) + "\n";

    // 2. Giả lập luồng vào/ra của Socket
    ByteArrayInputStream inputStream = new ByteArrayInputStream(jsonInput.getBytes());
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    when(mockSocket.getInputStream()).thenReturn(inputStream);
    when(mockSocket.getOutputStream()).thenReturn(outputStream);

    // 3. Khởi tạo đối tượng cần test
    ClientHandler handler = new ClientHandler(mockSocket, userSqlDAO, itemSqlDAO, auctionSqlDAO);

    // Chạy hàm run() trực tiếp trong luồng test (không tạo luồng mới để tránh bất đồng bộ khi kiểm thử)
    handler.run();

    // 4. Kiểm thử các hàm bổ trợ gửi tin nhắn
    handler.sendResponse("TEST_TYPE", "TEST_DATA");
    String outputData = outputStream.toString();

    // Đảm bảo dữ liệu xuất ra không rỗng và chứa đúng định dạng
    assertNotNull(outputData);
    assertTrue(outputData.contains("TEST_TYPE"));
    assertTrue(outputData.contains("TEST_DATA"));
  }
}