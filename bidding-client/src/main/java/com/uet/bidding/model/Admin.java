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

  /**
   * 1. Xóa phiên đấu giá (Mục 3.1.2: Quản lý sản phẩm)
   * Hàm này sẽ xóa phiên đấu giá khỏi danh sách quản lý của Server.
   */
  public boolean removeInvalidAuction(int auctionId, List<Auction> auctionList) {
    // Sau này bạn sẽ phát triển logic:
    // - Tìm đấu giá theo ID trong danh sách
    // - Kiểm tra xem đấu giá đã bắt đầu chưa (thường chỉ xóa phiên CHƯA bắt đầu hoặc vi phạm)
    // - Gọi DAO để xóa khỏi file JSON
    System.out.println("Admin " + getUsername() + " đang yêu cầu xóa phiên đấu giá ID: " + auctionId);

    // Logic mẫu:
    return auctionList.removeIf(a -> a.getId() == auctionId);
  }

  /**
   * 2. Khóa tài khoản người dùng (Mục 3.1.1: Quản lý người dùng)
   */
  public void banUser(User user) {
    if (user != null && !user.getRole().equals("ADMIN")) {
      user.setBanned(true);
      System.out.println("Tài khoản " + user.getUsername() + " đã bị khóa bởi Admin.");
    }
  }

  /**
   * 3. Mở khóa tài khoản người dùng
   */
  public void unbanUser(User user) {
    if (user != null) {
      user.setBanned(false);
      System.out.println("Tài khoản " + user.getUsername() + " đã được mở khóa.");
    }
  }

  /**
   * 4. Duyệt sản phẩm mới (Nếu hệ thống yêu cầu Admin duyệt trước khi đăng)
   */
  public void approveItem(Item item) {
    // Logic: Thay đổi trạng thái sản phẩm sang "APPROVED"
    System.out.println("Sản phẩm " + item.getName() + " đã được phê duyệt.");
  }
}