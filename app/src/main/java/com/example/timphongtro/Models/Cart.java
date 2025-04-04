package com.example.timphongtro.Models;

import java.util.ArrayList;
import java.util.Map;

public class Cart {
    private String serviceId, title, id_seller;
    private int amount, price;
    private ArrayList<String> images;

    public Cart(String serviceId, String title, String id_seller, int amount, int price, ArrayList<String> images) {
        this.serviceId = serviceId;
        this.title = title;
        this.id_seller = id_seller;
        this.amount = amount;
        this.price = price;
        this.images = images;
    }

    public Cart() {
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

    public String getId_seller() {
        return id_seller;
    }

    public void setId_seller(String id_seller) {
        this.id_seller = id_seller;
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

    public void setImages(Object imagesObj) {
        if (imagesObj == null) {
            this.images = new ArrayList<>();
        } else if (imagesObj instanceof ArrayList) {
            this.images = (ArrayList<String>) imagesObj;
        } else if (imagesObj instanceof Map) {
            Map<String, String> map = (Map<String, String>) imagesObj;
            this.images = new ArrayList<>(map.values());
        } else {
            this.images = new ArrayList<>();
        }
    }
}