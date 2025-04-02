package com.example.timphongtro.Entity;

import java.util.ArrayList;
import java.util.List;

public class Bill {
    private String id;
    private String userId;
    private ArrayList<Cart> items;
    private long totalAmount;
    private long timestamp;
    private int status; // 0: pending, 1: completed, 2: cancelled

    public Bill() {
    }

    public Bill(String id, String userId, ArrayList<Cart> items, long totalAmount, long timestamp) {
        this.id = id;
        this.userId = userId;
        this.items = items;
        this.totalAmount = totalAmount;
        this.timestamp = timestamp;
        this.status = 0;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public ArrayList<Cart> getItems() {
        return items;
    }

    public void setItems(ArrayList<Cart> items) {
        this.items = items;
    }

    public long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
