package com.uet.bidding.service;

import com.uet.bidding.dao.IUserDAO;
import com.uet.bidding.dao.UserSqlDAO; // Hoặc dùng DAOFactory như mình gợi ý ở trên
import com.uet.bidding.exception.AuthenticationException;
import com.uet.bidding.exception.UserException;
import com.uet.bidding.model.Bidder;
import com.uet.bidding.model.User;

import java.math.BigDecimal;

public class UserService {
  // Tạm thời khởi tạo cứng UserSqlDAO để bạn test với TiDB.
  private IUserDAO userDAO = new UserSqlDAO();

  /**
   * Nghiệp vụ Đăng ký (Mặc định đăng ký mới sẽ là người mua - Bidder)
   */
  public void registerBidder(String username, String password, String fullName, String email) throws UserException {
    // 1. Kiểm tra dữ liệu đầu vào (Validation)
    if (username.length() < 5) {
      throw new UserException("Tên đăng nhập phải có ít nhất 5 ký tự!");
    }
    if (password.length() < 6) {
      throw new UserException("Mật khẩu phải từ 6 ký tự trở lên!");
    }
    if (!email.contains("@")) {
      throw new UserException("Email không hợp lệ!");
    }

    // 2. Tạo đối tượng Bidder mới
    Bidder newBidder = new Bidder();
    newBidder.setUsername(username);
    newBidder.setPassword(password); // Thực tế người ta sẽ mã hóa MD5/Bcrypt ở đây
    newBidder.setFullName(fullName);
    newBidder.setEmail(email);
    newBidder.setTotalBids(0);
    newBidder.setAuctionsWon(0);

    // 3. Gọi DAO để lưu vào CSDL
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
    // SỬA LỖI: Kiểm tra amount <= 0 dùng compareTo
    // amount.compareTo(BigDecimal.ZERO) <= 0 nghĩa là số tiền nạp nhỏ hơn hoặc bằng 0
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new UserException("Số tiền nạp phải lớn hơn 0!");
    }

    // SỬA LỖI: Tính toán số dư mới bằng phương thức .add()
    BigDecimal currentBalance = user.getBalance();
    if (currentBalance == null) currentBalance = BigDecimal.ZERO;

    BigDecimal newBalance = currentBalance.add(amount);
    user.setBalance(newBalance);

    // Cập nhật xuống Database (UserSqlDAO đã nhận tham số User có balance là BigDecimal)
    userDAO.updateUser(user);
  }

  /**
   * Nghiệp vụ Cập nhật thông tin người dùng
   */
  public void updateUser(User user) throws UserException {
    // Có thể thêm các validate logic ở đây nếu cần (vd: check format email, phone)
    if (user.getFullName() == null || user.getFullName().trim().isEmpty()) {
      throw new UserException("Họ tên không được để trống!");
    }
    if (user.getEmail() == null || !user.getEmail().contains("@")) {
      throw new UserException("Email không hợp lệ!");
    }

    userDAO.updateUser(user);
  }
}