package com.uet.bidding.model;

// Tính Kế thừa: Bidder thừa hưởng mọi thứ từ User
public class Bidder extends User {
    
    public Bidder(int id, String username, String password, double balance) {
        super(id, username, password, balance); // Gọi constructor của lớp cha
    }

    @Override
    public String getRole() {
        return "BIDDER";
    }

    // Hành động đặc thù của người mua
    public void placeBid(Auction auction, double amount) {
        // Logic đặt giá sẽ được xử lý ở các tuần sau
        System.out.println(getUsername() + " đặt giá " + amount + " cho món đồ!");
    }
}