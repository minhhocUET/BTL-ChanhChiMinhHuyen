package com.uet.bidding.model;

// Tính Trừu tượng: Dùng abstract vì không có "User" chung chung, chỉ có Bidder, Seller hoặc Admin
public abstract class User {
    // Tính Đóng gói: Các thuộc tính đều là private
    private int id;
    private String username;
    private String password;
    private double balance;

    // Constructor
    public User(int id, String username, String password, double balance) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.balance = balance;
    }

    // Các hàm Getter/Setter để truy xuất an toàn
    public int getId() { return id; }
    public String getUsername() { return username; }
    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    // Tính Đa hình (Polymorphism): Phương thức ảo để các lớp con tự định nghĩa
    public abstract String getRole();
}