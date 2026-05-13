package com.uet.bidding.model;

public enum TransactionType {
  DEPOSIT,            // Nạp tiền vào tài khoản
  WITHDRAW,           // Rút tiền
  PAY_FOR_AUCTION,    // Thanh toán cho sản phẩm thắng đấu giá
  RECEIVE_FROM_AUCTION // Nhận tiền từ người thắng đấu giá
}