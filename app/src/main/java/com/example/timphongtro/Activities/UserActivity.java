package com.example.timphongtro.Activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.timphongtro.Adapters.RoomAdapter;
import com.example.timphongtro.Models.Room;
import com.example.timphongtro.Models.User;
import com.example.timphongtro.R;
import com.example.timphongtro.Utils.AuthUtils;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class UserActivity extends AppCompatActivity {

    private TextView username, phone, email, postCount, loveCount;
    private TextView profileTextView;
    private ShapeableImageView profileImageView;
    private RecyclerView rcvUser;
    private RoomAdapter roomAdapter;
    private FirebaseDatabase database;
    private DatabaseReference postRef;
    private ArrayList<Room> roomArrayList;
    private LinearLayout phoneLinear, emailLinear;
    private String originalPhoneNumber = "";
    private String originalEmail = "";
    private FirebaseUser currentUser;
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
        profileTextView = findViewById(R.id.profileTextView);
        profileImageView = findViewById(R.id.profileImageView);
        rcvUser = findViewById(R.id.rcvUser);
        emailLinear = findViewById(R.id.emailLinear);
        phoneLinear = findViewById(R.id.phoneLinear);
        
        findViewById(R.id.imageView_back).setOnClickListener(v -> finish());
        
        database = FirebaseDatabase.getInstance();
        postRef = database.getReference("Posts");
        roomArrayList = new ArrayList<>();

        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        phoneLinear.setOnClickListener(v -> {
            if (currentUser == null) {
                AuthUtils.showLoginRequiredDialog(this,"sao chép số điện thoại", "có thể");
                return;
            }
            copyToClipboard("Số điện thoại", originalPhoneNumber);
        });

        emailLinear.setOnClickListener(v -> {
            if (currentUser == null) {
                AuthUtils.showLoginRequiredDialog(this,"sao chép email", "có thể");
                return;
            }
            copyToClipboard("Email", originalEmail);
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

                        phone.setText(maskPhoneNumber(user.getPhone()));
                        email.setText(maskEmail(user.getEmail()));

                        updateProfileImage(user);

                        originalPhoneNumber = user.getPhone();
                        originalEmail = user.getEmail();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(UserActivity.this, 
                        "Lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void updateProfileImage(User user) {
        String avatarUrl = user.getAvatarUrl();

        profileTextView.setText(getFirstLetter(user.getName()));
        
        if (!TextUtils.isEmpty(avatarUrl)) {
            profileImageView.setVisibility(View.INVISIBLE);
            profileTextView.setVisibility(View.VISIBLE);
            
            Glide.with(this)
                .load(avatarUrl)
                .placeholder(R.drawable.avatar)
                .error(R.drawable.avatar)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        profileImageView.setVisibility(View.GONE);
                        profileTextView.setVisibility(View.VISIBLE);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        profileTextView.setVisibility(View.GONE);
                        profileImageView.setVisibility(View.VISIBLE);
                        return false;
                    }
                })
                .into(profileImageView);
        } else {
            profileImageView.setVisibility(View.GONE);
            profileTextView.setVisibility(View.VISIBLE);
        }
    }

    private String getFirstLetter(String input) {
        String[] words = input.split(" ");
        StringBuilder result = new StringBuilder();

        if (words.length == 1) {
            result.append(words[0].charAt(0));
        } else {
            for (int i = 0; i < 2; i++) {
                result.append(words[i].charAt(0));
            }
        }
        return result.toString().toUpperCase();
    }

    private void copyToClipboard(String label, String content) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, content);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Đã sao chép " + label.toLowerCase(), Toast.LENGTH_SHORT).show();
    }

    private void loadUserPosts(String userId) {
        postRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                roomArrayList.clear();
                totalPosts = 0;
                totalLoves = 0;

                if (snapshot.exists()) {
                    for (DataSnapshot roomSnapshot : snapshot.getChildren()) {
                        Room room = roomSnapshot.getValue(Room.class);
                        if (room != null && userId.equals(room.getId_own_post())) {
                            roomArrayList.add(room);
                            totalPosts++;

                            DataSnapshot lovesSnapshot = roomSnapshot.child("userLovePost");
                            if (lovesSnapshot.exists()) {
                                totalLoves += lovesSnapshot.getChildrenCount();
                            }
                        }
                    }
                }

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

    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 7 || phoneNumber.equals("Chưa cập nhật")) {
            return phoneNumber;
        }
        
        String firstPart = phoneNumber.substring(0, 3);
        String lastPart = phoneNumber.substring(phoneNumber.length() - 2);
        String middlePart = "*".repeat(phoneNumber.length() - 5);
        
        return firstPart + middlePart + lastPart;
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@") || email.equals("Chưa cập nhật")) {
            return email;
        }
        
        String[] parts = email.split("@");
        String localPart = parts[0];
        String domain = parts[1];
        
        if (localPart.length() <= 3) {
            return email;
        }
        
        String maskedLocal = localPart.substring(0, 2) + "*".repeat(localPart.length() - 2);
        return maskedLocal + "@" + domain;
    }
}