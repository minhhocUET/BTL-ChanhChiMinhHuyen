package com.uet.bidding;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.uet.bidding.model.NetworkMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.Scanner;

public class ClientApp {
  private static final Gson networkGson = new Gson();
  private static final Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();

  private static volatile boolean isRunning = true;

  public static void main(String[] args) {
    String hostname = "localhost";
    int port = 8888;

    System.out.println("=================================================");
    System.out.println("||    HỆ THỐNG ĐẤU GIÁ UET - CLIENT VERSION    ||");
    System.out.println("=================================================");

    try (Socket socket = new Socket(hostname, port);
         PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
         BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
         Scanner scanner = new Scanner(System.in)) {

      System.out.println("[Hệ thống] Đã kết nối thành công tới Server.\n");

      // Hiển thị menu hướng dẫn ngay khi kết nối thành công
      showMenu();

      // Luồng nhận dữ liệu
      Thread listenerThread = new Thread(() -> {
        try {
          String rawResponse;
          while (isRunning && (rawResponse = in.readLine()) != null) {
            handleServerResponse(rawResponse);
          }
        } catch (SocketException e) {
          if (isRunning) {
            System.err.println("\n❌ [Lỗi] Mất kết nối tới Server!");
          }
        } catch (IOException e) {
          if (isRunning) System.err.println("\n❌ [Lỗi Đọc Dữ Liệu]: " + e.getMessage());
        }
      });
      listenerThread.start();

      // Luồng chính: Đọc lệnh từ người dùng
      while (isRunning) {
        String input = scanner.nextLine().trim();

        // 1. Xử lý các lệnh đặc biệt (Local commands - không cần gửi lên server)
        if (input.equalsIgnoreCase("EXIT")) {
          System.out.println("Đang ngắt kết nối và thoát hệ thống...");
          isRunning = false;
          System.exit(0);
          break;
        }

        if (input.equalsIgnoreCase("HELP")) {
          showMenu();
          System.out.print("Nhập lệnh: ");
          continue;
        }

        if (input.isEmpty()) continue;

        // 2. Tách lệnh và gửi đi cho Server
        String[] parts = input.split(" ", 2);
        String type = parts[0].toUpperCase();
        String content = (parts.length > 1) ? parts[1] : "";

        NetworkMessage msg = new NetworkMessage(type, content);
        out.println(networkGson.toJson(msg));
      }

    } catch (IOException e) {
      System.err.println("❌ [Lỗi Hệ Thống] Kết nối thất bại: " + e.getMessage());
      System.out.println("Vui lòng kiểm tra xem Server đã được bật chưa.");
    }
  }

  // ==============================================================
  // BẢNG HƯỚNG DẪN TRỰC QUAN
  // ==============================================================
  private static void showMenu() {
    System.out.println("\n------------------- CÚ PHÁP LỆNH -------------------");
    System.out.println(" 1. Đăng ký      : REGISTER <tài_khoản> <mật_khẩu>");
    System.out.println("                   (VD: REGISTER huyen123 123456)");
    System.out.println(" 2. Đăng nhập    : LOGIN <tài_khoản> <mật_khẩu>");
    System.out.println("                   (VD: LOGIN huyen123 123456)");
    System.out.println(" 3. Đặt giá      : BID <mã_phiên> <số_tiền>");
    System.out.println("                   (VD: BID 1 5000000)");
    System.out.println(" 4. Xem trợ giúp : HELP");
    System.out.println(" 5. Thoát app    : EXIT");
    System.out.println("----------------------------------------------------");
  }

  private static void handleServerResponse(String rawResponse) {
    try {
      NetworkMessage serverMsg = networkGson.fromJson(rawResponse, NetworkMessage.class);
      Object responseData = serverMsg.getData();

      String displayString;
      if (responseData instanceof String) {
        displayString = (String) responseData;
      } else {
        displayString = "\n" + prettyGson.toJson(responseData);
      }

      System.out.print("\r");

      switch (serverMsg.getType()) {
        case "SUCCESS":
          System.out.println("✅ [Thành công]: " + displayString);
          break;
        case "ERROR":
          System.err.println("❌ [Lỗi]: " + displayString);
          break;
        case "BROADCAST":
          System.out.println("📣 [THÔNG BÁO]: " + displayString);
          break;
        case "LOGIN_SUCCESS":
          System.out.println("🎊 [Hệ thống]: Đăng nhập thành công!");
          // responseData lúc này là Object User, bạn có thể in ra số dư:
          // System.out.println("Số dư hiện tại: " + prettyGson.toJson(responseData));
          break;
        default:
          System.out.println("ℹ️ [" + serverMsg.getType() + "]: " + displayString);
          break;
      }

      System.out.print("Nhập lệnh: ");

    } catch (Exception e) {
      System.err.println("\n❌ [Lỗi xử lý JSON]: Máy chủ gửi chuỗi không hợp lệ.");
      System.out.print("Nhập lệnh: ");
    }
  }
}