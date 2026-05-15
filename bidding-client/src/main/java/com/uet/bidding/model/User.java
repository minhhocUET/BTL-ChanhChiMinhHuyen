package com.uet.bidding.model;

/**
 * Lớp User trừu tượng - Lớp cơ sở (Base Class)
 * Loại bỏ Serialization, sẵn sàng cho GSON/JSON
 */
public abstract class User extends Entity {

  private String username;
  private String password;
  private boolean isBanned = false; // Thuộc tính bắt buộc để Admin thực hiện quản lý (Mục 3.1.1)

  private String role;
  public User() {
  }

  public User(String username, String password) {
    this.username = username;
    this.password = password;
  }

  public User(int id, String username, String password) {
    this.setId(id);
    this.username = username;
    this.password = password;
  }

  // --- Getters & Setters ---
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
  }

  public boolean isBanned() {
    return isBanned;
  }

  public void setBanned(boolean banned) {
    isBanned = banned;
  }

  /**
   * Phương thức trừu tượng để phân định vai trò.
   * Admin sẽ trả về "ADMIN", Customer sẽ trả về "CUSTOMER".
   */
  public abstract String getRole();

  public void setRole(String role) {
    this.role = role;
  }
}