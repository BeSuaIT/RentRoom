package com.example.timphongtro.Activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.timphongtro.Models.User;
import com.example.timphongtro.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.regex.Pattern;
import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {
    private FirebaseDatabase database;
    private FirebaseUser currentUser;
    private DatabaseReference userRef;
    private User user;
    private EditText nameEditText, emailEditText, phoneEditText;
    private ImageView backButton, editImageButton;
    private LinearLayout emailContainer;
    private Button updateButton;
    private ActivityResultLauncher<Intent> phoneVerificationLauncher;
    private ShapeableImageView profileImageView;
    private TextView profileTextView;
    private Uri selectedImageUri;
    private ActivityResultLauncher<String> imagePickerLauncher;
    private StorageReference storageRef;
    private ValueEventListener userValueEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        registerActivityResultLauncher();
        registerImagePicker();
        initializeViews();
        initializeFirebase();
        initializeStorage();
        loadUserData();
    }

    private void registerActivityResultLauncher() {
        phoneVerificationLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    finish();
                }
            }
        );
    }

    private void registerImagePicker() {
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    updateProfileImagePreview();
                }
            }
        );
    }

    private void initializeViews() {
        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        backButton = findViewById(R.id.backButton);
        updateButton = findViewById(R.id.updateButton);
        emailContainer = findViewById(R.id.emailContainer);
        profileImageView = findViewById(R.id.profileImageView);
        profileTextView = findViewById(R.id.profileTextView);
        editImageButton = findViewById(R.id.editImageButton);

        emailEditText.setEnabled(false);

        backButton.setOnClickListener(v -> finish());
        updateButton.setOnClickListener(v -> validateAndUpdateProfile());
        emailContainer.setOnClickListener(v -> showToast("Không được chỉnh sửa trường email"));
        editImageButton.setOnClickListener(v -> showImageOptionsDialog());
    }

    private void showImageOptionsDialog() {
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_profile_image, null);
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(bottomSheetView);

        TextView tvChangeImage = bottomSheetView.findViewById(R.id.tvChangeImage);
        TextView tvRemoveImage = bottomSheetView.findViewById(R.id.tvRemoveImage);

        tvChangeImage.setOnClickListener(v -> {
            imagePickerLauncher.launch("image/*");
            dialog.dismiss();
        });

        tvRemoveImage.setOnClickListener(v -> {
            removeProfileImage();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void removeProfileImage() {
        if (TextUtils.isEmpty(user.getAvatarUrl())) {
            return;
        }

        if (user.getAvatarUrl().contains("Avatars")) {
            StorageReference photoRef = FirebaseStorage.getInstance().getReferenceFromUrl(user.getAvatarUrl());
            photoRef.delete().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    updateUserProfile(user.getName(), user.getPhone(), "");
                } else {
                    showToast("Lỗi xóa ảnh");
                }
            });
        } else {
            updateUserProfile(user.getName(), user.getPhone(), "");
        }
    }

    private void initializeFirebase() {
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        database = FirebaseDatabase.getInstance();
        userRef = database.getReference("Users/" + currentUser.getUid());
    }

    private void initializeStorage() {
        storageRef = FirebaseStorage.getInstance().getReference();
    }

    private void loadUserData() {
        userValueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    user = snapshot.getValue(User.class);
                    if (user != null) {
                        nameEditText.setText(user.getName());
                        emailEditText.setText(user.getEmail());

                        String phone = user.getPhone();
                        if (phone != null && 
                            !phone.trim().isEmpty() && 
                            !phone.equals("Chưa cập nhật") && 
                            !phone.equals("null")) {
                            phoneEditText.setText(phone);
                        } else {
                            phoneEditText.setText("");
                        }
                        
                        if (!isFinishing() && !isDestroyed()) {
                            updateProfileImage();
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showToast("Lỗi tải dữ liệu");
            }
        };

        userRef.addValueEventListener(userValueEventListener);
    }

    private void updateProfileImage() {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        
        String avatarUrl = user.getAvatarUrl();
        if (!TextUtils.isEmpty(avatarUrl)) {
            profileTextView.setVisibility(View.GONE);
            profileImageView.setVisibility(View.VISIBLE);
            Glide.with(this)
                .load(avatarUrl)
                .into(profileImageView);
        } else {
            profileImageView.setVisibility(View.GONE);
            profileTextView.setVisibility(View.VISIBLE);
            profileTextView.setText(getFirstLetter(user.getName()));
        }
    }

    @Override
    protected void onDestroy() {
        if (userRef != null && userValueEventListener != null) {
            userRef.removeEventListener(userValueEventListener);
        }
        super.onDestroy();
    }

    private void updateProfileImagePreview() {
        if (selectedImageUri != null && !isFinishing() && !isDestroyed()) {
            profileTextView.setVisibility(View.GONE);
            profileImageView.setVisibility(View.VISIBLE);
            Glide.with(this)
                .load(selectedImageUri)
                .into(profileImageView);
        }
    }

    private String getFirstLetter(String input) {
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

    private void uploadProfileImage(String name, String phone) {
        if (selectedImageUri == null) {
            updateUserProfile(name, phone, user.getAvatarUrl());
            return;
        }

        String fileName = "avatar_" + currentUser.getUid() + ".jpg";
        StorageReference avatarRef = storageRef.child("Avatars").child(fileName);

        avatarRef.putFile(selectedImageUri)
            .continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                return avatarRef.getDownloadUrl();
            })
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String downloadUrl = task.getResult().toString();
                    updateUserProfile(name, phone, downloadUrl);
                } else {
                    showToast("Lỗi tải ảnh lên");
                }
            });
    }

    private void updateUserProfile(String name, String phone, String avatarUrl) {
        String currentPhone = user.getPhone();
        if (currentPhone == null) currentPhone = "Chưa cập nhật";
        
        if (!phone.equals(currentPhone) && !phone.equals("Chưa cập nhật")) {
            Intent intent = new Intent(this, PhoneVerificationActivity.class);
            intent.putExtra("phone", phone);
            phoneVerificationLauncher.launch(intent);
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("phone", phone);
        updates.put("avatarUrl", avatarUrl);

        userRef.updateChildren(updates)
            .addOnSuccessListener(aVoid -> {
                if (avatarUrl.isEmpty()) {
                    showToast("Đã xóa ảnh đại diện");
                    profileImageView.setVisibility(View.GONE);
                    profileTextView.setVisibility(View.VISIBLE);
                    profileTextView.setText(getFirstLetter(name));
                } else {
                    showToast("Cập nhật thành công");
                }
                finish();
            })
            .addOnFailureListener(e -> showToast("Cập nhật thất bại: " + e.getMessage()));
    }

    private void validateAndUpdateProfile() {
        String name = nameEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            showToast("Vui lòng nhập họ và tên");
            return;
        }

        if (!TextUtils.isEmpty(phone) && !isValidPhoneNumber(phone)) {
            showToast("Vui lòng nhập đúng định dạng số điện thoại (10 chữ số)");
            return;
        }

        if (TextUtils.isEmpty(phone)) {
            phone = "Chưa cập nhật";
        }

        uploadProfileImage(name, phone);
    }

    private boolean isValidPhoneNumber(String phone) {
        return Pattern.matches("^\\d{10}$", phone);
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
}