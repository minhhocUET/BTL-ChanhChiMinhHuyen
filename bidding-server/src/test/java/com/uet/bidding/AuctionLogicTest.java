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
        // Khởi tạo handler với socket null để test logic thuần túy
        handler = new ClientHandler(null);
    }

    @Test
    void testBidLowerThanCurrentPrice() {
        // Đảm bảo handleAuctionLogic trong ClientHandler là PUBLIC
        assertThrows(InvalidBidException.class, () -> {
            handler.handleAuctionLogic("500");
        });
    }

    @Test
    void testLoginWithEmptyName() {
        // Đảm bảo handleLoginLogic trong ClientHandler là PUBLIC
        assertThrows(AuthenticationException.class, () -> {
            handler.handleLoginLogic("");
        });
    }
}