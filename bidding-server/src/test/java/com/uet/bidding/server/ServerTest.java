package com.uet.bidding.server;

import com.uet.bidding.model.NetworkMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ServerTest {

  @Mock private ClientHandler mockClient1;
  @Mock private ClientHandler mockClient2;

  @BeforeEach
  void setUp() {
    // Dọn sạch danh sách activeClients trước mỗi ca test để đảm bảo độc lập dữ liệu
    Server.activeClients.clear();
  }

  @Test
  void testAddAndRemoveClient() {
    // 1. Kiểm tra thêm client
    Server.activeClients.add(mockClient1);
    Server.activeClients.add(mockClient2);
    assertEquals(2, Server.activeClients.size(), "Danh sách phải chứa 2 kết nối.");

    // 2. Kiểm tra xóa client thông qua hàm của Server
    Server.removeClient(mockClient1);
    assertEquals(1, Server.activeClients.size());
    assertFalse(Server.activeClients.contains(mockClient1));
  }

  @Test
  void testBroadcastMessage() {
    NetworkMessage broadcastMsg = new NetworkMessage("BROADCAST_TEST", "Hello World!");

    Server.activeClients.add(mockClient1);
    Server.activeClients.add(mockClient2);

    // Gọi hàm broadcast hệ thống
    Server.broadcast(broadcastMsg);

    // Kiểm chứng: Tất cả các client đang active đều phải nhận được lệnh sendMessage đúng nội dung
    verify(mockClient1, times(1)).sendMessage(broadcastMsg);
    verify(mockClient2, times(1)).sendMessage(broadcastMsg);
  }
}