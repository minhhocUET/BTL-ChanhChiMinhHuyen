package com.uet.bidding.service;

import com.uet.bidding.dao.IUserDAO;
import com.uet.bidding.dao.UserSqlDAO;
import com.uet.bidding.exception.AuthenticationException;
import com.uet.bidding.exception.UserException;
import com.uet.bidding.model.Bidder;
import com.uet.bidding.model.User;

import java.math.BigDecimal;

public class UserService {
  private IUserDAO userDAO = new UserSqlDAO();

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
    // ✅ MỚI ĐĂNG KÝ → PROFILE CHƯA HOÀN THÀNH
    newBidder.setProfileComplete(false);

    userDAO.addUser(newBidder);
  }

  public User login(String username, String password) throws AuthenticationException {
    if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
      throw new AuthenticationException("Vui lòng nhập đầy đủ tài khoản và mật khẩu!");
    }
    return userDAO.checkLogin(username, password);
  }

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
   * ✅ CẬP NHẬT: Set isProfileComplete = true khi đầy đủ info
   */
  public void updateUser(User user) throws UserException {
    // 1-5. Validation cũ...
    if (user.getFullName() == null || user.getFullName().trim().isEmpty()) {
      throw new UserException("Họ và tên không được để trống!");
    }
    if (user.getEmail() == null || user.getEmail().trim().isEmpty() || !user.getEmail().contains("@")) {
      throw new UserException("Email không hợp lệ!");
    }
    if (user.getPhone() == null || user.getPhone().trim().isEmpty() || !user.getPhone().matches("\\d+")) {
      throw new UserException("Số điện thoại không hợp lệ!");
    }
    if (user.getAddress() == null || user.getAddress().trim().isEmpty()) {
      throw new UserException("Địa chỉ không được để trống!");
    }
    if (user.getLinkedBank() == null || user.getLinkedBank().trim().isEmpty()) {
      throw new UserException("Vui lòng nhập thông tin liên kết ngân hàng!");
    }

    // ✅ SET PROFILE COMPLETE = TRUE
    user.setProfileComplete(true);

    // Lưu xuống DB
    userDAO.updateUser(user);
  }
}