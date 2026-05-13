package com.uet.bidding.model;

import java.math.BigDecimal;

public class Customer extends User {

    private String fullName;
    private String email;
    private String phone;
    private String address;
    private BigDecimal balance = BigDecimal.ZERO;
    private String linkedBank;
    private boolean isProfileComplete = false;

    // --- THÊM HAI THUỘC TÍNH VAI TRÒ ---
    private Bidder bidderProfile;
    private Seller sellerProfile;

    public Customer() {
        // Khởi tạo sẵn profile để tránh NullPointerException
        this.bidderProfile = new Bidder();
        this.sellerProfile = new Seller();
    }

    // Constructor không có id
    public Customer(String username, String password, BigDecimal balance) {
        this(); // Gọi constructor trống để khởi tạo profile
        this.setUsername(username);
        this.setPassword(password);
        this.balance = balance;
    }

    // Constructor có id
    public Customer(int id, String username, String password, BigDecimal balance) {
        this(); // Gọi constructor trống để khởi tạo profile
        this.setId(id);
        this.setUsername(username);
        this.setPassword(password);
        this.balance = balance;
    }

    /**
     * SỬA LỖI: Triển khai phương thức abstract từ lớp User
     * Việc này giúp lớp Customer không còn bị báo lỗi "is not abstract and does not override..."
     */
    @Override
    public String getRole() {
        return "CUSTOMER";
    }

    // --- GETTER CHO CÁC VAI TRÒ ---
    public Bidder getBidderProfile() {
        return bidderProfile;
    }

    public Seller getSellerProfile() {
        return sellerProfile;
    }

    // Có thể thêm hàm kiểm tra nhanh vai trò nếu cần
    public boolean isActiveSeller() {
        return sellerProfile != null && sellerProfile.getStoreName() != null;
    }

    // --- CÁC GETTER/SETTER CŨ GIỮ NGUYÊN ---
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public String getLinkedBank() { return linkedBank; }
    public void setLinkedBank(String linkedBank) { this.linkedBank = linkedBank; }

    public boolean isProfileComplete() { return isProfileComplete; }
    public void setProfileComplete(boolean profileComplete) { isProfileComplete = profileComplete; }

    public boolean hasCompleteProfile() {
        return fullName != null && !fullName.trim().isEmpty() &&
            phone != null && !phone.trim().isEmpty() &&
            address != null && !address.trim().isEmpty() &&
            linkedBank != null && !linkedBank.trim().isEmpty();
    }

    public void addFunds(BigDecimal amount) {
        if (amount != null) {
            this.balance = this.balance.add(amount);
        }
    }

    public boolean withdraw(BigDecimal amount) {
        if (this.balance != null && amount != null && this.balance.compareTo(amount) >= 0) {
            this.balance = this.balance.subtract(amount);
            return true;
        }
        return false;
    }
}