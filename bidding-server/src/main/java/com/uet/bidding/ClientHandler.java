package com.uet.bidding;

import com.google.gson.Gson;
import com.uet.bidding.dao.AuctionSqlDAO;
import com.uet.bidding.dao.ItemFileDAO;
import com.uet.bidding.dao.UserSqlDAO;
import com.uet.bidding.exception.AuctionClosedException;
import com.uet.bidding.exception.AuthenticationException;
import com.uet.bidding.exception.InvalidBidException;
import com.uet.bidding.exception.UserException;
import com.uet.bidding.model.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ClientHandler implements Runnable, AuctionObserver {
  private final Socket clientSocket;
  private final UserSqlDAO userSqlDAO; // Sử dụng đúng SQL DAO
  private final ItemFileDAO itemFileDAO; // Thêm cái này
  private final AuctionSqlDAO auctionSqlDAO; // Thêm cái này
  private final Gson gson = new Gson();
  private PrintWriter out;
  private BufferedReader in;
  private User loggedInUser = null;

  public ClientHandler(Socket socket, UserSqlDAO userSqlDAO, ItemFileDAO itemFileDAO, AuctionSqlDAO auctionSqlDAO) {
    this.clientSocket = socket;
    this.userSqlDAO = userSqlDAO;
    this.itemFileDAO = itemFileDAO;
    this.auctionSqlDAO = auctionSqlDAO;
  }

  // Hàm giúp Server gọi để gửi tin nhắn cho Client (Dùng trong Broadcast)
  public void sendMessage(NetworkMessage msg) {
    if (out != null) {
      out.println(gson.toJson(msg));
    }
  }

  private void sendResponse(String type, Object data) {
    sendMessage(new NetworkMessage(type, data));
  }


  @Override
  public void run() {
    try {
      out = new PrintWriter(clientSocket.getOutputStream(), true);
      in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

      String inputLine;
      while ((inputLine = in.readLine()) != null) {
        try {
          NetworkMessage msg = gson.fromJson(inputLine, NetworkMessage.class);
          Object responseContent = "";

          switch (msg.getType()) {
            case "REGISTER":
              String regData = String.valueOf(msg.getData());
              handleRegister(regData); // Sửa hàm handleRegister nhận String
              responseContent = "Đăng ký thành công!";
              break;

            case "LOGIN":
              String credentials = String.valueOf(msg.getData());
              handleLoginLogic(credentials); // Check logic cấm (root, trống)

              String[] loginData = credentials.split(" ");
              if (loginData.length < 2) throw new AuthenticationException("Thiếu mật khẩu!");

              User user = userSqlDAO.checkLogin(loginData[0], loginData[1]);
              if (user != null) {
                this.loggedInUser = user; // Giữ nguyên hình hài nguyên bản của Admin, Seller hay Bidder
                sendResponse("LOGIN_SUCCESS", user);
                continue;
              } else {
                throw new AuthenticationException("Sai tên đăng nhập hoặc mật khẩu.");
              }

            case "BID":
              if (loggedInUser == null) {
                throw new AuthenticationException("Bạn phải đăng nhập trước khi đặt giá!");
              }

              String bidData = String.valueOf(msg.getData());
              String[] bidParts = bidData.split(" ");
              if (bidParts.length < 2) {
                throw new InvalidBidException("Sai cú pháp! Mẫu: BID <Mã_Phiên> <Số_tiền>");
              }

              int auctionId = Integer.parseInt(bidParts[0]);
              handleAuctionLogic(bidParts[1]); // Check số tiền > 0
              BigDecimal bidAmount = new BigDecimal(bidParts[1]);

              boolean success = AuctionManager.getInstance().placeBid(auctionId, loggedInUser.getUsername(), bidAmount);

              if (success) {
                responseContent = "Đặt giá thành công " + bidAmount + " VNĐ cho phiên #" + auctionId;
                // Thông báo cho mọi người qua Server
                Server.broadcast(new NetworkMessage("BROADCAST",
                    "Người dùng [" + loggedInUser.getUsername() + "] đã đặt giá " + bidAmount + " cho phiên #" + auctionId));
              }
              break;

            case "GET_ALL_AUCTIONS":
              // Lấy danh sách từ DAO và gửi về cho Client
              responseContent = auctionSqlDAO.getAllAuctions();
              break;

            case "UPDATE_PROFILE":
              // 1. Lấy dữ liệu JSON Client gửi lên
              String userJson = gson.toJson(msg.getData());

              // 2. Chuyển JSON thành Đối tượng User (Xử lý đa hình để tránh lỗi Abstract)
              com.google.gson.JsonObject jsonObject = gson.fromJson(userJson, com.google.gson.JsonObject.class);
              User updatedUser;

              if (jsonObject.has("role")) {
                String role = jsonObject.get("role").getAsString();
                if ("ADMIN".equals(role)) {
                  updatedUser = gson.fromJson(userJson, Admin.class);
                } else if ("SELLER".equals(role)) {
                  updatedUser = gson.fromJson(userJson, Seller.class);
                } else {
                  updatedUser = gson.fromJson(userJson, Bidder.class);
                }
              } else {
                updatedUser = gson.fromJson(userJson, Bidder.class); // Mặc định
              }

              // 3. Gọi UserService của SERVER để kiểm tra và lưu xuống Database (TiDB)
              // (Giả sử bạn đã import com.uet.bidding.service.UserService của server)
              com.uet.bidding.service.UserService serverUserService = new com.uet.bidding.service.UserService();
              serverUserService.updateUser(updatedUser);

              // 4. Nếu lưu thành công (không bị throw Exception), cập nhật lại user đang đăng nhập trên server
              this.loggedInUser = updatedUser;
              responseContent = "Cập nhật hồ sơ thành công!";
              break;
            // ... các case khác (LOGIN, REGISTER, BID...)

            case "GET_SYSTEM_STATS":
              // 1. Lấy số lượng người online thực tế
              int activeSessions = Server.activeClients.size();

              // 2. Lấy tổng số user từ DB (Cần hàm này trong UserSqlDAO)
              int totalUsers = userSqlDAO.getTotalUserCount();

              // 3. Số sản phẩm chờ duyệt
              int pendingItems = 8;

              Map<String, Integer> statsMap = new HashMap<>();
              statsMap.put("totalUsers", totalUsers);
              statsMap.put("activeSessions", activeSessions);
              statsMap.put("pendingItems", pendingItems);

              // Gửi về cho Admin Dashboard
              sendResponse("SYSTEM_STATS_RESPONSE", statsMap);
            case "ADD_BALANCE":
              if (loggedInUser == null) {
                throw new AuthenticationException("Bạn phải đăng nhập trước khi nạp tiền!");
              }

              // 1. Ép kiểu dữ liệu Client gửi lên thành BigDecimal
              // Chú ý: Gson có thể parse số thành dạng chuỗi "10000.0", nên ta cần đọc an toàn
              String amountStr = String.valueOf(msg.getData());
              BigDecimal amountToAdd = new BigDecimal(amountStr);

              if (amountToAdd.compareTo(BigDecimal.ZERO) <= 0) {
                throw new InvalidBidException("Số tiền nạp phải lớn hơn 0!");
              }

              // 2. Cập nhật số tiền vào trong Database (Gọi qua UserSqlDAO)
              userSqlDAO.updateBalance(loggedInUser.getUsername(), amountToAdd);

              // 3. Cập nhật số dư cho user đang lưu trên RAM của Server
              BigDecimal newBalance = loggedInUser.getBalance().add(amountToAdd);
              loggedInUser.setBalance(newBalance);

              // 4. Trả kết quả về cho Client để cập nhật UI
              // Gửi nguyên object User về để Client tự động cập nhật Session
              sendResponse("UPDATE_BALANCE_SUCCESS", loggedInUser);

              // Thoát khỏi case, không đi xuống default
              continue;

            default:
              responseContent = "Lệnh không hợp lệ!";
              break;
          }
          sendResponse("SUCCESS", responseContent);

        } catch (InvalidBidException | AuctionClosedException | AuthenticationException e) {
          sendResponse("ERROR", e.getMessage());
        } catch (Exception e) {
          sendResponse("ERROR", "Lỗi hệ thống: " + e.getMessage());
        }
      }
    } catch (IOException e) {
      System.out.println("Mất kết nối với client.");
    } finally {
      closeSocket();
    }
  }

  private void handleRegister(String regData) throws UserException {
    String[] regParts = regData.split(" ");
    if (regParts.length < 2) throw new UserException("Sai cú pháp! Mẫu: REGISTER <user> <pass>");

    Bidder newBidder = new Bidder();
    newBidder.setUsername(regParts[0]);
    newBidder.setPassword(regParts[1]);
    newBidder.setBalance(BigDecimal.ZERO);

    // Tạo user mới (giả sử dùng Bidder mặc định)
    userSqlDAO.addUser(newBidder);
  }

  public void handleLoginLogic(String credentials) throws AuthenticationException {
    if (credentials == null || credentials.trim().isEmpty()) {
      throw new AuthenticationException("Thông tin không được để trống!");
    }
    if (credentials.toLowerCase().contains("root")) {
      throw new AuthenticationException("Tài khoản 'root' đã bị khóa vì bảo mật!");
    }
  }

  public void handleAuctionLogic(String amountStr) throws InvalidBidException {
    try {
      BigDecimal bidAmount = new BigDecimal(amountStr);
      if (bidAmount.compareTo(BigDecimal.ZERO) <= 0) {
        throw new InvalidBidException("Giá đặt phải lớn hơn 0!");
      }
    } catch (NumberFormatException e) {
      throw new InvalidBidException("Số tiền không hợp lệ!");
    }
  }

  @Override
  public void updatePrice(String itemName, double newPrice, String topBidder) {
    String realtimeData = "Sản phẩm: " + itemName + " | Giá mới: " + newPrice + " | Dẫn đầu: " + topBidder;
    sendResponse("PRICE_UPDATE", realtimeData);
  }

  private void closeSocket() {
    // ĐÃ FIX: Gọi thẳng hàm removeClient mà ta đã định nghĩa ở Server
    Server.removeClient(this);

    try {
      if (clientSocket != null && !clientSocket.isClosed()) {
        clientSocket.close();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}