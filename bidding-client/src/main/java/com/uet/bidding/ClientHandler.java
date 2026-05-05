package com.uet.bidding;

import com.google.gson.Gson;
import com.uet.bidding.dao.UserDAO;
import com.uet.bidding.exception.AuctionClosedException;
import com.uet.bidding.exception.AuthenticationException;
import com.uet.bidding.exception.InvalidBidException;
import com.uet.bidding.model.AuctionManager;
import com.uet.bidding.model.Bidder;
import com.uet.bidding.model.NetworkMessage;
import com.uet.bidding.model.User;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.Socket;

public class ClientHandler implements Runnable {
  private final Socket clientSocket;
  private PrintWriter out;
  private BufferedReader in;
  private final UserDAO userDAO;
  private final Gson gson = new Gson();

  // Biến lưu trữ người dùng đang đăng nhập trên luồng (Socket) này
  private Bidder loggedInUser = null;

  public ClientHandler(Socket socket, UserDAO userDAO) {
    this.clientSocket = socket;
    this.userDAO = userDAO;
  }

  @Override
  public void run() {
    try {
      out = new PrintWriter(clientSocket.getOutputStream(), true);
      in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

      String inputLine;
      // Đọc dữ liệu JSON gửi từ Client
      while ((inputLine = in.readLine()) != null) {
        try {
          NetworkMessage msg = gson.fromJson(inputLine, NetworkMessage.class);
          Object responseContent = "";

          switch (msg.getType()) {
            case "LOGIN":
              String credentials = String.valueOf(msg.getData());
              handleLoginLogic(credentials);

              String[] loginData = credentials.split(" ");
              if (loginData.length < 2) throw new AuthenticationException("Thiếu mật khẩu!");

              // Gọi UserDAO check DB
              User user = userDAO.checkLogin(loginData[0], loginData[1]);
              if (user != null) {
                // Nếu thành công, lưu lại tài khoản vào biến loggedInUser
                this.loggedInUser = new Bidder(user.getId(), user.getUsername(), user.getPassword(), user.getBalance());
                responseContent = "Đăng nhập thành công! Xin chào " + user.getUsername();
              } else {
                throw new AuthenticationException("Sai tên đăng nhập hoặc mật khẩu.");
              }
              break;

            case "BID":
              if (loggedInUser == null) {
                throw new AuthenticationException("Bạn phải đăng nhập (LOGIN) trước khi đặt giá!");
              }

              // Client gửi chuỗi theo mẫu: BID <ID_Phiên> <Số_tiền> (Ví dụ: BID 1 5000000)
              String bidData = String.valueOf(msg.getData());
              String[] bidParts = bidData.split(" ");
              if (bidParts.length < 2) {
                throw new InvalidBidException("Sai cú pháp! Mẫu chuẩn: BID <Mã_Phiên> <Số_tiền>");
              }

              int auctionId = Integer.parseInt(bidParts[0]);
              BigDecimal bidAmount = new BigDecimal(bidParts[1]);

              // Gọi tới DAO và logic đồng bộ của hệ thống
              boolean success = AuctionManager.getInstance().placeBid(auctionId, loggedInUser, bidAmount);

              if (success) {
                responseContent = "Chúc mừng! Đặt giá thành công " + bidAmount + " VNĐ cho phiên #" + auctionId;
              }
              break;

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
      System.out.println("Mất kết nối với client: " + e.getMessage());
    } finally {
      closeSocket();
    }
  }

  // Hàm gửi trả JSON cho Client
  private void sendResponse(String type, Object data) {
    if (out != null) {
      NetworkMessage response = new NetworkMessage(type, data);
      out.println(gson.toJson(response));
    }
  }

  public void handleLoginLogic(String credentials) throws AuthenticationException {
    if (credentials == null || credentials.trim().isEmpty()) {
      throw new AuthenticationException("Tên đăng nhập không được để trống!");
    }
    if (credentials.toLowerCase().contains("root")) {
      throw new AuthenticationException("Tài khoản 'root' đã bị khóa!");
    }
  }

  private void closeSocket() {
    try {
      if (clientSocket != null && !clientSocket.isClosed()) {
        clientSocket.close();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}