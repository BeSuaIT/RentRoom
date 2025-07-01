package com.example.timphongtro.Models;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Cart {
    private String cartId;
    private String buyerId;
    private String sellerId;
    private Map<String, Integer> cartItems;

    public Cart() {
        this.cartItems = new HashMap<>();
    }

    public Cart(String buyerId, String sellerId) {
        this.cartId = generateCartId();
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.cartItems = new HashMap<>();
    }

    public Cart(String cartId, String buyerId, String sellerId) {
        this.cartId = cartId;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.cartItems = new HashMap<>();
    }

    public static String generateCartId() {
        return "CART_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    public void addOrUpdateItem(String serviceId, int amount) {
        if (cartItems.containsKey(serviceId)) {
            int currentAmount = cartItems.get(serviceId);
            cartItems.put(serviceId, currentAmount + amount);
        } else {
            cartItems.put(serviceId, amount);
        }
    }

    public String getCartId() {
        return cartId;
    }

    public void setCartId(String cartId) {
        this.cartId = cartId;
    }

    public String getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(String buyerId) {
        this.buyerId = buyerId;
    }

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public Map<String, Integer> getCartItems() {
        return cartItems != null ? cartItems : new HashMap<>();
    }

    public void setCartItems(Map<String, Integer> cartItems) {
        this.cartItems = cartItems != null ? cartItems : new HashMap<>();
    }
}