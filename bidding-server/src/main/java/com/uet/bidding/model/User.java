package com.uet.bidding.model;

import java.math.BigDecimal;

public abstract class User extends Entity {

  private String username;
  private String password;

  public User() {
  } // Constructor trống bắt buộc

  //constructor không có id
  public User(String username, String password, BigDecimal balance) {
    this.username = username;
    this.password = password;
  }

  //constructor có id
  public User(int id, String username, String password, BigDecimal balance) {
    super(); // Gọi constructor của Entity
    this.setId(id);
    this.username = username;
    this.password = password;
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
  }
}

