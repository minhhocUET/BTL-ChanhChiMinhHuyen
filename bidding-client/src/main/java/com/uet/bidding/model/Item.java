package com.uet.bidding.model;

public abstract class Item {
    private int id;
    private String name;

    public Item(int id, String name) {
        this.id = id;
        this.name = name;
    }
    // Getter / Setter
    public int getId() { return id; }
    public String getName() { return name; }
}