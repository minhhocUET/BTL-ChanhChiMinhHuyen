package com.uet.bidding.dao;

import com.uet.bidding.model.Item;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ItemDAO {
  private static final String FILE_PATH = "items.dat";
  private List<Item> items;

  public ItemDAO() {
    // Nạp danh sách sản phẩm từ file khi khởi tạo Server
    this.items = loadData();
  }

  // --- HÀM BỔ TRỢ: LƯU DỮ LIỆU ---
  private synchronized void saveData() {
    try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_PATH))) {
      oos.writeObject(items);
    } catch (IOException e) {
      System.err.println("Lỗi khi lưu file sản phẩm: " + e.getMessage());
    }
  }

  // --- HÀM BỔ TRỢ: ĐỌC DỮ LIỆU ---
  @SuppressWarnings("unchecked")
  private List<Item> loadData() {
    File file = new File(FILE_PATH);
    if (!file.exists()) return new ArrayList<>();

    try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
      return (List<Item>) ois.readObject();
    } catch (Exception e) {
      return new ArrayList<>();
    }
  }

  // --- CÁC HÀM NGHIỆP VỤ ---

  /**
   * Thêm sản phẩm mới (Dùng sau khi ItemFactory tạo ra Object).
   */
  public synchronized void addItem(Item item) {
    // Tự động gán ID tăng dần
    int nextId = items.stream().mapToInt(Item::getId).max().orElse(0) + 1;
    item.setId(nextId);

    items.add(item);
    saveData();
    System.out.println("Đã thêm sản phẩm mới: " + item.getName());
  }

  /**
   * Lấy toàn bộ danh sách sản phẩm để hiển thị trên Dashboard.
   */
  public List<Item> getAllItems() {
    return new ArrayList<>(items);
  }

  /**
   * Tìm sản phẩm theo ID (Dùng khi bắt đầu tạo một phiên đấu giá cho sản phẩm đó).
   */
  public Item findById(int id) {
    for (Item item : items) {
      if (item.getId() == id) {
        return item;
      }
    }
    return null;
  }

  /**
   * Xóa sản phẩm (Nếu cần).
   */
  public synchronized void deleteItem(int id) {
    items.removeIf(item -> item.getId() == id);
    saveData();
  }
}
