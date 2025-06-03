package com.example.timphongtro.Models;

import java.io.Serializable;

public class Contract implements Serializable {
    private String contractId;
    private String roomId;
    private String roomTitle;
    private String roomAddress;
    private long roomPrice;
    private String landlordName;
    private String landlordPhone;
    private String landlordId;
    private String tenantName;
    private String tenantPhone;
    private String tenantCCCD;
    private String startDate;
    private String endDate;
    private long createdAt;
    private int status; // 0: Draft, 1: Active, 2: Expired, 3: Terminated
    private String cccdFrontImage;
    private String cccdBackImage;

    public Contract() {}

    public Contract(String contractId, String roomId, String roomTitle, String roomAddress, 
                   long roomPrice, String landlordName, String landlordPhone, String landlordId,
                   String tenantName, String tenantPhone, String tenantCCCD, 
                   String startDate, String endDate, String cccdFrontImage, String cccdBackImage) {
        this.contractId = contractId;
        this.roomId = roomId;
        this.roomTitle = roomTitle;
        this.roomAddress = roomAddress;
        this.roomPrice = roomPrice;
        this.landlordName = landlordName;
        this.landlordPhone = landlordPhone;
        this.landlordId = landlordId;
        this.tenantName = tenantName;
        this.tenantPhone = tenantPhone;
        this.tenantCCCD = tenantCCCD;
        this.startDate = startDate;
        this.endDate = endDate;
        this.cccdFrontImage = cccdFrontImage;
        this.cccdBackImage = cccdBackImage;
        this.createdAt = System.currentTimeMillis();
        this.status = 1;
    }

    public String getContractId() { return contractId; }
    public void setContractId(String contractId) { this.contractId = contractId; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getRoomTitle() { return roomTitle; }
    public void setRoomTitle(String roomTitle) { this.roomTitle = roomTitle; }

    public String getRoomAddress() { return roomAddress; }
    public void setRoomAddress(String roomAddress) { this.roomAddress = roomAddress; }

    public long getRoomPrice() { return roomPrice; }
    public void setRoomPrice(long roomPrice) { this.roomPrice = roomPrice; }

    public String getLandlordName() { return landlordName; }
    public void setLandlordName(String landlordName) { this.landlordName = landlordName; }

    public String getLandlordPhone() { return landlordPhone; }
    public void setLandlordPhone(String landlordPhone) { this.landlordPhone = landlordPhone; }

    public String getLandlordId() { return landlordId; }
    public void setLandlordId(String landlordId) { this.landlordId = landlordId; }

    public String getTenantName() { return tenantName; }
    public void setTenantName(String tenantName) { this.tenantName = tenantName; }

    public String getTenantPhone() { return tenantPhone; }
    public void setTenantPhone(String tenantPhone) { this.tenantPhone = tenantPhone; }

    public String getTenantCCCD() { return tenantCCCD; }
    public void setTenantCCCD(String tenantCCCD) { this.tenantCCCD = tenantCCCD; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getCccdFrontImage() { return cccdFrontImage; }
    public void setCccdFrontImage(String cccdFrontImage) { this.cccdFrontImage = cccdFrontImage; }

    public String getCccdBackImage() { return cccdBackImage; }
    public void setCccdBackImage(String cccdBackImage) { this.cccdBackImage = cccdBackImage; }
}