package com.example.timphongtro.Models;

import java.util.ArrayList;

public class ImagesRoomClass {
    private ArrayList<String> images;

    public ImagesRoomClass() {
        this.images = new ArrayList<>();
    }

    public ImagesRoomClass(ArrayList<String> images) {
        this.images = images;
    }

    public ArrayList<String> getImages() {
        return images;
    }

    public void setImages(ArrayList<String> images) {
        this.images = images;
    }

    public void addImage(String imageUrl) {
        if (this.images == null) {
            this.images = new ArrayList<>();
        }
        this.images.add(imageUrl);
    }

    public String getFirstImage() {
        return images != null && !images.isEmpty() ? images.get(0) : "";
    }
}