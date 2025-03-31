package com.example.timphongtro.Activity;

import android.content.Intent;
import android.os.Bundle;

import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.timphongtro.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

public class ForgotPasswordActivity extends AppCompatActivity {

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        auth = FirebaseAuth.getInstance();
        EditText emailEditText = findViewById(R.id.emailEditText);
        Button forgotPasswordButton = findViewById(R.id.forgotPasswordButton);
        Button backButton = findViewById(R.id.backButton);

        backButton.setOnClickListener(v -> {
            Intent i = new Intent(ForgotPasswordActivity.this, LoginActivity.class);
            startActivity(i);
        });

        forgotPasswordButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            if (validateEmail(email)) {
                checkEmailAndSendReset(email);
            }
        });

    }

    private void checkEmailAndSendReset(String email) {
        // First check if user exists
        auth.signInWithEmailAndPassword(email, "dummy_password")
                .addOnFailureListener(e -> {
                    if (e instanceof FirebaseAuthInvalidUserException) {
                        // Email doesn't exist
                        Toast.makeText(this, "Email chưa được đăng ký", Toast.LENGTH_SHORT).show();
                    } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                        // Email exists, proceed with password reset
                        auth.sendPasswordResetEmail(email)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(this, "Đã gửi email khôi phục mật khẩu", Toast.LENGTH_SHORT).show();
                                        finish();
                                    } else {
                                        Toast.makeText(this, "Không thể gửi email khôi phục", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                });
    }

    private boolean validateEmail(String email) {
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Vui lòng nhập Email", Toast.LENGTH_SHORT).show();
            return false;
        }

        String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
        if (!email.matches(emailPattern)) {
            Toast.makeText(this, "Email không hợp lệ", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

}