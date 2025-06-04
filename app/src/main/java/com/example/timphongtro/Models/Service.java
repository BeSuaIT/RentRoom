package com.example.timphongtro.Models;

import java.util.ArrayList;
import java.util.Map;

public class Service {
    private String serviceId, createAt, description, id_own_post, id_seller, title;
    private ArrayList<String> images;
    private int price, sold, amount;

    public Service(int sold, String serviceId, String createAt, String description, String id_own_post, String id_seller, String title, ArrayList<String> images, int price, int amount) {
        this.sold = sold;
        this.serviceId = serviceId;
        this.createAt = createAt;
        this.description = description;
        this.id_own_post = id_own_post;
        this.id_seller = id_seller;
        this.title = title;
        this.images = images;
        this.price = price;
        this.amount = amount;
    }

    public Service() {
    }

    public int getSold() {
        return sold;
    }

    public void setSold(int sold) {
        this.sold = sold;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getCreateAt() {
        return createAt;
    }

    public void setCreateAt(String createAt) {
        this.createAt = createAt;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getId_own_post() {
        return id_own_post;
    }

    public void setId_own_post(String id_own_post) {
        this.id_own_post = id_own_post;
    }

    public String getId_seller() {
        return id_seller;
    }

    public void setId_seller(String id_seller) {
        this.id_seller = id_seller;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public ArrayList<String> getImages() {
        return images;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
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

    public String getFirstImage() {
        if (images != null && !images.isEmpty()) {
            return images.get(0);
        }
        return null;
    }

    public boolean hasImages() {
        return images != null && !images.isEmpty();
    }
}