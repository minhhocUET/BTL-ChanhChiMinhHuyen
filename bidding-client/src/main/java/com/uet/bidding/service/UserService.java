package com.uet.bidding.service;

import com.uet.bidding.model.Customer;
import com.uet.bidding.model.User;
import com.uet.bidding.network.ClientService;
import com.uet.bidding.util.UserSession;

import java.math.BigDecimal;

public class UserService {
  private ClientService clientService = ClientService.getInstance();

  public void login(String username, String password) {
    if (username == null || password == null || username.isEmpty()) return;
    clientService.sendRequest("LOGIN", username.trim() + " " + password.trim());
  }

  public void register(String username, String password) {
    if (username == null || password == null || username.isEmpty()) return;
    clientService.sendRequest("REGISTER", username.trim() + " " + password.trim());
  }

  public void addBalance(BigDecimal amount) {
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return;
    clientService.sendRequest("ADD_BALANCE", amount);
  }

  public void updateUser(User user) {
    if (user == null) return;
    if (user instanceof Customer customer) {
      if (customer.hasCompleteProfile()) customer.setProfileComplete(true);
      clientService.sendRequest("UPDATE_PROFILE", customer);
    } else {
      clientService.sendRequest("UPDATE_PROFILE", user);
    }
  }

  public void logout() {
    clientService.sendRequest("LOGOUT", "");
    UserSession.clear();
  }
}