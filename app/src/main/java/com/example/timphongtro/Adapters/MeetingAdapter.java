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
import com.example.timphongtro.Utils.GsonUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

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
        View view = LayoutInflater.from(context).inflate(R.layout.view_holder_schedule_visit_room, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MeetingAdapter.ViewHolder holder, int position) {
        Meeting meeting = schedules.get(position);
        holder.bind(meeting, position);
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
                        if (snapshot.exists()) {
                            User user = snapshot.getValue(User.class);
                            if (user != null) {
                                updateAvatarUI(holder, user, userName);
                            }
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

            //Setup background color cho text avatar
            setupAvatarColor(holder.circleImageView, schedules.get(holder.getAbsoluteAdapterPosition()));
        }
    }

    private void loadRoomInfo(ViewHolder holder, String roomId) {
        Room room = null;
        for (Room r : availableRooms) {
            if (r.getRoomID().equals(roomId)) {
                room = r;
                break;
            }
        }
        
        if (room != null) {
            updateRoomUI(holder, room);
        } else {
            holder.tvRoomTitle.setText("Phòng trọ");
            holder.tvAddress.setText("Đang tải thông tin...");

            FirebaseDatabase.getInstance().getReference("Rooms")
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
        holder.tvRoomTitle.setText(room.getRoomTitle() != null ? room.getRoomTitle() : "Phòng trọ");
        if (room.getAddress() != null) {
            String address = room.getAddress().getAddress_combine();
            holder.tvAddress.setText(address);
        } else {
            holder.tvAddress.setText("Địa chỉ không xác định");
        }
    }

    private void loadRoomData() {
        FirebaseDatabase.getInstance().getReference("Rooms")
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
        
        int status = schedule.getStatus();

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
        if (schedule == null) {
            avatarView.setBackgroundResource(R.drawable.circle_avatar_background);
            return;
        }

        int backgroundColor;
        int status = schedule.getStatus();
        
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

    private boolean shouldShowConfirmButtons(Meeting schedule, String userId) {
        return STATUS_PENDING == schedule.getStatus() && userId.equals(schedule.getIdTo());
    }

    private void navigateToDetail(Meeting schedule, String currentUserId) {
        Intent intent = new Intent(context, MeetingDetailActivity.class);

        String scheduleJson = GsonUtils.toJson(schedule);
        if (scheduleJson != null) {
            intent.putExtra("scheduleData", scheduleJson);
            
            boolean showButtons = shouldShowConfirmButtons(schedule, currentUserId);
            intent.putExtra("showbtn", showButtons ? 1 : 0);
            
            context.startActivity(intent);
        } else {
            Toast.makeText(context, "Lỗi mở chi tiết lịch hẹn", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int getItemCount() {
        return schedules.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvRoomTitle, tvTime, tvNote, tvAddress, tvStatus;
        TextView circleImageView;
        ShapeableImageView profileImageView;
        MaterialButton btn_revoke;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            
            tvName = itemView.findViewById(R.id.tvName);
            tvRoomTitle = itemView.findViewById(R.id.tvRoomTitle);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvNote = itemView.findViewById(R.id.tvNote);
            tvAddress = itemView.findViewById(R.id.tvAddress);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            circleImageView = itemView.findViewById(R.id.circleImageView);
            profileImageView = itemView.findViewById(R.id.profileImageView);
            btn_revoke = itemView.findViewById(R.id.btn_revoke);
        }

        public void bind(Meeting meeting, int position) {
            if (meeting == null) return;

            bindExistingData(meeting);
            setupRevokeButton(meeting, position);
            setupItemClickListener(meeting);
        }

        private void bindExistingData(Meeting meeting) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) return;
            
            String currentUserId = currentUser.getUid();

            String contactUserId = currentUserId.equals(meeting.getIdFrom()) ? 
                                  meeting.getIdTo() : meeting.getIdFrom();
            
            loadUserInfo(contactUserId);
            loadRoomInfo(this, meeting.getIdRoom());

            String formattedTime = formatTimestamp(meeting.getTimeVisitRoom());
            tvTime.setText(formattedTime);
            
            tvNote.setText(meeting.getNote() != null ? meeting.getNote() : "Không có ghi chú");
            setupStatusView(tvStatus, meeting, currentUserId);
        }

        private String formatTimestamp(long timestamp) {
            if (timestamp <= 0) {
                return "Chưa xác định thời gian";
            }

            try {
                Date date = new java.util.Date(timestamp);
                SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", new java.util.Locale("vi", "VN"));
                SimpleDateFormat dateTimeFormat = new SimpleDateFormat("HH:mm, dd/MM/yyyy", new java.util.Locale("vi", "VN"));

                String dayOfWeek = dayFormat.format(date);
                String dateTime = dateTimeFormat.format(date);

                return dayOfWeek + ", " + dateTime; // "Thứ Hai, 14:30, 25/12/2024"
            } catch (Exception e) {
                return "Thời gian không hợp lệ";
            }
        }

        private void loadUserInfo(String userId) {
            FirebaseDatabase.getInstance().getReference("Users")
                    .child(userId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                User user = snapshot.getValue(User.class);
                                if (user != null) {
                                    tvName.setText(user.getName() != null ? user.getName() : "Người dùng");
                                    loadUserAvatar(ViewHolder.this, userId, user.getName());
                                }
                            } else {
                                tvName.setText("Người dùng không tồn tại");

                                circleImageView.setVisibility(View.VISIBLE);
                                profileImageView.setVisibility(View.GONE);
                                circleImageView.setText("?");
                                setupAvatarColor(circleImageView, schedules.get(getAbsoluteAdapterPosition()));
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            tvName.setText("Lỗi tải thông tin");

                            circleImageView.setVisibility(View.VISIBLE);
                            profileImageView.setVisibility(View.GONE);
                            circleImageView.setText("?");
                            setupAvatarColor(circleImageView, schedules.get(getAbsoluteAdapterPosition()));
                        }
                    });
        }

        private void setupRevokeButton(Meeting meeting, int position) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) {
                btn_revoke.setVisibility(View.GONE);
                return;
            }

            if (meeting.getStatus() == STATUS_PENDING && 
                currentUser.getUid().equals(meeting.getIdFrom())) {
                btn_revoke.setVisibility(View.VISIBLE);
                btn_revoke.setOnClickListener(v -> showRevokeDialog(meeting, position));
            } else {
                btn_revoke.setVisibility(View.GONE);
            }
        }

        private void setupItemClickListener(Meeting meeting) {
            itemView.setOnClickListener(v -> {
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser != null) {
                    navigateToDetail(meeting, currentUser.getUid());
                }
            });
        }

        private void showRevokeDialog(Meeting meeting, int position) {
            new AlertDialog.Builder(context)
                    .setTitle("Thu hồi lịch hẹn")
                    .setMessage("Bạn có chắc chắn muốn thu hồi lịch hẹn này không?\n\nHành động này không thể hoàn tác.")
                    .setPositiveButton("Thu hồi", (dialog, which) -> {
                        revokeMeeting(meeting, position);
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        }

        private void revokeMeeting(Meeting meeting, int position) {
            if (meeting == null || TextUtils.isEmpty(meeting.getIdSchedule())) {
                Toast.makeText(context, "Lỗi: Thông tin lịch hẹn không hợp lệ", Toast.LENGTH_SHORT).show();
                return;
            }

            btn_revoke.setEnabled(false);
            btn_revoke.setText("Đang xử lý...");

            DatabaseReference meetingRef = FirebaseDatabase.getInstance()
                    .getReference("MeetingSchedules")
                    .child(meeting.getIdSchedule());

            meetingRef.removeValue().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(context, "Thu hồi lịch hẹn thành công", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Lỗi thu hồi lịch hẹn", Toast.LENGTH_SHORT).show();

                    btn_revoke.setEnabled(true);
                    btn_revoke.setText("Thu hồi");
                }
            });
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
