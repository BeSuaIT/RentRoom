package com.example.timphongtro.Utils;

import android.util.Log;
import com.example.timphongtro.Models.Meeting;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import androidx.annotation.NonNull;

/**
 * Lớp tiện ích xử lý các chức năng liên quan đến quản lý Meeting
 * Bao gồm: kiểm tra trạng thái, cập nhật meeting hết hạn
 */
public class MeetingUtils {
    private static final String TAG = "MeetingUtils";

    // Meeting statuses
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_APPROVED = 1;
    public static final int STATUS_REJECTED = 2;
    public static final int STATUS_EXPIRED = 3;

    /**
     * Interface callback để xử lý kết quả cập nhật meeting
     */
    public interface MeetingUpdateCallback {
        /**
         * Được gọi khi cập nhật thành công
         * @param updatedCount Số lượng meeting được cập nhật
         */
        void onSuccess(int updatedCount);

        /**
         * Được gọi khi có lỗi trong quá trình cập nhật
         * @param error Thông điệp lỗi
         */
        void onFailure(String error);
    }

    /**
     * Kiểm tra xem meeting có cần cập nhật trạng thái hay không
     * Chỉ cập nhật khi meeting đã quá thời gian hẹn
     *
     * @param meeting Meeting cần kiểm tra
     * @return true nếu cần cập nhật, false nếu không cần
     */
    public static boolean shouldUpdateStatus(Meeting meeting) {
        if (meeting == null) return false;

        // Không update nếu đã hết hạn rồi
        if (meeting.getStatus() == STATUS_EXPIRED) return false;

        // Chỉ update khi: thời gian hẹn đã qua và status chưa phải EXPIRED
        long now = System.currentTimeMillis();
        return (now > meeting.getTimeVisitRoom() && meeting.getStatus() != STATUS_EXPIRED);
    }

    /**
     * Lấy trạng thái hiện tại của meeting
     * Tự động tính toán dựa trên thời gian hiện tại
     *
     * @param meeting Meeting cần kiểm tra
     * @return Trạng thái hiện tại (0: pending, 1: approved, 2: rejected, 3: expired)
     */
    public static int getCurrentStatus(Meeting meeting) {
        if (meeting == null) return STATUS_PENDING;

        if (shouldUpdateStatus(meeting)) {
            return STATUS_EXPIRED; // Hết hạn
        }
        return meeting.getStatus();
    }

    /**
     * Kiểm tra meeting đã hết hạn hay chưa
     *
     * @param meeting Meeting cần kiểm tra
     * @return true nếu hết hạn, false nếu còn hiệu lực
     */
    public static boolean isExpired(Meeting meeting) {
        return getCurrentStatus(meeting) == STATUS_EXPIRED;
    }

    /**
     * Kiểm tra meeting có đang pending hay không
     *
     * @param meeting Meeting cần kiểm tra
     * @return true nếu đang pending, false nếu không
     */
    public static boolean isPending(Meeting meeting) {
        return meeting != null && getCurrentStatus(meeting) == STATUS_PENDING;
    }

    /**
     * Kiểm tra meeting có đã được xác nhận hay không
     *
     * @param meeting Meeting cần kiểm tra
     * @return true nếu đã xác nhận, false nếu không
     */
    public static boolean isApproved(Meeting meeting) {
        return meeting != null && getCurrentStatus(meeting) == STATUS_APPROVED;
    }

    /**
     * Kiểm tra meeting có bị từ chối hay không
     *
     * @param meeting Meeting cần kiểm tra
     * @return true nếu bị từ chối, false nếu không
     */
    public static boolean isRejected(Meeting meeting) {
        return meeting != null && getCurrentStatus(meeting) == STATUS_REJECTED;
    }

