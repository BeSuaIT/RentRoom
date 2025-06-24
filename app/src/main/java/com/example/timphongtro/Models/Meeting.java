package com.example.timphongtro.Models;

public class Meeting {
    private String name;
    private String phone;
    private String note;
    private long timeVisitRoom;
    private String idTo;
    private String idFrom;
    private int status; //0 la tao, 1 la chap nhan, 2 la tu choi
    private String idRoom;
    private String idSchedule;
    
    public Meeting() {
    }

    public Meeting(String idSchedule, String name, String phone, String note, long timeVisitRoom, String idTo, String idFrom, int status, String idRoom) {
        this.idSchedule = idSchedule;
        this.idTo = idTo;
        this.idRoom = idRoom;
        this.status = status;
        this.idFrom = idFrom;
        this.name = name;
        this.phone = phone;
        this.note = note;
        this.timeVisitRoom = timeVisitRoom;
    }

    public String getIdSchedule() {
        return idSchedule;
    }

    public void setIdSchedule(String idSchedule) {
        this.idSchedule = idSchedule;
    }

    public String getIdTo() {
        return idTo;
    }

    public void setIdTo(String idTo) {
        this.idTo = idTo;
    }

    public String getIdFrom() {
        return idFrom;
    }

    public void setIdFrom(String idFrom) {
        this.idFrom = idFrom;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getIdRoom() {
        return idRoom;
    }

    public void setIdRoom(String idRoom) {
        this.idRoom = idRoom;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public long getTimeVisitRoom() {
        return timeVisitRoom;
    }

    public void setTimeVisitRoom(long timeVisitRoom) {
        this.timeVisitRoom = timeVisitRoom;
    }
}
