package com.uet.bidding.model;

public abstract class Item {
  private int id;
  private String name;
  private String description;    // Mô tả chi tiết sản phẩm
  private double startingPrice;  // Giá khởi điểm
  private String imagePath;      // Link hoặc đường dẫn tới ảnh

  // Constructor
  public Item(int id, String name, String description, double startingPrice, String imagePath) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.startingPrice = startingPrice;
    this.imagePath = imagePath;
  }

  // Các hàm Getters và Setters
  public int getId() { return id; }
  public void setId(int id) { this.id = id; }

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }

  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }

  public double getStartingPrice() { return startingPrice; }
  public void setStartingPrice(double startingPrice) { this.startingPrice = startingPrice; }

  public String getImagePath() { return imagePath; }
  public void setImagePath(String imagePath) { this.imagePath = imagePath; }
}