package com.uet.bidding;

import com.uet.bidding.dao.UserDAO;
import com.uet.bidding.exception.AuctionClosedException;
import com.uet.bidding.exception.AuthenticationException;
import com.uet.bidding.exception.InvalidBidException;
import com.uet.bidding.model.NetworkMessage;
import com.uet.bidding.model.User;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {
  private final Socket clientSocket;
  private ObjectOutputStream out; // Khai báo ở đây để dùng được trong hàm sendResponse
  private ObjectInputStream in;
  private final UserDAO userDAO;

  public ClientHandler(Socket socket, UserDAO userDAO) {
    this.clientSocket = socket;
    this.userDAO = userDAO;
  }

  @Override
  public void run() {
    // Sử dụng try-with-resources để tự động đóng Stream như code cũ của bạn
    try (ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream());
         ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream())) {

      this.out = oos;
      this.in = ois;

      Object inputObject;
      // Đọc đối tượng NetworkMessage (Thay cho readLine cũ)
      while ((inputObject = in.readObject()) != null) {
        try {
          NetworkMessage msg = (NetworkMessage) inputObject;
          Object responseContent = "";

          switch (msg.getType()) {
            case "LOGIN":
              String credentials = (String) msg.getData();
              handleLoginLogic(credentials);

              String[] loginData = credentials.split(" ");
              if (loginData.length < 2) throw new AuthenticationException("Thiếu mật khẩu!");

              // Gọi DAO (Lúc này UserDAO đã dùng Serialization lưu file)
              User user = userDAO.checkLogin(loginData[0], loginData[1]);
              if (user != null) {
                responseContent = "Đăng nhập thành công! Xin chào " + user.getUsername();
              } else {
                throw new AuthenticationException("Sai tên đăng nhập hoặc mật khẩu.");
              }
              break;

            case "BID":
              // Giữ nguyên logic cũ của bạn
              String bidAmountStr = msg.getData().toString();
              handleAuctionLogic(bidAmountStr);
              responseContent = "Đặt giá thành công: " + bidAmountStr;
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
    } catch (IOException | ClassNotFoundException e) {
      System.out.println("Mất kết nối với client: " + e.getMessage());
    } finally {
      closeSocket();
    }
  }

  // Hàm bổ trợ giúp giải quyết lỗi đỏ trong ảnh của bạn
  private void sendResponse(String type, Object data) throws IOException {
    if (out != null) {
      out.writeObject(new NetworkMessage(type, data));
      out.flush();
    }
  }

  // --- CÁC HÀM LOGIC NGHIỆP VỤ (Giữ nguyên 100% của bạn) ---
  public void handleLoginLogic(String credentials) throws AuthenticationException {
    if (credentials == null || credentials.trim().isEmpty()) {
      throw new AuthenticationException("Tên đăng nhập không được để trống!");
    }
    if (credentials.toLowerCase().contains("root")) {
      throw new AuthenticationException("Tài khoản 'root' đã bị khóa!");
    }
  }

  public void handleAuctionLogic(String amountStr) throws InvalidBidException, AuctionClosedException {
    // Giữ nguyên logic check BigDecimal của bạn ở đây...
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
