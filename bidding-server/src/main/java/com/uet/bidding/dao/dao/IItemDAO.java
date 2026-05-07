package com.uet.bidding.dao.dao;

import com.uet.bidding.model.Item;

import java.util.List;

public interface IItemDAO {
  void addItem(Item item) throws Exception;
  List<Item> getAllItems();
  Item findById(int id) throws Exception;
  void updateItem(Item item) throws Exception;
  void deleteItem(int id) throws Exception;
}
