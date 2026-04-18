package com.uet.bidding.model;

public class ItemFactory {
    
    // Phương thức Factory: Trả về một đối tượng Item dựa trên loại (type) đầu vào
    public static Item createItem(String type, int id, String name) {
        if (type.equalsIgnoreCase("ELECTRONICS")) {
            return new Electronics(id, name);
        } else if (type.equalsIgnoreCase("ART")) {
            return new Art(id, name);
        }
        // Có thể thêm Vehicle, Jewelry... sau này
        throw new IllegalArgumentException("Loại sản phẩm không hợp lệ: " + type);
    }
}