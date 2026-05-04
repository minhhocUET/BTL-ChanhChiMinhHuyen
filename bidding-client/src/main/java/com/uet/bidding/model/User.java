package com.uet.bidding.model;

public class User {
  private int id;
  private String username;
  private String password;
  private String fullName;
  private String email;
  private String phone;
  private String address;
  private double balance;
  private String linkedBank;

  public User() {} // Constructor trống bắt buộc

  // Cần có đầy đủ Setter để LoginController và Bidder hoạt động
  public void setId(int id) { this.id = id; }
  public void setUsername(String username) { this.username = username; }
  public void setPassword(String password) { this.password = password; } // Đã bổ sung dòng này để fix lỗi
  public void setFullName(String fullName) { this.fullName = fullName; }
  public void setEmail(String email) { this.email = email; }
  public void setPhone(String phone) { this.phone = phone; }
  public void setAddress(String address) { this.address = address; }
  public void setBalance(double balance) { this.balance = balance; }
  public void setLinkedBank(String linkedBank) { this.linkedBank = linkedBank; }

  // Cần có Getter để UserProfileController hiển thị dữ liệu
  public String getFullName() { return fullName; }
  public String getEmail() { return email; }
  public String getPhone() { return phone; }
  public String getAddress() { return address; }
  public double getBalance() { return balance; }
  public String getLinkedBank() { return linkedBank; }
  public String getUsername() { return username; }

  // Hàm nạp tiền dùng cho màn hình UserProfile
  public void addFunds(double amount) { this.balance += amount; }
}