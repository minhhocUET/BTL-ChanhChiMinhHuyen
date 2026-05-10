package com.uet.bidding.model;

import java.io.Serializable;
import java.math.BigDecimal;

public abstract class User extends Entity implements Serializable {
  //Dòng này cố định phiên bản file, tránh lỗi khi sửa code sau này
  private static final long serialVersionUID = 1L;

  private String username;
  private String password;
  private String fullName;
  private String email;
  private String phone;
  private String address;
  private BigDecimal balance = BigDecimal.ZERO;
  private String linkedBank;
  private boolean isProfileComplete = false; //Mặc định là FAlSE

  public User() {
  } // Constructor trống bắt buộc

  //constructor không có id
  public User(String username, String password, BigDecimal balance) {
    this.username = username;
    this.password = password;
    this.balance = balance;
  }

  //constructor có id
  public User(int id, String username, String password, BigDecimal balance) {
    super(); // Gọi constructor của Entity
    this.setId(id);
    this.username = username;
    this.password = password;
    this.balance = balance;
  }

  // Cần có đầy đủ Setter để LoginController và Bidder hoạt động

  // Cần có Getter để UserProfileController hiển thị dữ liệu
  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public BigDecimal getBalance() {
    return balance;
  }

  public void setBalance(BigDecimal balance) {
    this.balance = balance;
  }

  public String getLinkedBank() {
    return linkedBank;
  }

  public void setLinkedBank(String linkedBank) {
    this.linkedBank = linkedBank;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  } // Đã bổ sung dòng này để fix lỗi

  public boolean isProfileComplete() {
    return isProfileComplete;
  }

  public void setProfileComplete(boolean profileComplete) {
    isProfileComplete = profileComplete;
  }

  // THÊM METHOD CHECK ĐẦY ĐỦ INFO
  public boolean hasCompleteProfile() {
    return fullName != null && !fullName.trim().isEmpty() &&
        phone != null && !phone.trim().isEmpty() &&
        address != null && !address.trim().isEmpty() &&
        linkedBank != null && !linkedBank.trim().isEmpty();
  }

  // Hàm nạp tiền dùng cho màn hình UserProfile
  public void addFunds(BigDecimal amount) {
    this.balance = this.balance.add(amount);
  }

  public abstract String getRole();

  public boolean withdraw(BigDecimal amount) {
    if (this.balance != null && this.balance.compareTo(amount) >= 0) {
      this.balance = this.balance.subtract(amount);
      return true;
    }
    return false;
  }
}