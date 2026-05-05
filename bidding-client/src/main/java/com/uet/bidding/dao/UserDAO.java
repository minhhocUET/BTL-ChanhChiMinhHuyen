package com.uet.bidding.dao;

import com.uet.bidding.model.Seller;
import com.uet.bidding.model.User;

import java.io.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {
  private static final String FILE_PATH = "users.dat";
  private List<User> users;

  public UserDAO() {
    this.users = loadData();

    // Tự động tạo dữ liệu mẫu nếu hệ thống chưa có ai.
    if (this.users.isEmpty()) {
      System.out.println("File dữ liệu trống. Đang tạo tài khoản mẫu...");
      // Lưu ý: pass các thông số ID, Username, Password, Balance cho khớp Constructor của bạn.
      addUser(new Seller(
          "hoang_an_99",              // username
          "matkhau123",               // password
          new BigDecimal("5000000"),  // balance
          4.9,                        // rating
          "0312456789",               // taxId
          "An Hoàng Luxury Watch"     // shopName
      ));
      addUser(new Seller(
          "hoang_an_99",              // username
          "matkhau123",               // password
          new BigDecimal("5000000"),  // balance
          4.9,                        // rating
          "0312456789",               // taxId
          "An Hoàng Luxury Watch"     // shopName
      ));
    }
  }

  // --- HÀM BỔ TRỢ: ĐỌC/GHI FILE ---

  private synchronized void saveData() {
    try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_PATH))) {
      oos.writeObject(users);
    } catch (IOException e) {
      System.err.println("Lỗi khi lưu file người dùng: " + e.getMessage());
    }
  }

  @SuppressWarnings("unchecked")
  private List<User> loadData() {
    File file = new File(FILE_PATH);
    if (!file.exists()) return new ArrayList<>();

    try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
      return (List<User>) ois.readObject();
    } catch (Exception e) {
      return new ArrayList<>();
    }
  }

  // --- CÁC HÀM NGHIỆP VỤ (SỬA DỰA TRÊN CODE CŨ) ---

  /**
   * Thêm user mới vào danh sách và lưu lại file.
   */
  public synchronized void addUser(User user) {
    // Tự động tạo ID (lấy ID lớn nhất + 1) tương đương AUTO_INCREMENT trong SQL.
    int nextId = users.stream().mapToInt(User::getId).max().orElse(0) + 1;
    user.setId(nextId);

    users.add(user);
    saveData(); // Lưu lại file ngay lập tức
    System.out.println("Đã thêm người dùng: " + user.getUsername());
  }

  /**
   * Kiểm tra đăng nhập (Thay thế câu lệnh SELECT * WHERE...).
   */
  public User checkLogin(String username, String password) {
    for (User u : users) {
      if (u.getUsername().equals(username) && u.getPassword().equals(password)) {
        return u;
      }
    }
    return null;
  }

  /**
   * Lấy toàn bộ danh sách user.
   */
  public List<User> getAllUsers() {
    return new ArrayList<>(users); // Trả về bản sao để an toàn dữ liệu
  }

  /**
   * Tìm user theo ID.
   */
  public User findById(int id) {
    for (User u : users) {
      if (u.getId() == id) {
        return u;
      }
    }
    return null;
  }
}
