package com.uet.bidding.model;

import java.io.Serializable;
import java.math.BigDecimal;

public abstract class Item implements Serializable {

  private static final long serialVersionUID = 1L;

  private int id;
  private String name;
  private String description;    // Mô tả chi tiết sản phẩm
  private BigDecimal startingPrice;  // Giá khởi điểm
  private String imagePath;      // Link hoặc đường dẫn tới ảnh
  private int sellerId;

  // Constructor dùng khi đọc từ dữ liệu
  public Item(int id, String name, String description, BigDecimal startingPrice, String imagePath, int  sellerId) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.startingPrice = startingPrice;
    this.imagePath = imagePath;
    this.sellerId = sellerId;
  }

  // Constructor dùng khi tạo mới (chưa có id)
  public Item(String name, String description, BigDecimal  startingPrice, String imagePath, int sellerId) {
    this.name = name;
    this.description = description;
    this.startingPrice = startingPrice;
    this.imagePath = imagePath;
    this.sellerId = sellerId;
  }

  // Các hàm Getters và Setters
  public int getId() { return id; }
  public void setId(int id) { this.id = id; }

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }

  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }

  public BigDecimal getStartingPrice() { return startingPrice; }
  public void setStartingPrice(BigDecimal startingPrice) { this.startingPrice = startingPrice; }

  public String getImagePath() { return imagePath; }
  public void setImagePath(String imagePath) { this.imagePath = imagePath; }

  public int getSellerId() { return sellerId; }
  public void setSellerId(int sellerId) { this.sellerId = sellerId; }

  // ================= ABSTRACT =================
  /**
   * Trả về loại item (ART / ELECTRONICS
   * -> dùng cho Factory
   */
  public abstract String getType();

  public String toString() {
    return "Item {" +
        "id = " + id +
        ", name = '" + name + '\'' +
        ", price = " + startingPrice +
        ", sellerId = " + sellerId +
        '}';
  }
}