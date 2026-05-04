package com.uet.bidding;

import com.google.gson.Gson;
import com.uet.bidding.model.NetworkMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class App {
  public static void main(String[] args) {
    String hostname = "localhost";
    int port = 8888;
    Gson gson = new Gson();
    Scanner scanner = new Scanner(System.in);

    System.out.println("=== HỆ THỐNG ĐẤU GIÁ UET - CLIENT (SPACE VERSION) ===");

    try (Socket socket = new Socket(hostname, port);
         PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
         BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

      System.out.println("[Hệ thống] Đã kết nối thành công.");
      System.out.println("[Hướng dẫn] Nhập lệnh: LOẠI_TIN_NHẮN NỘI_DUNG (Ví dụ: BID 500000)");
      System.out.println("Nhập 'EXIT' để thoát.");

      while (true) {
        System.out.print("\nNhập lệnh: ");
        String input = scanner.nextLine().trim(); // .trim() để xóa khoảng trắng thừa ở đầu/cuối

        if (input.equalsIgnoreCase("EXIT")) {
          break;
        }

        if (input.isEmpty()) continue;


        // Tách lệnh dựa trên dấu cách đầu tiên tìm thấy
        // Limit = 2 đảm bảo nếu nội dung có dấu cách (vd: "CHAT Hello bạn") thì vẫn lấy đủ
        String[] parts = input.split(" ", 2);

        String type = parts[0].toUpperCase(); // Chuyển "bid" thành "BID" cho chuyên nghiệp
        String content = (parts.length > 1) ? parts[1] : "";

        // Đóng gói và gửi JSON
        NetworkMessage msg = new NetworkMessage(type, content);
        out.println(gson.toJson(msg));

        // Nhận phản hồi
        String rawResponse = in.readLine(); // Nhận {"type":"SUCCESS", "content":"..."}
        if (rawResponse != null) {
          // Giải mã JSON thành đối tượng NetworkMessage
          NetworkMessage serverMsg = gson.fromJson(rawResponse, NetworkMessage.class);

          // Chỉ in ra phần content, có thể thêm tiền tố dựa trên Type
          if ("SUCCESS".equals(serverMsg.getType())) {
            System.out.println("[Server]: " + serverMsg.getContent());
          } else if ("ERROR".equals(serverMsg.getType())) {
            // In lỗi màu đỏ cho chuyên nghiệp (System.err)
            System.err.println("[Lỗi]: " + serverMsg.getContent());
          }
        }
      }

    } catch (IOException e) {
      System.err.println("[Lỗi] Kết nối thất bại: " + e.getMessage());
    } finally {
      scanner.close();
    }
  }
}