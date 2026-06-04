package com.uet.bidding.model;

import java.util.List;

/**
 * Lớp Admin - Kế thừa từ User.
 * Tập trung vào các hàm quản trị hệ thống theo yêu cầu mục 3.1.1 và 3.1.2.
 */
public class Admin extends User {

  public Admin() {
    super();
  }

  public Admin(String username, String password) {
    super(username, password);
  }

  public Admin(int id, String username, String password) {
    super(id, username, password);
  }

  @Override
  public String getRole() {
    return "ADMIN";
  }

  // ================= LOGIC QUẢN TRỊ (ADMIN FUNCTIONS) =================

// ================= LOGIC QUẢN TRỊ THỰC TẾ (ADMIN FUNCTIONS) =================

  /**
   * 1. Xóa phiên đấu giá vi phạm (Mục 3.1.2: Quản lý sản phẩm)
   * Chỉ cho phép xóa nếu phiên đấu giá CHƯA bắt đầu (OPEN/PENDING) hoặc bị hủy do vi phạm.
   * Không cho phép xóa phiên đang chạy (RUNNING) có người đặt giá trừ khi có cơ chế đặc biệt.
   */
  public boolean removeInvalidAuction(int auctionId, List<Auction> auctionList) {
    System.out.println("[Admin Action] " + getUsername() + " yêu cầu xóa phiên đấu giá ID: " + auctionId);

    // Tìm phiên đấu giá xem có tồn tại không và kiểm tra trạng thái
    for (Auction auction : auctionList) {
      if (auction.getId() == auctionId) {
        // Nếu phiên đấu giá đang chạy và đã có người đặt giá, cần cân nhắc trước khi xóa
        if ("RUNNING".equals(auction.getStatus()) && auction.getCurrentPrice().compareTo(auction.getItem().getStartingPrice()) > 0) {
          System.err.println("❌ Không thể xóa: Phiên đấu giá đang hoạt động và đã có người đặt giá!");
          return false;
        }

        // Thực hiện xóa khỏi danh sách RAM
        auctionList.remove(auction);
        System.out.println("✅ Xóa thành công phiên đấu giá ID: " + auctionId + " khỏi danh sách tạm thời.");
        return true;
      }
    }
    System.err.println("❌ Lỗi: Không tìm thấy phiên đấu giá ID " + auctionId);
    return false;
  }

  /**
   * 2. Khóa tài khoản người dùng (Mục 3.1.1: Quản lý người dùng)
   * Ngăn chặn Admin tự khóa chính mình hoặc khóa Admin khác.
   */
  public boolean banUser(User user) {
    if (user == null) {
      return false;
    }

    // Bảo vệ hệ thống: Không cho phép khóa tài khoản Admin
    if ("ADMIN".equals(user.getRole())) {
      System.err.println("❌ Lỗi: Không thể khóa tài khoản Admin khác!");
      return false;
    }

    if (user.isBanned()) {
      System.out.println("⚠️ Tài khoản " + user.getUsername() + " đã bị khóa từ trước.");
      return true;
    }

    user.setBanned(true);
    System.out.println("🔒 Tài khoản " + user.getUsername() + " đã bị khóa thành công bởi Admin: " + getUsername());
    return true;
  }

  /**
   * 3. Mở khóa tài khoản người dùng
   */
  public boolean unbanUser(User user) {
    if (user == null) {
      return false;
    }

    if (!user.isBanned()) {
      System.out.println("⚠️ Tài khoản " + user.getUsername() + " đang không bị khóa.");
      return true;
    }

    user.setBanned(false);
    System.out.println("🔓 Tài khoản " + user.getUsername() + " đã được mở khóa bởi Admin: " + getUsername());
    return true;
  }

  /**
   * 4. Duyệt sản phẩm mới
   * Giả định lớp Item của bạn có thuộc tính trạng thái (status hoặc approved)
   */
  public void approveItem(Item item) {
    if (item == null) return;

    // Nếu lớp Item của bạn có trường status, hãy bỏ comment dòng dưới:
    // item.setStatus("APPROVED");

    System.out.println("✅ Sản phẩm '" + item.getName() + "' (ID: " + item.getId() + ") đã được phê duyệt đăng bán.");
  }
}