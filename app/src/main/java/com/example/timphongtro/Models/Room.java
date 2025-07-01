package com.example.timphongtro.Models;

import java.util.ArrayList;

public class Room {
    private String ownerID, roomID, roomTitle, description, gender, phone, roomType;
    private long roomPrice, roomDeposit, electricPrice, waterPrice, internetPrice;
    private Address address;
    private int park_slot, people_in_room, roomStatus, floor, loveCount, roomSize;
    private ArrayList<String> images;
    private Object userLovePost;
    private ArrayList<Furniture> Furniture;
    private ArrayList<Utility> Utilities;

    public Room() {
    }

    public Room(String ownerID, String roomID, String roomTitle, long roomPrice, Address address, int roomSize, long roomDeposit,
                String description, String gender, int park_slot, int people_in_room, int roomStatus, String roomType, String phone,
                int floor, ArrayList<String> images, ArrayList<Furniture> Furniture, ArrayList<Utility> Utilities,
                long electricPrice, long waterPrice, long internetPrice) {
        this.ownerID = ownerID;
        this.roomID = roomID;
        this.roomTitle = roomTitle;
        this.roomPrice = roomPrice;
        this.address = address;
        this.roomSize = roomSize;
        this.roomDeposit = roomDeposit;
        this.description = description;
        this.gender = gender;
        this.park_slot = park_slot;
        this.people_in_room = people_in_room;
        this.roomStatus = roomStatus;
        this.roomType = roomType;
        this.phone = phone;
        this.floor = floor;
        this.electricPrice = electricPrice;
        this.waterPrice = waterPrice;
        this.internetPrice = internetPrice;
        this.images = images;
        this.Furniture = Furniture;
        this.Utilities = Utilities;
    }

    public long getElectricPrice() {
        return electricPrice;
    }

    public void setElectricPrice(long price_electric) {
        this.electricPrice = price_electric;
    }

    public long getWaterPrice() {
        return waterPrice;
    }

    public void setWaterPrice(long price_water) {
        this.waterPrice = price_water;
    }

    public long getInternetPrice() {
        return internetPrice;
    }

    public void setInternetPrice(long price_internet) {
        this.internetPrice = price_internet;
    }

    public String getRoomID() {
        return roomID;
    }

    public void setRoomID(String id_room) {
        this.roomID = id_room;
    }

    public String getRoomTitle() {
        return roomTitle;
    }

    public void setRoomTitle(String title_room) {
        this.roomTitle = title_room;
    }

    public long getRoomPrice() {
        return roomPrice;
    }

    public void setRoomPrice(long price_room) {
        this.roomPrice = price_room;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public int getRoomSize() {
        return roomSize;
    }

    public void setRoomSize(int area_room) {
        this.roomSize = area_room;
    }

    public long getRoomDeposit() {
        return roomDeposit;
    }

    public void setRoomDeposit(long deposit_room) {
        this.roomDeposit = deposit_room;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description_room) {
        this.description = description_room;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender_room) {
        this.gender = gender_room;
    }

    public int getpark_slot() {
        return park_slot;
    }

    public void setpark_slot(int park_slot) {
        this.park_slot = park_slot;
    }

    public int getpeople_in_room() {
        return people_in_room;
    }

    public void setpeople_in_room(int people_in_room) {
        this.people_in_room = people_in_room;
    }

    public int getRoomStatus() {
        return roomStatus;
    }

    public void setRoomStatus(int status_room) {
        this.roomStatus = status_room;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String type_room) {
        this.roomType = type_room;
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
    
    public ArrayList<Furniture> getFurniture() {
        return Furniture;
    }

    public void setFurniture(ArrayList<Furniture> roomFurniture) {
        this.Furniture = roomFurniture;
    }

    public int getFloor() {
        return floor;
    }

    public void setFloor(int floor) {
        this.floor = floor;
    }

    public ArrayList<Utility> getUtilities() {
        return Utilities;
    }

    public void setUtilities(ArrayList<Utility> roomUtilities) {
        this.Utilities = roomUtilities;
    }

    public Object getUserLovePost() {
        return userLovePost;
    }

    public void setUserLovePost(Object userLovePost) {
        this.userLovePost = userLovePost;
    }

    public String getOwnerID() {
        return ownerID;
    }

    public void setOwnerID(String id_own_post) {
        this.ownerID = id_own_post;
    }

    public int getLoveCount() {
        return loveCount;
    }

    public void setLoveCount(int loveCount) {
        this.loveCount = loveCount;
    }
}
