package com.uet.bidding;

import com.google.gson.Gson;
import com.uet.bidding.dao.DatabaseConnection;
import com.uet.bidding.dao.UserDAO;
import com.uet.bidding.exception.AuctionClosedException;
import com.uet.bidding.exception.AuthenticationException;
import com.uet.bidding.exception.InvalidBidException;
import com.uet.bidding.model.Bidder;
import com.uet.bidding.model.NetworkMessage;
import com.uet.bidding.model.Seller;
import com.uet.bidding.model.User;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.Socket;
import java.sql.Connection;

public class ClientHandler implements Runnable {
  private final Socket clientSocket;
  private final Gson gson = new Gson();

  public ClientHandler(Socket socket) {
    this.clientSocket = socket;
  }

  @Override
  public void run() {
    // Một khối try-with-resources duy nhất quản lý DB Connection và Socket IO
    try (Connection conn = DatabaseConnection.getConnection();
         BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
         PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

      UserDAO userDAO = new UserDAO(conn);
      String inputLine;

      while ((inputLine = in.readLine()) != null) {
        try {
          // 1. Giải mã tin nhắn JSON
          NetworkMessage msg = gson.fromJson(inputLine, NetworkMessage.class);
          String responseContent = "";

          // 2. Xử lý các loại tin nhắn
          switch (msg.getType()) {
            case "LOGIN":
              // Bước 1: Kiểm tra logic nghiệp vụ trước (tên trống, root...)
              handleLoginLogic(msg.getContent());

              // Bước 2: Kiểm tra trong Database
              String[] loginData = msg.getContent().split(" ");
              if (loginData.length < 2) throw new AuthenticationException("Thiếu mật khẩu!");

              User user = userDAO.checkLogin(loginData[0], loginData[1]);
              if (user != null) {
                responseContent = "Đăng nhập thành công! Xin chào " + user.getUsername()
                    + " (" + user.getRole() + ").";
              } else {
                throw new AuthenticationException("Sai tên đăng nhập hoặc mật khẩu.");
              }
              break;

            case "REGISTER":
              String[] regData = msg.getContent().split(" ");
              if (regData.length < 3) throw new Exception("Thiếu thông tin đăng ký.");

              User newUser = "BIDDER".equalsIgnoreCase(regData[2])
                  ? new Bidder(0, regData[0], regData[1], BigDecimal.ZERO)
                  : new Seller(0, regData[0], regData[1], BigDecimal.ZERO);

              userDAO.addUser(newUser);
              responseContent = "Đăng ký thành công cho " + regData[0] + "!";
              break;

            case "BID":
              // Gọi hàm xử lý logic đặt giá (Tuần 8)
              handleAuctionLogic(msg.getContent());
              responseContent = "Bạn đã đặt giá thành công: " + msg.getContent() + " VNĐ.";
              break;

            case "INFO":
              responseContent = "Thông tin sản phẩm: Bình hoa cổ thế kỷ 18.";
              break;

            default:
              responseContent = "Hệ thống không hiểu lệnh này!";
              break;
          }

          // 3. Phản hồi thành công về Client dạng JSON
          out.println(gson.toJson(new NetworkMessage("SUCCESS", responseContent)));

        } catch (InvalidBidException | AuctionClosedException | AuthenticationException e) {
          // Bắt các Custom Exception bạn đã tạo và gửi lỗi "có tâm" về Client
          out.println(gson.toJson(new NetworkMessage("ERROR", e.getMessage())));
        } catch (Exception e) {
          // Lỗi hệ thống khác
          out.println(gson.toJson(new NetworkMessage("ERROR", "Lỗi: " + e.getMessage())));
        }
      }
    } catch (IOException e) {
      System.out.println("Mất kết nối với client: " + e.getMessage());
    } catch (Exception e) {
      System.out.println("Lỗi Server: " + e.getMessage());
    } finally {
      closeSocket();
    }
  }

  // --- CÁC HÀM LOGIC NGHIỆP VỤ ---

  public void handleLoginLogic(String credentials) throws AuthenticationException {
    if (credentials == null || credentials.trim().isEmpty()) {
      throw new AuthenticationException("Tên đăng nhập không được để trống!");
    }
    if (credentials.toLowerCase().contains("root")) {
      throw new AuthenticationException("Tài khoản 'root' đã bị khóa vì lý do bảo mật!");
    }
  }

  public void handleAuctionLogic(String amountStr) throws InvalidBidException, AuctionClosedException {
    try {
      BigDecimal bidAmount = new BigDecimal(amountStr);
      BigDecimal currentMaxPrice = new BigDecimal("1000.00"); // Giả lập dữ liệu

      if (bidAmount.compareTo(BigDecimal.ZERO) <= 0) {
        throw new InvalidBidException("Giá đặt phải lớn hơn 0!");
      }
      if (bidAmount.compareTo(currentMaxPrice) <= 0) {
        throw new InvalidBidException("Giá của bạn (" + bidAmount + ") phải cao hơn giá hiện tại (" + currentMaxPrice + ")!");
      }
    } catch (NumberFormatException e) {
      throw new InvalidBidException("Vui lòng nhập số tiền hợp lệ!");
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