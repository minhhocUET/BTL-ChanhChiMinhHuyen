package com.uet.bidding.service;

import com.uet.bidding.dao.UserSqlDAO;
import com.uet.bidding.exception.AuthenticationException;
import com.uet.bidding.exception.UserException;
import com.uet.bidding.model.Bidder;
import com.uet.bidding.model.Customer;
import com.uet.bidding.model.Seller;
import com.uet.bidding.model.User;

import java.math.BigDecimal;

public class UserService {
  // Khởi tạo DAO (Dùng interface để linh hoạt)
  private UserSqlDAO userDAO = new UserSqlDAO();

  /**
   * Đăng ký Customer mới (Mặc định là vai trò khách hàng)
   */
  public void registerCustomer(String username, String password) throws UserException {
    if (username.length() < 5) {
      throw new UserException("Tên đăng nhập phải có ít nhất 5 ký tự!");
    }
    if (password.length() < 6) {
      throw new UserException("Mật khẩu phải từ 6 ký tự trở lên!");
    }

    // Tạo Customer (Constructor của Customer đã tự khởi tạo Bidder/Seller profile)
    Customer newCustomer = new Customer(username, password, BigDecimal.ZERO);

    userDAO.addUser(newCustomer);
  }

  /**
   * Nghiệp vụ Đăng nhập
   */
  public User login(String username, String password) throws AuthenticationException {
    if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
      throw new AuthenticationException("Vui lòng nhập đầy đủ tài khoản và mật khẩu!");
    }

    User user = userDAO.checkLogin(username, password);

    // Kiểm tra xem có bị Admin khóa không (Sử dụng isBanned ở lớp User cha)
    if (user != null && user.isBanned()) {
      throw new AuthenticationException("Tài khoản của bạn đã bị khóa bởi quản trị viên!");
    }

    return user;
  }

  /**
   * Nghiệp vụ Nạp tiền (Chỉ dành cho Customer)
   */
  public void addBalance(User user, BigDecimal amount) throws UserException {
    // Kiểm tra đa hình: Chỉ Customer mới có ví tiền
    if (!(user instanceof Customer)) {
      throw new UserException("Tài khoản này không hỗ trợ chức năng nạp tiền!");
    }

    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new UserException("Số tiền nạp phải lớn hơn 0!");
    }

    Customer customer = (Customer) user; // Ép kiểu để sử dụng các hàm của Customer
    customer.addFunds(amount);

    userDAO.updateProfile(customer);
  }

  /**
   * Cập nhật thông tin hồ sơ (Chỉ dành cho Customer)
   */
  public void updateCustomerProfile(User user) throws UserException {
    if (!(user instanceof Customer)) {
      throw new UserException("Người dùng không phải là khách hàng!");
    }

    Customer customer = (Customer) user;

    // Validation logic
    if (isEmpty(customer.getFullName())) throw new UserException("Họ và tên không được để trống!");
    if (isEmpty(customer.getEmail()) || !customer.getEmail().contains("@"))
      throw new UserException("Email không hợp lệ!");
    if (isEmpty(customer.getPhone()) || !customer.getPhone().matches("\\d+"))
      throw new UserException("Số điện thoại không hợp lệ!");
    if (isEmpty(customer.getAddress())) throw new UserException("Địa chỉ không được để trống!");
    customer.setProfileComplete(true);
    userDAO.updateProfile(customer);
  }

  /**
   * Nghiệp vụ Đăng ký trở thành Người bán (Mở cửa hàng)
   */
  public void registerSellerProfile(User user, String storeName, String description) throws UserException {
    if (!(user instanceof Customer)) {
      throw new UserException("Chỉ khách hàng mới có thể mở cửa hàng!");
    }

    Customer customer = (Customer) user;

    // Bắt buộc phải cập nhật đủ thông tin cá nhân thì mới cho bán hàng
    if (!customer.isProfileComplete()) {
      throw new UserException("Vui lòng cập nhật đầy đủ thông tin cá nhân (Họ tên, SĐT, Địa chỉ) trước khi mở cửa hàng!");
    }

    if (isEmpty(storeName)) {
      throw new UserException("Tên cửa hàng không được để trống!");
    }

    // Lấy profile Seller ra và cập nhật thông tin
    Seller sellerProfile = customer.getSellerProfile();
    sellerProfile.setStoreName(storeName);
    sellerProfile.setDescription(description);

    // Lưu lại vào Database
    userDAO.updateProfile(customer);
  }

  /**
   * Nghiệp vụ Đăng ký tham gia một phiên đấu giá (Của Bidder)
   * Thường gọi khi người dùng bấm nút "Đăng ký tham gia" trên giao diện
   */
  public void joinAuction(User user, int auctionId) throws UserException {
    if (!(user instanceof Customer)) {
      throw new UserException("Chỉ khách hàng mới có thể tham gia đấu giá!");
    }

    Customer customer = (Customer) user;

    // Bắt buộc phải cập nhật thông tin cá nhân thì mới cho mua hàng
    if (!customer.isProfileComplete()) {
      throw new UserException("Vui lòng cập nhật thông tin cá nhân trước khi tham gia đấu giá!");
    }

    Bidder bidderProfile = customer.getBidderProfile();

    // Dùng hàm bạn đã viết sẵn trong class Bidder
    bidderProfile.registerForAuction(auctionId);

    // Lưu lại vào Database
    userDAO.updateProfile(customer);
  }

  // Hàm tiện ích kiểm tra chuỗi rỗng
  private boolean isEmpty(String str) {
    return str == null || str.trim().isEmpty();
  }
}