package com.uet.bidding;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class App {
  public static void main(String[] args) {
    int port = 8888;

    // Sử dụng try-with-resources để tự động đóng ServerSocket
    try (ServerSocket serverSocket = new ServerSocket(port)) {
      System.out.println("Server đang chạy và lắng nghe tại cổng " + port + "...");

      while (true) {
        // Đợi và chấp nhận kết nối từ Client
        Socket clientSocket = serverSocket.accept();
        System.out.println("Có kết nối mới từ: " + clientSocket.getInetAddress());

        // Khởi tạo nhân viên (ClientHandler) để xử lý riêng cho khách này
        // Điều này giúp Server có thể phục vụ nhiều khách cùng lúc (Tuần 7)
        ClientHandler handler = new ClientHandler(clientSocket);
        Thread thread = new Thread(handler);
        thread.start();
      }
    } catch (IOException e) {
      System.err.println("Lỗi Server: " + e.getMessage());
    }
  }
}
