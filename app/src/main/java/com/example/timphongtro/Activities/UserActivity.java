package com.example.timphongtro.Activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
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
import com.example.timphongtro.Utils.GsonUtils;
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
        handleIntentData();
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
        postRef = database.getReference("Rooms");
        roomArrayList = new ArrayList<>();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        setupClickListeners();
    }

    private void setupClickListeners() {
        phoneLinear.setOnClickListener(v -> {
            if (currentUser == null) {
                AuthUtils.showLoginRequiredDialog(this, "sao chép số điện thoại", "có thể");
                return;
            }
            copyToClipboard("Số điện thoại", originalPhoneNumber);
        });

        emailLinear.setOnClickListener(v -> {
            if (currentUser == null) {
                AuthUtils.showLoginRequiredDialog(this, "sao chép email", "có thể");
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

    private void handleIntentData() {
        Bundle bundle = getIntent().getExtras();
        if (bundle == null) {
            showToast("Thiếu dữ liệu người dùng");
            finish();
            return;
        }

        String userDataJson = bundle.getString("userData");
        if (userDataJson != null) {
            User user = GsonUtils.fromJson(userDataJson, User.class);
            if (user != null) {
                displayUserData(user);
                loadUserPosts(getUserId(user));
                return;
            } else {
                showToast("Lỗi phân tích dữ liệu nguời dùng");
            }
        }

        String userId = bundle.getString("id_own_post");
        boolean isFallback = bundle.getBoolean("fallbackMode", false);
        
        if (userId != null) {
            if (isFallback) {
                showToast("Đang tải thông tin người dùng...");
            }
            loadUserDataFromFirebase(userId);
            loadUserPosts(userId);
        } else {
            showToast("Không tìm thấy thông tin người dùng");
            finish();
        }
    }

    private String getUserId(User user) {
        return user.getUid() != null ? user.getUid() : "";
    }

    private void displayUserData(User user) {
        if (user == null) return;

        username.setText(user.getName() != null ? user.getName() : "Không có tên");

        String userPhone = user.getPhone();
        if (userPhone == null || userPhone.trim().isEmpty()) {
            userPhone = "Chưa cập nhật";
        }
        phone.setText(maskPhoneNumber(userPhone));
        originalPhoneNumber = userPhone;

        String userEmail = user.getEmail();
        if (userEmail == null || userEmail.trim().isEmpty()) {
            userEmail = "Chưa cập nhật";
        }
        email.setText(maskEmail(userEmail));
        originalEmail = userEmail;

        updateProfileImage(user);
    }

    // Load user data từ Firebase (fallback)
    private void loadUserDataFromFirebase(String userId) {
        database.getReference("Users").child(userId)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        displayUserData(user);
                    } else {
                        showToast("Không tìm thấy thông tin người dùng");
                        finish();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    showToast("Lỗi tải dữ liệu: " + error.getMessage());
                    finish();
                }
            });
    }

    private void updateProfileImage(User user) {
        if (user == null) return;
        
        String avatarUrl = user.getAvatarUrl();
        String userName = user.getName() != null ? user.getName() : "User";

        profileTextView.setText(getFirstLetter(userName));
        
        if (!TextUtils.isEmpty(avatarUrl)) {
            profileImageView.setVisibility(View.INVISIBLE);
            profileTextView.setVisibility(View.VISIBLE);
            
            Glide.with(this)
                .load(avatarUrl)
                .placeholder(R.drawable.avatar)
                .error(R.drawable.avatar)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                @NonNull Target<Drawable> target, boolean isFirstResource) {
                        profileImageView.setVisibility(View.GONE);
                        profileTextView.setVisibility(View.VISIBLE);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(@NonNull Drawable resource, @NonNull Object model,
                                                   Target<Drawable> target, @NonNull DataSource dataSource,
                                                   boolean isFirstResource) {
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

    private void copyToClipboard(String label, String content) {
        if (content == null || content.equals("Chưa cập nhật")) {
            showToast("Thông tin " + label.toLowerCase() + " chưa được cập nhật");
            return;
        }
        
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, content);
        clipboard.setPrimaryClip(clip);
        showToast("Đã sao chép " + label.toLowerCase());
    }

    private void loadUserPosts(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            showToast("Không thể tải bài đăng: thiếu ID người dùng");
            return;
        }
        
        postRef.addListenerForSingleValueEvent(new ValueEventListener() {
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
                showToast("Lỗi tải bài đăng: " + error.getMessage());
            }
        });
    }

    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 7 || 
            phoneNumber.equals("Chưa cập nhật") || phoneNumber.trim().isEmpty()) {
            return "Chưa cập nhật";
        }
        
        // Remove spaces and special characters for masking
        String cleanPhone = phoneNumber.replaceAll("[^0-9]", "");
        
        if (cleanPhone.length() < 7) {
            return phoneNumber; // Return original if too short
        }
        
        String firstPart = cleanPhone.substring(0, 3);
        String lastPart = cleanPhone.substring(cleanPhone.length() - 2);
        String middlePart = "*".repeat(cleanPhone.length() - 5);
        
        return firstPart + middlePart + lastPart;
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@") || 
            email.equals("Chưa cập nhật") || email.trim().isEmpty()) {
            return "Chưa cập nhật";
        }
        
        String[] parts = email.split("@");
        String localPart = parts[0];
        String domain = parts[1];
        
        if (localPart.length() <= 3) {
            return email; // Don't mask very short emails
        }
        
        String maskedLocal = localPart.substring(0, 2) + "*".repeat(localPart.length() - 2);
        return maskedLocal + "@" + domain;
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}