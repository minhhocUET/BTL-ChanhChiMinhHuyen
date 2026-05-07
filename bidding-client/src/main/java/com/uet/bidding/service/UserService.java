package com.uet.bidding.service;

import com.uet.bidding.dao.IUserDAO;
import com.uet.bidding.dao.UserSqlDAO;
import com.uet.bidding.exception.AuthenticationException;
import com.uet.bidding.exception.UserException;
import com.uet.bidding.model.Bidder;
import com.uet.bidding.model.User;

import java.math.BigDecimal;

public class UserService {
  // Khởi tạo DAO giao tiếp với TiDB/MySQL
  private IUserDAO userDAO = new UserSqlDAO();

  /**
   * Nghiệp vụ Đăng ký (Rút gọn: Chỉ cần Username và Password)
   */
  public void registerBidder(String username, String password) throws UserException {
    if (username.length() < 5) {
      throw new UserException("Tên đăng nhập phải có ít nhất 5 ký tự!");
    }
    if (password.length() < 6) {
      throw new UserException("Mật khẩu phải từ 6 ký tự trở lên!");
    }

    Bidder newBidder = new Bidder();
    newBidder.setUsername(username);
    newBidder.setPassword(password);
    newBidder.setTotalBids(0);
    newBidder.setAuctionsWon(0);

    userDAO.addUser(newBidder);
  }

  /**
   * Nghiệp vụ Đăng nhập
   */
  public User login(String username, String password) throws AuthenticationException {
    if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
      throw new AuthenticationException("Vui lòng nhập đầy đủ tài khoản và mật khẩu!");
    }
    return userDAO.checkLogin(username, password);
  }

  /**
   * Nghiệp vụ Nạp tiền vào tài khoản
   */
  public void addBalance(User user, BigDecimal amount) throws UserException {
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new UserException("Số tiền nạp phải lớn hơn 0!");
    }

    BigDecimal currentBalance = user.getBalance();
    if (currentBalance == null) currentBalance = BigDecimal.ZERO;

    BigDecimal newBalance = currentBalance.add(amount);
    user.setBalance(newBalance);

    userDAO.updateUser(user);
  }

  /**
   * Nghiệp vụ Cập nhật thông tin người dùng (Bắt buộc nhập đủ 100% thông tin)
   */
  public void updateUser(User user) throws UserException {
    // 1. Kiểm tra Họ và Tên
    if (user.getFullName() == null || user.getFullName().trim().isEmpty()) {
      throw new UserException("Họ và tên không được để trống!");
    }

    // 2. Kiểm tra Email (Vừa check rỗng, vừa check định dạng có chữ @)
    if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
      throw new UserException("Email không được để trống!");
    } else if (!user.getEmail().contains("@")) {
      throw new UserException("Định dạng Email không hợp lệ!");
    }

    // 3. Kiểm tra Số điện thoại (Check rỗng và check chỉ chứa số)
    if (user.getPhone() == null || user.getPhone().trim().isEmpty()) {
      throw new UserException("Số điện thoại không được để trống!");
    } else if (!user.getPhone().matches("\\d+")) {
      throw new UserException("Số điện thoại chỉ được chứa các chữ số!");
    }

    // 4. Kiểm tra Địa chỉ
    if (user.getAddress() == null || user.getAddress().trim().isEmpty()) {
      throw new UserException("Địa chỉ không được để trống!");
    }

    // 5. Kiểm tra Ngân hàng liên kết
    if (user.getLinkedBank() == null || user.getLinkedBank().trim().isEmpty()) {
      throw new UserException("Vui lòng nhập thông tin liên kết ngân hàng!");
    }

    user.setProfileComplete(true);
    // NẾU VƯỢT QUA ĐƯỢC TOÀN BỘ 5 BÀI TEST TRÊN -> Mới cho phép cập nhật xuống CSDL
    userDAO.updateUser(user);
  }
}