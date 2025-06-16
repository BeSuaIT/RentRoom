package com.example.timphongtro.Utils;

import android.content.Context;
import android.content.SharedPreferences;

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
import java.util.function.Consumer;

/**
 * Lớp quản lý việc dọn dẹp dữ liệu Firebase Database
 * Xóa các dữ liệu liên quan đến user đã bị xóa khỏi hệ thống
 */
public class DataCleanupManager {
    private static final String CLEANUP_PREFS = "cleanup_prefs";
    private static final String LAST_CLEANUP_TIME = "last_cleanup_time";
    private static final long CLEANUP_INTERVAL = 24 * 60 * 60 * 1000; // 24 giờ

    private DatabaseReference databaseRef;
    private Runnable onCompleted;
    private Consumer<String> onFailed;

    public DataCleanupManager() {
        databaseRef = FirebaseDatabase.getInstance().getReference();
    }

    /**
     * Kiểm tra có cần thực hiện cleanup hay không
     */
    public boolean shouldPerformCleanup(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(CLEANUP_PREFS, Context.MODE_PRIVATE);
        long lastCleanup = prefs.getLong(LAST_CLEANUP_TIME, 0);
        return System.currentTimeMillis() - lastCleanup > CLEANUP_INTERVAL;
    }

    /**
     * Đánh dấu cleanup đã hoàn thành
     */
    public void markCleanupCompleted(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(CLEANUP_PREFS, Context.MODE_PRIVATE);
        prefs.edit().putLong(LAST_CLEANUP_TIME, System.currentTimeMillis()).apply();
    }

    /**
     * Thực hiện cleanup toàn bộ database
     */
    public void performDatabaseCleanup(Runnable onCompleted, Consumer<String> onFailed) {
        this.onCompleted = onCompleted;
        this.onFailed = onFailed;

        getValidUserIds(validUserIds -> {
            if (validUserIds != null) {
                cleanupAllNodes(validUserIds);
            } else {
                onFailed.accept("Failed to get valid user IDs");
            }
        });
    }

