package com.uet.bidding;

import com.uet.bidding.dao.AuctionSqlDAO;
import com.uet.bidding.exception.InvalidBidException;
import com.uet.bidding.exception.AuctionClosedException;
import com.uet.bidding.model.Auction;
import com.uet.bidding.model.AuctionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AuctionManagerTest {
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
  void testPlaceBidSuccessfully() throws Exception {
    int auctionId = 1;
    // QUAN TRỌNG: Phải để status là "RUNNING" thì hàm placeBid mới không văng Exception
    Auction auction = new Auction(101, new BigDecimal("1000"),
        LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(2));
    auction.setId(auctionId);
    auction.setStatus("RUNNING");
    manager.addAuction(auction);

    boolean result = manager.placeBid(auctionId, "UserA", new BigDecimal("1500"));

    assertTrue(result);
    assertEquals(0, new BigDecimal("1500").compareTo(manager.getAuction(auctionId).getCurrentPrice()));
    verify(mockDao, atLeastOnce()).updateAuction(auction);
  }

  @Test
  void testPlaceBidWhenStatusNotRunning() {
    int auctionId = 2;
    // Trạng thái "OPEN" nhưng chưa "RUNNING" -> class Auction sẽ văng AuctionClosedException
    Auction auction = new Auction(102, new BigDecimal("1000"),
        LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(2));
    auction.setId(auctionId);
    auction.setStatus("OPEN");
    manager.addAuction(auction);

    assertThrows(AuctionClosedException.class, () -> {
      manager.placeBid(auctionId, "UserB", new BigDecimal("1500"));
    });
  }

  @Test
  void testConcurrencySafety() throws InterruptedException {
    int auctionId = 3;
    Auction auction = new Auction(103, new BigDecimal("1000"),
        LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(2));
    auction.setId(auctionId);
    auction.setStatus("RUNNING");
    manager.addAuction(auction);

    int threadCount = 20;
    ExecutorService service = Executors.newFixedThreadPool(threadCount);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch endLatch = new CountDownLatch(threadCount);

    for (int i = 0; i < threadCount; i++) {
      final BigDecimal bidPrice = new BigDecimal(1100 + i);
      service.submit(() -> {
        try {
          startLatch.await();
          manager.placeBid(auctionId, "User", bidPrice);
        } catch (Exception ignored) {
        } finally {
          endLatch.countDown();
        }
      });
    }

    startLatch.countDown();
    endLatch.await(5, TimeUnit.SECONDS);
    service.shutdown();

    BigDecimal finalPrice = manager.getAuction(auctionId).getCurrentPrice();
    assertEquals(0, new BigDecimal("1119").compareTo(finalPrice));
  }
}