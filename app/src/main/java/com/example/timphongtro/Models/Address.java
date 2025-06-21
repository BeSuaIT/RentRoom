package com.example.timphongtro.Models;

public class Address {
    private String city, district, detail, address_combine;

    public Address() {}

    public Address(String city, String district, String detail) {
        this.city = city;
        this.district = district;
        this.detail = detail != null ? detail.trim() : "";
        this.address_combine = generateAddressCombine();
    }

    public Address(String city, String district) {
        this(city, district, "");
    }

    public Address(String city, String district, String detail, String address_combine) {
        this.city = city;
        this.district = district;
        this.detail = detail != null ? detail.trim() : "";
        this.address_combine = generateAddressCombine();
    }

    private String generateAddressCombine() {
        if (detail == null || detail.trim().isEmpty()) {
            return district + ", " + city;
        } else {
            return detail.trim() + ", " + district + ", " + city;
        }
    }

    public String getCity() {
        return city != null ? city : "";
    }

    public String getDistrict() {
        return district != null ? district : "";
    }

    public String getDetail() {
        return detail != null ? detail : "";
    }

    public String getAddress_combine() {
        if (address_combine == null || address_combine.isEmpty()) {
            address_combine = generateAddressCombine();
        }
        return address_combine;
    }

    public void setCity(String city) {
        this.city = city;
        this.address_combine = generateAddressCombine();
    }

    public void setDistrict(String district) {
        this.district = district;
        this.address_combine = generateAddressCombine();
    }

    public void setDetail(String detail) {
        this.detail = detail != null ? detail.trim() : "";
        this.address_combine = generateAddressCombine();
    }

    public void setAddress_combine(String address_combine) {
        this.address_combine = generateAddressCombine();
    }

    public String getShortAddress() {
        return getDistrict() + ", " + getCity();
    }
}
