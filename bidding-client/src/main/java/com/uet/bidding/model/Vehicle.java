package com.uet.bidding.model;

import java.math.BigDecimal;

public class Vehicle extends Item {

  // Các thuộc tính riêng của Vehicle (giống trong ảnh của bạn).
  private String brand;              // Hãng xe
  private String model;              // Dòng xe
  private Integer manufacturingYear; // Năm sản xuất
  private Double mileage;            // Số KM đã đi
  private String engineType;         // Loại động cơ
  private String fuelType;           // Loại nhiên liệu

  // ================= CONSTRUCTORS =================.

  /**
   * Constructor dùng khi đọc từ DB (đã có id).
   */
  public Vehicle(int id, String name, String description, BigDecimal startingPrice, String imagePath, int sellerId,
                 String brand, String model, Integer manufacturingYear, Double mileage, String engineType, String fuelType) {

    // Truyền 6 tham số lên cho class cha Item.
    super(id, name, description, startingPrice, imagePath, sellerId);

    // Gán giá trị cho các thuộc tính riêng của Vehicle.
    this.brand = brand;
    this.model = model;
    this.manufacturingYear = manufacturingYear;
    this.mileage = mileage;
    this.engineType = engineType;
    this.fuelType = fuelType;
  }

  /**
   * Constructor dùng khi tạo mới (chưa có id).
   */
  public Vehicle(String name, String description, BigDecimal startingPrice, String imagePath, int sellerId,
                 String brand, String model, Integer manufacturingYear, Double mileage, String engineType, String fuelType) {

    // Truyền 5 tham số lên cho class cha Item (không có id).
    super(name, description, startingPrice, imagePath, sellerId);

    this.brand = brand;
    this.model = model;
    this.manufacturingYear = manufacturingYear;
    this.mileage = mileage;
    this.engineType = engineType;
    this.fuelType = fuelType;
  }

  // ================= OVERRIDE ABSTRACT METHODS =================.

  @Override
  public String getType() {
    return "VEHICLE";
  }

  // ================= GETTERS AND SETTERS =================.

  public String getBrand() {
    return brand;
  }

  public void setBrand(String brand) {
    this.brand = brand;
  }

  public String getModel() {
    return model;
  }

  public void setModel(String model) {
    this.model = model;
  }

  public Integer getManufacturingYear() {
    return manufacturingYear;
  }

  public void setManufacturingYear(Integer manufacturingYear) {
    this.manufacturingYear = manufacturingYear;
  }

  public Double getMileage() {
    return mileage;
  }

  public void setMileage(Double mileage) {
    this.mileage = mileage;
  }

  public String getEngineType() {
    return engineType;
  }

  public void setEngineType(String engineType) {
    this.engineType = engineType;
  }

  public String getFuelType() {
    return fuelType;
  }

  public void setFuelType(String fuelType) {
    this.fuelType = fuelType;
  }

  // ================= TO STRING =================

  @Override
  public String toString() {
    return "Vehicle {" +
        "id = " + getId() +
        ", name = '" + getName() + '\'' +
        ", brand = '" + brand + '\'' +
        ", model = '" + model + '\'' +
        ", year = " + manufacturingYear +
        ", startingPrice = " + getStartingPrice() +
        ", sellerId = " + getSellerId() +
        '}';
  }
}