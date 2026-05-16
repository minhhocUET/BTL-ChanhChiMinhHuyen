package com.uet.bidding.network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.uet.bidding.controller.AdminDashboardController;
import com.uet.bidding.controller.Main;
import com.uet.bidding.model.*;
import com.uet.bidding.util.UserSession;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.Socket;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class ClientService {
  private static volatile ClientService instance;
  private final Gson gson = GsonFactory.getInstance();
  private final Map<String, CompletableFuture<NetworkMessage>> pendingRequests = new ConcurrentHashMap<>();
  private Socket socket;
  private PrintWriter out;
  private BufferedReader in;
  private boolean isRunning = false;

  private ClientService() {
  }

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

  // Trong ClientService.java - Cập nhật lại vòng lặp đọc
  private void listenFromServer() {
    try {
      String jsonResponse;
      while (isRunning && (jsonResponse = in.readLine()) != null) {
        try {
          System.out.println("📥 Nhận từ Server: " + jsonResponse);
          NetworkMessage msg = gson.fromJson(jsonResponse, NetworkMessage.class);

          // ƯU TIÊN 1: Trả kết quả về cho hàm đang đợi (CompletableFuture)
          if (msg.getRequestId() != null && pendingRequests.containsKey(msg.getRequestId())) {
            System.out.println("✅ Khớp RequestId: " + msg.getRequestId());
            pendingRequests.remove(msg.getRequestId()).complete(msg);
          }
          // ƯU TIÊN 2: Các tin nhắn hệ thống (Broadcast, Update tự động)
          else {
            handleResponse(msg);
          }
        } catch (Exception e) {
          System.err.println("❌ Lỗi xử lý tin nhắn: " + e.getMessage());
        }
      }
    } catch (IOException e) {
      System.err.println("❌ Mất kết nối Socket!");
      isRunning = false;
      Platform.runLater(() -> Main.changeScene("/Login.fxml", "Đăng nhập", 400, 500));
    }
  }

  /**
   * HÀM QUAN TRỌNG: Biến Map thành Object thật (Admin hoặc Customer)
   */
  public User parseUser(Object data) {
    if (data == null) return null;
    try {
      Gson gson = GsonFactory.getInstance();
      String json = gson.toJson(data);

      // 1. Chuyển thành JsonObject để soi trường "role"
      JsonObject obj = gson.fromJson(json, JsonObject.class);

      if (obj.has("role")) {
        String role = obj.get("role").getAsString();

        if ("admin".equalsIgnoreCase(role)) {
          return gson.fromJson(json, Admin.class); // Trả về đối tượng Admin
        }
      }

      // Mặc định là Customer nếu không phải admin
      return gson.fromJson(json, Customer.class);
    } catch (Exception e) {
      System.err.println("❌ Lỗi parse User: " + e.getMessage());
      return null;
    }
  }

  private void handleResponse(NetworkMessage msg) {
    switch (msg.getType()) {
      case "UPDATE_PROFILE_SUCCESS":
        processUpdateProfile(msg);
        break;

      case "UPDATE_BALANCE_SUCCESS":
        processUpdateBalance(msg);
        break;

      case "ERROR":
        String errorMsg = String.valueOf(msg.getData());
        Platform.runLater(() -> showAlert("Thất bại", errorMsg, Alert.AlertType.ERROR));
        break;

      case "BROADCAST":
        String info = String.valueOf(msg.getData());
        // Thông báo cho người dùng hoặc cập nhật bảng đấu giá realtime
        Platform.runLater(() -> {
          // Bạn có thể hiển thị một thông báo nhỏ (Toast) hoặc cập nhật ListView
          System.out.println("📢 Thông báo hệ thống: " + info);
        });
        break;

      // Trong ClientService.java, hàm handleResponse:

      case "SYSTEM_STATS_RESPONSE":
        // Chuyển data sang Map
        String json = gson.toJson(msg.getData());
        Map<String, Double> stats = gson.fromJson(json, new TypeToken<Map<String, Double>>() {
        }.getType());

        // Đẩy dữ liệu sang Controller
        if (AdminDashboardController.getInstance() != null) {
          AdminDashboardController.getInstance().updateStatsUI(stats);
        }
        break;
    }
  }

  // --- CÁC HÀM XỬ LÝ LOGIC CHI TIẾT ---

  private void processLogin(NetworkMessage msg) {
    User user = parseUserFromJson(msg.getData()); // Dùng hàm parse mới ở trên

    if (user != null) {
      UserSession.setCurrentUser(user);

      Platform.runLater(() -> {
        // Kiểm tra CHÍNH XÁC kiểu đối tượng
        if (user instanceof Admin) {
          System.out.println("✅ Chuyển vào màn hình Admin");
          Main.changeScene("/AdminDashboard.fxml", "Admin Control Panel", 1100, 800);
        } else if (user instanceof Customer) {
          System.out.println("✅ Chuyển vào màn hình Khách hàng");
          Main.changeScene("/AuctionList.fxml", "Hệ thống Đấu giá", 1000, 700);
        }
      });
    } else {
      System.err.println("❌ Không thể xác định loại người dùng để chuyển màn hình!");
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
            "Số dư mới: " + String.format("%,.0f", currentBalance.doubleValue()) + " VNĐ",
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
    System.out.println("DEBUG - Dữ liệu nhận từ Server: " + json); // Xem Log ở đây!

    JsonObject obj = gson.fromJson(json, JsonObject.class);

    // Ưu tiên kiểm tra trường 'role' (Phải khớp chính xác chữ hoa/thường với DB)
    if (obj.has("role")) {
      String role = obj.get("role").getAsString().toUpperCase();
      System.out.println("DEBUG - Role nhận được: " + role);

      if ("ADMIN".equals(role)) return gson.fromJson(json, Admin.class);
      if ("CUSTOMER".equals(role)) return gson.fromJson(json, Customer.class);
    }

    // Cách dự phòng: Kiểm tra đặc điểm nhận dạng
    if (obj.has("adminLevel")) return gson.fromJson(json, Admin.class);
    if (obj.has("balance")) return gson.fromJson(json, Customer.class);

    return null;
  }

  private void showAlert(String title, String content, Alert.AlertType type) {
    Alert alert = new Alert(type);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(content);
    alert.showAndWait();
  }

  // --- 1. SỬA LẠI HÀM sendRequest ---
// Đổi từ void sang trả về CompletableFuture
  public CompletableFuture<NetworkMessage> sendRequest(String type, Object data) {
    CompletableFuture<NetworkMessage> future = new CompletableFuture<>();
    if (out != null) {
      String reqId = UUID.randomUUID().toString(); // Tạo ID ngẫu nhiên
      NetworkMessage msg = new NetworkMessage(type, data);
      msg.setRequestId(reqId);

      pendingRequests.put(reqId, future); // Đưa vào danh sách chờ
      out.println(gson.toJson(msg));
    } else {
      future.completeExceptionally(new RuntimeException("Mất kết nối máy chủ!"));
    }
    return future;
  }

  public void disconnect() {
    try {
      isRunning = false;
      if (in != null) in.close();
      if (out != null) out.close();
      if (socket != null) socket.close();
      System.out.println("🔌 Đã ngắt kết nối an toàn.");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}