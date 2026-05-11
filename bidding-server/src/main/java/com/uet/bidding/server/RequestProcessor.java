package com.uet.bidding.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.uet.bidding.dao.*;
import com.uet.bidding.exception.*;
import com.uet.bidding.model.*;
import com.uet.bidding.service.AuctionManager;
import com.uet.bidding.service.UserService;
import java.math.BigDecimal;


public class RequestProcessor {
    private final Gson gson = new Gson();
    private final UserSqlDAO userSqlDAO;
    private final AuctionSqlDAO auctionSqlDAO;
    private final UserService serverUserService = new UserService();

    public RequestProcessor(UserSqlDAO userSqlDAO, AuctionSqlDAO auctionSqlDAO) {
        this.userSqlDAO = userSqlDAO;
        this.auctionSqlDAO = auctionSqlDAO;
    }

    public void processRequest(NetworkMessage msg, ClientHandler handler) throws Exception {
        switch (msg.getType()) {
            case "LOGIN":
                String credentials = String.valueOf(msg.getData());
                handleLoginLogic(credentials); // Logic check root/trống của bạn
                String[] loginData = credentials.split(" ");
                if (loginData.length < 2) throw new AuthenticationException("Thiếu mật khẩu!");

                User user = userSqlDAO.checkLogin(loginData[0], loginData[1]);
                if (user != null) {
                    handler.setLoggedInUser(user);
                    handler.sendResponse("LOGIN_SUCCESS", user);
                } else {
                    throw new AuthenticationException("Sai tên đăng nhập hoặc mật khẩu.");
                }
                break;

            case "UPDATE_PROFILE":
                String userJson = gson.toJson(msg.getData());
                JsonObject jsonObject = gson.fromJson(userJson, JsonObject.class);

                User updatedUser;
                // Logic đa hình: Chỉ phân biệt Admin và Customer (chứa Seller/Bidder bên trong)
                if (jsonObject.has("role") && "ADMIN".equals(jsonObject.get("role").getAsString())) {
                    updatedUser = gson.fromJson(userJson, Admin.class);
                } else {
                    // Mọi trường hợp khác đều là Customer (vì Seller/Bidder chỉ là thuộc tính)
                    updatedUser = gson.fromJson(userJson, Customer.class);
                }

                // Nếu là Customer, gọi service để validate 5 bước (fullName, email, phone...)
                if (updatedUser instanceof Customer customer) {
                    serverUserService.updateUser(customer);
                }

                handler.setLoggedInUser(updatedUser);
                handler.sendResponse("SUCCESS", "Cập nhật hồ sơ thành công!");
                break;

            case "ADD_BALANCE":
                if (handler.getLoggedInUser() == null) throw new AuthenticationException("Phải đăng nhập!");

                // Chỉ Customer mới có thuộc tính Balance
                if (handler.getLoggedInUser() instanceof Customer customer) {
                    BigDecimal amountToAdd = new BigDecimal(String.valueOf(msg.getData()));
                    if (amountToAdd.compareTo(BigDecimal.ZERO) <= 0) throw new InvalidBidException("Phải > 0!");

                    // 1. Cập nhật DB
                    userSqlDAO.updateBalance(customer.getUsername(), amountToAdd);

                    // 2. Cập nhật RAM (Customer mới có hàm setBalance và getBalance)
                    BigDecimal newBalance = customer.getBalance().add(amountToAdd);
                    customer.setBalance(newBalance);

                    handler.sendResponse("UPDATE_BALANCE_SUCCESS", customer);
                } else {
                    throw new Exception("Tài khoản Admin không có chức năng số dư!");
                }
                break;

            case "BID":
                if (handler.getLoggedInUser() == null) throw new AuthenticationException("Phải đăng nhập!");
                String[] bidParts = String.valueOf(msg.getData()).split(" ");
                if (bidParts.length < 2) throw new InvalidBidException("Sai cú pháp!");

                int auctionId = Integer.parseInt(bidParts[0]);
                handleAuctionLogic(bidParts[1]); // Logic check giá > 0
                BigDecimal bidAmount = new BigDecimal(bidParts[1]);

                // Đặt giá dùng username từ User (Abstract)
                boolean success = AuctionManager.getInstance().placeBid(auctionId, handler.getLoggedInUser().getUsername(), bidAmount);
                if (success) {
                    Server.broadcast(new NetworkMessage("BROADCAST",
                            "Người dùng [" + handler.getLoggedInUser().getUsername() + "] đã đặt giá " + bidAmount));
                    handler.sendResponse("SUCCESS", "Đặt giá thành công!");
                }
                break;

            case "REGISTER":
                handleRegister(String.valueOf(msg.getData()));
                handler.sendResponse("SUCCESS", "Đăng ký thành công!");
                break;

            case "GET_ALL_AUCTIONS":
                handler.sendResponse("SUCCESS", auctionSqlDAO.getAllAuctions());
                break;

            default:
                handler.sendResponse("ERROR", "Lệnh không hợp lệ!");
                break;
        }
    }

    // --- Giữ nguyên logic khởi tạo của bạn, ép về Customer ---
    private void handleRegister(String regData) throws UserException {
        String[] regParts = regData.split(" ");
        if (regParts.length < 2) throw new UserException("Sai cú pháp!");

        Customer newCustomer = new Customer();
        newCustomer.setUsername(regParts[0]);
        newCustomer.setPassword(regParts[1]);
        newCustomer.setBalance(BigDecimal.ZERO);
        // Ở đây mặc định mới đăng ký thì chưa là Seller/Bidder active (tùy logic thuộc tính của bạn)

        userSqlDAO.addUser(newCustomer);
    }

    private void handleLoginLogic(String credentials) throws AuthenticationException {
        if (credentials == null || credentials.trim().isEmpty()) throw new AuthenticationException("Trống!");
        if (credentials.toLowerCase().contains("root")) throw new AuthenticationException("root bị khóa!");
    }

    private void handleAuctionLogic(String amountStr) throws InvalidBidException {
        if (new BigDecimal(amountStr).compareTo(BigDecimal.ZERO) <= 0) throw new InvalidBidException("Giá > 0!");
    }
}