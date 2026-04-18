package com.uet.bidding.model;

import java.math.BigDecimal;

/**
 * Abstract class Item
 * - Đại diện cho sản phẩm đấu giá
 * - Không tạo trực tiếp, chỉ dùng qua Art / Electronics
 */
public abstract class Item {

    protected int id;
    protected String name;
    protected String description;
    protected BigDecimal startingPrice;
    protected int sellerId;

    /**
     * Constructor dùng khi đọc từ DB (có id)
     */
    public Item(int id, String name, String description, BigDecimal startingPrice, int sellerId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.sellerId = sellerId;
    }

    /**
     * Constructor dùng khi tạo mới (chưa có id)
     */
    public Item(String name, String description, BigDecimal startingPrice, int sellerId) {
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.sellerId = sellerId;
    }

    // ================== GETTER ==================

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getStartingPrice() {
        return startingPrice;
    }

    public int getSellerId() {
        return sellerId;
    }

    // ================== SETTER ==================

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStartingPrice(BigDecimal startingPrice) {
        this.startingPrice = startingPrice;
    }

    // ================== ABSTRACT ==================

    /**
     * Trả về loại item (ART / ELECTRONICS)
     * → dùng cho Factory + DB
     */
    public abstract String getType();

    // ================== DEBUG ==================

    @Override
    public String toString() {
        return "Item{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", price=" + startingPrice +
                ", sellerId=" + sellerId +
                '}';
    }
}