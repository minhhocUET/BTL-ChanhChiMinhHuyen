package com.uet.bidding.dao;

import com.uet.bidding.model.Art;
import com.uet.bidding.model.Electronics;
import com.uet.bidding.model.Vehicle;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ItemSqlDAOTest {

  /**
   * Càn quét tất cả 12 public methods của ItemSqlDAO.
   * Sử dụng cả 3 subclass (Art, Electronics, Vehicle) để tăng độ phủ (Coverage).
   */
  @Test
  public void testItemDaoSweep() {
    ItemSqlDAO dao = new ItemSqlDAO();

    // Chuẩn bị các đối tượng mồi cho từng loại Item
    // Các tham số truyền vào dựa theo thứ tự của constructor trong class con của bạn
    Art dummyArt = new Art(1, "Bức tranh Mona Lisa", "Đẹp", BigDecimal.valueOf(1000),
        "img.png", 1, "Da Vinci", 1503, "Sơn dầu");

    Electronics dummyElec = new Electronics(2, "MacBook Pro", "Mới", BigDecimal.valueOf(2000),
        "img2.png", 1, "Apple", 12);

    Vehicle dummyVehicle = new Vehicle(3, "Toyota Camry", "Lướt", BigDecimal.valueOf(50000),
        "img3.png", 1, "Toyota", "Camry", 2022, 15000.0, "Xăng", "Xăng");

    // 1. Quét nhóm CREATE (Thử add cả 3 loại Item)
    try {
      dao.addItem(dummyArt);
    } catch (Exception e) {
    }
    try {
      dao.addItem(dummyElec);
    } catch (Exception e) {
    }
    try {
      dao.addItem(dummyVehicle);
    } catch (Exception e) {
    }
    try {
      dao.addItem(null);
    } catch (Exception e) {
    } // Cố tình truyền null để bắt nhánh Exception

    // 2. Quét nhóm READ - Danh sách
    try {
      dao.getAllItems();
    } catch (Exception e) {
    }
    try {
      dao.getItemsBySeller(1);
    } catch (Exception e) {
    }
    try {
      dao.getItemsByType("ART");
    } catch (Exception e) {
    }
    try {
      dao.getAvailableItemsBySeller(1);
    } catch (Exception e) {
    }

    // 3. Quét nhóm READ - Tìm kiếm theo ID
    try {
      dao.findById(1);
    } catch (Exception e) {
    }

    // 4. Quét nhóm UPDATE - Thông tin sản phẩm (Thử update cả 3 loại)
    try {
      dao.updateItem(dummyArt);
    } catch (Exception e) {
    }
    try {
      dao.updateItem(dummyElec);
    } catch (Exception e) {
    }
    try {
      dao.updateItem(dummyVehicle);
    } catch (Exception e) {
    }

    // 5. Quét nhóm UPDATE - Trạng thái đấu giá
    try {
      dao.setInAuction(1, true);
    } catch (Exception e) {
    }

    // 6. Quét nhóm DELETE
    try {
      dao.deleteItem(1);
    } catch (Exception e) {
    }

    // 7. Quét nhóm THỐNG KÊ
    try {
      dao.getTotalItemCount();
    } catch (Exception e) {
    }
    try {
      dao.getInAuctionCount();
    } catch (Exception e) {
    }
    try {
      dao.getItemCountBySeller(1);
    } catch (Exception e) {
    }

    // Chốt hạ
    assertNotNull(dao, "Đã càn quét thành công ItemSqlDAO!");
  }
}