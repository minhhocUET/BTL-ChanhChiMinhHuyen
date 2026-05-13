package com.uet.bidding.util;

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
import java.util.function.Consumer; // Thêm import này

public class ClientApp {
  private static final Gson networkGson = new Gson();
  private static final Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();
  // Các thông số Server để dùng chung
  private static final String HOSTNAME = "localhost";
  private static final int PORT = 8888;
  private static volatile boolean isRunning = true;

  // ==============================================================
  // HÀM MỚI: Dùng cho JavaFX UI gửi yêu cầu và nhận phản hồi
  // ==============================================================
  public static void sendRequest(NetworkMessage request, Consumer<NetworkMessage> callback) {
    new Thread(() -> {
      try (Socket socket = new Socket(HOSTNAME, PORT);
           PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
           BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

        // 1. Gửi tin nhắn đi
        out.println(networkGson.toJson(request));

        // 2. Đợi phản hồi từ Server
        String rawResponse = in.readLine();
        if (rawResponse != null) {
          NetworkMessage response = networkGson.fromJson(rawResponse, NetworkMessage.class);
          // 3. Trả dữ liệu về cho Controller thông qua callback
          callback.accept(response);
        }
      } catch (IOException e) {
        System.err.println("❌ [Lỗi UI Request]: " + e.getMessage());
      }
    }).start();
  }

  // ==============================================================
  // CODE CŨ CỦA BẠN (GIỮ NGUYÊN ĐỂ CHẠY CONSOLE NẾU CẦN)
  // ==============================================================
  public static void main(String[] args) {
    System.out.println("=================================================");
    System.out.println("||    HỆ THỐNG ĐẤU GIÁ UET - CLIENT VERSION    ||");
    System.out.println("=================================================");

    try (Socket socket = new Socket(HOSTNAME, PORT);
         PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
         BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
         Scanner scanner = new Scanner(System.in)) {

      System.out.println("[Hệ thống] Đã kết nối thành công tới Server.\n");
      showMenu();

      Thread listenerThread = new Thread(() -> {
        try {
          String rawResponse;
          while (isRunning && (rawResponse = in.readLine()) != null) {
            handleServerResponse(rawResponse);
          }
        } catch (SocketException e) {
          if (isRunning) System.err.println("\n❌ [Lỗi] Mất kết nối tới Server!");
        } catch (IOException e) {
          if (isRunning) System.err.println("\n❌ [Lỗi Đọc Dữ Liệu]: " + e.getMessage());
        }
      });
      listenerThread.start();

      while (isRunning) {
        String input = scanner.nextLine().trim();
        if (input.equalsIgnoreCase("EXIT")) {
          isRunning = false;
          System.exit(0);
          break;
        }
        if (input.equalsIgnoreCase("HELP")) {
          showMenu();
          continue;
        }
        if (input.isEmpty()) continue;

        String[] parts = input.split(" ", 2);
        String type = parts[0].toUpperCase();
        String content = (parts.length > 1) ? parts[1] : "";

        NetworkMessage msg = new NetworkMessage(type, content);
        out.println(networkGson.toJson(msg));
      }
    } catch (IOException e) {
      System.err.println("❌ [Lỗi Hệ Thống] Kết nối thất bại: " + e.getMessage());
    }
  }

  private static void showMenu() {
    System.out.println("\n------------------- CÚ PHÁP LỆNH -------------------");
    System.out.println(" 1. Đăng ký      : REGISTER <tài_khoản> <mật_khẩu>");
    System.out.println(" 2. Đăng nhập    : LOGIN <tài_khoản> <mật_khẩu>");
    System.out.println(" 3. Đặt giá      : BID <mã_phiên> <số_tiền>");
    System.out.println(" 5. Thoát app    : EXIT");
    System.out.println("----------------------------------------------------");
  }

  private static void handleServerResponse(String rawResponse) {
    try {
      NetworkMessage serverMsg = networkGson.fromJson(rawResponse, NetworkMessage.class);
      Object responseData = serverMsg.getData();
      System.out.println("ℹ️ [" + serverMsg.getType() + "]: " + responseData);
    } catch (Exception e) {
      System.err.println("❌ [Lỗi xử lý JSON]");
    }
  }
}