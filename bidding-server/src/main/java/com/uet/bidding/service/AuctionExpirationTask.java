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
      // Lấy toàn bộ danh sách phiên từ DB
      List<Auction> allAuctions = auctionSqlDAO.getAllAuctions();
      LocalDateTime now = LocalDateTime.now();

      for (Auction auction : allAuctions) {
        if (auction == null || auction.getStatus() == null) {
          continue;
        }

        String status = auction.getStatus().toUpperCase();

        // CHỈ QUÉT những phiên chưa kết thúc (OPEN hoặc RUNNING)
        // Bỏ qua ngay lập tức nếu phiên đã FINISHED, PAID, hoặc CANCELED
        if ("FINISHED".equals(status) || "PAID".equals(status) || "CANCELED".equals(status)) {
          continue;
        }

        // So sánh thời gian kết thúc <= Hiện tại
        if (auction.getEndTime() != null && !auction.getEndTime().isAfter(now)) {
          System.out.println("⏰ [Server] Phiên #" + auction.getId() + " đã hết giờ. Đang tự động đóng...");

          try {
            // Thực hiện chốt phiên dưới DB
            auctionSqlDAO.finishAuction(auction.getId());

            // Đọc lại trạng thái thực tế xem phiên đã sang FINISHED chưa trước khi phát loa
            Auction updated = auctionSqlDAO.findById(auction.getId());
            if (updated != null && "FINISHED".equalsIgnoreCase(updated.getStatus())) {
              updated.setRegisteredCount(auctionSqlDAO.getRegistrationCount(auction.getId()));

              // Broadcast thông báo cho Client
              Server.broadcast(new NetworkMessage("AUCTION_UPDATED", updated));
              Server.broadcast(new NetworkMessage("BROADCAST",
                  "🎉 Phiên đấu giá [" + updated.getItem().getName() + "] đã chính thức khép lại!"));
            }
          } catch (Exception ex) {
            System.err.println("❌ Lỗi xử lý đóng phiên #" + auction.getId() + ": " + ex.getMessage());
          }
        }
      }
    } catch (Exception e) {
      System.err.println("❌ [Server] Lỗi trong luồng quét phiên đấu giá: " + e.getMessage());
      e.printStackTrace();
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
