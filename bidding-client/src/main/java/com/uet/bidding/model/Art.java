package com.uet.bidding.model;

import java.math.BigDecimal;

public class Art extends Item {

  private static final long serialVersionUID = 1L;

  private String author;
  private int creationYear;
  private String material;

  public Art(int id, String name, String description, BigDecimal startingPrice, String imagePath, int sellerId,
             String author, int creationYear, String material) {
    super(id, name, description, startingPrice, imagePath, sellerId);
    this.author = author;
    this.creationYear = creationYear;
    this.material = material;
  }

  public Art(String name, String description, BigDecimal startingPrice, String imagePath, int sellerId,
             String author, int creationYear, String material) {
    super(name, description, startingPrice, imagePath, sellerId);
    this.author = author;
    this.creationYear = creationYear;
    this.material = material;
  }

  public String getAuthor() { return author; }
  public void setAuthor(String author) { this.author = author; }

  public int getCreationYear() { return creationYear; }
  public void setCreationYear(int creationYear) { this.creationYear = creationYear; }

  public String getMaterial() { return material; }
  public void setMaterial(String material) { this.material = material; }

  @Override // Thêm dòng này để báo cho Java biết đây là hàm ghi đè từ lớp cha
  public String getType() { return "ART"; }
}