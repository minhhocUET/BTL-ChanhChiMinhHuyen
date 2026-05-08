package com.uet.bidding.network;

import com.google.gson.Gson;
import com.uet.bidding.model.NetworkMessage;
import com.uet.bidding.model.User;
import com.uet.bidding.ui.Main;
import javafx.application.Platform;

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
        // Khi Server gửi gì về, nó sẽ hiện ở đây
        System.out.println("📩 Server phản hồi: " + jsonResponse);

        // 1. Giải mã JSON thành NetworkMessage
        NetworkMessage msg = gson.fromJson(jsonResponse, NetworkMessage.class);

        handleResponse(msg);

        // Sau này mình sẽ thêm logic đẩy dữ liệu này lên giao diện ở đây
      }
    } catch (IOException e) {
      System.err.println("❌ Kết nối bị ngắt đột ngột!");
      isRunning = false;
    }
  }

  private void handleResponse(NetworkMessage msg) {
    switch (msg.getType()) {
      case "LOGIN_SUCCESS":
        // Ép kiểu data (đang là LinkedTreeMap) về đối tượng User
        String jsonData = gson.toJson(msg.getData());
        com.google.gson.JsonObject jsonObject = gson.fromJson(jsonData, com.google.gson.JsonObject.class);

        User loggedInUser = null;

        // 2. Kiểm tra xem Server có gửi trường "role" về không
        if (jsonObject.has("role")) {
          String role = jsonObject.get("role").getAsString();

          if ("ADMIN".equals(role)) {
            loggedInUser = gson.fromJson(jsonData, com.uet.bidding.model.Admin.class);
          } else if ("SELLER".equals(role)) {
            loggedInUser = gson.fromJson(jsonData, com.uet.bidding.model.Seller.class);
          } else {
            loggedInUser = gson.fromJson(jsonData, com.uet.bidding.model.Bidder.class);
          }
        } else {
          // Nếu không có role, dùng cách cũ là check field đặc trưng
          if (jsonData.contains("adminLevel")) {
            loggedInUser = gson.fromJson(jsonData, com.uet.bidding.model.Admin.class);
          } else {
            loggedInUser = gson.fromJson(jsonData, com.uet.bidding.model.Bidder.class);
          }
        }

        // 3. Lưu Session và chuyển màn hình
        if (loggedInUser != null) {
          com.uet.bidding.util.UserSession.setCurrentUser(loggedInUser);
          Platform.runLater(() -> {
            if ("ADMIN".equals(com.uet.bidding.util.UserSession.getCurrentUser().getRole())) {
              Main.changeScene("/AdminDashboard.fxml", "Admin Control Panel", 1100, 800);
            } else {
              Main.changeScene("/AuctionList.fxml", "Hệ thống Đấu giá VNU", 1000, 700);
            }
          });
        }
        break;

      case "REGISTER_SUCCESS":
        System.out.println("✅ Đăng ký thành công!");
        Platform.runLater(() -> {
          // Bạn có thể hiện Alert thông báo hoặc tự chuyển về màn login
          Main.changeScene("/Login.fxml", "Đăng nhập", 400, 500);
        });
        break;

      case "UPDATE_BALANCE_SUCCESS":
        String newUserData = gson.toJson(msg.getData());
        com.google.gson.JsonObject balanceJson = gson.fromJson(newUserData, com.google.gson.JsonObject.class);

        User tempUser = null; // Tạo biến tạm
        if (balanceJson.has("role")) {
          String role = balanceJson.get("role").getAsString();
          if ("ADMIN".equals(role)) {
            tempUser = gson.fromJson(newUserData, com.uet.bidding.model.Admin.class);
          } else if ("SELLER".equals(role)) {
            tempUser = gson.fromJson(newUserData, com.uet.bidding.model.Seller.class);
          } else {
            tempUser = gson.fromJson(newUserData, com.uet.bidding.model.Bidder.class);
          }
        } else {
          tempUser = gson.fromJson(newUserData, com.uet.bidding.model.Bidder.class);
        }

        if (tempUser != null) {
          // Cập nhật Session ngay lập tức
          com.uet.bidding.util.UserSession.setCurrentUser(tempUser);

          // Tạo một biến final để dùng trong Lambda
          final User finalUser = tempUser;

          Platform.runLater(() -> {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
            alert.setTitle("Nạp tiền thành công");
            alert.setHeaderText(null);
            // Sử dụng biến finalUser ở đây
            alert.setContentText("Số dư mới: " + String.format("%,.0f", finalUser.getBalance()) + " VNĐ");
            alert.showAndWait();
          });
        }
        break;

      case "ERROR":
        String errorMsg = String.valueOf(msg.getData());
        System.err.println("❌ Lỗi từ Server: " + errorMsg);

        // Đẩy lỗi lên giao diện nếu đang ở màn hình Login
        Platform.runLater(() -> {
          // Mẹo: Bạn có thể tạo một biến static trong LoginController hoặc 1 Alert
          alertError("Đăng nhập thất bại", errorMsg);
        });
        break;
    }
  }

  private void alertError(String title, String content) {
    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
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