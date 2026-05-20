package com.uet.bidding.model;

import java.math.BigDecimal;

public abstract class Item extends Entity {

  private String name;
  private String description; // Mô tả chi tiết sản phẩm
  private BigDecimal startingPrice; // Giá khởi điểm
  private String imagePath; // legacy; prefer imageData
  private String imageData; // base64 image stored in DB
  private int sellerId;
  private String city;
  // Thuộc tính quan trọng để kiểm soát luồng đấu giá
  private boolean inAuction = false;

// Constructor dùng khi đọc từ dữ liệu

  public Item(int id, String name, String description, BigDecimal startingPrice, String imagePath, int sellerId) {
    super(); // Gọi constructor của Entity
    this.setId(id);
    this.name = name;
    this.description = description;
    this.startingPrice = startingPrice;
    this.imagePath = imagePath;
    this.sellerId = sellerId;

  }

// Constructor dùng khi tạo mới (chưa có id)

  public Item(String name, String description, BigDecimal startingPrice, String imagePath, int sellerId) {
    this.name = name;
    this.description = description;
    this.startingPrice = startingPrice;
    this.imagePath = imagePath;
    this.sellerId = sellerId;
  }


// Các hàm Getters và Setters

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public BigDecimal getStartingPrice() {
    return startingPrice;
  }

  public void setStartingPrice(BigDecimal startingPrice) {
    this.startingPrice = startingPrice;
  }

  public String getImagePath() {
    return imagePath;
  }

  public void setImagePath(String imagePath) {
    this.imagePath = imagePath;
  }

  public String getImageData() {
    return imageData;
  }

  public void setImageData(String imageData) {
    this.imageData = imageData;
  }

  public int getSellerId() {
    return sellerId;
  }

  public void setSellerId(int sellerId) {
    this.sellerId = sellerId;
  }

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public boolean isInAuction() {
    return inAuction;
  }

  /**
   * Cập nhật trạng thái đấu giá của món hàng
   *
   * @param inAuction true nếu bắt đầu đấu giá, false nếu phiên đấu giá kết thúc hoặc bị hủy
   */
  public void setInAuction(boolean inAuction) {
    this.inAuction = inAuction;
  }

  public String getProductType() {
    if (this instanceof Electronics) return "Điện tử";
    if (this instanceof Art) return "Nghệ thuật";
    if (this instanceof Vehicle) return "Phương tiện";
    return "Khác";
  }
// ================= ABSTRACT =================

  /**
   * Trả về loại item (ART / ELECTRONICS / Vehicle)
   * -> dùng cho Factory
   */
  public abstract String getType();

  public String toString() {
    return "Item {" +
        "id = " + this.getId() +
        ", name = '" + name + '\'' +
        ", price = " + startingPrice +
        ", sellerId = " + sellerId +
        '}';
  }
}