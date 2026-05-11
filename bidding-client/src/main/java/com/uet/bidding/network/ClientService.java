package com.uet.bidding.network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.uet.bidding.model.*;
import com.uet.bidding.controller.Main;
import com.uet.bidding.util.UserSession;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.Socket;

public class ClientService {
  private static volatile ClientService instance;
  private final Gson gson = new Gson();
  private Socket socket;
  private PrintWriter out;
  private BufferedReader in;
  private boolean isRunning = false;

  private ClientService() {}

  public static ClientService getInstance() {
    if (instance == null) {
      synchronized (ClientService.class) {
        if (instance == null) instance = new ClientService();
      }
    }
    return instance;
  }

  public void connect(String host, int port) throws IOException {
    if (socket != null && !socket.isClosed()) return;

    this.socket = new Socket(host, port);
    this.out = new PrintWriter(socket.getOutputStream(), true);
    this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    this.isRunning = true;

    new Thread(this::listenFromServer).start();
    System.out.println("🚀 Kết nối mạng thành công!");
  }

  private void listenFromServer() {
    try {
      String jsonResponse;
      while (isRunning && (jsonResponse = in.readLine()) != null) {
        NetworkMessage msg = gson.fromJson(jsonResponse, NetworkMessage.class);
        handleResponse(msg);
      }
    } catch (IOException e) {
      System.err.println("❌ Kết nối bị ngắt đột ngột!");
      isRunning = false;
    }
  }

  private void handleResponse(NetworkMessage msg) {
    switch (msg.getType()) {
      case "LOGIN_SUCCESS":
        processLogin(msg);
        break;

      case "UPDATE_PROFILE_SUCCESS":
        processUpdateProfile(msg);
        break;

      case "UPDATE_BALANCE_SUCCESS":
        processUpdateBalance(msg);
        break;

      case "REGISTER_SUCCESS":
        Platform.runLater(() -> Main.changeScene("/Login.fxml", "Đăng nhập", 400, 500));
        break;

      case "ERROR":
        String errorMsg = String.valueOf(msg.getData());
        Platform.runLater(() -> showAlert("Thất bại", errorMsg, Alert.AlertType.ERROR));
        break;
    }
  }

  // --- CÁC HÀM XỬ LÝ LOGIC CHI TIẾT ---

  private void processLogin(NetworkMessage msg) {
    User user = parseUserFromJson(msg.getData());
    if (user != null) {
      UserSession.setCurrentUser(user);
      Platform.runLater(() -> {
        // Kiểm tra vai trò để chuyển màn hình
        if (user instanceof Admin) {
          Main.changeScene("/AdminDashboard.fxml", "Admin Control Panel", 1100, 800);
        } else if (user instanceof Customer) {
          Main.changeScene("/AuctionList.fxml", "Hệ thống Đấu giá VNU", 1000, 700);
        }
      });
    }
  }

  private void processUpdateProfile(NetworkMessage msg) {
    User updatedUser = parseUserFromJson(msg.getData());
    if (updatedUser != null) UserSession.setCurrentUser(updatedUser);

    Platform.runLater(() -> showAlert("Thành công",
            "Hồ sơ của bạn đã được cập nhật đầy đủ lên hệ thống!", Alert.AlertType.INFORMATION));
  }

  private void processUpdateBalance(NetworkMessage msg) {
    User user = parseUserFromJson(msg.getData());

    if (user instanceof Customer) {
      Customer customer = (Customer) user; // Ép kiểu về Customer để lấy balance
      UserSession.setCurrentUser(customer);

      BigDecimal currentBalance = customer.getBalance();

      Platform.runLater(() -> {
        showAlert("Nạp tiền thành công",
                "Số dư mới: " + String.format("%,.0f", currentBalance) + " VNĐ",
                Alert.AlertType.INFORMATION);
      });
    }
  }

  /**
   * HÀM HELPER: Xác định đúng loại User từ JSON
   */
  private User parseUserFromJson(Object data) {
    if (data == null) return null;

    String json = gson.toJson(data);
    JsonObject obj = gson.fromJson(json, JsonObject.class);

    // Cách 1: Dựa vào trường 'role' nếu Server có gửi về
    if (obj.has("role")) {
      String role = obj.get("role").getAsString();
      if ("ADMIN".equalsIgnoreCase(role)) return gson.fromJson(json, Admin.class);
      if ("CUSTOMER".equalsIgnoreCase(role)) return gson.fromJson(json, Customer.class);
    }

    // Cách 2: Dựa vào thuộc tính đặc thù (Phòng hờ Server không gửi role)
    // Admin có adminLevel, Customer có balance
    if (obj.has("adminLevel")) {
      return gson.fromJson(json, Admin.class);
    } else if (obj.has("balance")) {
      return gson.fromJson(json, Customer.class);
    }

    return null;
  }

  private void showAlert(String title, String content, Alert.AlertType type) {
    Alert alert = new Alert(type);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(content);
    alert.showAndWait();
  }

  public void sendRequest(String type, Object data) {
    if (out != null) {
      out.println(gson.toJson(new NetworkMessage(type, data)));
    }
  }
}