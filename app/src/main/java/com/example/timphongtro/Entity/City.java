package com.example.timphongtro.Entity;

import java.util.ArrayList;
import java.util.List;

public class City {
    private String id_city;
    private String name;
    private ArrayList<District> districts;

    public City() {
    }
    public City(String id_city, String name, ArrayList<District> districts) {
        this.id_city = id_city;
        this.name = name;
        this.districts = districts;
    }

    public String getId_city() {
        return id_city;
    }

    public void setId_city(String id_city) {
        this.id_city = id_city;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<District> getDistricts() {
        return districts;
    }

    public void setDistricts(ArrayList<District> districts) {
        this.districts = districts;
    }
}
