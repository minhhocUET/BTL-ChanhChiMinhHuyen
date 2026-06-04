package com.uet.bidding.util;

import com.uet.bidding.model.Item;

/** Holds the inventory item selected for "create auction" navigation. */
public final class CreateAuctionContext {
  private static Item selectedItem;

  private CreateAuctionContext() {}

  public static void set(Item item) {
    selectedItem = item;
  }

  public static Item get() {
    return selectedItem;
  }

  public static void clear() {
    selectedItem = null;
  }
}
