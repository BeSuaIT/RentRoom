package com.example.timphongtro.Models;

import java.util.ArrayList;

public class CartItem {
    private String serviceId;
    private String title;
    private String sellerId;
    private int amount;
    private int price;
    private ArrayList<String> images;
    private String cartId;

    public CartItem() {
    }

    public CartItem(String serviceId, String title, String sellerId, int amount, int price, ArrayList<String> images, String cartId) {
        this.serviceId = serviceId;
        this.title = title;
        this.sellerId = sellerId;
        this.amount = amount;
        this.price = price;
        this.images = images;
        this.cartId = cartId;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public ArrayList<String> getImages() {
        return images;
    }

    public void setImages(ArrayList<String> images) {
        this.images = images;
    }

    public String getCartId() {
        return cartId;
    }

    public void setCartId(String cartId) {
        this.cartId = cartId;
    }
}