package com.example.timphongtro.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.example.timphongtro.Models.Room;
import com.example.timphongtro.Models.ScheduleVisitRoomClass;
import com.example.timphongtro.Models.User;
import com.example.timphongtro.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import java.text.DecimalFormat;

public class MeetingDetailActivity extends AppCompatActivity {
    private ImageView imageViewBack;
    private CardView cardViewRoom;
    private LinearLayout userPost;
    private Button btnRefuse, btnAccept;
    private ImageView imgPost;
    private TextView PostTitle, RoomCost, DistrictName, DienTich, Size;
    private TextView tvName, tvTime, tvNote;
    private TextView tvprofileDetail, textViewNameUser;
    private ScheduleVisitRoomClass schedule;
    private Room room;
    private User user;
    private DatabaseReference scheduleRef;
    private DecimalFormat decimalFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meeting_detail);

        decimalFormat = new DecimalFormat("#,###");
        
        initializeViews();
        setupViews();
        loadScheduleData();
    }

    private void initializeViews() {
        imageViewBack = findViewById(R.id.imageViewBack);
        cardViewRoom = findViewById(R.id.cardViewRoom);
        userPost = findViewById(R.id.userPost);
        btnRefuse = findViewById(R.id.btnRefuse);
        btnAccept = findViewById(R.id.btnAccept);
        imgPost = findViewById(R.id.imgPost);
        PostTitle = findViewById(R.id.PostTitle);
        RoomCost = findViewById(R.id.RoomCost);
        DistrictName = findViewById(R.id.DistrictName);
        DienTich = findViewById(R.id.DienTich);
        Size = findViewById(R.id.Size);
        tvName = findViewById(R.id.tvName);
        tvTime = findViewById(R.id.tvTime);
        tvNote = findViewById(R.id.tvNote);
        tvprofileDetail = findViewById(R.id.tvprofileDetail);
        textViewNameUser = findViewById(R.id.textViewNameUser);
    }

    private void setupViews() {
        imageViewBack.setOnClickListener(v -> finish());
        cardViewRoom.setOnClickListener(v -> navigateToRoomDetail());
        userPost.setOnClickListener(v -> navigateToUserProfile());
        
        setupActionButtons();
    }

    private void setupActionButtons() {
        boolean showButtons = getIntent().getIntExtra("showbtn", 1) == 1;
        btnRefuse.setVisibility(showButtons ? View.VISIBLE : View.GONE);
        btnAccept.setVisibility(showButtons ? View.VISIBLE : View.GONE);

        btnRefuse.setOnClickListener(v -> updateScheduleStatus("2", "Bạn từ chối thành công"));
        btnAccept.setOnClickListener(v -> updateScheduleStatus("1", "Bạn xác nhận thành công"));
    }

    private void loadScheduleData() {
        String scheduleString = getIntent().getStringExtra("scheduleData");
        if (scheduleString == null) {
            finish();
            return;
        }

        schedule = new Gson().fromJson(scheduleString, ScheduleVisitRoomClass.class);
        updateScheduleUI();
        loadRoomData();
        loadUserData();
    }

    private void updateScheduleUI() {
        tvName.setText(schedule.getName());
        tvTime.setText(schedule.getTimeVisitRoom());
        tvNote.setText(schedule.getNote());
    }

    private void loadRoomData() {
        DatabaseReference roomRef = FirebaseDatabase.getInstance()
            .getReference("Posts")
            .child(schedule.getIdRoom());

        roomRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                room = snapshot.getValue(Room.class);
                if (room != null) {
                    updateRoomUI();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showToast("Lỗi: " + error.getMessage());
            }
        });
    }

    private void updateRoomUI() {
        PostTitle.setText(room.getTitle_room());
        String formattedPrice = decimalFormat.format(room.getPrice_room()) + " đ/tháng";
        RoomCost.setText(formattedPrice);
        DistrictName.setText(room.getAddress().getAddress_combine());
        DienTich.setText("Diện tích: " + room.getArea_room());
        Size.setText("Số người: " + room.getPerson_in_room());
        
        Glide.with(this)
            .load(room.getFirstImage())
            .into(imgPost);
    }

    private void loadUserData() {
        DatabaseReference userOwnPostRef = FirebaseDatabase.getInstance()
            .getReference("Users")
            .child(schedule.getIdFrom());

        userOwnPostRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                user = snapshot.getValue(User.class);
                if (user != null) {
                    tvprofileDetail.setText(getFirstLetter(user.getName()));
                    textViewNameUser.setText(user.getName());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showToast("Lỗi: " + error.getMessage());
            }
        });
    }

    private void updateScheduleStatus(String status, String message) {
        scheduleRef = FirebaseDatabase.getInstance()
            .getReference("MeetingSchedules")
            .child(schedule.getIdSchedule());
            
        scheduleRef.child("status").setValue(status)
            .addOnSuccessListener(unused -> {
                showToast(message);
                finish();
            })
            .addOnFailureListener(e -> showToast("Lỗi: " + e.getMessage()));
    }

    private void navigateToRoomDetail() {
        if (room != null) {
            Intent intent = new Intent(this, PostDetailActivity.class);
            intent.putExtra("DataRoom", room.toString());
            startActivity(intent);
        }
    }

    private void navigateToUserProfile() {
        Intent intent = new Intent(this, UserActivity.class);
        intent.putExtra("id_own_post", schedule.getIdFrom());
        startActivity(intent);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    public static String getFirstLetter(String input) {
        String[] words = input.split(" ");
        StringBuilder result = new StringBuilder();

        if (words.length == 1) {
            // Nếu chuỗi chỉ có 1 từ, lấy chữ đầu từ đó
            result.append(words[0].charAt(0));
        } else {
            // Nếu chuỗi có nhiều từ, lấy chữ cái đầu của từ thứ 1 và 2
            for (int i = 0; i < 2; i++) {
                result.append(words[i].charAt(0));
            }
        }

        return result.toString().toUpperCase();
    }
}