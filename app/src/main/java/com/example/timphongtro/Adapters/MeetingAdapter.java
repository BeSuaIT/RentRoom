package com.example.timphongtro.Adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.timphongtro.Activities.MeetingDetailActivity;
import com.example.timphongtro.Models.Meeting;
import com.example.timphongtro.Models.Room;
import com.example.timphongtro.Models.User;
import com.example.timphongtro.R;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import java.util.ArrayList;

public class MeetingAdapter extends RecyclerView.Adapter<MeetingAdapter.ViewHolder> {
    private static final int STATUS_PENDING = 0;
    private static final int STATUS_APPROVED = 1;
    private static final int STATUS_REJECTED = 2;

    Context context;
    ArrayList<Meeting> schedules;
    ArrayList<Room> availableRooms;

    public MeetingAdapter(Context context, ArrayList<Meeting> schedules) {
        this.context = context;
        this.schedules = schedules;
        this.availableRooms = new ArrayList<>();
        loadRoomData();
    }

    @NonNull
    @Override
    public MeetingAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.view_holder_schedule_visit_room, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MeetingAdapter.ViewHolder holder, int position) {
        Meeting schedule = schedules.get(position);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        
        if (schedule == null || currentUser == null) return;

        holder.tvName.setText(schedule.getName());
        holder.tvTime.setText(formatDateTime(schedule.getTimeVisitRoom()));

        loadUserAvatar(holder, schedule.getIdFrom(), schedule.getName());

        loadRoomInfo(holder, schedule.getIdRoom());

        String note = schedule.getNote();
        if (note == null || note.trim().isEmpty() || "Không có ghi chú".equals(note)) {
            holder.tvNote.setText("Không có ghi chú đặc biệt");
            holder.tvNote.setTextColor(ContextCompat.getColor(context, R.color.text_hint));
        } else {
            holder.tvNote.setText(note);
            holder.tvNote.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
        }

        setupStatusView(holder.tvStatus, schedule, currentUser.getUid());

        if (shouldShowConfirmButtons(schedule, currentUser.getUid())) {
            holder.tvStatus.setOnClickListener(v -> showConfirmationDialog(schedule));
            holder.tvStatus.setClickable(true);
        } else {
            holder.tvStatus.setOnClickListener(null);
            holder.tvStatus.setClickable(false);
        }

        holder.itemView.setOnClickListener(v -> navigateToDetail(schedule, currentUser.getUid()));
    }

    private void loadUserAvatar(ViewHolder holder, String userId, String userName) {
        holder.circleImageView.setText(getFirstLetter(userName));
        holder.profileImageView.setVisibility(View.GONE);
        holder.circleImageView.setVisibility(View.VISIBLE);

        FirebaseDatabase.getInstance().getReference("Users")
                .child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User user = snapshot.getValue(User.class);
                        if (user != null) {
                            updateAvatarUI(holder, user, userName);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void updateAvatarUI(ViewHolder holder, User user, String fallbackName) {
        String avatarUrl = user != null ? user.getAvatarUrl() : null;
        String displayName = user != null ? user.getName() : fallbackName;
        
        if (!TextUtils.isEmpty(avatarUrl)) {
            holder.circleImageView.setVisibility(View.GONE);
            holder.profileImageView.setVisibility(View.VISIBLE);

            Glide.with(context)
                    .load(avatarUrl)
                    .placeholder(R.drawable.avatar)
                    .error(R.drawable.avatar)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(holder.profileImageView);
        } else {
            holder.profileImageView.setVisibility(View.GONE);
            holder.circleImageView.setVisibility(View.VISIBLE);
            holder.circleImageView.setText(getFirstLetter(displayName));

            setupAvatarColor(holder.circleImageView, schedules.get(holder.getAbsoluteAdapterPosition()));
        }
    }

    private void loadRoomInfo(ViewHolder holder, String roomId) {
        Room room = null;
        for (Room r : availableRooms) {
            if (r.getId_room().equals(roomId)) {
                room = r;
                break;
            }
        }
        
        if (room != null) {
            updateRoomUI(holder, room);
        } else {
            holder.tvRoomTitle.setText("Phòng trọ");
            holder.tvAddress.setText("Đang tải thông tin...");

            FirebaseDatabase.getInstance().getReference("Posts")
                    .child(roomId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Room loadedRoom = snapshot.getValue(Room.class);
                            if (loadedRoom != null) {
                                updateRoomUI(holder, loadedRoom);
                                availableRooms.add(loadedRoom);
                            } else {
                                holder.tvRoomTitle.setText("Phòng không tìm thấy");
                                holder.tvAddress.setText("Thông tin không có sẵn");
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            holder.tvRoomTitle.setText("Lỗi tải thông tin");
                            holder.tvAddress.setText("Vui lòng thử lại");
                        }
                    });
        }
    }

    private void updateRoomUI(ViewHolder holder, Room room) {
        holder.tvRoomTitle.setText(room.getTitle_room());
        if (room.getAddress() != null) {
            String address = room.getAddress().getAddress_combine() + ", " +
                           room.getAddress().getDistrict() + ", " +
                           room.getAddress().getCity();
            holder.tvAddress.setText(address);
        } else {
            holder.tvAddress.setText("Địa chỉ không xác định");
        }
    }

    private void loadRoomData() {
        FirebaseDatabase.getInstance().getReference("Posts")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        availableRooms.clear();
                        if (snapshot.exists()) {
                            for (DataSnapshot roomSnapshot : snapshot.getChildren()) {
                                Room room = roomSnapshot.getValue(Room.class);
                                if (room != null) {
                                    availableRooms.add(room);
                                }
                            }
                        }
                        notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void setupStatusView(TextView statusView, Meeting schedule, String userId) {
        int backgroundColor;
        String statusText;
        
        boolean isReceiver = userId.equals(schedule.getIdTo());
        boolean isSender = userId.equals(schedule.getIdFrom());
        
        int status = Integer.parseInt(schedule.getStatus());

        switch (status) {
            case STATUS_PENDING:
                if (isReceiver) {
                    backgroundColor = R.color.status_pending;
                    statusText = "Xác nhận";
                } else if (isSender) {
                    backgroundColor = R.color.status_sent;
                    statusText = "Đã gửi";
                } else {
                    backgroundColor = R.color.gray_500;
                    statusText = "Chờ xử lý";
                }
                break;
                
            case STATUS_APPROVED:
                backgroundColor = R.color.status_approved;
                statusText = "Đã xác nhận";
                break;
                
            case STATUS_REJECTED:
                backgroundColor = R.color.status_rejected;
                statusText = "Đã từ chối";
                break;
                
            default:
                backgroundColor = R.color.gray_500;
                statusText = "Không xác định";
                break;
        }

        // Create rounded background
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(14f);
        background.setColor(ContextCompat.getColor(context, backgroundColor));
        
        statusView.setBackground(background);
        statusView.setText(statusText);
        statusView.setTextColor(ContextCompat.getColor(context, R.color.white_100));
    }

    private void setupAvatarColor(TextView avatarView, Meeting schedule) {
        int backgroundColor;
        int status = Integer.parseInt(schedule.getStatus());
        
        switch (status) {
            case STATUS_PENDING:
                backgroundColor = R.color.orange_100;
                break;
            case STATUS_APPROVED:
                backgroundColor = R.color.green_100;
                break;
            case STATUS_REJECTED:
                backgroundColor = R.color.red_100;
                break;
            default:
                backgroundColor = R.color.gray_500;
                break;
        }

        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.OVAL);
        background.setColor(ContextCompat.getColor(context, backgroundColor));
        
        avatarView.setBackground(background);
    }

    private String formatDateTime(String dateTime) {
        if (dateTime == null || dateTime.isEmpty()) {
            return "Chưa xác định";
        }
        
        try {
            if (dateTime.contains(",")) {
                String[] parts = dateTime.split(",");
                if (parts.length >= 2) {
                    String time = parts[0].trim();
                    String date = parts[1].trim();
                    return time + ", " + date;
                }
            }
            return dateTime;
        } catch (Exception e) {
            return dateTime;
        }
    }

    private boolean shouldShowConfirmButtons(Meeting schedule, String userId) {
        return STATUS_PENDING == Integer.parseInt(schedule.getStatus()) 
               && userId.equals(schedule.getIdTo());
    }

    private void showConfirmationDialog(Meeting schedule) {
        new AlertDialog.Builder(context)
                .setTitle("Xác nhận lịch hẹn")
                .setMessage("Bạn có muốn xác nhận lịch hẹn này không?")
                .setPositiveButton("Xác nhận", (dialog, which) ->
                    updateScheduleStatus(schedule, STATUS_APPROVED))
                .setNegativeButton("Từ chối", (dialog, which) ->
                    updateScheduleStatus(schedule, STATUS_REJECTED))
                .setNeutralButton("Hủy", null)
                .show();
    }

    private void updateScheduleStatus(Meeting schedule, int status) {
        FirebaseDatabase.getInstance()
                .getReference("MeetingSchedules")
                .child(schedule.getIdSchedule())
                .child("status")
                .setValue(String.valueOf(status))
                .addOnSuccessListener(unused -> {
                    String message = status == STATUS_APPROVED ? 
                            "✅ Đã xác nhận lịch hẹn" : "Đã từ chối lịch hẹn";
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateToDetail(Meeting schedule, String currentUserId) {
        Intent intent = new Intent(context, MeetingDetailActivity.class);
        
        intent.putExtra("scheduleData", new Gson().toJson(schedule));
        
        boolean showButtons = shouldShowConfirmButtons(schedule, currentUserId);
        intent.putExtra("showbtn", showButtons ? 1 : 0);
        
        context.startActivity(intent);
    }

    @Override
    public int getItemCount() {
        return schedules.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvTime, tvNote, tvStatus, tvRoomTitle, tvAddress, circleImageView;
        ShapeableImageView profileImageView;

        public ViewHolder(@NonNull View view) {
            super(view);
            circleImageView = view.findViewById(R.id.circleImageView);
            profileImageView = view.findViewById(R.id.profileImageView);
            tvName = view.findViewById(R.id.tvName);
            tvTime = view.findViewById(R.id.tvTime);
            tvNote = view.findViewById(R.id.tvNote);
            tvStatus = view.findViewById(R.id.tvStatus);
            tvRoomTitle = view.findViewById(R.id.tvRoomTitle);
            tvAddress = view.findViewById(R.id.tvAddress);
        }
    }

    public static String getFirstLetter(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "?";
        }
        
        String[] words = input.trim().split("\\s+");
        StringBuilder result = new StringBuilder();

        if (words.length == 1) {
            result.append(words[0].charAt(0));
        } else {
            for (int i = 0; i < Math.min(words.length, 2); i++) {
                if (words[i].length() > 0) {
                    result.append(words[i].charAt(0));
                }
            }
        }

        return result.toString().toUpperCase();
    }
}
