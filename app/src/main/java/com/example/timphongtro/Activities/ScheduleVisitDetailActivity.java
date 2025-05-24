package com.example.timphongtro.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.timphongtro.Models.Room;
import com.example.timphongtro.Models.ScheduleVisitRoomClass;
import com.example.timphongtro.Models.User;
import com.example.timphongtro.R;
import com.example.timphongtro.databinding.ActivityScheduleVisitDetailBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import java.text.DecimalFormat;

public class ScheduleVisitDetailActivity extends AppCompatActivity {
    private ScheduleVisitRoomClass schedule;
    private Room room;
    private User user;
    private DatabaseReference scheduleRef;
    private ActivityScheduleVisitDetailBinding binding;
    private DecimalFormat decimalFormat;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityScheduleVisitDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        decimalFormat = new DecimalFormat("#,###");
        
        setupViews();
        loadScheduleData();
    }

    private void setupViews() {
        binding.imageViewBack.setOnClickListener(v -> finish());
        binding.cardViewRoom.setOnClickListener(v -> navigateToRoomDetail());
        binding.userPost.setOnClickListener(v -> navigateToUserProfile());
        
        setupActionButtons();
    }

    private void setupActionButtons() {
        boolean showButtons = getIntent().getIntExtra("showbtn", 1) == 1;
        binding.btnRefuse.setVisibility(showButtons ? View.VISIBLE : View.GONE);
        binding.btnAccept.setVisibility(showButtons ? View.VISIBLE : View.GONE);

        binding.btnRefuse.setOnClickListener(v -> updateScheduleStatus("2", "Bạn từ chối thành công"));
        binding.btnAccept.setOnClickListener(v -> updateScheduleStatus("1", "Bạn xác nhận thành công"));
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
        binding.tvName.setText(schedule.getName());
        binding.tvTime.setText(schedule.getTimeVisitRoom());
        binding.tvNote.setText(schedule.getNote());
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
        binding.PostTitle.setText(room.getTitle_room());
        String formattedPrice = decimalFormat.format(room.getPrice_room()) + " đ/tháng";
        binding.RoomCost.setText(formattedPrice);
        binding.DistrictName.setText(room.getAddress().getAddress_combine());
        binding.DienTich.setText("Diện tích: " + room.getArea_room());
        binding.Size.setText("Số người: " + room.getPerson_in_room());
        
        Glide.with(this)
            .load(room.getFirstImage())
            .into(binding.imgPost);
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
                    binding.tvprofileDetail.setText(getFirstLetter(user.getName()));
                    binding.textViewNameUser.setText(user.getName());
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
            Intent intent = new Intent(this, DetailRoomActivity.class);
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