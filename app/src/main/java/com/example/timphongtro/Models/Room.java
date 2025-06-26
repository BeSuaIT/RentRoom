package com.example.timphongtro.Models;

import java.util.ArrayList;

public class Room {
    private String id_own_post, id_room, title_room, description_room, gender_room, phone, type_room;
    private long price_room, deposit_room, price_electric, price_water, price_internet;
    private Address address;
    private int park_slot, person_in_room, status_room, floor, loveCount, area_room;
    private ArrayList<String> images;
    private Object userLovePost;
    private ArrayList<Furniture> roomFurniture;
    private ArrayList<Utility> roomUtilities;

    public Room() {
    }

    public Room(String id_own_post, String id_room, String title_room, long price_room, Address address, int area_room, long deposit_room,
                String description_room, String gender_room, int park_slot, int person_in_room, int status_room, String type_room, String phone,
                int floor, ArrayList<String> images, ArrayList<Furniture> roomFurniture, ArrayList<Utility> roomUtilities,
                long price_electric, long price_water, long price_internet) {
        this.id_own_post = id_own_post;
        this.id_room = id_room;
        this.title_room = title_room;
        this.price_room = price_room;
        this.address = address;
        this.area_room = area_room;
        this.deposit_room = deposit_room;
        this.description_room = description_room;
        this.gender_room = gender_room;
        this.park_slot = park_slot;
        this.person_in_room = person_in_room;
        this.status_room = status_room;
        this.type_room = type_room;
        this.phone = phone;
        this.floor = floor;
        this.price_electric = price_electric;
        this.price_water = price_water;
        this.price_internet = price_internet;
        this.images = images;
        this.roomFurniture = roomFurniture;
        this.roomUtilities = roomUtilities;
    }

    public long getPrice_electric() {
        return price_electric;
    }

    public void setPrice_electric(long price_electric) {
        this.price_electric = price_electric;
    }

    public long getPrice_water() {
        return price_water;
    }

    public void setPrice_water(long price_water) {
        this.price_water = price_water;
    }

    public long getPrice_internet() {
        return price_internet;
    }

    public void setPrice_internet(long price_internet) {
        this.price_internet = price_internet;
    }

    public String getId_room() {
        return id_room;
    }

    public void setId_room(String id_room) {
        this.id_room = id_room;
    }

    public String getTitle_room() {
        return title_room;
    }

    public void setTitle_room(String title_room) {
        this.title_room = title_room;
    }

    public long getPrice_room() {
        return price_room;
    }

    public void setPrice_room(long price_room) {
        this.price_room = price_room;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public int getArea_room() {
        return area_room;
    }

    public void setArea_room(int area_room) {
        this.area_room = area_room;
    }

    public long getDeposit_room() {
        return deposit_room;
    }

    public void setDeposit_room(long deposit_room) {
        this.deposit_room = deposit_room;
    }

    public String getDescription_room() {
        return description_room;
    }

    public void setDescription_room(String description_room) {
        this.description_room = description_room;
    }

    public String getGender_room() {
        return gender_room;
    }

    public void setGender_room(String gender_room) {
        this.gender_room = gender_room;
    }

    public int getPark_slot() {
        return park_slot;
    }

    public void setPark_slot(int park_slot) {
        this.park_slot = park_slot;
    }

    public int getPerson_in_room() {
        return person_in_room;
    }

    public void setPerson_in_room(int person_in_room) {
        this.person_in_room = person_in_room;
    }

    public int getStatus_room() {
        return status_room;
    }

    public void setStatus_room(int status_room) {
        this.status_room = status_room;
    }

    public String getType_room() {
        return type_room;
    }

    public void setType_room(String type_room) {
        this.type_room = type_room;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public ArrayList<String> getImages() {
        return images;
    }

    public void setImages(ArrayList<String> images) {
        this.images = images;
    }

    public String getFirstImage() {
        return images != null && !images.isEmpty() ? images.get(0) : "";
    }
    
    public ArrayList<Furniture> getRoomFurniture() {
        return roomFurniture;
    }

    public void setRoomFurniture(ArrayList<Furniture> roomFurniture) {
        this.roomFurniture = roomFurniture;
    }

    public int getFloor() {
        return floor;
    }

    public void setFloor(int floor) {
        this.floor = floor;
    }

    public ArrayList<Utility> getRoomUtilities() {
        return roomUtilities;
    }

    public void setRoomUtilities(ArrayList<Utility> roomUtilities) {
        this.roomUtilities = roomUtilities;
    }

    public Object getUserLovePost() {
        return userLovePost;
    }

    public void setUserLovePost(Object userLovePost) {
        this.userLovePost = userLovePost;
    }

    public String getId_own_post() {
        return id_own_post;
    }

    public void setId_own_post(String id_own_post) {
        this.id_own_post = id_own_post;
    }

    public int getLoveCount() {
        return loveCount;
    }

    public void setLoveCount(int loveCount) {
        this.loveCount = loveCount;
    }
}
