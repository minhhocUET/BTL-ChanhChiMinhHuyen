package com.uet.bidding.model;

public class Art extends Item {
  private String author;       // Tên tác giả
  private int creationYear;    // Năm sáng tác
  private String material;     // Chất liệu (VD: Sơn dầu, lụa...)

  // Constructor gọi super() để nạp dữ liệu cho class cha
  public Art(int id, String name, String description, double startingPrice, String imagePath,
             String author, int creationYear, String material) {
    super(id, name, description, startingPrice, imagePath); // Gọi Constructor của Item
    this.author = author;
    this.creationYear = creationYear;
    this.material = material;
  }

  // Các hàm Getters và Setters riêng của Art
  public String getAuthor() { return author; }
  public void setAuthor(String author) { this.author = author; }

  public int getCreationYear() { return creationYear; }
  public void setCreationYear(int creationYear) { this.creationYear = creationYear; }

  public String getMaterial() { return material; }
  public void setMaterial(String material) { this.material = material; }
}