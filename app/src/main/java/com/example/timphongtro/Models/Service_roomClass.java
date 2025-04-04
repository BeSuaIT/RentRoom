package com.example.timphongtro.Models;
 
public class Service_roomClass {
    private String name;
    private String img;
    private String measures;
    private long price;

    public Service_roomClass() {
    }

    public Service_roomClass(String name, String img, String measures, long price) {
        this.name = name;
        this.img = img;
        this.measures = measures;
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImg() {
        return img; 
    }

    public void setImg(String img) {
        this.img = img;
    }

    public String getMeasures() {
        return measures;
    }

    public void setMeasures(String measures) {
        this.measures = measures;
    }

    public long getPrice() {
        return price;
    }

    public void setPrice(long price) {
        this.price = price;
    }
}
