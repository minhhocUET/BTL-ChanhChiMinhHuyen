package com.uet.bidding.exception;

import java.io.Serializable;

public class AuctionClosedException extends Exception implements Serializable {
  // Thêm ID để đảm bảo Client nhận diện đúng loại lỗi từ Server gửi về
  private static final long serialVersionUID = 1L;

  public AuctionClosedException(String message) {
    super(message);
  }
}