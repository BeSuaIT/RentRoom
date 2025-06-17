package com.example.timphongtro.Models;

public class Contract {
    private String contractId;
    private String roomId;
    private String landlordId;
    private String landlordName;
    private String landlordPhone;
    private String tenantName;
    private String tenantPhone;
    private String tenantCCCD;
    private String cccdFrontImage;
    private String cccdBackImage;
    private String startDate;
    private String endDate;
    private int status; // 0: Nháp, 1: Đang hiệu lực, 2: Hết hạn, 3: Đã chấm dứt
    private long createdAt;

    public Contract() {}

    public Contract(String contractId, String roomId, String landlordId, String landlordName, 
                   String landlordPhone, String tenantName, String tenantPhone, String tenantCCCD,
                   String cccdFrontImage, String cccdBackImage, String startDate, String endDate, 
                   int status, long createdAt) {
        this.contractId = contractId;
        this.roomId = roomId;
        this.landlordId = landlordId;
        this.landlordName = landlordName;
        this.landlordPhone = landlordPhone;
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

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}