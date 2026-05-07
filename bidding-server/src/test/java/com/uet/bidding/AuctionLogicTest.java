package com.uet.bidding;

import com.uet.bidding.exception.AuthenticationException;
import com.uet.bidding.exception.InvalidBidException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class AuctionLogicTest {
  private ClientHandler handler;

  @BeforeEach
  void setUp() {
    // [ĐÃ SỬA] Khởi tạo handler với 2 tham số (Socket, UserDAO) đều là null để test logic thuần túy
    handler = new ClientHandler(null, null, null, null);
  }

  @Test
  void testInvalidBidAmount() {
    // [ĐÃ SỬA] Test với giá trị âm hoặc bằng 0 để đảm bảo ném ra InvalidBidException
    assertThrows(InvalidBidException.class, () -> {
      handler.handleAuctionLogic("-500");
    });
  }

  @Test
  void testLoginWithEmptyName() {
    assertThrows(AuthenticationException.class, () -> {
      handler.handleLoginLogic("");
    });
  }
}