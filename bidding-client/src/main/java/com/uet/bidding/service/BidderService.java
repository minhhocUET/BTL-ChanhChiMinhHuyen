package com.uet.bidding.service;

import com.uet.bidding.model.Customer;
import com.uet.bidding.network.ClientService;
import com.uet.bidding.util.UserSession;
import java.math.BigDecimal;

public class BidderService {
    private ClientService clientService = ClientService.getInstance();

    public void placeBid(int auctionId, BigDecimal amount) {
        Customer current = UserSession.getLoggedInCustomer();

        // Kiểm tra nhanh tại Client để tránh gửi rác lên Server
        if (current == null) return;
        if (!current.isProfileComplete()) {
            System.err.println("Chưa xong profile!"); // Bạn có thể gọi Alert ở đây
            return;
        }

        // Gửi yêu cầu đặt giá
        // Bạn có thể gửi một mảng Object hoặc tạo một class BidRequest
        clientService.sendRequest("PLACE_BID", new Object[]{auctionId, amount});
    }
}