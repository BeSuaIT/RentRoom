package com.example.timphongtro.Models;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Contract {
    private String contractId;
    private String roomId;
    private String landlordId;
    private String landlordName;
    private String landlordPhone;
    private String tenantId;
    private String tenantName;
    private String tenantPhone;
    private String tenantCCCD;
    private String cccdFrontImage;
    private String cccdBackImage;
    private long startDate;
    private long endDate;
    private int status; // 0: Nháp, 1: Đang hiệu lực, 2: Hết hạn
    private long createdAt;

    public Contract() {}

    public Contract(String contractId, String roomId, String landlordId, String landlordName, 
                   String landlordPhone, String tenantId, String tenantName, String tenantPhone, 
                   String tenantCCCD, String cccdFrontImage, String cccdBackImage, 
                   long startDate, long endDate, int status, long createdAt) {
        this.contractId = contractId;
        this.roomId = roomId;
        this.landlordId = landlordId;
        this.landlordName = landlordName;
        this.landlordPhone = landlordPhone;
        this.tenantId = tenantId;
        this.tenantName = tenantName;
        this.tenantPhone = tenantPhone;
        this.tenantCCCD = tenantCCCD;
        this.cccdFrontImage = cccdFrontImage;
        this.cccdBackImage = cccdBackImage;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getFormattedStartDate() {
        return formatTimestamp(this.startDate);
    }

    public String getFormattedEndDate() {
        return formatTimestamp(this.endDate);
    }

    public String getFormattedPeriod() {
        return getFormattedStartDate() + " - " + getFormattedEndDate();
    }

    public boolean shouldUpdateStatus() {
        if (this.status == 0) return false; 

        if (this.status == 2) return false;

        long now = System.currentTimeMillis();
        return (now > this.endDate && this.status == 1);
    }

    public void autoUpdateStatusIfNeeded() {
        if (shouldUpdateStatus()) {
            this.status = 2;
        }
    }

    public int getCurrentStatus() {
        autoUpdateStatusIfNeeded();
        return this.status;
    }

    public boolean isExpired() {
        return getCurrentStatus() == 2;
    }

    private static String formatTimestamp(long timestamp) {
        if (timestamp <= 0) return "Chưa xác định";
        
        try {
            Date date = new Date(timestamp);
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy", new Locale("vi", "VN"));
            return formatter.format(date);
        } catch (Exception e) {
            return "Thời gian không hợp lệ";
        }
    }

    public String getContractId() { return contractId; }
    public void setContractId(String contractId) { this.contractId = contractId; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getLandlordId() { return landlordId; }
    public void setLandlordId(String landlordId) { this.landlordId = landlordId; }

    public String getLandlordName() { return landlordName; }
    public void setLandlordName(String landlordName) { this.landlordName = landlordName; }

    public String getLandlordPhone() { return landlordPhone; }
    public void setLandlordPhone(String landlordPhone) { this.landlordPhone = landlordPhone; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getTenantName() { return tenantName; }
    public void setTenantName(String tenantName) { this.tenantName = tenantName; }

    public String getTenantPhone() { return tenantPhone; }
    public void setTenantPhone(String tenantPhone) { this.tenantPhone = tenantPhone; }

    public String getTenantCCCD() { return tenantCCCD; }
    public void setTenantCCCD(String tenantCCCD) { this.tenantCCCD = tenantCCCD; }

    public String getCccdFrontImage() { return cccdFrontImage; }
    public void setCccdFrontImage(String cccdFrontImage) { this.cccdFrontImage = cccdFrontImage; }

    public String getCccdBackImage() { return cccdBackImage; }
    public void setCccdBackImage(String cccdBackImage) { this.cccdBackImage = cccdBackImage; }

    public long getStartDate() { return startDate; }
    public void setStartDate(long startDate) { this.startDate = startDate; }

    public long getEndDate() { return endDate; }
    public void setEndDate(long endDate) { this.endDate = endDate; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}