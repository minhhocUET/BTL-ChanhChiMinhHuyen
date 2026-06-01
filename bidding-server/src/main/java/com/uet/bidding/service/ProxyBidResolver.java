package com.uet.bidding.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * One-pass proxy (auto) bid resolution — no client/server ping-pong.
 * <p>
 * Second-price style: among bidders sorted by max ceiling descending,
 * winner pays min(winnerCeiling, secondCeiling + increment).
 */
public final class ProxyBidResolver {

  private ProxyBidResolver() {
  }

  public static class Participant {
    public final int bidderId;
    public final BigDecimal maxBid;

    public Participant(int bidderId, BigDecimal maxBid) {
      this.bidderId = bidderId;
      this.maxBid = maxBid;
    }
  }

  public static class Outcome {
    public final int winnerId;
    public final BigDecimal finalPrice;
    /** Loser's last competing bid (for history), or null if none. */
    public final BigDecimal runnerUpBid;
    public final int runnerUpBidderId;

    public Outcome(int winnerId, BigDecimal finalPrice, int runnerUpBidderId, BigDecimal runnerUpBid) {
      this.winnerId = winnerId;
      this.finalPrice = finalPrice;
      this.runnerUpBidderId = runnerUpBidderId;
      this.runnerUpBid = runnerUpBid;
    }

    public boolean changesPrice(BigDecimal currentPrice, int currentLeaderId) {
      return winnerId != currentLeaderId || finalPrice.compareTo(currentPrice) > 0;
    }
  }

  /**
   * @param currentPrice     price before resolution
   * @param increment        minimum bid step
   * @param currentLeaderId  -1 if none
   * @param participants     each bidder's ceiling (auto max or current bid for leader)
   * @param balanceByBidder  latest wallet balance per bidder
   * @return outcome or null if no proxy should change the auction
   */
  public static Outcome resolve(
      BigDecimal currentPrice,
      BigDecimal increment,
      int currentLeaderId,
      List<Participant> participants,
      java.util.function.IntFunction<BigDecimal> balanceByBidder) {

    if (participants == null || participants.isEmpty()) {
      return null;
    }

    BigDecimal minNext = currentPrice.add(increment);
    List<Participant> eligible = new ArrayList<>();
    for (Participant p : participants) {
      if (p.maxBid == null || p.maxBid.compareTo(minNext) < 0) {
        continue;
      }
      BigDecimal balance = balanceByBidder.apply(p.bidderId);
      if (balance == null || balance.compareTo(minNext) < 0) {
        continue;
      }
      eligible.add(p);
    }

    if (eligible.isEmpty()) {
      return null;
    }

    eligible.sort(Comparator
        .comparing((Participant p) -> p.maxBid, Comparator.reverseOrder())
        .thenComparingInt(p -> p.bidderId));

    Participant winner = eligible.get(0);
    if (eligible.size() == 1 && winner.bidderId == currentLeaderId) {
      return null;
    }

    BigDecimal secondCeiling = eligible.size() > 1
        ? eligible.get(1).maxBid
        : (currentLeaderId >= 0 ? ceilingFor(currentLeaderId, participants, currentPrice) : currentPrice);

    BigDecimal finalPrice = winner.maxBid.min(secondCeiling.add(increment));
    if (finalPrice.compareTo(minNext) < 0) {
      finalPrice = minNext.min(winner.maxBid);
    }

    BigDecimal winnerBalance = balanceByBidder.apply(winner.bidderId);
    if (winnerBalance == null || finalPrice.compareTo(winnerBalance) > 0) {
      return null;
    }

    if (winner.bidderId == currentLeaderId && finalPrice.compareTo(currentPrice) <= 0) {
      return null;
    }

    int runnerUpId = -1;
    BigDecimal runnerUpBid = null;
    if (eligible.size() > 1) {
      Participant runner = eligible.get(1);
      runnerUpId = runner.bidderId;
      runnerUpBid = secondCeiling.add(increment).min(runner.maxBid);
      if (runnerUpBid.compareTo(currentPrice) <= 0) {
        runnerUpBid = null;
        runnerUpId = -1;
      }
    }

    return new Outcome(winner.bidderId, finalPrice, runnerUpId, runnerUpBid);
  }

  private static BigDecimal ceilingFor(int bidderId, List<Participant> all, BigDecimal fallback) {
    for (Participant p : all) {
      if (p.bidderId == bidderId) {
        return p.maxBid;
      }
    }
    return fallback;
  }
}
