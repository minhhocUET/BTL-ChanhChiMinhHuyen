package com.uet.bidding.service;

import com.uet.bidding.model.Customer;
import com.uet.bidding.model.User;
import com.uet.bidding.network.ClientService;
import com.uet.bidding.util.UserSession;
import org.mindrot.jbcrypt.BCrypt;

import java.math.BigDecimal;

public class UserService {
  private ClientService clientService = ClientService.getInstance();

  /**
   * Đăng nhập phía Client: Chỉ gửi username và mật khẩu thuần lên để Server đối sánh BCrypt
   */
  public void login(String username, String password) {
    if (username == null || password == null || username.trim().isEmpty() || password.isEmpty()) return;

    // Giữ nguyên gửi chuỗi thuần, Server sẽ dùng BCrypt.checkpw để xác thực dưới DAO
    clientService.sendRequest("LOGIN", username.trim() + " " + password.trim());
  }

  /**
   * Đăng ký phía Client: Thực hiện băm mật khẩu ngay tại chỗ trước khi truyền qua mạng
   */
  public void register(String username, String password) {
    if (username == null || password == null || username.trim().isEmpty() || password.length() < 6) return;

    // 1. Thực hiện mã hóa băm mật khẩu bằng BCrypt ngay tại Client
    // Hàm gensalt(12) tạo chuỗi muối ngẫu nhiên để chống tấn công bảng băm (Rainbow Table)
    String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(12));

    // 2. Đóng gói chuỗi đã mã hóa gửi qua mạng sang Server xử lý chèn DB
    // Server nhận được chuỗi "$2a$12$..." này chỉ việc INSERT thẳng vào bảng users mà không cần băm lại
    clientService.sendRequest("REGISTER", username.trim() + " " + hashedPassword);

    System.out.println("Client đã băm bảo mật mật khẩu thành công trước khi gửi mạng.");
  }

  public void addBalance(BigDecimal amount) {
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return;
    clientService.sendRequest("ADD_BALANCE", amount);
  }

  public void updateUser(User user) {
    if (user == null) return;
    if (user instanceof Customer customer) {
      // Tận dụng hàm kiểm tra profile có sẵn của em để đồng bộ trạng thái thực thể
      if (customer.hasCompleteProfile()) {
        customer.setProfileComplete(true);
      }
      clientService.sendRequest("UPDATE_PROFILE", customer);
    } else {
      clientService.sendRequest("UPDATE_PROFILE", user);
    }
  }

  public void logout() {
    clientService.sendRequest("LOGOUT", "");
    UserSession.clear();
  }
}
