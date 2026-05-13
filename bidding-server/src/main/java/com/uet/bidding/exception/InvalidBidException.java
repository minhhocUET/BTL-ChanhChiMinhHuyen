package com.uet.bidding.exception;

// Kế thừa Exception để tạo lỗi của riêng mình
public class InvalidBidException extends Exception  {

  public InvalidBidException(String message) {
    super(message);
  }
}