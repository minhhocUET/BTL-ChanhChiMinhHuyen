package com.uet.bidding;

import com.uet.bidding.dao.AuctionSqlDAO;
import com.uet.bidding.dao.ItemFileDAO; // Đã đổi từ ItemDAO sang ItemFileDAO
import com.uet.bidding.dao.UserSqlDAO; // Đã đổi từ UserDAO sang UserSqlDAO
import com.uet.bidding.model.NetworkMessage;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server { // Đây là file chạy chính của SERVER

  private static final int SHUTDOWN_DELAY_MS = 60000; // 60.000 mili-giây = 60 giây

  private static final int MAX_THREADS = 50; // Giới hạn tối đa 50 client xử lý cùng lúc
  // KHAI BÁO THREAD POOL: Thay vì tạo Thread thủ công, ta dùng Pool để quản lý
  private static final ExecutorService threadPool = Executors.newFixedThreadPool(MAX_THREADS);
  // ==============================================================
  // 1. CÁC BIẾN QUẢN LÝ MẠNG VÀ AUTO-SHUTDOWN
  // ==============================================================
  // Danh sách lưu trữ các Client đang kết nối (Dùng CopyOnWriteArraySet để chống lỗi đa luồng)
  public static Set<ClientHandler> activeClients = new CopyOnWriteArraySet<>();
  private static Timer autoShutdownTimer;

  // Hàm gửi tin nhắn Broadcast cho tất cả Client đang online
  public static void broadcast(NetworkMessage message) {
    for (ClientHandler client : activeClients) {
      client.sendMessage(message);
    }
  }

  // --- HÀM BẮT ĐẦU ĐẾM NGƯỢC ---
  public static synchronized void startShutdownTimer() {
    if (autoShutdownTimer != null) {
      autoShutdownTimer.cancel(); // Hủy bộ đếm cũ (nếu có) để đếm lại từ đầu
    }
    autoShutdownTimer = new Timer();
    System.out.println("⚠️ [Cảnh báo] Không còn Client nào kết nối. Server sẽ tự tắt sau 60 giây...");

    autoShutdownTimer.schedule(new TimerTask() {
      @Override
      public void run() {
        // Hết 30s, kiểm tra lại lần cuối cho chắc chắn là vẫn không có ai
        if (activeClients.isEmpty()) {
          System.out.println("🛑 [Hệ thống] Đã hết 60 giây. Đang tự động tắt Server để giải phóng RAM!");
          System.exit(0); // Tắt cứng Server ngay lập tức
        }
      }
    }, SHUTDOWN_DELAY_MS);
  }

  // --- HÀM HỦY ĐẾM NGƯỢC (KHI CÓ NGƯỜI VÀO) ---
  public static synchronized void cancelShutdownTimer() {
    if (autoShutdownTimer != null) {
      autoShutdownTimer.cancel();
      autoShutdownTimer = null;
      System.out.println("✅ [Hệ thống] Đã hủy lệnh tắt Server do có Client đang hoạt động.");
    }
  }

  public static void main(String[] args) {
    int port = 8888;

    // 1. KHỞI TẠO CÁC DAO
    UserSqlDAO userSqlDAO = new UserSqlDAO();
    ItemFileDAO itemFileDAO = new ItemFileDAO();
    AuctionSqlDAO auctionSqlDAO = new AuctionSqlDAO();

    // 2. NẠP DỮ LIỆU TỪ FILE LÊN RAM
    System.out.println("Đang khởi động hệ thống và nạp dữ liệu...");

    // Sử dụng try-with-resources để tự động đóng ServerSocket
    try (ServerSocket serverSocket = new ServerSocket(port)) {
      System.out.println("Server đang chạy và lắng nghe tại cổng " + port + "...");

      // Bật đếm ngược ngay khi Server vừa khởi động
      startShutdownTimer();

      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        System.out.println("\n🛑 Đang giải phóng tài nguyên Thread Pool...");
        threadPool.shutdown();
      }));

      while (true) {
        // Đợi và chấp nhận kết nối từ Client
        Socket clientSocket = serverSocket.accept();

        // NGAY KHI CÓ NGƯỜI KẾT NỐI -> HỦY BỘ ĐẾM NGƯỢC NGAY LẬP TỨC
        cancelShutdownTimer();

        System.out.println("Có kết nối mới từ: " + clientSocket.getInetAddress());

        // 3. FIX LỖI XUNG ĐỘT: Truyền thêm userDAO vào để khớp với Constructor
        ClientHandler handler = new ClientHandler(clientSocket, userSqlDAO, itemFileDAO, auctionSqlDAO);

        // LƯU NGƯỜI CHƠI VÀO DANH SÁCH QUẢN LÝ
        activeClients.add(handler);

        threadPool.execute(handler);
      }
    } catch (IOException e) {
      System.err.println("Lỗi Server: " + e.getMessage());
    }
  }
}