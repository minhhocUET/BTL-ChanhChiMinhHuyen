package com.uet.bidding;

import com.uet.bidding.dao.AuctionSqlDAO;
import com.uet.bidding.exception.AuctionClosedException;
import com.uet.bidding.model.Auction;
import com.uet.bidding.service.AuctionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AuctionLogicTest {
  private AuctionManager manager;
  private AuctionSqlDAO mockDao;

  @BeforeEach
  void setUp() {
    mockDao = mock(AuctionSqlDAO.class);
    when(mockDao.getAllAuctions()).thenReturn(new ArrayList<>());

    manager = AuctionManager.getInstance();
    manager.reset();
    manager.initialize(mockDao);
  }

  @Test
  void testBidOnClosedAuction() {
    int auctionId = 5;
    Auction auction = new Auction(105, new BigDecimal("1000"),
        LocalDateTime.now().minusHours(5), LocalDateTime.now().minusHours(1));
    auction.setId(auctionId);
    auction.setStatus("FINISHED"); // Giả lập trạng thái đã kết thúc
    manager.addAuction(auction);

    // Theo code của bạn: status != "RUNNING" thì văng AuctionClosedException
    assertThrows(AuctionClosedException.class, () -> {
      manager.placeBid(auctionId, "UserLate", new BigDecimal("2000"));
    });
  }

  @Test
  void testAuctionDataIntegrity() {
    int auctionId = 8;
    Auction auction = new Auction(108, new BigDecimal("5000"),
        LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1));
    auction.setId(auctionId);
    auction.setStatus("RUNNING");
    manager.addAuction(auction);

    Auction savedAuction = manager.getAuction(auctionId);
    assertNotNull(savedAuction);
    assertEquals(auctionId, savedAuction.getId());
    assertEquals(0, new BigDecimal("5000").compareTo(savedAuction.getCurrentPrice()));
    assertTrue(savedAuction.isActive()); // isActive trả về true nếu là OPEN hoặc RUNNING
  }
}