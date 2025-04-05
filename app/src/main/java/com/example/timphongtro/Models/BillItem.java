package com.example.timphongtro.Models;

public class BillItem {
    private String title;
    private int quantity;
    private long price;

    public BillItem(String title, int quantity, long price) {
        this.title = title;
        this.quantity = quantity;
        this.price = price;
    }

    // Getters
    public String getTitle() { return title; }
    public int getQuantity() { return quantity; }
    public long getPrice() { return price; }
    public long getTotal() { return price * quantity; }
}
