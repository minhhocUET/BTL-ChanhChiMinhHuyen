package com.uet.bidding.service;

import com.uet.bidding.dao.AuctionSqlDAO;
import com.uet.bidding.model.Auction;
import com.uet.bidding.model.NetworkMessage;
import com.uet.bidding.server.Server;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AuctionExpirationTask {
  // Tạo một luồng (thread) chạy ngầm độc lập
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
  private final AuctionSqlDAO auctionSqlDAO;

  public AuctionExpirationTask(AuctionSqlDAO auctionSqlDAO) {
    this.auctionSqlDAO = auctionSqlDAO;
  }

  /**
   * Hàm này được gọi khi Server khởi động
   */
  public void start() {
    // Cấu hình: Delay ban đầu 0 giây, lặp lại sau mỗi 5 giây
    scheduler.scheduleAtFixedRate(this::checkAndCloseAuctions, 0, 5, TimeUnit.SECONDS);
    System.out.println("✅ [Server] Đã khởi động luồng ngầm quét thời gian đấu giá (5s/lần).");
  }

  /**
   * Core logic quét và đóng phiên
   */
  private void checkAndCloseAuctions() {
    try {
      // 1. Lấy tất cả các phiên đang chạy (RUNNING)
      List<Auction> runningAuctions = auctionSqlDAO.getRunningAuctionsForHall();
      LocalDateTime now = LocalDateTime.now();

      for (Auction auction : runningAuctions) {
        // 2. So sánh: Nếu thời gian kết thúc <= thời gian hiện tại
        if (auction.getEndTime() != null && !auction.getEndTime().isAfter(now)) {
          System.out.println("⏰ [Server] Phiên #" + auction.getId() + " đã hết giờ. Đang tự động đóng...");

          // 3. Gọi hàm của DAO để lưu trạng thái FINISHED và sinh Transaction
          auctionSqlDAO.finishAuction(auction.getId());

          // 4. Cập nhật lại số liệu mới nhất từ DB
          Auction updated = auctionSqlDAO.findById(auction.getId());
          updated.setRegisteredCount(auctionSqlDAO.getRegistrationCount(auction.getId()));

          // 5. Phát loa (Broadcast) báo cho TẤT CẢ các Client đang mở app biết để ép UI cập nhật
          Server.broadcast(new NetworkMessage("AUCTION_UPDATED", updated));
          Server.broadcast(new NetworkMessage("BROADCAST",
              "🎉 Phiên đấu giá [" + updated.getItem().getName() + "] đã chính thức khép lại!"));
        }
      }
    } catch (Exception e) {
      System.err.println("❌ [Server] Lỗi trong luồng quét phiên đấu giá: " + e.getMessage());
      e.printStackTrace(); // In ra log để dễ debug nếu có lỗi DB
    }
  }

  /**
   * Dùng khi tắt Server để giải phóng RAM
   */
  public void stop() {
    scheduler.shutdown();
    System.out.println("🛑 [Server] Đã tắt luồng quét đấu giá.");
  }
}
