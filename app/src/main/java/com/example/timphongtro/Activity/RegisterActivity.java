package com.example.timphongtro.Activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.timphongtro.Entity.User;
import com.example.timphongtro.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends AppCompatActivity {

    private EditText nameEditText, emailEditText, passwordEditText;
    private TextView loginTextView;
    private Button registerButton;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize views
        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        registerButton = findViewById(R.id.registerButton);
        loginTextView = findViewById(R.id.loginTextView);
        firebaseAuth = FirebaseAuth.getInstance();

        loginTextView.setOnClickListener(v -> {
            Intent i = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(i);
        });

        registerButton.setOnClickListener(v -> {
            String name = nameEditText.getText().toString();
            String email = emailEditText.getText().toString();
            String password = passwordEditText.getText().toString();

            if (TextUtils.isEmpty(name)) {
                nameEditText.setError("Vui lòng nhập tên");
                nameEditText.requestFocus();
            }
            if (TextUtils.isEmpty(email)) {
                emailEditText.setError("Vui lòng nhập email");
                emailEditText.requestFocus();
            }
            if (TextUtils.isEmpty(password)) {
                passwordEditText.setError("Vui lòng nhập mật khẩu");
                passwordEditText.requestFocus();
            }
            if (password.length() < 6) {
                passwordEditText.setError("Mật khẩu phải có ít nhất 6 ký tự");
                passwordEditText.requestFocus();
            }
            firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (user != null) {
                        user.sendEmailVerification().addOnCompleteListener(verificationTask -> {
                            if (verificationTask.isSuccessful()) {
                                Toast.makeText(getApplicationContext(), "Đăng ký thành công. Vui lòng kiểm tra email để xác minh.", Toast.LENGTH_SHORT).show();
                                registerButton.setEnabled(false);
                                saveUserData(user);
                                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                startActivity(intent);
                            } else {
                                Toast.makeText(getApplicationContext(), "Gửi email xác minh thất bại.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Đăng ký thất bại.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    // Save user data to Firebase Realtime Database
    private void saveUserData(FirebaseUser user) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference();
        String uid = user.getUid();
        String email = user.getEmail();
        String name = nameEditText.getText().toString();
        String phone = "";
        String permission = "user";

        User userData = new User(email, uid, name, phone, permission);
        databaseReference.child("Users").child(uid).setValue(userData);
    }
}