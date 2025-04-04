package com.example.timphongtro.Models;

public class Address {
    String city, district, detail, ward, address_combine;

    Address() {

    }

    public Address(String city, String district, String detail, String ward, String address_combine) {
        this.city = city;
        this.district = district;
        this.detail = detail;
        this.ward = ward;
        this.address_combine = address_combine;
    }

    public Address(String city, String district, String detail, String ward) {
        this.city = city;
        this.district = district;
        this.detail = detail;
        this.ward = ward;
        this.address_combine = detail + ", " + district + ", " + city;
    }

    public Address(String city, String district) {
        this.city = city;
        this.district = district;
        this.address_combine = district + ", " + city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getWard() {
        return ward;
    }

    public void setWard(String ward) {
        this.ward = ward;
    }

    public String getAddress_combine() {
        return address_combine;
    }

    public void setAddress_combine(String address_combine) {
        this.address_combine = address_combine;
    }

    public String getCity() {
        return city;
    }

    public String getDetail() {
        return detail;
    }

    public String getDistrict() {
        return district;
    }


}
