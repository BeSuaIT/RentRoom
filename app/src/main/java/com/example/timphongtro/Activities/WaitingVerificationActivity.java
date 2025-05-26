package com.example.timphongtro.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.timphongtro.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class WaitingVerificationActivity extends AppCompatActivity {
    private FirebaseAuth firebaseAuth;
    private Handler verificationHandler;
    private Runnable verificationChecker;
    private TextView statusTextView;
    private Button resendButton, changeEmailButton;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting_verification);

        firebaseAuth = FirebaseAuth.getInstance();
        userEmail = getIntent().getStringExtra("email");

        initViews();
        setupBackPressHandler();
        startVerificationCheck();
    }

    private void setupBackPressHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Toast.makeText(WaitingVerificationActivity.this, 
                    "Vui lòng xác minh email để tiếp tục", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initViews() {
        statusTextView = findViewById(R.id.statusTextView);
        resendButton = findViewById(R.id.resendButton);
        changeEmailButton = findViewById(R.id.changeEmailButton);

        statusTextView.setText("Chúng tôi đã gửi email xác minh đến:\n" + userEmail +
                "\n\nVui lòng kiểm tra hộp thư và click vào link xác minh.");

        resendButton.setOnClickListener(v -> resendVerificationEmail());
        changeEmailButton.setOnClickListener(v -> changeEmail());
    }

    private void startVerificationCheck() {
        verificationHandler = new Handler(Looper.getMainLooper());
        verificationChecker = new Runnable() {
            @Override
            public void run() {
                checkEmailVerification();
                verificationHandler.postDelayed(this, 3000);
            }
        };
        verificationHandler.post(verificationChecker);
    }

    private void checkEmailVerification() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            user.reload().addOnCompleteListener(task -> {
                if (task.isSuccessful() && user.isEmailVerified()) {
                    stopVerificationCheck();
                    navigateToRoleSelection();
                }
            });
        }
    }

    private void stopVerificationCheck() {
        if (verificationHandler != null && verificationChecker != null) {
            verificationHandler.removeCallbacks(verificationChecker);
        }
    }

    private void navigateToRoleSelection() {
        Intent intent = new Intent(this, RoleSelectionActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void resendVerificationEmail() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            resendButton.setEnabled(false);
            resendButton.setText("Đang gửi...");

            user.sendEmailVerification()
                    .addOnCompleteListener(task -> {
                        resendButton.setEnabled(true);
                        resendButton.setText("Gửi lại email");

                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Email xác minh đã được gửi lại", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Gửi email thất bại", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void changeEmail() {
        stopVerificationCheck();
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            user.delete().addOnCompleteListener(task -> {
                firebaseAuth.signOut();
                Intent intent = new Intent(this, RegisterActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopVerificationCheck();
    }
}