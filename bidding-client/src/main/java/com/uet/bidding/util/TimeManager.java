package com.uet.bidding.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class TimeManager {
  // Độ lệch thời gian giữa Server và Client (tính bằng mili-giây)
  // Công thức: offset = ServerTime - ClientTime
  private static long timeOffset = 0;

  public static void setTimeOffset(long offset) {
    timeOffset = offset;
  }

  /**
   * 🌟 THAY THẾ CHO LocalDateTime.now()
   * Hàm này lấy giờ Client hiện tại, cộng thêm độ lệch pha để khớp 100% với Server
   */
  public static LocalDateTime getNow() {
    long currentClientTime = System.currentTimeMillis();
    long synchronizedTime = currentClientTime + timeOffset;

    // Chuyển số mili-giây thành LocalDateTime chuẩn múi giờ Việt Nam
    return Instant.ofEpochMilli(synchronizedTime)
        .atZone(ZoneId.of("Asia/Ho_Chi_Minh"))
        .toLocalDateTime();
  }
}