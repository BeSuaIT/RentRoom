package com.example.timphongtro.Utils;

import android.util.Log;
import com.example.timphongtro.Models.Contract;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import androidx.annotation.NonNull;

public class ContractUtils {
    private static final String TAG = "ContractUtils";
    
    // ✅ CHECK NẾU CONTRACT CẦN UPDATE STATUS
    public static boolean shouldUpdateStatus(Contract contract) {
        // Nếu đang ở trạng thái nháp (0) thì không auto update
        if (contract.getStatus() == 0) return false; 
        
        // Nếu đã hết hạn rồi thì không cần update nữa
        if (contract.getStatus() == 2) return false;
        
        // Chỉ update khi: status = 1 (đang hiệu lực) nhưng thời gian đã hết
        long now = System.currentTimeMillis();
        return (now > contract.getEndDate() && contract.getStatus() == 1);
    }

    // ✅ AUTO UPDATE STATUS NẾU CẦN
    public static int getCurrentStatus(Contract contract) {
        if (shouldUpdateStatus(contract)) {
            return 2; // Hết hạn
        }
        return contract.getStatus();
    }

    // ✅ CHECK HẾT HẠN
    public static boolean isExpired(Contract contract) {
        return getCurrentStatus(contract) == 2;
    }

    // ✅ SYNC STATUS VỚI DATABASE
    public static void syncContractStatusWithDatabase(Contract contract) {
        // Lấy status cũ
        int oldStatus = contract.getStatus();
        
        // Tính status mới
        int newStatus = getCurrentStatus(contract);
        
        // Nếu status thay đổi, cập nhật database
        if (oldStatus != newStatus) {
            DatabaseReference contractRef = FirebaseDatabase.getInstance()
                    .getReference("Contracts")
                    .child(contract.getContractId())
                    .child("status");
            
            contractRef.setValue(newStatus)
                    .addOnSuccessListener(aVoid -> {
                        // Update local object
                        contract.setStatus(newStatus);
                        Log.d(TAG, "Contract status updated: " + contract.getContractId() + " -> " + newStatus);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to update contract status: " + e.getMessage());
                    });
        }
    }
    
    // ✅ BATCH UPDATE TẤT CẢ CONTRACTS HẾT HẠN
    public static void batchUpdateExpiredContracts() {
        DatabaseReference contractsRef = FirebaseDatabase.getInstance().getReference("Contracts");
        
        contractsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long currentTime = System.currentTimeMillis();
                int updatedCount = 0;
                
                for (DataSnapshot contractSnapshot : snapshot.getChildren()) {
                    Long endDate = contractSnapshot.child("endDate").getValue(Long.class);
                    Integer status = contractSnapshot.child("status").getValue(Integer.class);
                    
                    if (endDate != null && status != null) {
                        // ✅ NẾU HẾT HẠN NHƯNG STATUS CHƯA UPDATE
                        if (currentTime > endDate && status == 1) {
                            contractSnapshot.getRef().child("status").setValue(2);
                            updatedCount++;
                        }
                    }
                }
                
                Log.d(TAG, "Updated " + updatedCount + " expired contracts");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to update expired contracts: " + error.getMessage());
            }
        });
    }
    
    // ✅ DỌNG DẸP CÁC TRƯỜNG KHÔNG CẦN THIẾT TRONG DATABASE
    public static void cleanupUnnecessaryFields() {
        DatabaseReference contractsRef = FirebaseDatabase.getInstance().getReference("Contracts");
        
        contractsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int cleanedCount = 0;
                
                for (DataSnapshot contractSnapshot : snapshot.getChildren()) {
                    DatabaseReference contractRef = contractSnapshot.getRef();
                    boolean needsCleanup = false;
                    
                    // ✅ CHECK XEM CÓ CÁC TRƯỜNG KHÔNG CẦN THIẾT KHÔNG
                    if (contractSnapshot.hasChild("currentStatus") ||
                        contractSnapshot.hasChild("expired") ||
                        contractSnapshot.hasChild("formattedEndDate") ||
                        contractSnapshot.hasChild("formattedPeriod") ||
                        contractSnapshot.hasChild("formattedStartDate")) {
                        needsCleanup = true;
                    }
                    
                    if (needsCleanup) {
                        // ✅ XÓA CÁC TRƯỜNG KHÔNG CẦN THIẾT
                        contractRef.child("currentStatus").removeValue();
                        contractRef.child("expired").removeValue();
                        contractRef.child("formattedEndDate").removeValue();
                        contractRef.child("formattedPeriod").removeValue();
                        contractRef.child("formattedStartDate").removeValue();
                        cleanedCount++;
                        
                        // ✅ ĐỒNG THỜI UPDATE STATUS NẾU CẦN
                        Long endDate = contractSnapshot.child("endDate").getValue(Long.class);
                        Integer status = contractSnapshot.child("status").getValue(Integer.class);
                        
                        if (endDate != null && status != null) {
                            long now = System.currentTimeMillis();
                            // Nếu hết hạn nhưng status chưa update
                            if (now > endDate && status == 1) {
                                contractRef.child("status").setValue(2);
                            }
                        }
                    }
                }
                
                Log.d(TAG, "Cleaned up " + cleanedCount + " contracts");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to cleanup unnecessary fields: " + error.getMessage());
            }
        });
    }
}