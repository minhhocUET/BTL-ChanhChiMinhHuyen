package com.uet.bidding;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class AuctionServer {
    public static void main(String[] args) {
        int port = 8080; // Cổng kết nối (giống như số nhà)

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("🟢 Server Đấu Giá đang chạy và lắng nghe ở cổng " + port + "...");

            while (true) {
                // Lệnh accept() sẽ chặn luồng và chờ đợi cho đến khi có Client kết nối
                Socket clientSocket = serverSocket.accept();
                System.out.println("🎉 Đã có một người tham gia đấu giá (Client) kết nối thành công: " + clientSocket.getInetAddress());

                // (Tạm thời chỉ in ra, lát nữa chúng ta sẽ xử lý việc nhận/gửi tin nhắn sau)
            }
        } catch (IOException e) {
            System.out.println("🔴 Lỗi khi khởi động Server: " + e.getMessage());
        }
    }
}