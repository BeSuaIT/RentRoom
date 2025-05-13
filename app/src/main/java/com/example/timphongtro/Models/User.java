package com.example.timphongtro.Models;

public class User {
    private String email, uid, phone, name, permission;
    private long createdAt;
    private String avatarUrl;

    public User() {
    }

    public User(String email, String uid, String name, String phone, String permission, Long createdAt, String avatarUrl) {
        this.email = email;
        this.uid = uid;
        this.name = name;
        this.phone = phone;
        this.permission = permission;
        this.createdAt = createdAt;
        this.avatarUrl = avatarUrl;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}
