package com.uet.bidding.util;

import com.uet.bidding.model.Auction;

/** Holds the auction selected from seller dashboard for detail view. */
public final class SellerAuctionContext {
  private static Auction selectedAuction;

  private SellerAuctionContext() {}

  public static void set(Auction auction) {
    selectedAuction = auction;
  }

  public static Auction get() {
    return selectedAuction;
  }

  public static void clear() {selectedAuction = null;}
}