    /**
     * Đồng bộ trạng thái meeting với Firebase Database
     * Tự động cập nhật trạng thái nếu meeting đã hết hạn
     *
     * @param meeting Meeting cần đồng bộ
     */
    public static void syncMeetingStatusWithDatabase(Meeting meeting) {
        if (meeting == null || meeting.getIdSchedule() == null) {
            Log.w(TAG, "Meeting hoặc Meeting ID không hợp lệ");
            return;
        }

        // Lấy status cũ
        int oldStatus = meeting.getStatus();

        // Tính status mới
        int newStatus = getCurrentStatus(meeting);

        // Nếu status thay đổi, cập nhật database
        if (oldStatus != newStatus) {
            DatabaseReference meetingRef = FirebaseDatabase.getInstance()
                    .getReference("MeetingSchedules")
                    .child(meeting.getIdSchedule())
                    .child("status");

            meetingRef.setValue(newStatus)
                    .addOnSuccessListener(aVoid -> {
                        // Update local object
                        meeting.setStatus(newStatus);
                        Log.d(TAG, "Updated meeting status: " + meeting.getIdSchedule() + " -> " + newStatus);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to update meeting status: " + e.getMessage());
                    });
        }
    }

    /**
     * Cập nhật hàng loạt tất cả các meeting đã hết hạn trong database
     * Sử dụng khi khởi động app hoặc định kỳ để đồng bộ dữ liệu
     */
    public static void batchUpdateExpiredMeetings() {
        batchUpdateExpiredMeetings(null);
    }

    /**
     * Cập nhật hàng loạt tất cả các meeting đã hết hạn trong database với callback
     *
     * @param callback Callback xử lý kết quả cập nhật
     */
    public static void batchUpdateExpiredMeetings(MeetingUpdateCallback callback) {
        DatabaseReference meetingsRef = FirebaseDatabase.getInstance().getReference("MeetingSchedules");

        meetingsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long currentTime = System.currentTimeMillis();
                int updatedCount = 0;

                for (DataSnapshot meetingSnapshot : snapshot.getChildren()) {
                    Long timeVisitRoom = meetingSnapshot.child("timeVisitRoom").getValue(Long.class);
                    Integer status = meetingSnapshot.child("status").getValue(Integer.class);

                    if (timeVisitRoom != null && status != null) {
                        // Nếu hết hạn nhưng status chưa update
                        if (currentTime > timeVisitRoom && status != STATUS_EXPIRED) {
                            meetingSnapshot.getRef().child("status").setValue(STATUS_EXPIRED);
                            updatedCount++;
                        }
                    }
                }

                Log.d(TAG, "Đã cập nhật " + updatedCount + " meeting hết hạn");

                if (callback != null) {
                    callback.onSuccess(updatedCount);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                String errorMsg = "Lỗi cập nhật meeting hết hạn: " + error.getMessage();
                Log.e(TAG, errorMsg);

                if (callback != null) {
                    callback.onFailure(errorMsg);
                }
            }
        });
    }

    /**
     * Lấy text hiển thị cho status
     *
     * @param status Status code
     * @return Text hiển thị
     */
    public static String getStatusText(int status) {
        switch (status) {
            case STATUS_PENDING: return "Chờ xác nhận";
            case STATUS_APPROVED: return "Đã xác nhận";
            case STATUS_REJECTED: return "Đã từ chối";
            case STATUS_EXPIRED: return "Hết hạn";
            default: return "Không xác định";
        }
    }

    /**
     * Lấy text hiển thị cho status trong adapter
     *
     * @param status Status code
     * @param isReceiver Có phải người nhận không
     * @param isSender Có phải người gửi không
     * @return Text hiển thị
     */
    public static String getAdapterStatusText(int status, boolean isReceiver, boolean isSender) {
        switch (status) {
            case STATUS_PENDING:
                if (isReceiver) {
                    return "Xác nhận";
                } else if (isSender) {
                    return "Đã gửi";
                } else {
                    return "Chờ xử lý";
                }
            case STATUS_APPROVED: return "Đã xác nhận";
            case STATUS_REJECTED: return "Đã từ chối";
            case STATUS_EXPIRED: return "Hết hạn";
            default: return "Không xác định";
        }
    }
}