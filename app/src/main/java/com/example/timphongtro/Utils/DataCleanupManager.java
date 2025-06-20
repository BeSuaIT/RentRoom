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
        getValidUserIds(validUserIds -> {
            if (validUserIds != null) {
                cleanupPosts(validUserIds);
                cleanupUsersNestedData(validUserIds);
                cleanupMeetingSchedules(validUserIds);
                cleanupBills(validUserIds);
                cleanupServices(validUserIds);
                cleanupCarts(validUserIds);
                cleanupContracts(validUserIds);

                if (onCompleted != null) {
                    onCompleted.run();
                }
            } else {
                if (onFailed != null) {
                    onFailed.accept("Failed to get valid user IDs");
                }
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
     * Cleanup Posts node
     */
    private void cleanupPosts(List<String> validUserIds) {
        databaseRef.child("Posts").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Object> updates = new HashMap<>();

                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    String ownerId = postSnapshot.child("id_own_post").getValue(String.class);
                    if (ownerId != null && !validUserIds.contains(ownerId)) {
                        updates.put(postSnapshot.getKey(), null);
                    }
                    
                    DataSnapshot userLovePostSnapshot = postSnapshot.child("userLovePost");
                    if (userLovePostSnapshot.exists()) {
                        for (DataSnapshot loveSnapshot : userLovePostSnapshot.getChildren()) {
                            String userId = loveSnapshot.getKey();
                            if (userId != null && !validUserIds.contains(userId)) {
                                updates.put(postSnapshot.getKey() + "/userLovePost/" + userId, null);
                            }
                        }
                    }
                }

                if (!updates.isEmpty()) {
                    databaseRef.child("Posts").updateChildren(updates);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    /**
     * Cleanup dữ liệu nested trong Users
     */
    private void cleanupUsersNestedData(List<String> validUserIds) {
        databaseRef.child("Users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String userId = userSnapshot.getKey();
                    if (userId != null && validUserIds.contains(userId)) {
                        // Cleanup histories
                        DataSnapshot historiesSnapshot = userSnapshot.child("histories");
                        if (historiesSnapshot.exists()) {
                            checkAndCleanupRoomReferences(userId, "histories", historiesSnapshot);
                        }

                        // Cleanup followPosts
                        DataSnapshot followPostsSnapshot = userSnapshot.child("followPosts");
                        if (followPostsSnapshot.exists()) {
                            checkAndCleanupRoomReferences(userId, "followPosts", followPostsSnapshot);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    /**
     * Helper method: Kiểm tra và cleanup room references
     */
    private void checkAndCleanupRoomReferences(String userId, String nodeName, DataSnapshot roomRefsSnapshot) {
        databaseRef.child("Posts").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot postsSnapshot) {
                List<String> validRoomIds = new ArrayList<>();
                for (DataSnapshot postSnapshot : postsSnapshot.getChildren()) {
                    validRoomIds.add(postSnapshot.getKey());
                }

                Map<String, Object> updates = new HashMap<>();
                for (DataSnapshot roomRefSnapshot : roomRefsSnapshot.getChildren()) {
                    String roomId = roomRefSnapshot.getKey();
                    if (roomId != null && !validRoomIds.contains(roomId)) {
                        updates.put(userId + "/" + nodeName + "/" + roomId, null);
                    }
                }

                if (!updates.isEmpty()) {
                    databaseRef.child("Users").updateChildren(updates);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    /**
     * Cleanup MeetingSchedules node
     */
    private void cleanupMeetingSchedules(List<String> validUserIds) {
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
                    databaseRef.child("MeetingSchedules").updateChildren(updates);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    /**
     * Cleanup Bills node
     */
    private void cleanupBills(List<String> validUserIds) {
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
                    databaseRef.child("Bills").updateChildren(updates);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    /**
     * Cleanup Services node
     */
    private void cleanupServices(List<String> validUserIds) {
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
                    databaseRef.child("Services").updateChildren(updates);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    /**
     * Cleanup Carts node
     */
    private void cleanupCarts(List<String> validUserIds) {
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
                    databaseRef.child("Carts").updateChildren(updates);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    /**
     * Cleanup Contracts node
     */
    private void cleanupContracts(List<String> validUserIds) {
        databaseRef.child("Contracts").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Object> updates = new HashMap<>();

                for (DataSnapshot contractSnapshot : snapshot.getChildren()) {
                    String landlordId = contractSnapshot.child("landlordId").getValue(String.class);
                    if (landlordId != null && !validUserIds.contains(landlordId)) {
                        updates.put(contractSnapshot.getKey(), null);
                    }
                }

                if (!updates.isEmpty()) {
                    databaseRef.child("Contracts").updateChildren(updates);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}
