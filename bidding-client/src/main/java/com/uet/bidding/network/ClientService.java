package com.uet.bidding.network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.uet.bidding.model.NetworkMessage;
import com.uet.bidding.model.User;
import com.uet.bidding.ui.Main;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientService {
  private static ClientService instance;
  private final Gson gson = new Gson();
  private Socket socket;
  private PrintWriter out;
  private BufferedReader in;
  private boolean isRunning = false;

  // Singleton: Gọi ở bất cứ đâu bằng ClientService.getInstance()
  public static ClientService getInstance() {
    if (instance == null) instance = new ClientService();
    return instance;
  }

  public void connect(String host, int port) throws IOException {
    if (socket != null && !socket.isClosed()) return;

    this.socket = new Socket(host, port);
    this.out = new PrintWriter(socket.getOutputStream(), true);
    this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    this.isRunning = true;

    // Bật luồng nghe ngầm để đợi Server gửi tin nhắn về
    new Thread(this::listenFromServer).start();
    System.out.println("🚀 Kết nối mạng thành công!");
  }

  private void listenFromServer() {
    try {
      String jsonResponse;
      while (isRunning && (jsonResponse = in.readLine()) != null) {
        System.out.println("📩 Server phản hồi: " + jsonResponse);

        // 1. Giải mã JSON thành NetworkMessage
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
        // Sử dụng hàm helper để ép kiểu đúng lớp con, tránh lỗi Abstract Class
        User loggedInUser = parseUserFromJson(msg.getData());

        if (loggedInUser != null) {
          com.uet.bidding.util.UserSession.setCurrentUser(loggedInUser);
          Platform.runLater(() -> {
            // Kiểm tra quyền Admin bằng instanceof an toàn tuyệt đối với Abstract Class
            if (loggedInUser instanceof com.uet.bidding.model.Admin) {
              Main.changeScene("/AdminDashboard.fxml", "Admin Control Panel", 1100, 800);
            } else {
              Main.changeScene("/AuctionList.fxml", "Hệ thống Đấu giá VNU", 1000, 700);
            }
          });
        }
        break;

      case "UPDATE_PROFILE_SUCCESS":
        // QUAN TRỌNG: Nạp lại Session bằng dữ liệu mới từ Server (đã có isProfileComplete = true)
        User updatedProfileUser = parseUserFromJson(msg.getData());
        if (updatedProfileUser != null) {
          com.uet.bidding.util.UserSession.setCurrentUser(updatedProfileUser);
        }

        Platform.runLater(() -> {
          Alert alert = new Alert(Alert.AlertType.INFORMATION);
          alert.setTitle("Thành công");
          alert.setHeaderText(null);
          alert.setContentText("Hồ sơ của bạn đã được cập nhật đầy đủ lên cơ sở dữ liệu hệ thống!");
          alert.showAndWait();
        });
        break;

      case "UPDATE_BALANCE_SUCCESS":
        User balanceUser = parseUserFromJson(msg.getData());

        if (balanceUser != null) {
          // Cập nhật Session ngay lập tức
          com.uet.bidding.util.UserSession.setCurrentUser(balanceUser);
          final User finalUser = balanceUser;

          Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Nạp tiền thành công");
            alert.setHeaderText(null);
            alert.setContentText("Số dư mới: " + String.format("%,.0f", finalUser.getBalance()) + " VNĐ");
            alert.showAndWait();
          });
        }
        break;

      case "REGISTER_SUCCESS":
        System.out.println("✅ Đăng ký thành công!");
        Platform.runLater(() -> {
          Main.changeScene("/Login.fxml", "Đăng nhập", 400, 500);
        });
        break;

      case "ERROR":
        String errorMsg = String.valueOf(msg.getData());
        System.err.println("❌ Lỗi từ Server: " + errorMsg);
        Platform.runLater(() -> alertError("Thất bại", errorMsg));
        break;
    }
  }

  /**
   * HÀM HELPER: Xử lý chuyên biệt cho Abstract Class (User)
   * Xác định đúng đối tượng con (Admin/Seller/Bidder) dựa trên các key trong JSON
   */
  private User parseUserFromJson(Object data) {
    if (data == null) return null;

    String jsonData = gson.toJson(data);
    JsonObject jsonObject = gson.fromJson(jsonData, JsonObject.class);

    // Nếu Server có gửi kèm trường role
    if (jsonObject.has("role")) {
      String role = jsonObject.get("role").getAsString();
      if ("ADMIN".equals(role)) {
        return gson.fromJson(jsonData, com.uet.bidding.model.Admin.class);
      } else if ("SELLER".equals(role)) {
        return gson.fromJson(jsonData, com.uet.bidding.model.Seller.class);
      } else {
        return gson.fromJson(jsonData, com.uet.bidding.model.Bidder.class);
      }
    }

    // Fallback: Dựa vào các trường đặc trưng của từng class
    if (jsonObject.has("adminLevel")) {
      return gson.fromJson(jsonData, com.uet.bidding.model.Admin.class);
    } else if (jsonObject.has("rating") || jsonObject.has("shopName")) {
      return gson.fromJson(jsonData, com.uet.bidding.model.Seller.class);
    } else {
      return gson.fromJson(jsonData, com.uet.bidding.model.Bidder.class);
    }
  }

  private void alertError(String title, String content) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle(title);
    alert.setContentText(content);
    alert.showAndWait();
  }

  // Hàm gửi tin nhắn đi cực tiện lợi
  public void sendRequest(String type, Object data) {
    if (out != null) {
      NetworkMessage msg = new NetworkMessage(type, data);
      String json = gson.toJson(msg);
      out.println(json);
    }
  }
}