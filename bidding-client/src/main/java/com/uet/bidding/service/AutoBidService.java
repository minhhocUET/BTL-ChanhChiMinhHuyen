package com.uet.bidding.service;

import com.uet.bidding.model.Customer;
import com.uet.bidding.model.NetworkMessage;
import com.uet.bidding.network.ClientService;
import com.uet.bidding.util.UserSession;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

public class AutoBidService {

    private final ClientService client = ClientService.getInstance();

    public CompletableFuture<NetworkMessage> enable(int auctionId, BigDecimal maxBid) {
        Customer c = UserSession.getLoggedInCustomer();
        if (c == null) {
            return CompletableFuture.failedFuture(new RuntimeException("Đăng nhập trước!"));
        }
        return client.sendRequest("SET_AUTO_BID", auctionId + " " + maxBid.toPlainString());
    }

    public CompletableFuture<NetworkMessage> disable(int auctionId) {
        return client.sendRequest("REMOVE_AUTO_BID", String.valueOf(auctionId));
    }
}