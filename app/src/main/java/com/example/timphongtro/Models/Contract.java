package com.example.timphongtro.Models;

public class Contract {
    // ✅ CHỈ 15 TRƯỜNG ĐƯỢC LƯU VÀO DATABASE
    private String cccdBackImage;
    private String cccdFrontImage;
    private String contractId;
    private long createdAt;
    private long endDate;
    private String landlordId;
    private String landlordName;
    private String landlordPhone;
    private String roomId;
    private long startDate;
    private int status; // 0: Nháp, 1: Đang hiệu lực, 2: Hết hạn
    private String tenantCCCD;
    private String tenantId;
    private String tenantName;
    private String tenantPhone;

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

    // Constructor without tenantId
    public Contract(String contractId, String roomId, String landlordId, String landlordName, 
                   String landlordPhone, String tenantName, String tenantPhone, String tenantCCCD,
                   String cccdFrontImage, String cccdBackImage, long startDate, long endDate, 
                   int status, long createdAt) {
        this(contractId, roomId, landlordId, landlordName, landlordPhone, null, 
             tenantName, tenantPhone, tenantCCCD, cccdFrontImage, cccdBackImage, 
             startDate, endDate, status, createdAt);
    }

    // ✅ CHỈ GETTERS VÀ SETTERS - KHÔNG CÓ HELPER METHODS
    public String getCccdBackImage() { return cccdBackImage; }
    public void setCccdBackImage(String cccdBackImage) { this.cccdBackImage = cccdBackImage; }

    public String getCccdFrontImage() { return cccdFrontImage; }
    public void setCccdFrontImage(String cccdFrontImage) { this.cccdFrontImage = cccdFrontImage; }

    public String getContractId() { return contractId; }
    public void setContractId(String contractId) { this.contractId = contractId; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getEndDate() { return endDate; }
    public void setEndDate(long endDate) { this.endDate = endDate; }

    public String getLandlordId() { return landlordId; }
    public void setLandlordId(String landlordId) { this.landlordId = landlordId; }

    public String getLandlordName() { return landlordName; }
    public void setLandlordName(String landlordName) { this.landlordName = landlordName; }

    public String getLandlordPhone() { return landlordPhone; }
    public void setLandlordPhone(String landlordPhone) { this.landlordPhone = landlordPhone; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public long getStartDate() { return startDate; }
    public void setStartDate(long startDate) { this.startDate = startDate; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getTenantCCCD() { return tenantCCCD; }
    public void setTenantCCCD(String tenantCCCD) { this.tenantCCCD = tenantCCCD; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getTenantName() { return tenantName; }
    public void setTenantName(String tenantName) { this.tenantName = tenantName; }

    public String getTenantPhone() { return tenantPhone; }
    public void setTenantPhone(String tenantPhone) { this.tenantPhone = tenantPhone; }
}