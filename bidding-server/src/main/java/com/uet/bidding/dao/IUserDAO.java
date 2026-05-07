package com.uet.bidding.dao;

import com.uet.bidding.exception.AuthenticationException;
import com.uet.bidding.exception.UserException;
import com.uet.bidding.model.User;

import java.util.List;

public interface IUserDAO {

  //CREATE: Thêm mới người dùng
  void addUser(User user) throws UserException;

  //READ: Lấy thông tin người dùng
  List<User> getAllUsers();
  User findById(int id) throws UserException;

  //UPDATE: Cập nhật thông tin người dùng (khi nạp tiền, trừ tiền bid, sửa profile)
  void updateUser(User user) throws UserException;

  //DELETE: Xóa người dùng (Admin dùng)
  void deleteUser(int id) throws UserException;

  //============== Logic nghiệp vụ ==============

  //Kiểm tra đăng nhập
  User checkLogin(String username, String password) throws AuthenticationException;
}
