package com.example.timphongtro.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lớp quản lý việc dọn dẹp dữ liệu Firebase Database
 * Xóa các dữ liệu liên quan đến user đã bị xóa khỏi hệ thống
 */
public class DataCleanupManager {
    private static final String TAG = "DataCleanupManager";

    private static final String CLEANUP_PREFS = "cleanup_prefs";

    private static final String LAST_CLEANUP_TIME = "last_cleanup_time";

    private static final long CLEANUP_INTERVAL = 24 * 60 * 60 * 1000; // 24 giờ

    private DatabaseReference databaseRef;

    private CleanupCallback cleanupCallback;

    /**
     * Interface để callback kết quả cleanup
     */
    public interface CleanupCallback {
        /**
         * Được gọi khi cleanup hoàn thành thành công
         */
        void onCleanupCompleted();
        
        /**
         * Được gọi khi cleanup thất bại
         * @param error Thông báo lỗi chi tiết
         */
        void onCleanupFailed(String error);
    }

    /**
     * Constructor khởi tạo DataCleanupManager
     * Tự động khởi tạo Firebase Database reference
     */
    public DataCleanupManager() {
        databaseRef = FirebaseDatabase.getInstance().getReference();
    }

    /**
     * Kiểm tra có cần thực hiện cleanup hay không dựa vào thời gian lần cuối
     * 
     * @param context Context của Activity/Application để truy cập SharedPreferences
     * @return true nếu cần cleanup (đã quá 24h kể từ lần cuối), false nếu không cần
     */
    public boolean shouldPerformCleanup(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(CLEANUP_PREFS, Context.MODE_PRIVATE);
        long lastCleanup = prefs.getLong(LAST_CLEANUP_TIME, 0);
        return System.currentTimeMillis() - lastCleanup > CLEANUP_INTERVAL;
    }

    /**
     * Đánh dấu cleanup đã hoàn thành và lưu timestamp hiện tại
     * 
     * @param context Context của Activity/Application để truy cập SharedPreferences
     */
    public void markCleanupCompleted(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(CLEANUP_PREFS, Context.MODE_PRIVATE);
        prefs.edit().putLong(LAST_CLEANUP_TIME, System.currentTimeMillis()).apply();
    }

    /**
     * Thực hiện cleanup toàn bộ database
     * Đây là method chính để bắt đầu quá trình cleanup
     * 
     * @param callback Callback để nhận kết quả cleanup (thành công/thất bại)
     */
    public void performDatabaseCleanup(CleanupCallback callback) {
        this.cleanupCallback = callback;
        Log.d(TAG, "Starting database cleanup...");

        // Lấy danh sách user hợp lệ trước khi cleanup
        getValidUserIds(validUserIds -> {
            if (validUserIds != null) {
                cleanupAllNodes(validUserIds);
            } else {
                callback.onCleanupFailed("Failed to get valid user IDs");
            }
        });
    }

