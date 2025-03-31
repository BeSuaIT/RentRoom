package com.example.timphongtro.Entity;

public class District {
    private String id_district, img_district, name;

    public District() {
    }
    public District(String id_district, String img_district, String name) {
        this.id_district = id_district;
        this.img_district = img_district;
        this.name = name;
    }

    public String getId_district() {
        return id_district;
    }

    public void setId_district(String id_district) {
        this.id_district = id_district;
    }

    public String getImg_district() {
        return img_district;
    }

    public void setImg_district(String img_district) {
        this.img_district = img_district;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
