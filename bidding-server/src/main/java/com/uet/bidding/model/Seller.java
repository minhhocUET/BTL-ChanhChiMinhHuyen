package com.uet.bidding.model;

import java.math.BigDecimal;

public class Seller extends User {

  private static final long serialVersionUID = 1L;

  // Các thuộc tính riêng biệt của người bán
  private Double rating;    // Điểm đánh giá uy tín (ví dụ: 4.8)
  private String taxId;     // Mã số thuế hoặc CCCD để xác minh
  private String shopName;  // Tên gian hàng của người bán

  // ================= CONSTRUCTORS =================

  // Constructor rỗng (Bắt buộc phải có để đọc dữ liệu từ DB/File)
  public Seller() {
    super(); // Tự động gọi constructor rỗng của User
  }

  /**
   * Constructor dùng khi tạo mới người bán (chưa có ID từ DB)
   */
  public Seller(String username, String password, BigDecimal balance,
                Double rating, String taxId, String shopName) {
    // Truyền các thuộc tính cơ bản lên cho class cha (User)
    super(username, password, balance);

    // Gán các thuộc tính riêng của Seller
    this.rating = rating;
    this.taxId = taxId;
    this.shopName = shopName;
  }

  /**
   * Constructor dùng khi đọc dữ liệu người bán từ Database lên (đã có ID)
   */
  public Seller(int id, String username, String password, BigDecimal balance,
                Double rating, String taxId, String shopName) {
    // Truyền các thuộc tính lên constructor có ID của class cha
    super(id, username, password, balance);

    this.rating = rating;
    this.taxId = taxId;
    this.shopName = shopName;
  }

  // ================= OVERRIDE =================

  /**
   * Ghi đè hàm getRole() của User để trả về quyền cụ thể.
   * Cực kỳ quan trọng để phân quyền trong LoginController
   */
  @Override
  public String getRole() {
    return "SELLER";
  }

  // ================= GETTERS AND SETTERS =================

  public Double getRating() {
    return rating;
  }

  public void setRating(Double rating) {
    this.rating = rating;
  }

  public String getTaxId() {
    return taxId;
  }

  public void setTaxId(String taxId) {
    this.taxId = taxId;
  }

  public String getShopName() {
    return shopName;
  }

  public void setShopName(String shopName) {
    this.shopName = shopName;
  }

  // ================= TO STRING =================

  @Override
  public String toString() {
    return "Seller {" +
            "id = " + getId() + // Mặc dù User không có hàm getId() trong code bạn gửi, nhưng nó kế thừa từ Entity nên chắc chắn sẽ có hàm này
            ", username = '" + getUsername() + '\'' +
            ", shopName = '" + shopName + '\'' +
            ", rating = " + rating +
            ", balance = " + getBalance() +
            '}';
  }
}
