package com.example.timphongtro.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.timphongtro.Models.User;
import com.example.timphongtro.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.regex.Pattern;

public class EditProfileActivity extends AppCompatActivity {
    private FirebaseDatabase database;
    private FirebaseUser currentUser;
    private DatabaseReference userRef;
    private User user;
    private EditText nameEditText, emailEditText, phoneEditText;
    private ImageView backButton;
    private LinearLayout emailContainer;
    private Button updateButton;
    private ActivityResultLauncher<Intent> phoneVerificationLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        registerActivityResultLauncher();
        initializeViews();
        initializeFirebase();
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

    private void initializeViews() {
        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        backButton = findViewById(R.id.backButton);
        updateButton = findViewById(R.id.updateButton);
        emailContainer = findViewById(R.id.emailContainer);

        emailEditText.setEnabled(false);

        backButton.setOnClickListener(v -> finish());
        updateButton.setOnClickListener(v -> validateAndUpdateProfile());
        emailContainer.setOnClickListener(v -> showToast("Không được chỉnh sửa trường email"));
    }

    private void initializeFirebase() {
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        database = FirebaseDatabase.getInstance();
        userRef = database.getReference("Users/" + currentUser.getUid());
    }

    private void loadUserData() {
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    user = snapshot.getValue(User.class);
                    if (user != null) {
                        nameEditText.setText(user.getName());
                        emailEditText.setText(user.getEmail());
                        phoneEditText.setText(user.getPhone());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showToast("Lỗi tải dữ liệu");
            }
        });
    }

    private void validateAndUpdateProfile() {
        String name = nameEditText.getText().toString();
        String phone = phoneEditText.getText().toString();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(phone)) {
            showToast("Vui lòng nhập đầy đủ các trường thông tin");
            return;
        }

        if (!isValidPhoneNumber(phone)) {
            showToast("Vui lòng nhập đúng định dạng số điện thoại");
            return;
        }

        updateUserProfile(name, phone);
    }

    private boolean isValidPhoneNumber(String phone) {
        return Pattern.matches("^\\d{10}$", phone);
    }

    private void updateUserProfile(String name, String phone) {
        if (!phone.equals(user.getPhone())) {
            Intent intent = new Intent(this, PhoneVerificationActivity.class);
            intent.putExtra("phone", phone);
            phoneVerificationLauncher.launch(intent);
            return;
        }

        User updatedUser = new User(
                user.getEmail(),
                currentUser.getUid(),
                name,
                user.getPhone(),
                user.getPermission(),
                user.getCreatedAt()
        );

        userRef.setValue(updatedUser)
                .addOnSuccessListener(aVoid -> {
                    showToast("Cập nhật thành công");
                    finish();
                })
                .addOnFailureListener(e ->
                    showToast("Cập nhật thất bại")
                );
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
}