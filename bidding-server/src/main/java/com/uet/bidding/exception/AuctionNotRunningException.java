package com.uet.bidding.exception;

public class AuctionNotRunningException extends Exception {
    public AuctionNotRunningException(String message) {
        super(message);
    }
}