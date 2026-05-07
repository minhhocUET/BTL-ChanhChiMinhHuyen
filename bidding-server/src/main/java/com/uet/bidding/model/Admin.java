package com.uet.bidding.model;

public class Admin extends User {
  private Integer adminLevel;
  private String department;

  // Getters và Setters

  public Integer getAdminLevel() {
    return adminLevel;
  }

  public void setAdminLevel(Integer adminLevel) {
    this.adminLevel = adminLevel;
  }

  public String getDepartment() { return department; }

  public void setDepartment(String department) {
    this.department = department;
  }
}