    /**
     * Lấy danh sách UID của user còn tồn tại
     */
    private void getValidUserIds(Consumer<List<String>> callback) {
        databaseRef.child("Users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> validUserIds = new ArrayList<>();
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    validUserIds.add(userSnapshot.getKey());
                }
                callback.accept(validUserIds);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.accept(null);
            }
        });
    }

    /**
     * Cleanup tất cả các node liên quan
     */
    private void cleanupAllNodes(List<String> validUserIds) {
        CleanupTaskCounter taskCounter = new CleanupTaskCounter(8, onCompleted);

        cleanupPosts(validUserIds, taskCounter);
        cleanupHistories(validUserIds, taskCounter);
        cleanupMeetingSchedules(validUserIds, taskCounter);
        cleanupBills(validUserIds, taskCounter);
        cleanupServices(validUserIds, taskCounter);
        cleanupCarts(validUserIds, taskCounter);
        cleanupFollowPosts(validUserIds, taskCounter);
        cleanupContracts(validUserIds, taskCounter);
    }

    /**
     * Cleanup Posts node
     */
    private void cleanupPosts(List<String> validUserIds, CleanupTaskCounter counter) {
        databaseRef.child("Posts").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Object> updates = new HashMap<>();
                AtomicInteger removedCount = new AtomicInteger(0);

                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    String ownerId = postSnapshot.child("id_own_post").getValue(String.class);
                    if (ownerId != null && !validUserIds.contains(ownerId)) {
                        updates.put(postSnapshot.getKey(), null);
                        removedCount.incrementAndGet();
                    }
                    
                    DataSnapshot userLovePostSnapshot = postSnapshot.child("userLovePost");
                    if (userLovePostSnapshot.exists()) {
                        for (DataSnapshot loveSnapshot : userLovePostSnapshot.getChildren()) {
                            String userId = loveSnapshot.getKey();
                            if (userId != null && !validUserIds.contains(userId)) {
                                updates.put(postSnapshot.getKey() + "/userLovePost/" + userId, null);
                                removedCount.incrementAndGet();
                            }
                        }
                    }
                }

                if (!updates.isEmpty()) {
                    databaseRef.child("Posts").updateChildren(updates)
                        .addOnCompleteListener(task -> counter.taskCompleted());
                } else {
                    counter.taskCompleted();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                counter.taskCompleted();
            }
        });
    }

    /**
     * Cleanup Histories node
     */
    private void cleanupHistories(List<String> validUserIds, CleanupTaskCounter counter) {
        databaseRef.child("Histories").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Object> updates = new HashMap<>();

                for (DataSnapshot historySnapshot : snapshot.getChildren()) {
                    String userId = historySnapshot.getKey();
                    if (userId != null && !validUserIds.contains(userId)) {
                        updates.put(userId, null);
                    }
                }

                if (!updates.isEmpty()) {
                    databaseRef.child("Histories").updateChildren(updates)
                        .addOnCompleteListener(task -> counter.taskCompleted());
                } else {
                    counter.taskCompleted();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                counter.taskCompleted();
            }
        });
    }

    /**
     * Cleanup MeetingSchedules node
     */
    private void cleanupMeetingSchedules(List<String> validUserIds, CleanupTaskCounter counter) {
        databaseRef.child("MeetingSchedules").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Object> updates = new HashMap<>();

                for (DataSnapshot scheduleSnapshot : snapshot.getChildren()) {
                    String idFrom = scheduleSnapshot.child("idFrom").getValue(String.class);
                    String idTo = scheduleSnapshot.child("idTo").getValue(String.class);
                    
                    if ((idFrom != null && !validUserIds.contains(idFrom)) ||
                        (idTo != null && !validUserIds.contains(idTo))) {
                        updates.put(scheduleSnapshot.getKey(), null);
                    }
                }

                if (!updates.isEmpty()) {
                    databaseRef.child("MeetingSchedules").updateChildren(updates)
                        .addOnCompleteListener(task -> counter.taskCompleted());
                } else {
                    counter.taskCompleted();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                counter.taskCompleted();
            }
        });
    }

    /**
     * Cleanup Bills node
     */
    private void cleanupBills(List<String> validUserIds, CleanupTaskCounter counter) {
        databaseRef.child("Bills").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Object> updates = new HashMap<>();

                for (DataSnapshot billSnapshot : snapshot.getChildren()) {
                    String userId = billSnapshot.child("userId").getValue(String.class);
                    String sellerId = billSnapshot.child("sellerId").getValue(String.class);
                    
                    if ((userId != null && !validUserIds.contains(userId)) ||
                        (sellerId != null && !validUserIds.contains(sellerId))) {
                        updates.put(billSnapshot.getKey(), null);
                    }
                }

                if (!updates.isEmpty()) {
                    databaseRef.child("Bills").updateChildren(updates)
                        .addOnCompleteListener(task -> counter.taskCompleted());
                } else {
                    counter.taskCompleted();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                counter.taskCompleted();
            }
        });
    }

    /**
     * Cleanup Services node
     */
    private void cleanupServices(List<String> validUserIds, CleanupTaskCounter counter) {
        databaseRef.child("Services").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Object> updates = new HashMap<>();

                for (DataSnapshot categorySnapshot : snapshot.getChildren()) {
                    for (DataSnapshot serviceSnapshot : categorySnapshot.getChildren()) {
                        String sellerId = serviceSnapshot.child("id_seller").getValue(String.class);
                        if (sellerId != null && !validUserIds.contains(sellerId)) {
                            updates.put(categorySnapshot.getKey() + "/" + serviceSnapshot.getKey(), null);
                        }
                    }
                }

                if (!updates.isEmpty()) {
                    databaseRef.child("Services").updateChildren(updates)
                        .addOnCompleteListener(task -> counter.taskCompleted());
                } else {
                    counter.taskCompleted();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                counter.taskCompleted();
            }
        });
    }

    /**
     * Cleanup Carts node
     */
    private void cleanupCarts(List<String> validUserIds, CleanupTaskCounter counter) {
        databaseRef.child("Carts").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Object> updates = new HashMap<>();

                for (DataSnapshot cartSnapshot : snapshot.getChildren()) {
                    String userId = cartSnapshot.getKey();
                    if (userId != null && !validUserIds.contains(userId)) {
                        updates.put(userId, null);
                    } else {
                        for (DataSnapshot sellerSnapshot : cartSnapshot.getChildren()) {
                            String sellerId = sellerSnapshot.getKey();
                            if (sellerId != null && !validUserIds.contains(sellerId)) {
                                updates.put(userId + "/" + sellerId, null);
                            }
                        }
                    }
                }

                if (!updates.isEmpty()) {
                    databaseRef.child("Carts").updateChildren(updates)
                        .addOnCompleteListener(task -> counter.taskCompleted());
                } else {
                    counter.taskCompleted();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                counter.taskCompleted();
            }
        });
    }

    /**
     * Cleanup FollowPosts node
     */
    private void cleanupFollowPosts(List<String> validUserIds, CleanupTaskCounter counter) {
        databaseRef.child("FollowPosts").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Object> updates = new HashMap<>();

                for (DataSnapshot followSnapshot : snapshot.getChildren()) {
                    String userId = followSnapshot.getKey();
                    if (userId != null && !validUserIds.contains(userId)) {
                        updates.put(userId, null);
                    }
                }

                if (!updates.isEmpty()) {
                    databaseRef.child("FollowPosts").updateChildren(updates)
                        .addOnCompleteListener(task -> counter.taskCompleted());
                } else {
                    counter.taskCompleted();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                counter.taskCompleted();
            }
        });
    }

    /**
     * ✅ Cleanup Contracts node - METHOD MỚI
     */
    private void cleanupContracts(List<String> validUserIds, CleanupTaskCounter counter) {
        databaseRef.child("Contracts").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Object> updates = new HashMap<>();

                for (DataSnapshot contractSnapshot : snapshot.getChildren()) {
                    // ✅ Kiểm tra landlordId - chủ trọ
                    String landlordId = contractSnapshot.child("landlordId").getValue(String.class);
                    
                    // ✅ Nếu chủ trọ không còn tồn tại → xóa contract
                    if (landlordId != null && !validUserIds.contains(landlordId)) {
                        updates.put(contractSnapshot.getKey(), null);
                    }
                    
                    // ✅ Note: Không xóa contract nếu chỉ tenant không tồn tại
                    // vì tenant có thể được tạo thủ công trong contract
                    // chỉ xóa khi landlord (người tạo contract) không tồn tại
                }

                if (!updates.isEmpty()) {
                    databaseRef.child("Contracts").updateChildren(updates)
                        .addOnCompleteListener(task -> counter.taskCompleted());
                } else {
                    counter.taskCompleted();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                counter.taskCompleted();
            }
        });
    }

    /**
     * Class helper để đếm task hoàn thành
     */
    private static class CleanupTaskCounter {
        private final int totalTasks;
        private int completedTasks = 0;
        private final Runnable onAllCompleted;

        public CleanupTaskCounter(int totalTasks, Runnable onAllCompleted) {
            this.totalTasks = totalTasks;
            this.onAllCompleted = onAllCompleted;
        }

        public synchronized void taskCompleted() {
            completedTasks++;
            if (completedTasks >= totalTasks) {
                onAllCompleted.run();
            }
        }
    }
}
