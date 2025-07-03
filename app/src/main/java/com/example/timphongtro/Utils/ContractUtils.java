package com.example.timphongtro.Utils;

import android.util.Log;
import com.example.timphongtro.Models.Contract;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import androidx.annotation.NonNull;

/**
 * Lớp tiện ích xử lý các chức năng liên quan đến quản lý hợp đồng
 * Bao gồm: kiểm tra trạng thái, cập nhật hợp đồng hết hạn, dọn dẹp dữ liệu
 */
public class ContractUtils {
    private static final String TAG = "ContractUtils";
    
    /**
     * Interface callback để xử lý kết quả cập nhật hợp đồng
     */
    public interface ContractUpdateCallback {
        /**
         * Được gọi khi cập nhật thành công
         * @param updatedCount Số lượng hợp đồng được cập nhật
         */
        void onSuccess(int updatedCount);
        
        /**
         * Được gọi khi có lỗi trong quá trình cập nhật
         * @param error Thông điệp lỗi
         */
        void onFailure(String error);
    }
    
    /**
     * Kiểm tra xem hợp đồng có cần cập nhật trạng thái hay không
     * Chỉ cập nhật khi hợp đồng đang hiệu lực nhưng đã hết hạn
     * 
     * @param contract Hợp đồng cần kiểm tra
     * @return true nếu cần cập nhật, false nếu không cần
     */
    public static boolean shouldUpdateStatus(Contract contract) {
        if (contract == null) return false;

        if (contract.getStatus() == 0) return false; 

        if (contract.getStatus() == 2) return false;
        
        // Chỉ update khi: status = 1 (đang hiệu lực) nhưng thời gian đã hết
        long now = System.currentTimeMillis();
        return (now > contract.getEndDate() && contract.getStatus() == 1);
    }

    /**
     * Lấy trạng thái hiện tại của hợp đồng
     * Tự động tính toán dựa trên thời gian hiện tại
     * 
     * @param contract Hợp đồng cần kiểm tra
     * @return Trạng thái hiện tại (0: nháp, 1: hiệu lực, 2: hết hạn)
     */
    public static int getCurrentStatus(Contract contract) {
        if (contract == null) return 0;
        
        if (shouldUpdateStatus(contract)) {
            return 2; // Hết hạn
        }
        return contract.getStatus();
    }

    /**
     * Kiểm tra hợp đồng đã hết hạn hay chưa
     * 
     * @param contract Hợp đồng cần kiểm tra
     * @return true nếu hết hạn, false nếu còn hiệu lực
     */
    public static boolean isExpired(Contract contract) {
        return getCurrentStatus(contract) == 2;
    }
    
    /**
     * Kiểm tra hợp đồng có đang hiệu lực hay không
     * 
     * @param contract Hợp đồng cần kiểm tra
     * @return true nếu đang hiệu lực, false nếu không
     */
    public static boolean isActive(Contract contract) {
        return getCurrentStatus(contract) == 1;
    }
    
    /**
     * Kiểm tra hợp đồng có ở trạng thái nháp hay không
     * 
     * @param contract Hợp đồng cần kiểm tra
     * @return true nếu là nháp, false nếu không
     */
    public static boolean isDraft(Contract contract) {
        return contract != null && contract.getStatus() == 0;
    }

    /**
     * Đồng bộ trạng thái hợp đồng với Firebase Database
     * Tự động cập nhật trạng thái nếu hợp đồng đã hết hạn
     * 
     * @param contract Hợp đồng cần đồng bộ
     */
    public static void syncContractStatusWithDatabase(Contract contract) {
        if (contract == null || contract.getContractId() == null) {
            Log.w(TAG, "Contract hoặc Contract ID không hợp lệ");
            return;
        }
        
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
                    })
                    .addOnFailureListener(e -> {
                    });
        }
    }
    
    /**
     * Cập nhật hàng loạt tất cả các hợp đồng đã hết hạn trong database
     * Sử dụng khi khởi động app hoặc định kỳ để đồng bộ dữ liệu
     */
    public static void batchUpdateExpiredContracts() {
        batchUpdateExpiredContracts(null);
    }
    
    /**
     * Cập nhật hàng loạt tất cả các hợp đồng đã hết hạn trong database với callback
     * 
     * @param callback Callback xử lý kết quả cập nhật
     */
    public static void batchUpdateExpiredContracts(ContractUpdateCallback callback) {
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
                        // Nếu hết hạn nhưng status chưa update
                        if (currentTime > endDate && status == 1) {
                            contractSnapshot.getRef().child("status").setValue(2);
                            updatedCount++;
                        }
                    }
                }
                
                Log.d(TAG, "Đã cập nhật " + updatedCount + " hợp đồng hết hạn");
                
                if (callback != null) {
                    callback.onSuccess(updatedCount);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                String errorMsg = "Lỗi cập nhật hợp đồng hết hạn: " + error.getMessage();
                Log.e(TAG, errorMsg);
                
                if (callback != null) {
                    callback.onFailure(errorMsg);
                }
            }
        });
    }
}