package com.uet.bidding.service;

import com.uet.bidding.model.User;
import com.uet.bidding.network.ClientService;

import java.math.BigDecimal;

/**
 * Lớp này chịu trách nhiệm gửi các yêu cầu nghiệp vụ từ UI tới Server
 * thông qua ClientService (Socket).
 */
public class UserService {
  // Lấy instance duy nhất của ClientService để sử dụng ống dẫn Socket
  private ClientService clientService = ClientService.getInstance();

  /**
   * Gửi yêu cầu đăng nhập lên Server.
   * Lưu ý: Hàm trả về void vì kết quả sẽ được nhận sau ở luồng đọc dữ liệu của ClientService.
   */
  public void login(String username, String password) {
    if (username == null || password == null) return;

    // Gửi chuỗi định dạng "user pass" để Server dễ dàng split(" ")
    String credentials = username.trim() + " " + password.trim();

    // Gửi gói tin có type là LOGIN
    clientService.sendRequest("LOGIN", username.trim() + " " + password.trim());  }

  /**
   * Gửi yêu cầu đăng ký tài khoản mới.
   */
  public void register(String username, String password) {
    if (username == null || password == null) return;

    String regData = username.trim() + " " + password.trim();

    // Gửi gói tin có type là REGISTER
    clientService.sendRequest("REGISTER", regData);
  }

  /**
   * Gửi yêu cầu nạp tiền.
   * Server sẽ nhận số tiền này và cập nhật vào Database cho User đang đăng nhập.
   */
  public void addBalance(BigDecimal amount) {
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return;

    // Gửi gói tin có type là ADD_BALANCE kèm theo số tiền (Object)
    clientService.sendRequest("ADD_BALANCE", amount);
  }

  /**
   * Gửi yêu cầu cập nhật thông tin cá nhân.
   * Truyền nguyên đối tượng User, Gson sẽ tự động biến thành JSON gửi đi.
   */
  public void updateUser(User user) {
    if (user == null) return;

    // Trước khi gửi, đảm bảo trạng thái profile được đánh dấu (fix lỗi symbol bạn gặp)
    user.setProfileComplete(true);

    // Gửi gói tin có type là UPDATE_PROFILE
    clientService.sendRequest("UPDATE_PROFILE", user);
  }

  /**
   * Yêu cầu Server gửi lại danh sách đấu giá mới nhất.
   */
  public void fetchAllAuctions() {
    clientService.sendRequest("GET_ALL_AUCTIONS", "");
  }
}