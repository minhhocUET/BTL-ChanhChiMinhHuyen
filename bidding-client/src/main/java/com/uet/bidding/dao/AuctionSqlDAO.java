package com.uet.bidding.dao;

import com.uet.bidding.model.Auction;
import com.uet.bidding.model.AuctionState;

import java.util.List;

public class AuctionSqlDAO implements IAuctionDAO {
  public void addAuction(Auction auction) throws Exception {
  }

  public List<Auction> getAllAuctions() {
    return null;
  }

  public List<Auction> getAuctionsByStatus(AuctionState status) { // Rất quan trọng để hiển thị lên GUI
    return null;
  }

  public Auction findById(int id) throws Exception {
    return null;
  }

  public void updateAuction(Auction auction) throws Exception {
  }
}
