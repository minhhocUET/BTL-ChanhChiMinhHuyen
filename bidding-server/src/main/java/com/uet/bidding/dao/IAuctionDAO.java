package com.uet.bidding.dao;

import com.uet.bidding.model.Auction;
import com.uet.bidding.model.AuctionState;

import java.util.List;

public interface IAuctionDAO {
  void addAuction(Auction auction) throws Exception;
  List<Auction> getAllAuctions();
  List<Auction> getAuctionsByStatus(AuctionState status); // Rất quan trọng để hiển thị lên GUI
  Auction findById(int id) throws Exception;
  void updateAuction(Auction auction) throws Exception;
}