package com.uet.bidding.service;

import com.uet.bidding.model.Customer;
import com.uet.bidding.model.NetworkMessage;
import com.uet.bidding.model.User;
import com.uet.bidding.network.ClientService;
import com.uet.bidding.util.UserSession;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

/**
 * UserService (Client-side)
 * <p>
 * Nguyên tắc mật khẩu đã được sửa lại:
 * - Client gửi plain-text cho CẢ login lẫn register
 * - Server chịu trách nhiệm băm BCrypt khi register
 * - Server chịu trách nhiệm BCrypt.checkpw() khi login
 * <p>
 * Lý do KHÔNG băm tại Client:
 * - Băm tại Client rồi Server băm lại = double hash → checkpw() luôn false
 * - BCrypt.gensalt() mỗi lần tạo salt khác nhau → hash gửi lên ≠ hash trong DB
 */
public class UserService {
  private final ClientService clientService = ClientService.getInstance();

  /**
   * Đăng nhập: gửi plain-text, Server dùng BCrypt.checkpw() để xác thực.
   */
  public CompletableFuture<NetworkMessage> login(String username, String password) {
    if (username == null || password == null) return CompletableFuture.completedFuture(null);
    return clientService.sendRequest("LOGIN", username.trim() + " " + password);
  }

  public CompletableFuture<NetworkMessage> register(String username, String password) {
    if (username == null || password == null) return CompletableFuture.completedFuture(null);
    return clientService.sendRequest("REGISTER", username.trim() + " " + password);
  }

  public void addBalance(BigDecimal amount) {
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return;
    clientService.sendRequest("ADD_BALANCE", amount);
  }

  public void updateUser(User user) {
    if (user == null) return;
    if (user instanceof Customer customer) {
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