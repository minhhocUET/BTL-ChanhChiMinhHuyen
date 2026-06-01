package com.uet.bidding.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProxyBidResolverTest {

  @Test
  void twoProxyBiddersSecondPrice() {
    List<ProxyBidResolver.Participant> list = List.of(
        new ProxyBidResolver.Participant(1, new BigDecimal("100")),
        new ProxyBidResolver.Participant(2, new BigDecimal("80"))
    );
    ProxyBidResolver.Outcome o = ProxyBidResolver.resolve(
        new BigDecimal("50"),
        new BigDecimal("10"),
        -1,
        list,
        id -> new BigDecimal("1000000"));

    assertNotNull(o);
    assertEquals(1, o.winnerId);
    assertEquals(0, new BigDecimal("90").compareTo(o.finalPrice));
  }

  @Test
  void leaderAlreadyWinningNoChange() {
    List<ProxyBidResolver.Participant> list = List.of(
        new ProxyBidResolver.Participant(1, new BigDecimal("100"))
    );
    ProxyBidResolver.Outcome o = ProxyBidResolver.resolve(
        new BigDecimal("95"),
        new BigDecimal("10"),
        1,
        list,
        id -> new BigDecimal("1000000"));

    assertNull(o);
  }

  @Test
  void insufficientBalanceExcluded() {
    List<ProxyBidResolver.Participant> list = List.of(
        new ProxyBidResolver.Participant(1, new BigDecimal("100"))
    );
    ProxyBidResolver.Outcome o = ProxyBidResolver.resolve(
        new BigDecimal("50"),
        new BigDecimal("10"),
        -1,
        list,
        id -> new BigDecimal("55"));

    assertNull(o);
  }
}
