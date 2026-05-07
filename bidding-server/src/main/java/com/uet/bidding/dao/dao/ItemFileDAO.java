package com.uet.bidding.dao.dao;

import com.uet.bidding.model.Item;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ItemFileDAO {
  private static final String FILE_PATH = "items.dat";
  private List<Item> items;
  private AtomicInteger lastId;

  public ItemFileDAO() {
    this.items = loadData();
    int maxId = items.stream().mapToInt(Item::getId).max().orElse(0);
    this.lastId = new AtomicInteger(maxId);
  }

  private synchronized void saveData() {
    try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_PATH))) {
      oos.writeObject(items);
    } catch (IOException e) {
      System.err.println("Lỗi khi lưu file sản phẩm: " + e.getMessage());
    }
  }

  @SuppressWarnings("unchecked")
  private List<Item> loadData() {
    File file = new File(FILE_PATH);
    if (!file.exists()) return new ArrayList<>();

    try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
      return (List<Item>) ois.readObject();
    } catch (Exception e) {
      System.err.println("Cảnh báo: Lỗi khi đọc file items.dat!");
      e.printStackTrace();
      return new ArrayList<>();
    }
  }

  public synchronized void addItem(Item item) {
    item.setId(lastId.incrementAndGet());
    items.add(item);
    saveData();
    System.out.println("Đã thêm sản phẩm mới: " + item.getName());
  }

  // SỬA: Thêm synchronized
  public synchronized List<Item> getAllItems() {
    return new ArrayList<>(items);
  }

  // SỬA: Thêm synchronized
  public synchronized Item findById(int id) {
    for (Item item : items) {
      if (item.getId() == id) {
        return item;
      }
    }
    return null;
  }

  // BỔ SUNG: Hàm cập nhật sản phẩm
  public synchronized void updateItem(Item updatedItem) {
    for (int i = 0; i < items.size(); i++) {
      if (items.get(i).getId() == updatedItem.getId()) {
        items.set(i, updatedItem);
        saveData();
        System.out.println("Đã cập nhật sản phẩm: " + updatedItem.getName());
        return;
      }
    }
    System.err.println("Không tìm thấy sản phẩm có ID: " + updatedItem.getId());
  }

  public synchronized void deleteItem(int id) {
    boolean isRemoved = items.removeIf(item -> item.getId() == id);
    if (isRemoved) {
      saveData();
      System.out.println("Đã xóa sản phẩm ID: " + id);
    }
  }
}