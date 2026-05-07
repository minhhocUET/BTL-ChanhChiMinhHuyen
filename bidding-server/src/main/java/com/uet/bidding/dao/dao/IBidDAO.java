package com.uet.bidding.dao.dao;

import com.uet.bidding.model.Bid;

import java.util.List;

public interface IBidDAO {
  void addBid(Bid bid) throws Exception;
  List<Bid> getBidsByAuction(int auctionId); // Lấy lịch sử của 1 phiên
  List<Bid> getBidsByUser(int userId);       // Xem người này đã đấu giá những gì
  Bid getHighestBid(int auctionId);          // Lấy giá cao nhất hiện tại
}

