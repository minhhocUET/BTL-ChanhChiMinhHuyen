package com.uet.bidding.exception;

import java.io.Serializable;

public class AuthenticationException extends Exception implements Serializable {
  // ID định danh phiên bản để Client và Server luôn hiểu nhau
  private static final long serialVersionUID = 1L;

  public AuthenticationException(String message) {
    super(message);
  }
}
