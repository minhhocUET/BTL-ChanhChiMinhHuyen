package com.uet.bidding;

import com.google.gson.Gson;
import com.uet.bidding.exception.AuctionClosedException;
import com.uet.bidding.exception.AuthenticationException;
import com.uet.bidding.exception.InvalidBidException;
import com.uet.bidding.model.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.Socket;

public class ClientHandler implements Runnable, AuctionObserver {
  private final Socket clientSocket;
  private final UserDAO userDAO;
  private final Gson gson = new Gson();
  private PrintWriter out;
  private BufferedReader in;
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
                this.loggedInUser = new Bidder(user.getId(), user.getUsername(), user.getPassword(), user.getBalance(), user.getEmail());
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

              handleAuctionLogic(bidParts[1]);

              BigDecimal bidAmount = new BigDecimal(bidParts[1]);

              // Gọi tới DAO và logic đồng bộ của hệ thống
              boolean success = AuctionManager.getInstance().placeBid(auctionId, loggedInUser.getUsername(), bidAmount);

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

  public void handleAuctionLogic(String amountStr) throws InvalidBidException, AuctionClosedException {
    try {
      BigDecimal bidAmount = new BigDecimal(amountStr);
      if (bidAmount.compareTo(BigDecimal.ZERO) <= 0) {
        throw new InvalidBidException("Giá đặt phải lớn hơn 0!");
      }
    } catch (NumberFormatException e) {
      throw new InvalidBidException("Vui lòng nhập số tiền hợp lệ!");
    }
  }

  public void updatePrice(String itemName, double newPrice, String topBidder) {
    String realtimeData = "Sản phẩm: "+ itemName + " | Giá mới: " + newPrice + " | Đang dẫn đầu: " + topBidder;
    sendResponse("PRICE_UPDATE", realtimeData);
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

  public static class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final UserDAO userDAO;
    private final Gson gson = new Gson();
    private PrintWriter out;
    private BufferedReader in;
    private Bidder loggedInUser = null;

    public ClientHandler(Socket socket, UserDAO userDAO) {
      this.clientSocket = socket;
      this.userDAO = userDAO;
    }

    // ==============================================================
    // HÀM GỬI TIN NHẮN (GIÚP SERVER GỌI ĐƯỢC - FIX LỖI sendMessage)
    // ==============================================================
    public void sendMessage(NetworkMessage msg) {
      if (out != null) {
        out.println(gson.toJson(msg));
      }
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
              case "LOGIN":
                String credentials = String.valueOf(msg.getData());
                String[] loginData = credentials.split(" ");

                if (loginData.length < 2) {
                  throw new AuthenticationException("Sai cú pháp! Mẫu: LOGIN <tài_khoản> <mật_khẩu>");
                }

                // Kiểm tra các logic cấm (Ví dụ: root)
                handleLoginLogic(loginData[0]);

                User user = userDAO.checkLogin(loginData[0], loginData[1]);
                if (user != null) {
                  this.loggedInUser = new Bidder(user.getId(), user.getUsername(), user.getPassword(), user.getBalance(), user.getEmail());
                  responseContent = "Đăng nhập thành công! Xin chào " + user.getUsername();
                } else {
                  throw new AuthenticationException("Sai tên đăng nhập hoặc mật khẩu.");
                }
                break;

              case "BID":
                if (loggedInUser == null) {
                  throw new AuthenticationException("Bạn phải đăng nhập (LOGIN) trước khi đặt giá!");
                }

                String bidData = String.valueOf(msg.getData());
                String[] bidParts = bidData.split(" ");
                if (bidParts.length < 2) {
                  throw new InvalidBidException("Sai cú pháp! Mẫu chuẩn: BID <Mã_Phiên> <Số_tiền>");
                }

                int auctionId = Integer.parseInt(bidParts[0]);
                BigDecimal bidAmount = new BigDecimal(bidParts[1]);

                boolean success = AuctionManager.getInstance().placeBid(auctionId, loggedInUser, bidAmount);
                if (success) {
                  responseContent = "Chúc mừng! Đặt giá thành công " + bidAmount + " VNĐ cho phiên #" + auctionId;

                  // Gửi thông báo cho TẤT CẢ mọi người biết có người vừa nâng giá
                  Server.broadcast(new NetworkMessage("BROADCAST",
                      "Người dùng [" + loggedInUser.getUsername() + "] đã đặt giá " + bidAmount + " cho phiên #" + auctionId));
                }
                break;

              case "REGISTER":
                String regData = (String) msg.getData();
                String[] regParts = regData.split(" ");

                if (regParts.length < 2) {
                  throw new AuthenticationException("Sai cú pháp! Mẫu chuẩn: REGISTER <tài_khoản> <mật_khẩu>");
                }

                String newUsername = regParts[0];
                String newPassword = regParts[1];

                boolean isExist = userDAO.getAllUsers().stream()
                    .anyMatch(u -> u.getUsername().equalsIgnoreCase(newUsername));

                if (isExist) {
                  throw new AuthenticationException("Tên đăng nhập '" + newUsername + "' đã tồn tại!");
                }

                User newUser = new Seller("hoang_an_99",             // username
                    "matkhau123",              // password
                    new BigDecimal("5000000"), // balance (5 triệu VNĐ)
                    4.9,                       // rating (4.9 sao)
                    "0312456789",              // taxId (Mã số thuế)
                    "An Hoàng Luxury Watch");
                userDAO.addUser(newUser);
                responseContent = "Đăng ký thành công tài khoản [" + newUsername + "]!";
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
        System.out.println("Mất kết nối với client.");
      } finally {
        // Dọn dẹp danh sách khi Client thoát
        Server.activeClients.remove(this);
        closeSocket();

        System.out.println("Một Client đã thoát. Còn lại: " + Server.activeClients.size());

        // Kích hoạt đếm ngược 30s nếu không còn ai
        if (Server.activeClients.isEmpty()) {
          Server.startShutdownTimer();
        }
      }
    }

    private void sendResponse(String type, Object data) {
      sendMessage(new NetworkMessage(type, data));
    }

    // Sửa lại hàm check logic đăng nhập cho hợp lý
    public void handleLoginLogic(String username) throws AuthenticationException {
      if (username == null || username.trim().isEmpty()) {
        throw new AuthenticationException("Tên đăng nhập không được để trống!");
      }
      if (username.toLowerCase().equals("root")) {
        throw new AuthenticationException("Tài khoản 'root' đã bị khóa vì lý do bảo mật!");
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
}