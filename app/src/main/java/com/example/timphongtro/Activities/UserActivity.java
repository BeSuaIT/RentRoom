package com.example.timphongtro.Activities;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.timphongtro.Adapters.RoomAdapter;
import com.example.timphongtro.Models.Room;
import com.example.timphongtro.Models.User;
import com.example.timphongtro.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class UserActivity extends AppCompatActivity {

    private TextView username, phone, email, circleImageView, postCount, loveCount;
    private RecyclerView rcvUser;
    private RoomAdapter roomAdapter;
    private FirebaseDatabase database;
    private DatabaseReference postRef;
    private ArrayList<Room> roomArrayList;
    private LinearLayout phoneLinear, emailLinear;
    private static final int CALL_PHONE_PERMISSION_REQUEST_CODE = 1;
    private int totalPosts = 0;
    private int totalLoves = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);

        initializeViews();
        setupRecyclerView();

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            String userId = bundle.getString("id_own_post");
            if (userId != null) {
                loadUserData(userId);
                loadUserPosts(userId);
            }
        }
    }

    private void initializeViews() {
        username = findViewById(R.id.username);
        phone = findViewById(R.id.phone);
        email = findViewById(R.id.email);
        postCount = findViewById(R.id.postCount);
        loveCount = findViewById(R.id.loveCount);
        circleImageView = findViewById(R.id.circleImageView);
        rcvUser = findViewById(R.id.rcvUser);
        emailLinear = findViewById(R.id.emailLinear);
        phoneLinear = findViewById(R.id.phoneLinear);
        
        findViewById(R.id.imageView_back).setOnClickListener(v -> finish());
        
        database = FirebaseDatabase.getInstance();
        postRef = database.getReference("Rooms");
        roomArrayList = new ArrayList<>();

        phoneLinear.setOnClickListener(v -> {
            String phoneNumber = phone.getText().toString();
            showOptionsDialog("Số điện thoại", phoneNumber);
        });

        emailLinear.setOnClickListener(v -> {
            String emailAddress = email.getText().toString();
            copyToClipboard("Email", emailAddress);
        });
    }

    private void setupRecyclerView() {
        roomAdapter = new RoomAdapter(this, roomArrayList);
        rcvUser.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        rcvUser.setAdapter(roomAdapter);
    }

    private void loadUserData(String userId) {
        database.getReference("Users").child(userId)
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        username.setText(user.getName());
                        phone.setText(user.getPhone());
                        email.setText(user.getEmail());
                        circleImageView.setText(getFirstLetter(user.getName()));
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(UserActivity.this, 
                        "Lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
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

    private void showOptionsDialog(String title, final String content) {
        new AlertDialog.Builder(this)
            .setTitle(title)
            .setItems(new String[]{"Gọi điện", "Sao chép"}, (dialog, which) -> {
                switch (which) {
                    case 0:
                        if (checkCallPermission()) {
                            makePhoneCall(content);
                        }
                        break;
                    case 1:
                        copyToClipboard(title, content);
                        break;
                }
            })
            .show();
    }

    private void copyToClipboard(String label, String content) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, content);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Đã sao chép " + label.toLowerCase(), Toast.LENGTH_SHORT).show();
    }

    private boolean checkCallPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.CALL_PHONE},
                    CALL_PHONE_PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    private void makePhoneCall(String phoneNumber) {
        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:" + phoneNumber));
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CALL_PHONE_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                makePhoneCall(phone.getText().toString());
            } else {
                Toast.makeText(this, "Quyền gọi điện bị từ chối", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadUserPosts(String userId) {
        postRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                roomArrayList.clear();
                totalPosts = 0;
                totalLoves = 0;

                if (snapshot.exists()) {
                    // Loop through room types (Tro, ChungCuMini)
                    for (DataSnapshot typeSnapshot : snapshot.getChildren()) {
                        // Loop through rooms
                        for (DataSnapshot roomSnapshot : typeSnapshot.getChildren()) {
                            Room room = roomSnapshot.getValue(Room.class);
                            if (room != null && userId.equals(room.getId_own_post())) {
                                // Count posts
                                totalPosts++;
                                
                                // Count total loves from userLovePost
                                DataSnapshot lovesSnapshot = roomSnapshot.child("userLovePost");
                                if (lovesSnapshot.exists()) {
                                    totalLoves += lovesSnapshot.getChildrenCount();
                                }

                                // Only add available rooms to the list
                                if (room.getStatus_room() != 1) {
                                    roomArrayList.add(room);
                                }
                            }
                        }
                    }
                }

                // Update UI
                postCount.setText(String.valueOf(totalPosts));
                loveCount.setText(String.valueOf(totalLoves));
                roomAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(UserActivity.this, 
                    "Lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}