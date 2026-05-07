package com.uet.bidding;

import com.uet.bidding.dao.AuctionDAO;
import com.uet.bidding.dao.ItemDAO;
import com.uet.bidding.dao.UserDAO;
import com.uet.bidding.model.AuctionManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {// Đây là file chạy chính của SERVER

  private static final int MAX_THREADS = 50;
  private static final ExecutorService threadPool = Executors.newFixedThreadPool(MAX_THREADS);

  public static void main(String[] args) {
    int port = 8888;

    // 1. KHỞI TẠO CÁC DAO
    UserDAO userDAO = new UserDAO();
    ItemDAO itemDAO = new ItemDAO();
    AuctionDAO auctionDAO = new AuctionDAO();

    // 2. NẠP DỮ LIỆU TỪ FILE LÊN RAM
    System.out.println("Đang khởi động hệ thống và nạp dữ liệu...");
    AuctionManager.getInstance().initialize(auctionDAO);

    // Sử dụng try-with-resources để tự động đóng ServerSocket
    try (ServerSocket serverSocket = new ServerSocket(port)) {
      System.out.println("Server đang chạy với Thread Pool (Max: " + MAX_THREADS + ")...");
      System.out.println("Server đang chạy và lắng nghe tại cổng " + port + "...");

      // Thêm Shutdown Hook để đóng Pool êm đẹp khi tắt Server
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        System.out.println("\nĐang đóng Thread Pool...");
        threadPool.shutdown();
      }));

      while (true) {
        // Đợi và chấp nhận kết nối từ Client
        Socket clientSocket = serverSocket.accept();
        System.out.println("Có kết nối mới từ: " + clientSocket.getInetAddress());

        // 3. FIX LỖI XUNG ĐỘT: Truyền thêm userDAO vào để khớp với Constructor
        ClientHandler handler = new ClientHandler(clientSocket, userDAO);
        threadPool.execute(handler);
      }
    } catch (IOException e) {
      System.err.println("Lỗi Server: " + e.getMessage());
    }
  }
}