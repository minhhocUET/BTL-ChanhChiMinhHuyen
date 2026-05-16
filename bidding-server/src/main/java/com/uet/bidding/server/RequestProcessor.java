package com.uet.bidding.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.uet.bidding.dao.AuctionSqlDAO;
import com.uet.bidding.dao.UserSqlDAO;
import com.uet.bidding.exception.AuthenticationException;
import com.uet.bidding.exception.InvalidBidException;
import com.uet.bidding.exception.UserException;
import com.uet.bidding.model.Customer;
import com.uet.bidding.model.GsonFactory;
import com.uet.bidding.model.NetworkMessage;
import com.uet.bidding.model.User;
import com.uet.bidding.service.AuctionManager;

import java.math.BigDecimal;

public class RequestProcessor {
  private final Gson gson = GsonFactory.getInstance();
  private final UserSqlDAO userSqlDAO;
  private final AuctionSqlDAO auctionSqlDAO;

  public RequestProcessor(UserSqlDAO userSqlDAO, AuctionSqlDAO auctionSqlDAO) {
    this.userSqlDAO = userSqlDAO;
    this.auctionSqlDAO = auctionSqlDAO;
  }

  public void processRequest(NetworkMessage msg, ClientHandler handler) {
    String reqId = msg.getRequestId(); // Lấy ID yêu cầu để trả lời chính xác

    try {
      switch (msg.getType()) {
        case "LOGIN":
          handleLogin(msg, handler);
          break;

        case "REGISTER":
          handleRegisterRequest(msg, handler);
          break;

        case "UPDATE_PROFILE":
          handleUpdateProfile(msg, handler);
          break;

        case "ADD_BALANCE":
          handleAddBalance(msg, handler);
          break;

        case "BID":
          handleBid(msg, handler);
          break;

        case "GET_ALL_AUCTIONS":
          handler.sendResponse("SUCCESS", auctionSqlDAO.getAllAuctions(), reqId);
          break;

        case "CREATE_AUCTION":
          handleCreateAuction(msg, handler);
          break;

        case "LOGOUT":
          handler.setLoggedInUser(null);
          handler.sendResponse("SUCCESS", "Đã đăng xuất khỏi hệ thống.", reqId);
          break;

        default:
          handler.sendResponse("ERROR", "Lệnh không hợp lệ hoặc chưa được hỗ trợ!", reqId);
          break;
      }
    } catch (Exception e) {
      // Đảm bảo mọi lỗi phát sinh không mong muốn đều được gửi về Client kèm ID
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
      if (!(handler.getLoggedInUser() instanceof Customer)) {
        throw new InvalidBidException("Chỉ khách hàng mới được đặt giá!");
      }
      Customer bidder = (Customer) handler.getLoggedInUser();

      String[] bidParts = String.valueOf(msg.getData()).split(" ");
      if (bidParts.length < 2) throw new InvalidBidException("Sai cú pháp đặt giá!");

      int auctionId = Integer.parseInt(bidParts[0]);
      BigDecimal bidAmount = new BigDecimal(bidParts[1]);

      boolean success = AuctionManager.getInstance().placeBid(auctionId, bidder, bidAmount);
      if (success) {
        Server.broadcast(new NetworkMessage("BROADCAST",
            "Người dùng [" + bidder.getUsername() + "] đã đặt giá " + bidAmount + " cho mã " + auctionId));
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