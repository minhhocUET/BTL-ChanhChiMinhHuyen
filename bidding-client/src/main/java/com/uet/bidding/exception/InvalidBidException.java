package com.uet.bidding.exception;

import java.io.Serializable;

// Kế thừa Exception để tạo lỗi của riêng mình
public class InvalidBidException extends Exception implements Serializable {

  // ID định danh để đảm bảo tính đồng bộ dữ liệu
  private static final long serialVersionUID = 1L;

  public InvalidBidException(String message) {
    super(message);
  }
}