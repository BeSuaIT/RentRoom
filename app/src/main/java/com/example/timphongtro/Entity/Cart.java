package com.example.timphongtro.Entity;

public class Cart {
    private String serviceId;
    private int amount;

    public Cart(String serviceId, int amount) {
        this.serviceId = serviceId;
        this.amount = amount;
    }

    public Cart() {
        // Required empty constructor for Firebase
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }
}