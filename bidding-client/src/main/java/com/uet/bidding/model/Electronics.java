package com.uet.bidding.model;

public class Electronics extends Item {
  private String brand;         // Thương hiệu (VD: Apple, Dell...)
  private int warrantyMonths;   // Số tháng bảo hành

  // Constructor
  public Electronics(int id, String name, String description, double startingPrice, String imagePath,
                     String brand, int warrantyMonths) {
    super(id, name, description, startingPrice, imagePath);
    this.brand = brand;
    this.warrantyMonths = warrantyMonths;
  }

  // Các hàm Getters và Setters riêng của Electronics
  public String getBrand() { return brand; }
  public void setBrand(String brand) { this.brand = brand; }

  public int getWarrantyMonths() { return warrantyMonths; }
  public void setWarrantyMonths(int warrantyMonths) { this.warrantyMonths = warrantyMonths; }
}