    /**
     * Lấy danh sách UID của user còn tồn tại trong database
     * 
     * @param callback Callback nhận danh sách UID hợp lệ hoặc null nếu lỗi
     */
    private void getValidUserIds(UserIdsCallback callback) {
        databaseRef.child("Users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> validUserIds = new ArrayList<>();
                // Duyệt qua tất cả user trong node Users
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    validUserIds.add(userSnapshot.getKey()); // Thêm UID vào danh sách
                }
                Log.d(TAG, "Found " + validUserIds.size() + " valid users");
                callback.onResult(validUserIds);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to get valid user IDs: " + error.getMessage());
                callback.onResult(null);
            }
        });
    }

    /**
     * Cleanup tất cả các node liên quan đến user
     * 
     * @param validUserIds Danh sách UID của user hợp lệ để so sánh và giữ lại
     */
    private void cleanupAllNodes(List<String> validUserIds) {
        int totalTasks = 7; // Tổng số node cần cleanup
        
        // Counter để đếm số task hoàn thành
        CleanupTaskCounter taskCounter = new CleanupTaskCounter(totalTasks, () -> {
            Log.d(TAG, "Database cleanup completed successfully");
            cleanupCallback.onCleanupCompleted();
        });

        // Cleanup từng node một cách bất đồng bộ
        cleanupPosts(validUserIds, taskCounter);
        cleanupHistories(validUserIds, taskCounter);
        cleanupMeetingSchedules(validUserIds, taskCounter);
        cleanupBills(validUserIds, taskCounter);
        cleanupServices(validUserIds, taskCounter);
        cleanupCarts(validUserIds, taskCounter);
        cleanupFollowPosts(validUserIds, taskCounter);
    }

    /**
     * Cleanup Posts node - xóa post của user không tồn tại và userLovePost không hợp lệ
     * 
     * @param validUserIds Danh sách UID hợp lệ để so sánh
     * @param counter Counter để đếm task hoàn thành
     */
    private void cleanupPosts(List<String> validUserIds, CleanupTaskCounter counter) {
        databaseRef.child("Posts").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Object> updates = new HashMap<>(); // Map chứa các update cần thực hiện
                AtomicInteger removedCount = new AtomicInteger(0); // Đếm số item bị xóa (thread-safe)

                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    // Kiểm tra chủ sở hữu post
                    String ownerId = postSnapshot.child("id_own_post").getValue(String.class);
                    if (ownerId != null && !validUserIds.contains(ownerId)) {
                        // User không tồn tại, xóa toàn bộ post
                        updates.put(postSnapshot.getKey(), null);
                        removedCount.incrementAndGet();
                    }
                    
                    // Cleanup userLovePost trong mỗi post (những user đã like)
                    DataSnapshot userLovePostSnapshot = postSnapshot.child("userLovePost");
                    if (userLovePostSnapshot.exists()) {
                        for (DataSnapshot loveSnapshot : userLovePostSnapshot.getChildren()) {
                            String userId = loveSnapshot.getKey();
                            if (userId != null && !validUserIds.contains(userId)) {
                                // User đã like không còn tồn tại, xóa like
                                updates.put(postSnapshot.getKey() + "/userLovePost/" + userId, null);
                                removedCount.incrementAndGet();
                            }
                        }
                    }
                }

                // Thực hiện update nếu có thay đổi
                if (!updates.isEmpty()) {
                    databaseRef.child("Posts").updateChildren(updates)
                        .addOnCompleteListener(task -> {
                            Log.d(TAG, "Removed " + removedCount.get() + " invalid posts and love records");
                            counter.taskCompleted();
                        });
                } else {
                    Log.d(TAG, "No invalid posts found");
                    counter.taskCompleted();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to cleanup Posts: " + error.getMessage());
                counter.taskCompleted(); // Vẫn đánh dấu hoàn thành để không block
            }
        });
    }

    /**
     * Cleanup Histories node - xóa lịch sử xem của user không tồn tại
     * 
     * @param validUserIds Danh sách UID hợp lệ để so sánh
     * @param counter Counter để đếm task hoàn thành
     */
    private void cleanupHistories(List<String> validUserIds, CleanupTaskCounter counter) {
        databaseRef.child("Histories").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Object> updates = new HashMap<>();
                AtomicInteger removedCount = new AtomicInteger(0);

                for (DataSnapshot historySnapshot : snapshot.getChildren()) {
                    String userId = historySnapshot.getKey(); // Key chính là userId
                    if (userId != null && !validUserIds.contains(userId)) {
                        // User không tồn tại, xóa toàn bộ lịch sử của user này
                        updates.put(userId, null);
                        removedCount.incrementAndGet();
                    }
                }

                if (!updates.isEmpty()) {
                    databaseRef.child("Histories").updateChildren(updates)
                        .addOnCompleteListener(task -> {
                            Log.d(TAG, "Removed " + removedCount.get() + " invalid histories");
                            counter.taskCompleted();
                        });
                } else {
                    Log.d(TAG, "No invalid histories found");
                    counter.taskCompleted();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to cleanup Histories: " + error.getMessage());
                counter.taskCompleted();
            }
        });
    }

    /**
     * Cleanup MeetingSchedules node - xóa lịch hẹn có người gửi hoặc người nhận không tồn tại
     * 
     * @param validUserIds Danh sách UID hợp lệ để so sánh
     * @param counter Counter để đếm task hoàn thành
     */
    private void cleanupMeetingSchedules(List<String> validUserIds, CleanupTaskCounter counter) {
        databaseRef.child("MeetingSchedules").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Object> updates = new HashMap<>();
                AtomicInteger removedCount = new AtomicInteger(0);

                for (DataSnapshot scheduleSnapshot : snapshot.getChildren()) {
                    // Lấy ID người gửi yêu cầu hẹn
                    String idFrom = scheduleSnapshot.child("idFrom").getValue(String.class);
                    // Lấy ID người nhận yêu cầu hẹn
                    String idTo = scheduleSnapshot.child("idTo").getValue(String.class);
                    
                    // Nếu một trong hai user không tồn tại, xóa lịch hẹn
                    if ((idFrom != null && !validUserIds.contains(idFrom)) ||
                        (idTo != null && !validUserIds.contains(idTo))) {
                        updates.put(scheduleSnapshot.getKey(), null);
                        removedCount.incrementAndGet();
                    }
                }

                if (!updates.isEmpty()) {
                    databaseRef.child("MeetingSchedules").updateChildren(updates)
                        .addOnCompleteListener(task -> {
                            Log.d(TAG, "Removed " + removedCount.get() + " invalid meeting schedules");
                            counter.taskCompleted();
                        });
                } else {
                    Log.d(TAG, "No invalid meeting schedules found");
                    counter.taskCompleted();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to cleanup MeetingSchedules: " + error.getMessage());
                counter.taskCompleted();
            }
        });
    }

    /**
     * Cleanup Bills node - xóa hóa đơn có người mua hoặc người bán không tồn tại
     * 
     * @param validUserIds Danh sách UID hợp lệ để so sánh
     * @param counter Counter để đếm task hoàn thành
     */
    private void cleanupBills(List<String> validUserIds, CleanupTaskCounter counter) {
        databaseRef.child("Bills").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Object> updates = new HashMap<>();
                AtomicInteger removedCount = new AtomicInteger(0);

                for (DataSnapshot billSnapshot : snapshot.getChildren()) {
                    // Lấy ID người mua
                    String userId = billSnapshot.child("userId").getValue(String.class);
                    // Lấy ID người bán
                    String sellerId = billSnapshot.child("sellerId").getValue(String.class);
                    
                    // Nếu người mua hoặc người bán không tồn tại, xóa hóa đơn
                    if ((userId != null && !validUserIds.contains(userId)) ||
                        (sellerId != null && !validUserIds.contains(sellerId))) {
                        updates.put(billSnapshot.getKey(), null);
                        removedCount.incrementAndGet();
                    }
                }

                if (!updates.isEmpty()) {
                    databaseRef.child("Bills").updateChildren(updates)
                        .addOnCompleteListener(task -> {
                            Log.d(TAG, "Removed " + removedCount.get() + " invalid bills");
                            counter.taskCompleted();
                        });
                } else {
                    Log.d(TAG, "No invalid bills found");
                    counter.taskCompleted();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to cleanup Bills: " + error.getMessage());
                counter.taskCompleted();
            }
        });
    }

    /**
     * Cleanup Services node - xóa dịch vụ có người bán không tồn tại
     * 
     * @param validUserIds Danh sách UID hợp lệ để so sánh
     * @param counter Counter để đếm task hoàn thành
     */
    private void cleanupServices(List<String> validUserIds, CleanupTaskCounter counter) {
        databaseRef.child("Services").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Object> updates = new HashMap<>();
                AtomicInteger removedCount = new AtomicInteger(0);

                // Duyệt qua từng category dịch vụ
                for (DataSnapshot categorySnapshot : snapshot.getChildren()) {
                    // Duyệt qua từng dịch vụ trong category
                    for (DataSnapshot serviceSnapshot : categorySnapshot.getChildren()) {
                        // Lấy ID người bán dịch vụ
                        String sellerId = serviceSnapshot.child("id_seller").getValue(String.class);
                        if (sellerId != null && !validUserIds.contains(sellerId)) {
                            // Người bán không tồn tại, xóa dịch vụ
                            updates.put(categorySnapshot.getKey() + "/" + serviceSnapshot.getKey(), null);
                            removedCount.incrementAndGet();
                        }
                    }
                }

                if (!updates.isEmpty()) {
                    databaseRef.child("Services").updateChildren(updates)
                        .addOnCompleteListener(task -> {
                            Log.d(TAG, "Removed " + removedCount.get() + " invalid services");
                            counter.taskCompleted();
                        });
                } else {
                    Log.d(TAG, "No invalid services found");
                    counter.taskCompleted();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to cleanup Services: " + error.getMessage());
                counter.taskCompleted();
            }
        });
    }

    /**
     * Cleanup Carts node - xóa giỏ hàng của user không tồn tại và item từ seller không tồn tại
     * 
     * @param validUserIds Danh sách UID hợp lệ để so sánh
     * @param counter Counter để đếm task hoàn thành
     */
    private void cleanupCarts(List<String> validUserIds, CleanupTaskCounter counter) {
        databaseRef.child("Carts").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Object> updates = new HashMap<>();
                AtomicInteger removedCount = new AtomicInteger(0);

                for (DataSnapshot cartSnapshot : snapshot.getChildren()) {
                    String userId = cartSnapshot.getKey(); // Key chính là userId
                    if (userId != null && !validUserIds.contains(userId)) {
                        // User không tồn tại, xóa toàn bộ cart của user này
                        updates.put(userId, null);
                        removedCount.incrementAndGet();
                    } else {
                        // User hợp lệ, kiểm tra từng seller trong cart
                        for (DataSnapshot sellerSnapshot : cartSnapshot.getChildren()) {
                            String sellerId = sellerSnapshot.getKey(); // Key là sellerId
                            if (sellerId != null && !validUserIds.contains(sellerId)) {
                                // Seller không tồn tại, xóa item của seller này khỏi cart
                                updates.put(userId + "/" + sellerId, null);
                                removedCount.incrementAndGet();
                            }
                        }
                    }
                }

                if (!updates.isEmpty()) {
                    databaseRef.child("Carts").updateChildren(updates)
                        .addOnCompleteListener(task -> {
                            Log.d(TAG, "Removed " + removedCount.get() + " invalid cart items");
                            counter.taskCompleted();
                        });
                } else {
                    Log.d(TAG, "No invalid cart items found");
                    counter.taskCompleted();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to cleanup Carts: " + error.getMessage());
                counter.taskCompleted();
            }
        });
    }

    /**
     * Cleanup FollowPosts node - xóa danh sách theo dõi của user không tồn tại
     * 
     * @param validUserIds Danh sách UID hợp lệ để so sánh
     * @param counter Counter để đếm task hoàn thành
     */
    private void cleanupFollowPosts(List<String> validUserIds, CleanupTaskCounter counter) {
        databaseRef.child("FollowPosts").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Object> updates = new HashMap<>();
                AtomicInteger removedCount = new AtomicInteger(0);

                for (DataSnapshot followSnapshot : snapshot.getChildren()) {
                    String userId = followSnapshot.getKey(); // Key chính là userId
                    if (userId != null && !validUserIds.contains(userId)) {
                        // User không tồn tại, xóa toàn bộ danh sách theo dõi của user này
                        updates.put(userId, null);
                        removedCount.incrementAndGet();
                    }
                }

                if (!updates.isEmpty()) {
                    databaseRef.child("FollowPosts").updateChildren(updates)
                        .addOnCompleteListener(task -> {
                            Log.d(TAG, "Removed " + removedCount.get() + " invalid follow posts");
                            counter.taskCompleted();
                        });
                } else {
                    Log.d(TAG, "No invalid follow posts found");
                    counter.taskCompleted();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to cleanup FollowPosts: " + error.getMessage());
                counter.taskCompleted();
            }
        });
    }

    // Helper interfaces và classes

    /**
     * Interface callback để nhận danh sách UID hợp lệ
     */
    private interface UserIdsCallback {
        /**
         * Được gọi khi có kết quả lấy danh sách UID
         * @param userIds Danh sách UID hợp lệ, null nếu có lỗi
         */
        void onResult(List<String> userIds);
    }

    /**
     * Class helper để đếm số task cleanup hoàn thành
     * Thread-safe để đảm bảo không bị race condition
     */
    private static class CleanupTaskCounter {
        /** Tổng số task cần hoàn thành */
        private final int totalTasks;
        
        /** Số task đã hoàn thành */
        private int completedTasks = 0;
        
        /** Callback được gọi khi tất cả task hoàn thành */
        private final Runnable onAllCompleted;

        /**
         * Constructor khởi tạo counter
         * @param totalTasks Tổng số task cần hoàn thành
         * @param onAllCompleted Callback được gọi khi tất cả task hoàn thành
         */
        public CleanupTaskCounter(int totalTasks, Runnable onAllCompleted) {
            this.totalTasks = totalTasks;
            this.onAllCompleted = onAllCompleted;
        }

        /**
         * Đánh dấu một task đã hoàn thành
         * Synchronized để đảm bảo thread-safe
         */
        public synchronized void taskCompleted() {
            completedTasks++;
            Log.d(TAG, "Task completed: " + completedTasks + "/" + totalTasks);
            if (completedTasks >= totalTasks) {
                onAllCompleted.run(); // Gọi callback khi tất cả task hoàn thành
            }
        }
    }
}
