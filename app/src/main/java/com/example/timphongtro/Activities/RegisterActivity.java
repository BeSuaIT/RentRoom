package com.example.timphongtro.Activities;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;

import com.example.timphongtro.Models.User;
import com.example.timphongtro.R;
import com.example.timphongtro.Utils.Validator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends AppCompatActivity {
    private EditText nameEditText, emailEditText, passwordEditText;
    private AlertDialog loadingDialog;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initFirebase();
        initViews();
    }

    private void initFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("Users");
    }

    private void initViews() {
        if (firebaseAuth.getCurrentUser() != null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        findViewById(R.id.loginTextView).setOnClickListener(v -> startActivity(new Intent(this, LoginActivity.class)));
        findViewById(R.id.registerButton).setOnClickListener(v -> handleRegister());
    }

    private void handleRegister() {
        String name = nameEditText.getText().toString();
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        if (!validateInput(name, email, password)) return;

        showLoadingDialog();
        checkEmailExists(email, password);
    }

    private boolean validateInput(String name, String email, String password) {
        if (TextUtils.isEmpty(name)) {
            setError(nameEditText, "Vui lòng nhập tên");
            return false;
        }
        if (TextUtils.isEmpty(email)) {
            setError(emailEditText, "Vui lòng nhập email");
            return false;
        }
        if (!Validator.isValidEmail(email)) {
            setError(emailEditText, "Email không hợp lệ");
            return false;
        }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            setError(passwordEditText, "Mật khẩu phải có ít nhất 6 ký tự");
            return false;
        }
        return true;
    }

    private void checkEmailExists(String email, String password) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult().getUser() != null) {
                    sendVerificationEmail(task.getResult().getUser());
                } else {
                    hideLoadingDialog();
                    if (task.getException() instanceof FirebaseAuthException) {
                        FirebaseAuthException e = (FirebaseAuthException) task.getException();
                        if (e.getErrorCode().equals("ERROR_EMAIL_ALREADY_IN_USE")) {
                            setError(emailEditText, "Email này đã được sử dụng");
                        } else {
                            showToast("Lỗi đăng ký: " + e.getMessage());
                        }
                    } else {
                        showToast("Lỗi đăng ký");
                    }
                }
            });
    }

    private void sendVerificationEmail(FirebaseUser user) {
        user.sendEmailVerification()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    saveUserData(user);
                } else {
                    hideLoadingDialog();
                    showToast("Gửi email xác minh thất bại");
                }
            });
    }

    private void saveUserData(FirebaseUser user) {
        User userData = new User(
            user.getEmail(),
            user.getUid(),
            nameEditText.getText().toString(),
            "",
            "user",
            System.currentTimeMillis(),
                ""
        );

        databaseReference.child(user.getUid()).setValue(userData)
            .addOnSuccessListener(unused -> {
                hideLoadingDialog();
                showToast("Đăng ký thành công. Vui lòng kiểm tra email để xác minh tài khoản");
                firebaseAuth.signOut();

                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            });
    }

    private void showLoadingDialog() {
        if (loadingDialog == null) {
            loadingDialog = new AlertDialog.Builder(this)
                .setView(R.layout.progress_layout)
                .setCancelable(false)
                .create();
        }
        loadingDialog.show();
    }

    private void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    private void setError(EditText editText, String error) {
        editText.setError(error);
        editText.requestFocus();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}