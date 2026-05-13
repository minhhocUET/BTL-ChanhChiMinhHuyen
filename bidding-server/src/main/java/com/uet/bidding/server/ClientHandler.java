package com.uet.bidding.server;

import com.google.gson.Gson;
import com.uet.bidding.dao.AuctionSqlDAO;
import com.uet.bidding.dao.ItemSqlDAO;
import com.uet.bidding.dao.UserSqlDAO;
import com.uet.bidding.model.AuctionObserver;
import com.uet.bidding.model.NetworkMessage;
import com.uet.bidding.model.User;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable, AuctionObserver {
  private final Socket clientSocket;
  private final RequestProcessor processor; // Thêm processor
  private final Gson gson = new Gson();
  private PrintWriter out;
  private User loggedInUser = null;

  public ClientHandler(Socket socket, UserSqlDAO userSqlDAO, ItemSqlDAO itemFileDAO, AuctionSqlDAO auctionSqlDAO) {
    this.clientSocket = socket;
    // Khởi tạo processor với các DAO tương ứng
    this.processor = new RequestProcessor(userSqlDAO, auctionSqlDAO);
  }

  @Override
  public void run() {
    try {
      out = new PrintWriter(clientSocket.getOutputStream(), true);
      BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

      String inputLine;
      while ((inputLine = in.readLine()) != null) {
        try {
          NetworkMessage msg = gson.fromJson(inputLine, NetworkMessage.class);
          // Ủy quyền xử lý cho processor
          processor.processRequest(msg, this);
        } catch (Exception e) {
          sendResponse("ERROR", e.getMessage());
        }
      }
    } catch (IOException e) {
      System.out.println("Mất kết nối với client.");
    } finally {
      closeSocket();
    }
  }

  public void sendResponse(String type, Object data) {
    if (out != null) out.println(gson.toJson(new NetworkMessage(type, data)));
  }

  public void sendMessage(NetworkMessage msg) {
    sendResponse(msg.getType(), msg.getData());
  }

  public User getLoggedInUser() {
    return loggedInUser;
  }

  public void setLoggedInUser(User user) {
    this.loggedInUser = user;
  }

  @Override
  public void updatePrice(String itemName, double newPrice, String topBidder) {
    sendResponse("PRICE_UPDATE", "Sản phẩm: " + itemName + " | Giá mới: " + newPrice);
  }

  private void closeSocket() {
    Server.removeClient(this);
    try {
      if (clientSocket != null) clientSocket.close();
    } catch (IOException e) {
    }
  }
}