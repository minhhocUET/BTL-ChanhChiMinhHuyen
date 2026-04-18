package com.uet.bidding.model;

public class Seller extends User {

    public Seller(int id, String username, String password, double balance) {
        super(id, username, password, balance);
    }

    @Override
    public String getRole() {
        return "SELLER";
    }

    // Hành động đặc thù của người bán
    public void createItem(String itemName, String description) {
        System.out.println(getUsername() + " vừa tạo món đồ mới: " + itemName);
    }
}