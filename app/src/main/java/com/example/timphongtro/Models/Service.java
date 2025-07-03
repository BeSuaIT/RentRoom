package com.example.timphongtro.Models;

import java.util.ArrayList;
import java.util.Map;

public class Service {
    private String serviceId, description, id_seller, title, category;
    private ArrayList<String> images;
    private int price, sold, amount;
    private long createdAt;

    public Service() {
    }

    public Service(String serviceId, long createdAt, String description, String id_seller,
                   String title, String category, ArrayList<String> images, int price, int sold, int amount) {
        this.serviceId = serviceId;
        this.createdAt = createdAt;
        this.description = description;
        this.id_seller = id_seller;
        this.title = title;
        this.category = category;
        this.images = images;
        this.price = price;
        this.sold = sold;
        this.amount = amount;
    }

    public String getServiceId() { return serviceId; }
    public void setServiceId(String serviceId) { this.serviceId = serviceId; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getId_seller() { return id_seller; }
    public void setId_seller(String id_seller) { this.id_seller = id_seller; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }

    public int getSold() { return sold; }
    public void setSold(int sold) { this.sold = sold; }

    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }

    public ArrayList<String> getImages() { return images; }

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

    public boolean hasImages() {
        return images != null && !images.isEmpty();
    }
}