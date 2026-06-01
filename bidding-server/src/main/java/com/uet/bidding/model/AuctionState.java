package com.uet.bidding.model;

public enum AuctionState {

  RUNNING,    // Đang trong thời gian đấu giá
  FINISHED,   // Đã kết thúc thời gian
  PAID,       // Đã thanh toán
  CANCELED    // Bị hủy
}
