package com.uet.bidding.model;

import java.math.BigDecimal;

// Tính Trừu tượng: Dùng abstract vì không có "User" chung chung, chỉ có Bidder, Seller hoặc Admin
public abstract class User {
    // Tính Đóng gói: Các thuộc tính đều là private
    private int id;
    private String username;
    private String password;
    private BigDecimal balance;

    // Constructor
    public User(int id, String username, String password, BigDecimal balance) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.balance = balance;
    }

    // Các hàm Getter/Setter để truy xuất an toàn
    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public String getPassword() {
        return password;
    }

    // Tính Đa hình (Polymorphism): Phương thức ảo để các lớp con tự định nghĩa
    public abstract String getRole();

    public boolean withdraw(BigDecimal amount) {
        if (this.balance != null && this.balance.compareTo(amount) >= 0) {
            this.balance = this.balance.subtract(amount);
            return true;
        }
        return false;
    }
}
