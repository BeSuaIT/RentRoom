package com.example.timphongtro.Models;

import java.util.ArrayList;
import java.util.Map;

public class Bill {
    private String id;
    private String userId;
    private String sellerId;
    private Map<String, Integer> items;
    private long totalAmount;
    private long orderDate;
    private int status;
    private String city;
    private String district;
    private String paymentMethod;
    private String detailAddress;

    public Bill() {
    }

    public Bill(String id, String userId, String sellerId, Map<String, Integer> items, long totalAmount, long orderDate, int status, String city, String district, String paymentMethod, String detailAddress) {
        this.id = id;
        this.userId = userId;
        this.sellerId = sellerId;
        this.items = items;
        this.totalAmount = totalAmount;
        this.orderDate = orderDate;
        this.status = status;
        this.city = city;
        this.district = district;
        this.paymentMethod = paymentMethod;
        this.detailAddress = detailAddress;
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

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public Map<String, Integer> getItems() {
        return items;
    }

    public void setItems(Map<String, Integer> items) {
        this.items = items;
    }

    public long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public long getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(long orderDate) {
        this.orderDate = orderDate;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getDetailAddress() {
        return detailAddress;
    }

    public void setDetailAddress(String detailAddress) {
        this.detailAddress = detailAddress;
    }
}
