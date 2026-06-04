package com.uet.bidding.model;

import java.math.BigDecimal;

public abstract class Item extends Entity {

  private String name;
  private String description; // Mô tả chi tiết sản phẩm
  private BigDecimal startingPrice; // Giá khởi điểm
  private String imagePath; // legacy; prefer imageData
  private String imageData; // base64 image from DB
  private int sellerId;
  private String city;
  // Thuộc tính quan trọng để kiểm soát luồng đấu giá
  private boolean inAuction = false;

  // ─── THUỘC TÍNH MỚI BỔ SUNG ───────────────────────────────────────
  // Trạng thái phê duyệt: "PENDING" (Chờ duyệt), "APPROVED" (Đã duyệt), "REJECTED" (Bị từ chối)
  private String status = "PENDING";
  private String rejectionReason;

  // 🌟 THÊM DÒNG NÀY: Một biến type vật lý để Gson ở Server có thể nhìn thấy và đóng gói
  private String type;

  // Constructor dùng khi đọc từ dữ liệu
  public Item(int id, String name, String description, BigDecimal startingPrice, String imagePath, int sellerId) {
    super(); // Gọi constructor của Entity
    this.setId(id);
    this.name = name;
    this.description = description;
    this.startingPrice = startingPrice;
    this.imagePath = imagePath;
    this.sellerId = sellerId;
    this.status = "PENDING"; // Mặc định khi đọc hoặc khởi tạo
    // 🌟 THÊM DÒNG NÀY: Ép hàm abstract nạp giá trị vào biến type ngay khi tạo object
    this.type = this.getType();
  }

// Constructor dùng khi tạo mới (chưa có id)

  public Item(String name, String description, BigDecimal startingPrice, String imagePath, int sellerId) {
    this.name = name;
    this.description = description;
    this.startingPrice = startingPrice;
    this.imagePath = imagePath;
    this.sellerId = sellerId;
    this.status = "PENDING"; // Mặc định chờ duyệt
    // 🌟 THÊM DÒNG NÀY: Ép hàm abstract nạp giá trị vào biến type ngay khi tạo object
    this.type = this.getType();
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

  // ─── GETTER & SETTER CHO TYPE ─────────────────────────────────────

  public void setType(String type) {
    this.type = type;
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

  // ─── GETTER & SETTER CHO STATUS MỚI ───────────────────────────────

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getRejectionReason() {
    return rejectionReason;
  }

  public void setRejectionReason(String rejectionReason) {
    this.rejectionReason = rejectionReason;
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