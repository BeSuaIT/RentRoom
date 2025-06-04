package com.example.timphongtro.Activities;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.example.timphongtro.Models.Room;
import com.example.timphongtro.Models.Meeting;
import com.example.timphongtro.Models.User;
import com.example.timphongtro.R;
import com.example.timphongtro.Utils.GsonUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;

public class MeetingDetailActivity extends AppCompatActivity {
    
    private static final int STATUS_PENDING = 0;
    private static final int STATUS_APPROVED = 1;
    private static final int STATUS_REJECTED = 2;
    private ImageView imageViewBack;
    private TextView tvStatusIndicator;
    private MaterialCardView cardViewRoom, bottomActionCard;
    private LinearLayout userPost;
    private MaterialButton btnRefuse, btnAccept;
    private ImageView imgPost;
    private TextView PostTitle, RoomCost, DistrictName, DienTich, Size;
    private TextView circleAvatar, tvName, tvTime, tvNote;
    private ShapeableImageView profileImageView;
    private TextView tvprofileDetail, textViewNameUser;
    private ShapeableImageView ownerProfileImageView;
    private Meeting schedule;
    private Room room;
    private User bookerUser, ownerUser;
    private FirebaseUser currentUser;
    private DatabaseReference scheduleRef;
    private DecimalFormat decimalFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meeting_detail);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        decimalFormat = new DecimalFormat("#,###");
        
        initializeViews();
        setupViews();
        loadScheduleData();
    }

    private void initializeViews() {
        imageViewBack = findViewById(R.id.imageViewBack);
        tvStatusIndicator = findViewById(R.id.tvStatusIndicator);
        cardViewRoom = findViewById(R.id.cardViewRoom);
        userPost = findViewById(R.id.userPost);
        bottomActionCard = findViewById(R.id.bottomActionCard);
        btnRefuse = findViewById(R.id.btnRefuse);
        btnAccept = findViewById(R.id.btnAccept);
        imgPost = findViewById(R.id.imgPost);
        PostTitle = findViewById(R.id.PostTitle);
        RoomCost = findViewById(R.id.RoomCost);
        DistrictName = findViewById(R.id.DistrictName);
        DienTich = findViewById(R.id.DienTich);
        Size = findViewById(R.id.Size);
        circleAvatar = findViewById(R.id.circleAvatar);
        profileImageView = findViewById(R.id.profileImageView);
        tvName = findViewById(R.id.tvName);
        tvTime = findViewById(R.id.tvTime);
        tvNote = findViewById(R.id.tvNote);
        tvprofileDetail = findViewById(R.id.tvprofileDetail);
        ownerProfileImageView = findViewById(R.id.ownerProfileImageView);
        textViewNameUser = findViewById(R.id.textViewNameUser);
    }

    private void setupViews() {
        imageViewBack.setOnClickListener(v -> finish());
        cardViewRoom.setOnClickListener(v -> navigateToRoomDetail());
        userPost.setOnClickListener(v -> navigateToOwnerProfile());
        profileImageView.setOnClickListener(v -> navigateToBookerProfile());
        circleAvatar.setOnClickListener(v -> navigateToBookerProfile());
        
        setupActionButtons();
    }

    private void setupActionButtons() {
        boolean showButtons = getIntent().getIntExtra("showbtn", 1) == 1;
        bottomActionCard.setVisibility(showButtons ? View.VISIBLE : View.GONE);

        btnRefuse.setOnClickListener(v -> updateScheduleStatus(STATUS_REJECTED, "Đã từ chối lịch hẹn"));
        btnAccept.setOnClickListener(v -> updateScheduleStatus(STATUS_APPROVED, "Đã xác nhận lịch hẹn"));
    }

    private void loadScheduleData() {
        String scheduleString = getIntent().getStringExtra("scheduleData");
        if (scheduleString == null) {
            showToast("Lỗi: Không có dữ liệu lịch hẹn");
            finish();
            return;
        }

        schedule = GsonUtils.fromJson(scheduleString, Meeting.class);
        
        if (schedule != null) {
            updateScheduleUI();
            loadRoomData();
            loadBookerUserData();
        } else {
            showToast("Lỗi: Dữ liệu lịch hẹn không hợp lệ");
            finish();
        }
    }

    private void updateScheduleUI() {
        if (schedule == null) return;

        tvName.setText(schedule.getName() != null ? schedule.getName() : "Không có tên");
        tvTime.setText(formatDateTime(schedule.getTimeVisitRoom()));

        String note = schedule.getNote();
        if (note == null || note.trim().isEmpty() || "Không có ghi chú".equals(note)) {
            tvNote.setText("Không có ghi chú đặc biệt");
            tvNote.setTextColor(ContextCompat.getColor(this, R.color.text_hint));
        } else {
            tvNote.setText(note);
            tvNote.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        }

        String avatarText = getFirstLetter(schedule.getName());
        circleAvatar.setText(avatarText);
        profileImageView.setVisibility(View.GONE);
        circleAvatar.setVisibility(View.VISIBLE);

        setupStatusIndicator();
    }

    private void loadBookerUserData() {
        if (schedule == null) return;
        
        DatabaseReference userRef = FirebaseDatabase.getInstance()
            .getReference("Users")
            .child(schedule.getIdFrom());

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                bookerUser = snapshot.getValue(User.class);
                if (bookerUser != null) {
                    updateBookerAvatarUI();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void updateBookerAvatarUI() {
        if (bookerUser == null) return;
        
        String avatarUrl = bookerUser.getAvatarUrl();
        String displayName = bookerUser.getName();
        
        if (!TextUtils.isEmpty(avatarUrl)) {
            circleAvatar.setVisibility(View.GONE);
            profileImageView.setVisibility(View.VISIBLE);

            Glide.with(this)
                    .load(avatarUrl)
                    .placeholder(R.drawable.avatar)
                    .error(R.drawable.avatar)
                    .into(profileImageView);
        } else {
            profileImageView.setVisibility(View.GONE);
            circleAvatar.setVisibility(View.VISIBLE);
            circleAvatar.setText(getFirstLetter(displayName));
        }

        if (!TextUtils.isEmpty(displayName)) {
            tvName.setText(displayName);
        }
    }

    private void setupStatusIndicator() {
        if (schedule == null || currentUser == null) return;
        
        int status = Integer.parseInt(schedule.getStatus());
        boolean isReceiver = currentUser.getUid().equals(schedule.getIdTo());
        boolean isSender = currentUser.getUid().equals(schedule.getIdFrom());
        
        int backgroundColor;
        String statusText;
        
        switch (status) {
            case STATUS_PENDING:
                if (isReceiver) {
                    backgroundColor = R.color.status_pending;
                    statusText = "Chờ xác nhận";
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
        background.setColor(ContextCompat.getColor(this, backgroundColor));
        
        tvStatusIndicator.setBackground(background);
        tvStatusIndicator.setText(statusText);
        
        // Update avatar color based on status (only for text avatar)
        if (circleAvatar.getVisibility() == View.VISIBLE) {
            setupAvatarColor(backgroundColor);
        }
    }

    private void setupAvatarColor(int colorRes) {
        GradientDrawable avatarBackground = new GradientDrawable();
        avatarBackground.setShape(GradientDrawable.OVAL);
        avatarBackground.setColor(ContextCompat.getColor(this, colorRes));
        
        circleAvatar.setBackground(avatarBackground);
    }

    private void loadRoomData() {
        if (schedule == null) return;
        
        DatabaseReference roomRef = FirebaseDatabase.getInstance()
            .getReference("Posts")
            .child(schedule.getIdRoom());

        roomRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                room = snapshot.getValue(Room.class);
                if (room != null) {
                    updateRoomUI();
                    loadOwnerUserData();
                } else {
                    showToast("Không tìm thấy thông tin phòng");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showToast("Lỗi tải thông tin phòng: " + error.getMessage());
            }
        });
    }

    private void updateRoomUI() {
        if (room == null) return;

        PostTitle.setText(room.getTitle_room() != null ? room.getTitle_room() : "Phòng trọ");
        String formattedPrice = decimalFormat.format(room.getPrice_room()) + " đ/tháng";
        RoomCost.setText(formattedPrice);
        DienTich.setText((room.getArea_room() != null ? room.getArea_room() : "0") + "m²");
        Size.setText(room.getPerson_in_room() + " người");

        if (room.getAddress() != null) {
            String address = room.getAddress().getDistrict() + ", " + room.getAddress().getCity();
            DistrictName.setText(address);
        } else {
            DistrictName.setText("Địa chỉ không xác định");
        }

        if (room.getFirstImage() != null && !room.getFirstImage().isEmpty()) {
            RequestOptions requestOptions = new RequestOptions()
                    .transform(new RoundedCorners(24));
            
            Glide.with(this)
                    .load(room.getFirstImage())
                    .apply(requestOptions)
                    .placeholder(R.drawable.img_no_image)
                    .error(R.drawable.img_no_image)
                    .into(imgPost);
        } else {
            imgPost.setImageResource(R.drawable.img_no_image);
        }
    }

    private void loadOwnerUserData() {
        if (room == null) return;
        
        DatabaseReference userRef = FirebaseDatabase.getInstance()
            .getReference("Users")
            .child(room.getId_own_post());

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ownerUser = snapshot.getValue(User.class);
                if (ownerUser != null) {
                    updateOwnerUI();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void updateOwnerUI() {
        if (ownerUser == null) return;
        
        String avatarUrl = ownerUser.getAvatarUrl();
        String displayName = ownerUser.getName();
        String avatarText = getFirstLetter(displayName);
        tvprofileDetail.setText(avatarText);
        textViewNameUser.setText(displayName != null ? displayName : "Chủ phòng");
        
        if (!TextUtils.isEmpty(avatarUrl)) {
            tvprofileDetail.setVisibility(View.GONE);
            ownerProfileImageView.setVisibility(View.VISIBLE);

            Glide.with(this)
                    .load(avatarUrl)
                    .placeholder(R.drawable.avatar)
                    .error(R.drawable.avatar)
                    .into(ownerProfileImageView);
        } else {
            ownerProfileImageView.setVisibility(View.GONE);
            tvprofileDetail.setVisibility(View.VISIBLE);

            GradientDrawable ownerAvatarBackground = new GradientDrawable();
            ownerAvatarBackground.setShape(GradientDrawable.OVAL);
            ownerAvatarBackground.setColor(ContextCompat.getColor(this, R.color.blue_100));
            
            tvprofileDetail.setBackground(ownerAvatarBackground);
        }
    }

    private void updateScheduleStatus(int status, String message) {
        if (schedule == null) {
            showToast("Lỗi: Không có dữ liệu lịch hẹn");
            return;
        }
        
        scheduleRef = FirebaseDatabase.getInstance()
            .getReference("MeetingSchedules")
            .child(schedule.getIdSchedule());
            
        scheduleRef.child("status").setValue(String.valueOf(status))
            .addOnSuccessListener(unused -> {
                showToast(message);

                schedule.setStatus(String.valueOf(status));
                setupStatusIndicator();
                bottomActionCard.setVisibility(View.GONE);

                // Delay before closing
                new android.os.Handler().postDelayed(this::finish, 1500);
            })
            .addOnFailureListener(e -> {
                showToast("Lỗi cập nhật: " + e.getMessage());
            });
    }

    private void navigateToRoomDetail() {
        if (room != null) {
            String roomJson = GsonUtils.toJson(room);
            if (roomJson != null) {
                Intent intent = new Intent(this, PostDetailActivity.class);
                intent.putExtra("DataRoom", roomJson);
                startActivity(intent);
            } else {
                showToast("Lỗi mở chi tiết phòng");
            }
        } else {
            showToast("Thông tin phòng chưa được tải");
        }
    }

    private void navigateToOwnerProfile() {
        if (ownerUser != null) {
            String userJson = GsonUtils.toJson(ownerUser);
            if (userJson != null) {
                Intent intent = new Intent(this, UserActivity.class);
                intent.putExtra("userData", userJson);
                intent.putExtra("userType", "owner"); // Đánh dấu là chủ phòng
                startActivity(intent);
                return;
            } else {
                showToast("Lỗi phân tích dữ liệu chủ phòng");
            }
        }
        
        // Fallback nếu chưa load owner user
        if (room != null) {
            Intent intent = new Intent(this, UserActivity.class);
            intent.putExtra("id_own_post", room.getId_own_post());
            intent.putExtra("fallbackMode", true);
            intent.putExtra("userType", "owner");
            startActivity(intent);
        } else {
            showToast("Thông tin chủ phòng chưa được tải");
        }
    }

    private void navigateToBookerProfile() {
        if (bookerUser != null) {
            String userJson = GsonUtils.toJson(bookerUser);
            if (userJson != null) {
                Intent intent = new Intent(this, UserActivity.class);
                intent.putExtra("userData", userJson);
                intent.putExtra("userType", "booker"); // Đánh dấu là người đặt lịch
                startActivity(intent);
                return;
            } else {
                showToast("Lỗi phân tích dữ liệu người đặt lịch");
            }
        }
        
        // Fallback nếu chưa load booker user
        if (schedule != null) {
            Intent intent = new Intent(this, UserActivity.class);
            intent.putExtra("id_own_post", schedule.getIdFrom());
            intent.putExtra("fallbackMode", true);
            intent.putExtra("userType", "booker");
            startActivity(intent);
        } else {
            showToast("Thông tin người đặt lịch chưa được tải");
        }
    }

    private String formatDateTime(String dateTime) {
        if (dateTime == null || dateTime.trim().isEmpty()) {
            return "Chưa xác định thời gian";
        }
        
        try {
            // format: "14:30, 25/12/2024"
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

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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