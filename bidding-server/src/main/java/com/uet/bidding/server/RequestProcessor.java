package com.uet.bidding.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.uet.bidding.dao.AuctionSqlDAO;
import com.uet.bidding.dao.ItemSqlDAO;
import com.uet.bidding.dao.UserSqlDAO;
import com.uet.bidding.exception.AuthenticationException;
import com.uet.bidding.exception.InvalidBidException;
import com.uet.bidding.exception.UserException;
import com.uet.bidding.model.*;
import com.uet.bidding.service.AuctionManager;

import java.math.BigDecimal;
import java.util.List;

public class RequestProcessor {
  private final Gson gson = GsonFactory.create();
  private final UserSqlDAO userSqlDAO;
  private final AuctionSqlDAO auctionSqlDAO;
  //private final ItemFileDAO itemSqlDAO;

  public RequestProcessor(UserSqlDAO userSqlDAO, AuctionSqlDAO auctionSqlDAO) {
    this.userSqlDAO = userSqlDAO;
    this.auctionSqlDAO = auctionSqlDAO;
  }

  public void processRequest(NetworkMessage msg, ClientHandler handler) throws Exception {
    switch (msg.getType()) {
      case "LOGIN":
        String credentials = String.valueOf(msg.getData());
        handleLoginLogic(credentials);
        String[] loginData = credentials.split(" ");
        if (loginData.length < 2) throw new AuthenticationException("Thiếu mật khẩu!");

        User user = userSqlDAO.checkLogin(loginData[0].trim(), loginData[1].trim());
        if (user != null) {
          if (user.isBanned()) throw new AuthenticationException("Tài khoản đã bị khóa!");
          handler.setLoggedInUser(user);
          handler.sendResponse("LOGIN_SUCCESS", user);
        } else {
          throw new AuthenticationException("Sai tên đăng nhập hoặc mật khẩu.");
        }
        break;

      case "UPDATE_PROFILE":
        String userJson = gson.toJson(msg.getData());
        JsonObject jsonObject = gson.fromJson(userJson, JsonObject.class);

        // Nếu là ADMIN → từ chối
        if (jsonObject.has("role") && "ADMIN".equals(jsonObject.get("role").getAsString())) {
          handler.sendResponse("UPDATE_PROFILE_ERROR", "Không thể cập nhật hồ sơ của Admin.");
          break;
        }

        // Chỉ xử lý Customer
        Customer customer = gson.fromJson(userJson, Customer.class);
        try {
          userSqlDAO.updateProfile(customer);
          handler.setLoggedInUser(customer);
          handler.sendResponse("UPDATE_PROFILE_SUCCESS", customer);
        } catch (UserException e) {
          handler.sendResponse("UPDATE_PROFILE_ERROR", e.getMessage());
        }
        break;

      case "ADD_BALANCE":
        if (handler.getLoggedInUser() == null) throw new AuthenticationException("Phải đăng nhập!");

        // Đổi tên 'customer' thành 'loggedInCustomer'
        if (handler.getLoggedInUser() instanceof Customer loggedInCustomer) {
          BigDecimal amountToAdd = new BigDecimal(String.valueOf(msg.getData()));
          if (amountToAdd.compareTo(BigDecimal.ZERO) <= 0) throw new InvalidBidException("Phải > 0!");

          userSqlDAO.updateBalance(loggedInCustomer.getId(), amountToAdd);
          loggedInCustomer.addFunds(amountToAdd);

          handler.sendResponse("UPDATE_BALANCE_SUCCESS", loggedInCustomer);
        } else {
          throw new Exception("Tài khoản Admin không có chức năng số dư!");
        }
        break;

      case "BID":
        if (handler.getLoggedInUser() == null) throw new AuthenticationException("Phải đăng nhập!");

        // SỬA LỖI TẠI ĐÂY: Ép kiểu sang Customer để truyền vào placeBid
        if (!(handler.getLoggedInUser() instanceof Customer)) {
          throw new InvalidBidException("Chỉ khách hàng mới được đặt giá!");
        }
        Customer bidder = (Customer) handler.getLoggedInUser();

        String[] bidParts = String.valueOf(msg.getData()).split(" ");
        if (bidParts.length < 2) throw new InvalidBidException("Sai cú pháp!");

        int auctionId = Integer.parseInt(bidParts[0]);
        handleAuctionLogic(bidParts[1]);
        BigDecimal bidAmount = new BigDecimal(bidParts[1]);

        // Truyền đối tượng 'bidder' vào thay vì 'username'
        boolean success = AuctionManager.getInstance().placeBid(auctionId, bidder, bidAmount);
        if (success) {
          Server.broadcast(new NetworkMessage("BROADCAST",
              "Người dùng [" + bidder.getUsername() + "] đã đặt giá " + bidAmount));
          handler.sendResponse("SUCCESS", "Đặt giá thành công!");
        }
        break;

      case "REGISTER":
        handleRegister(String.valueOf(msg.getData()));
        handler.sendResponse("REGISTER_SUCCESS", "Đăng ký thành công!");
        break;

      case "GET_ALL_AUCTIONS":
        handler.sendResponse("SUCCESS", auctionSqlDAO.getAllAuctions());
        break;

      case "CREATE_AUCTION":
        if (handler.getLoggedInUser() == null) throw new AuthenticationException("Phải đăng nhập!");

        String[] auctionParts = String.valueOf(msg.getData()).split(" ");
        int itemId = Integer.parseInt(auctionParts[0]);
        BigDecimal startPrice = new BigDecimal(auctionParts[1]);
        int durationMins = Integer.parseInt(auctionParts[2]);

        // Giả sử bạn có ItemDAO để lấy Item từ ID
        //Item item = itemSqlDAO.getItemById(itemId);
        // Ở đây mình gọi tạm qua AuctionManager để tạo phiên
        // Auction newAuction = AuctionManager.getInstance().createAuction(item, LocalDateTime.now().plusMinutes(durationMins));

        // Sau khi tạo xong, thông báo cho tất cả mọi người có hàng mới
        Server.broadcast(new NetworkMessage("NEW_AUCTION_ADDED", "Sản phẩm mới vừa lên sàn!"));
        handler.sendResponse("SUCCESS", "Phiên đấu giá đã được kích hoạt trên hệ thống!");
        break;

      case "UPDATE_SELLER_RATING":
        // Nhận mảng Object: [storeName, newAverage, comment]
        List<Object> ratingData = (List<Object>) msg.getData();
        String storeName = (String) ratingData.get(0);
        double newRating = (double) ratingData.get(1);
        String comment = (String) ratingData.get(2);

        // Cập nhật vào DB thông qua DAO
        //userSqlDAO.updateSellerRating(storeName, newRating);

        // Lưu log comment vào bảng review (nếu có)
        System.out.println("Cửa hàng " + storeName + " vừa nhận đánh giá: " + newRating + " sao. Nội dung: " + comment);
        handler.sendResponse("SUCCESS", "Cảm ơn bạn đã đánh giá!");
        break;

      case "GET_BY_CITY":
        String city = String.valueOf(msg.getData());
        // Lọc danh sách từ AuctionManager hoặc DAO
        //handler.sendResponse("SUCCESS", auctionSqlDAO.getAuctionsByCity(city));
        break;

      case "LOGOUT":
        handler.setLoggedInUser(null);
        handler.sendResponse("SUCCESS", "Đã đăng xuất khỏi hệ thống.");
        break;

      default:
        handler.sendResponse("ERROR", "Lệnh không hợp lệ!");
        break;
    }
  }

  private void handleRegister(String regData) throws UserException {
    String[] regParts = regData.split(" ");
    if (regParts.length < 2) throw new UserException("Sai cú pháp!");

    // ---> SỬA Ở ĐÂY: Băm mật khẩu bằng BCrypt trước khi tạo User <---
    String plainPassword = regParts[1].trim();
    String hashedPassword = org.mindrot.jbcrypt.BCrypt.hashpw(plainPassword, org.mindrot.jbcrypt.BCrypt.gensalt(12));

    // Truyền hashedPassword thay vì password nguyên bản
    Customer newCustomer = new Customer(regParts[0], hashedPassword, BigDecimal.ZERO);
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