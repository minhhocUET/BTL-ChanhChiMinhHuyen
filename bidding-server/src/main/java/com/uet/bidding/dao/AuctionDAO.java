package com.uet.bidding.dao;

import com.uet.bidding.model.Auction;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class AuctionDAO {
  private static final String FILE_PATH = "auctions.dat";
  private List<Auction> auctions;

  public AuctionDAO() {
    this.auctions = loadData();
  }

  // --- HÀM BỔ TRỢ ---
  private synchronized void saveData() {
    try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_PATH))) {
      oos.writeObject(auctions);
    } catch (IOException e) {
      System.err.println("Lỗi lưu file đấu giá: " + e.getMessage());
    }
  }

  @SuppressWarnings("unchecked")
  private List<Auction> loadData() {
    File file = new File(FILE_PATH);
    if (!file.exists()) return new ArrayList<>();
    try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
      return (List<Auction>) ois.readObject();
    } catch (Exception e) {
      return new ArrayList<>();
    }
  }

  // --- CÁC HÀM NGHIỆP VỤ ---

  public synchronized void addAuction(Auction auction) {
    int nextId = auctions.stream().mapToInt(Auction::getId).max().orElse(0) + 1;
    auction.setId(nextId);
    auctions.add(auction);
    saveData();
  }

  public List<Auction> getAllAuctions() {
    return new ArrayList<>(auctions);
  }

  /**
   * Cập nhật một phiên đấu giá (Dùng khi có người đặt giá mới).
   */
  public synchronized void updateAuction(Auction updatedAuction) {
    for (int i = 0; i < auctions.size(); i++) {
      if (auctions.get(i).getId() == updatedAuction.getId()) {
        auctions.set(i, updatedAuction);
        saveData();
        return;
      }
    }
  }
}
