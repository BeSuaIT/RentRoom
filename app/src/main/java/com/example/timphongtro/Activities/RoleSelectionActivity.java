package com.example.timphongtro.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.example.timphongtro.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RoleSelectionActivity extends AppCompatActivity {
    private RadioGroup roleRadioGroup;
    private Button confirmButton;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_selection);

        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("Users");

        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            navigateToLogin();
            return;
        }

        if (!user.isEmailVerified()) {
            Intent intent = new Intent(this, WaitingVerificationActivity.class);
            intent.putExtra("email", user.getEmail());
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        checkUserRole(user);
    }

    private void checkUserRole(FirebaseUser user) {
        databaseReference.child(user.getUid()).child("role").get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    if (task.getResult().exists() && 
                        task.getResult().getValue() != null &&
                        !task.getResult().getValue().toString().isEmpty()) {
                        navigateToMain();
                    } else {
                        initViews();
                        setupBackPressHandler();
                    }
                } else {
                    initViews();
                    setupBackPressHandler();
                }
            });
    }

    private void setupBackPressHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Toast.makeText(RoleSelectionActivity.this, 
                    "Vui lòng chọn vai trò để tiếp tục", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initViews() {
        roleRadioGroup = findViewById(R.id.roleRadioGroup);
        confirmButton = findViewById(R.id.confirmButton);

        roleRadioGroup.check(R.id.tenantRadio);

        findViewById(R.id.landlordCard).setOnClickListener(v -> {
            roleRadioGroup.check(R.id.landlordRadio);
        });

        findViewById(R.id.tenantCard).setOnClickListener(v -> {
            roleRadioGroup.check(R.id.tenantRadio);
        });

        confirmButton.setOnClickListener(v -> handleRoleSelection());
    }

    private void handleRoleSelection() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            navigateToLogin();
            return;
        }

        if (!user.isEmailVerified()) {
            Toast.makeText(this, "Email chưa được xác minh", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, WaitingVerificationActivity.class);
            intent.putExtra("email", user.getEmail());
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        int selectedId = roleRadioGroup.getCheckedRadioButtonId();
        
        if (selectedId == -1) {
            Toast.makeText(this, "Vui lòng chọn vai trò", Toast.LENGTH_SHORT).show();
            return;
        }

        String selectedRole;
        if (selectedId == R.id.landlordRadio) {
            selectedRole = "Chủ trọ";
        } else {
            selectedRole = "Người thuê";
        }

        confirmButton.setEnabled(false);
        confirmButton.setText("Đang cập nhật...");

        databaseReference.child(user.getUid()).child("role").setValue(selectedRole)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                    navigateToMain();
                })
                .addOnFailureListener(e -> {
                    confirmButton.setEnabled(true);
                    confirmButton.setText("Xác nhận");
                    Toast.makeText(this, "Lỗi cập nhật thông tin", Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}