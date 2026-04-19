package com.uet.bidding;

import com.google.gson.Gson;
import com.uet.bidding.model.NetworkMessage;
import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private Gson gson = new Gson(); // Máy tháo dỡ JSON

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String inputLine;
            // Vòng lặp nhận tin nhắn liên tục từ Client
            while ((inputLine = in.readLine()) != null) {

                try {
                    // 1. Giải mã chuỗi JSON thành đối tượng Java (Kiến thức Tuần 6)
                    NetworkMessage msg = gson.fromJson(inputLine, NetworkMessage.class);

                    // 2. Phân loại và xử lý hành động
                    String response;
                    switch (msg.getType()) {
                        case "LOGIN":
                            response = "Chào mừng " + msg.getContent() + " đã đăng nhập!";
                            break;
                        case "BID":
                            response = "Bạn đã đặt giá: " + msg.getContent() + " VNĐ. Đang kiểm tra...";
                            // Sau này bạn sẽ thêm logic kiểm tra giá cao nhất ở đây
                            break;
                        case "INFO":
                            response = "Thông tin sản phẩm: Bình hoa cổ thế kỷ 18.";
                            break;
                        default:
                            response = "Hệ thống không hiểu lệnh này!";
                            break;
                    }

                    // 3. Phản hồi lại cho Client
                    out.println(response);

                } catch (Exception e) {
                    out.println("Lỗi: Định dạng tin nhắn không hợp lệ.");
                }
            }
        } catch (IOException e) {
            System.out.println("Mất kết nối với client: " + e.getMessage());
        } finally {
            closeSocket();
        }
    }

    private void closeSocket() {
        try {
            if (clientSocket != null) clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
