package com.uet.bidding.service;

import com.uet.bidding.model.Customer;
import com.uet.bidding.model.NetworkMessage;
import com.uet.bidding.network.ClientService;
import com.uet.bidding.util.UserSession;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

public class BidderService {

  private final ClientService clientService = ClientService.getInstance();

  public CompletableFuture<NetworkMessage> placeBid(int auctionId, BigDecimal amount) {
    Customer current = UserSession.getLoggedInCustomer();
    if (current == null) {
      return CompletableFuture.failedFuture(
              new RuntimeException("Vui lòng đăng nhập!"));
    }
    if (!current.isProfileComplete()) {
      return CompletableFuture.failedFuture(
              new RuntimeException("Hoàn thiện hồ sơ trước khi đặt giá!"));
    }
    // Server đọc: "auctionId amount"
    String data = auctionId + " " + amount.toPlainString();
    return clientService.sendRequest("BID", data);
  }

  public CompletableFuture<NetworkMessage> registerForAuction(int auctionId) {
    Customer current = UserSession.getLoggedInCustomer();
    if (current == null) {
      return CompletableFuture.failedFuture(new RuntimeException("Vui lòng đăng nhập!"));
    }
    if (!current.isProfileComplete()) {
      return CompletableFuture.failedFuture(
          new RuntimeException("Hoàn thiện hồ sơ trước khi đăng ký!"));
    }
    return clientService.sendRequest("REGISTER_FOR_AUCTION", auctionId);
  }

  public CompletableFuture<NetworkMessage> checkRegistration(int auctionId) {
    return clientService.sendRequest("IS_REGISTERED_FOR_AUCTION", auctionId);
  }

  public CompletableFuture<NetworkMessage> loadMyRegistrations() {
    Customer current = UserSession.getLoggedInCustomer();
    if (current == null) {
      return CompletableFuture.failedFuture(new RuntimeException("Vui lòng đăng nhập!"));
    }
    return clientService.sendRequest("GET_MY_REGISTRATIONS", current.getId());
  }
}