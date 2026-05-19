package com.uet.bidding.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.uet.bidding.dao.AuctionSqlDAO;
import com.uet.bidding.dao.BidSqlDAO;
import com.uet.bidding.dao.ReviewSqlDAO;
import com.uet.bidding.dao.UserSqlDAO;
import com.uet.bidding.exception.AuthenticationException;
import com.uet.bidding.exception.InvalidBidException;
import com.uet.bidding.exception.UserException;
import com.uet.bidding.model.*;
import com.uet.bidding.service.AuctionManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class RequestProcessor {
  private final Gson gson = GsonFactory.getInstance();
  private final UserSqlDAO userSqlDAO;
  private final AuctionSqlDAO auctionSqlDAO;

  public RequestProcessor(UserSqlDAO userSqlDAO, AuctionSqlDAO auctionSqlDAO) {
    this.userSqlDAO = userSqlDAO;
    this.auctionSqlDAO = auctionSqlDAO;
  }

  public void processRequest(NetworkMessage msg, ClientHandler handler) {
    String reqId = msg.getRequestId();

    try {
      switch (msg.getType()) {
        case "LOGIN" -> handleLogin(msg, handler);
        case "REGISTER" -> handleRegisterRequest(msg, handler);
        case "UPDATE_PROFILE" -> handleUpdateProfile(msg, handler);
        case "ADD_BALANCE" -> handleAddBalance(msg, handler);
        case "BID" -> handleBid(msg, handler);
        case "GET_BID_HISTORY" -> handleGetBidHistory(msg, handler);
        case "GET_ALL_AUCTIONS" ->
                handler.sendResponse("SUCCESS", auctionSqlDAO.getAllAuctions(), reqId);
        case "CREATE_AUCTION" -> handleCreateAuction(msg, handler);
        case "SET_AUTO_BID" -> handleSetAutoBid(msg, handler);
        case "REMOVE_AUTO_BID" -> handleRemoveAutoBid(msg, handler);
        case "ADD_REVIEW" -> handleAddReview(msg, handler);
        case "GET_REVIEWS_BY_SELLER" -> handleGetReviewsBySeller(msg, handler);
        case "LOGOUT" -> {
          handler.setLoggedInUser(null);
          handler.sendResponse("SUCCESS", "Đã đăng xuất khỏi hệ thống.", reqId);
        }
        default -> handler.sendResponse("ERROR", "Lệnh không hợp lệ hoặc chưa được hỗ trợ!", reqId);
      }
    } catch (Exception e) {
      handler.sendResponse("ERROR", "Lỗi hệ thống: " + e.getMessage(), reqId);
      e.printStackTrace();
    }
  }

  // --- CÁC HÀM XỬ LÝ CHI TIẾT ---

  private void handleLogin(NetworkMessage msg, ClientHandler handler) {
    try {
      String credentials = String.valueOf(msg.getData());
      handleLoginLogic(credentials);
      String[] loginData = credentials.split(" ");
      if (loginData.length < 2) throw new AuthenticationException("Thiếu mật khẩu!");

      User user = userSqlDAO.checkLogin(loginData[0].trim(), loginData[1].trim());
      if (user != null) {
        if (user.isBanned()) throw new AuthenticationException("Tài khoản đã bị khóa!");
        handler.setLoggedInUser(user);
        handler.sendResponse("LOGIN_SUCCESS", user, msg.getRequestId());
      } else {
        throw new AuthenticationException("Sai tên đăng nhập hoặc mật khẩu.");
      }
    } catch (Exception e) {
      handler.sendResponse("ERROR", e.getMessage(), msg.getRequestId());
    }
  }

  private void handleRegisterRequest(NetworkMessage msg, ClientHandler handler) {
    try {
      handleRegister(String.valueOf(msg.getData()));
      handler.sendResponse("REGISTER_SUCCESS", "Đăng ký thành công!", msg.getRequestId());
    } catch (Exception e) {
      handler.sendResponse("ERROR", e.getMessage(), msg.getRequestId());
    }
  }

  private void handleUpdateProfile(NetworkMessage msg, ClientHandler handler) {
    try {
      String userJson = gson.toJson(msg.getData());
      JsonObject jsonObject = gson.fromJson(userJson, JsonObject.class);

      if (jsonObject.has("role") && "ADMIN".equals(jsonObject.get("role").getAsString())) {
        handler.sendResponse("ERROR", "Không thể cập nhật hồ sơ của Admin.", msg.getRequestId());
        return;
      }

      Customer customer = gson.fromJson(userJson, Customer.class);
      userSqlDAO.updateProfile(customer);
      handler.setLoggedInUser(customer);
      handler.sendResponse("UPDATE_PROFILE_SUCCESS", customer, msg.getRequestId());
    } catch (Exception e) {
      handler.sendResponse("ERROR", "Cập nhật thất bại: " + e.getMessage(), msg.getRequestId());
    }
  }

  private void handleAddBalance(NetworkMessage msg, ClientHandler handler) throws Exception {
    if (handler.getLoggedInUser() == null) {
      handler.sendResponse("ERROR", "Vui lòng đăng nhập!", msg.getRequestId());
      return;
    }

    if (handler.getLoggedInUser() instanceof Customer loggedInCustomer) {
      BigDecimal amountToAdd = new BigDecimal(String.valueOf(msg.getData()));
      if (amountToAdd.compareTo(BigDecimal.ZERO) <= 0) {
        handler.sendResponse("ERROR", "Số tiền phải lớn hơn 0!", msg.getRequestId());
        return;
      }

      userSqlDAO.updateBalance(loggedInCustomer.getId(), amountToAdd);
      loggedInCustomer.addFunds(amountToAdd);
      handler.sendResponse("UPDATE_BALANCE_SUCCESS", loggedInCustomer, msg.getRequestId());
    } else {
      handler.sendResponse("ERROR", "Admin không có chức năng số dư!", msg.getRequestId());
    }
  }

  private void handleBid(NetworkMessage msg, ClientHandler handler) {
    try {
      if (!(handler.getLoggedInUser() instanceof Customer bidder)) {
        throw new InvalidBidException("Chỉ khách hàng mới được đặt giá!");
      }

      String[] bidParts = String.valueOf(msg.getData()).split(" ");
      if (bidParts.length < 2) throw new InvalidBidException("Sai cú pháp! Gửi: auctionId sốTiền");

      int auctionId = Integer.parseInt(bidParts[0]);
      BigDecimal bidAmount = new BigDecimal(bidParts[1]);

      LocalDateTime endBefore = auctionSqlDAO.findById(auctionId).getEndTime();

      boolean success = AuctionManager.getInstance().placeBid(auctionId, bidder, bidAmount);
      if (success) {
        Auction updated = auctionSqlDAO.findById(auctionId);

        boolean extended = updated.getEndTime().isAfter(endBefore);
        if (extended) {
          Server.broadcast(new NetworkMessage("BROADCAST",
                  "⏱ Phiên #" + auctionId + " được gia hạn thêm "
                          + updated.getAntiSnipeExtensionMinutes() + " phút (anti-sniping)!"));
        }

        Server.broadcast(new NetworkMessage("BROADCAST",
                "Người dùng [" + bidder.getUsername() + "] đặt giá "
                        + bidAmount + " cho phiên #" + auctionId));

        Server.broadcast(new NetworkMessage("AUCTION_UPDATED", updated));
        handler.sendResponse("SUCCESS", "Đặt giá thành công!", msg.getRequestId());
      }
    } catch (Exception e) {
      handler.sendResponse("ERROR", e.getMessage(), msg.getRequestId());
    }
  }

  private void handleCreateAuction(NetworkMessage msg, ClientHandler handler) {
    // Logic tạo đấu giá của bạn
    // ...
    Server.broadcast(new NetworkMessage("NEW_AUCTION_ADDED", "Sản phẩm mới vừa lên sàn!"));
    handler.sendResponse("SUCCESS", "Phiên đấu giá đã được kích hoạt!", msg.getRequestId());
  }
  private void handleGetBidHistory(NetworkMessage msg, ClientHandler handler) {
    try {
      int auctionId = Integer.parseInt(String.valueOf(msg.getData()));
      List<Bid> bids = new BidSqlDAO().getBidsByAuction(auctionId);
      handler.sendResponse("SUCCESS", bids, msg.getRequestId());
    } catch (Exception e) {
      handler.sendResponse("ERROR", e.getMessage(), msg.getRequestId());
    }
  }

  private void handleSetAutoBid(NetworkMessage msg, ClientHandler handler) {
    try {
      if (!(handler.getLoggedInUser() instanceof Customer customer)) {
        throw new UserException("Phải đăng nhập bằng tài khoản khách hàng!");
      }
      String[] parts = String.valueOf(msg.getData()).trim().split("\\s+");
      if (parts.length < 2) throw new UserException("Sai cú pháp! Gửi: auctionId maxBid");

      int auctionId = Integer.parseInt(parts[0]);
      BigDecimal maxBid = new BigDecimal(parts[1]);

      auctionSqlDAO.setAutoBid(auctionId, customer.getId(), maxBid);
      handler.sendResponse("SUCCESS", "Đã bật đấu giá tự động!", msg.getRequestId());
    } catch (Exception e) {
      handler.sendResponse("ERROR", e.getMessage(), msg.getRequestId());
    }
  }

  private void handleRemoveAutoBid(NetworkMessage msg, ClientHandler handler) {
    try {
      if (!(handler.getLoggedInUser() instanceof Customer customer)) {
        throw new UserException("Phải đăng nhập!");
      }
      int auctionId = Integer.parseInt(String.valueOf(msg.getData()).trim());
      auctionSqlDAO.removeAutoBid(auctionId, customer.getId());
      handler.sendResponse("SUCCESS", "Đã tắt đấu giá tự động!", msg.getRequestId());
    } catch (Exception e) {
      handler.sendResponse("ERROR", e.getMessage(), msg.getRequestId());
    }
  }

  private void handleAddReview(NetworkMessage msg, ClientHandler handler) {
    try {
      if (!(handler.getLoggedInUser() instanceof Customer customer)) {
        throw new UserException("Phải đăng nhập!");
      }

      JsonObject obj = gson.fromJson(gson.toJson(msg.getData()), JsonObject.class);
      int auctionId = obj.get("auctionId").getAsInt();
      int sellerId = obj.get("sellerId").getAsInt();
      int stars = obj.get("stars").getAsInt();
      String comment = obj.get("comment").getAsString();

      if (stars < 1 || stars > 5) throw new UserException("Số sao phải từ 1 đến 5!");

      Auction auction = auctionSqlDAO.findById(auctionId);
      if (!"FINISHED".equals(auction.getStatus())) {
        throw new UserException("Chỉ được đánh giá sau khi phiên đã kết thúc!");
      }
      if (auction.getHighestBidder() == null
              || auction.getHighestBidder().getId() != customer.getId()) {
        throw new UserException("Chỉ người thắng đấu giá mới được đánh giá!");
      }

      new ReviewSqlDAO().addReview(auctionId, sellerId, customer.getId(), stars, comment);
      handler.sendResponse("SUCCESS", "Cảm ơn bạn đã đánh giá!", msg.getRequestId());
    } catch (Exception e) {
      handler.sendResponse("ERROR", e.getMessage(), msg.getRequestId());
    }
  }

  private void handleGetReviewsBySeller(NetworkMessage msg, ClientHandler handler) {
    try {
      int sellerId = Integer.parseInt(String.valueOf(msg.getData()).trim());
      List<Review> reviews = new ReviewSqlDAO().getReviewsBySeller(sellerId);
      handler.sendResponse("SUCCESS", reviews, msg.getRequestId());
    } catch (Exception e) {
      handler.sendResponse("ERROR", e.getMessage(), msg.getRequestId());
    }
  }

  // --- HÀM HELPER LOGIC ---

  private void handleRegister(String regData) throws UserException {
    String[] regParts = regData.split(" ");
    if (regParts.length < 2) throw new UserException("Vui lòng nhập đầy đủ thông tin đăng ký!");

    String plainPassword = regParts[1].trim();
    // Băm mật khẩu BCrypt
    String hashedPassword = org.mindrot.jbcrypt.BCrypt.hashpw(plainPassword, org.mindrot.jbcrypt.BCrypt.gensalt(12));

    Customer newCustomer = new Customer(regParts[0].trim(), hashedPassword, BigDecimal.ZERO);
    userSqlDAO.addUser(newCustomer);
  }

  private void handleLoginLogic(String credentials) throws AuthenticationException {
    if (credentials == null || credentials.trim().isEmpty()) throw new AuthenticationException("Dữ liệu trống!");
    if (credentials.toLowerCase().contains("root"))
      throw new AuthenticationException("Tài khoản root không được phép truy cập từ Client!");
  }
}