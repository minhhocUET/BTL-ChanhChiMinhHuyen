package com.uet.bidding.service;

import com.uet.bidding.network.ClientService;

public class AuctionService {
    private ClientService clientService = ClientService.getInstance();

    // Lấy toàn bộ danh sách
    public void fetchAllAuctions() {
        clientService.sendRequest("GET_ALL_AUCTIONS", "");
    }

    // Lọc theo thành phố (Logic bạn cần ở TableView)
    public void fetchAuctionsByCity(String city) {
        clientService.sendRequest("GET_BY_CITY", city);
    }
}