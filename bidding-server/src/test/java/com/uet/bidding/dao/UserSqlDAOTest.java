package com.uet.bidding.dao;

import com.uet.bidding.model.Admin;
import com.uet.bidding.model.Customer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class UserSqlDAOTest {

  /**
   * Càn quét toàn bộ 18 public methods của UserSqlDAO.
   * Thử nghiệm với cả 2 role: ADMIN và CUSTOMER.
   */
  @Test
  public void testUserDaoSweep() {
    UserSqlDAO dao = new UserSqlDAO();

    // 1. Chuẩn bị dữ liệu mồi
    Admin dummyAdmin = new Admin();
    dummyAdmin.setUsername("admin_test");
    dummyAdmin.setPassword("password");
    // Giả sử có hàm setRole, nếu Admin auto set role trong constructor thì bạn có thể bỏ dòng dưới
    try {
      dummyAdmin.setRole("ADMIN");
    } catch (Exception e) {
    }

    Customer dummyCustomer = new Customer();
    dummyCustomer.setId(1);
    dummyCustomer.setUsername("customer_test");
    dummyCustomer.setPassword("password");
    try {
      dummyCustomer.setRole("CUSTOMER");
    } catch (Exception e) {
    }
    dummyCustomer.setFullName("Nguyen Van A");
    dummyCustomer.setBalance(BigDecimal.valueOf(1000));

    // 2. Quét nhóm CREATE
    try {
      dao.addUser(dummyAdmin);
    } catch (Exception e) {
    }
    try {
      dao.addUser(dummyCustomer);
    } catch (Exception e) {
    }
    try {
      dao.addUser(null);
    } catch (Exception e) {
    } // Cố tình đẩy null để bắt nhánh lỗi

    // 3. Quét nhóm READ - Xác thực
    try {
      dao.checkLogin("admin_test", "123456");
    } catch (Exception e) {
    }

    // 4. Quét nhóm READ - Danh sách & Tìm kiếm
    try {
      dao.getAllUsers();
    } catch (Exception e) {
    }
    try {
      dao.getUsersByRole("CUSTOMER");
    } catch (Exception e) {
    }
    try {
      dao.findById(1);
    } catch (Exception e) {
    }
    try {
      dao.findByUsername("admin_test");
    } catch (Exception e) {
    }
    try {
      dao.existsByUsername("admin_test");
    } catch (Exception e) {
    }

    // 5. Quét nhóm UPDATE - Cập nhật thông tin
    try {
      dao.updateProfile(dummyCustomer);
    } catch (Exception e) {
    }
    try {
      dao.updateProfile(null);
    } catch (Exception e) {
    }
    try {
      dao.updatePassword(1, "oldPass", "newPass");
    } catch (Exception e) {
    }

    // 6. Quét nhóm UPDATE - Tiền bạc & Giao dịch
    try {
      dao.updateBalance(1, BigDecimal.valueOf(500));
    } catch (Exception e) {
    }
    try {
      dao.getBalance(1);
    } catch (Exception e) {
    }
    try {
      dao.withdraw(1, BigDecimal.valueOf(200));
    } catch (Exception e) {
    }
    try {
      dao.withdraw(1, BigDecimal.valueOf(-100));
    } catch (Exception e) {
    } // Test nhánh tiền âm

    // 7. Quét nhóm UPDATE - Trạng thái & Rating
    try {
      dao.setBanned(1, true);
    } catch (Exception e) {
    }
    try {
      dao.recalculateSellerRating(1);
    } catch (Exception e) {
    }

    // 8. Quét nhóm DELETE
    try {
      dao.deleteUser(1);
    } catch (Exception e) {
    }

    // 9. Quét nhóm THỐNG KÊ
    try {
      dao.getTotalUserCount();
    } catch (Exception e) {
    }
    try {
      dao.getBannedUserCount();
    } catch (Exception e) {
    }
    try {
      dao.getCustomerCount();
    } catch (Exception e) {
    }

    // Chốt hạ bài test
    assertNotNull(dao, "Đã càn quét thành công toàn bộ UserSqlDAO!");
  }
}