package com.uet.bidding.server;

import com.uet.bidding.dao.AuctionSqlDAO;
import com.uet.bidding.dao.ItemFileDAO;
import com.uet.bidding.dao.UserSqlDAO;
import com.uet.bidding.model.NetworkMessage;
import com.uet.bidding.service.AuctionManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server { // Đây là file chạy chính của SERVER

  private static final int MAX_THREADS = 50; // Giới hạn tối đa 50 client xử lý cùng lúc
  // KHAI BÁO THREAD POOL: Thay vì tạo Thread thủ công, ta dùng Pool để quản lý
  private static final ExecutorService threadPool = Executors.newFixedThreadPool(MAX_THREADS);

  // ==============================================================
  // 1. CÁC BIẾN QUẢN LÝ MẠNG
  // ==============================================================
  // Danh sách lưu trữ các Client đang kết nối (Dùng CopyOnWriteArraySet để chống lỗi đa luồng)
  public static Set<ClientHandler> activeClients = new CopyOnWriteArraySet<>();

  // Hàm gửi tin nhắn Broadcast cho tất cả Client đang online
  public static void broadcast(NetworkMessage message) {
    for (ClientHandler client : activeClients) {
      client.sendMessage(message);
    }
  }

  // Xóa client khỏi danh sách khi họ ngắt kết nối
  public static void removeClient(ClientHandler handler) {
    activeClients.remove(handler);
    System.out.println("Một Client đã thoát. Hiện còn: " + activeClients.size() + " kết nối.");
  }

  public static void main(String[] args) {
    int port = 8888;

    // 1. KHỞI TẠO CÁC DAO
    UserSqlDAO userSqlDAO = new UserSqlDAO(); // Dùng SQL cho User
    ItemFileDAO itemFileDAO = new ItemFileDAO(); // Dùng File cho Item
    AuctionSqlDAO auctionSqlDAO = new AuctionSqlDAO();
    AuctionManager.getInstance().initialize(auctionSqlDAO);

    // 2. NẠP DỮ LIỆU TỪ FILE LÊN RAM
    System.out.println("Đang khởi động hệ thống và nạp dữ liệu...");

    // Sử dụng try-with-resources để tự động đóng ServerSocket
    try (ServerSocket serverSocket = new ServerSocket(port)) {
      System.out.println("Server đang chạy 24/24 và lắng nghe tại cổng " + port + "...");

      // Hook để tự động dọn dẹp ThreadPool nếu có ai đó tắt server bằng tay (Ctrl+C)
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        System.out.println("\n🛑 Đang giải phóng tài nguyên Thread Pool...");
        threadPool.shutdown();
      }));

      while (true) {
        // Đợi và chấp nhận kết nối từ Client
        Socket clientSocket = serverSocket.accept();

        System.out.println("Có kết nối mới từ: " + clientSocket.getInetAddress());

        // 3. Khởi tạo handler cho client mới
        ClientHandler handler = new ClientHandler(clientSocket, userSqlDAO, itemFileDAO, auctionSqlDAO);

        // LƯU NGƯỜI CHƠI VÀO DANH SÁCH QUẢN LÝ
        activeClients.add(handler);

        // Giao việc cho ThreadPool xử lý
        threadPool.execute(handler);
      }
    } catch (IOException e) {
      System.err.println("Lỗi Server: " + e.getMessage());
    }
  }
}