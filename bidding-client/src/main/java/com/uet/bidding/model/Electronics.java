package com.uet.bidding.model;

import java.math.BigDecimal;

public class Electronics extends Item {

  private static final long serialVersionUID = 1L;

  private String brand;
  private int warrantyMonths;

  // Constructor 1: khi đọc dữ liệu
  public Electronics(int id, String name, String description, BigDecimal startingPrice, String imagePath, int sellerId,
                     String brand, int warrantyMonths) {
    super(id, name, description, startingPrice, imagePath, sellerId);
    this.brand = brand;
    this.warrantyMonths = warrantyMonths;
  }

  // Constructor 2: khi tạo mới (Đã sửa lỗi quên gán warrantyMonths)
  public Electronics(String name, String description, BigDecimal startingPrice, String imagePath, int sellerId,
                     String brand, int warrantyMonths) {
    super(name, description, startingPrice, imagePath, sellerId);
    this.brand = brand;
    this.warrantyMonths = warrantyMonths; // THÊM DÒNG NÀY
  }

  // Các hàm Getters và Setters
  public String getBrand() { return brand; }
  public void setBrand(String brand) { this.brand = brand; }

  public int getWarrantyMonths() { return warrantyMonths; }
  public void setWarrantyMonths(int warrantyMonths) { this.warrantyMonths = warrantyMonths; }

  @Override
  public String getType() { return "ELECTRONICS"; }
}