package com.example.timphongtro.Activities;

import static android.widget.Toast.LENGTH_SHORT;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;

import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.timphongtro.R;
import com.example.timphongtro.Utils.Validator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

public class ForgotPasswordActivity extends AppCompatActivity {

    private FirebaseAuth auth = FirebaseAuth.getInstance();
    private EditText emailEditText;
    private AlertDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        initViews();
    }

    private void initViews() {
        emailEditText = findViewById(R.id.emailEditText);

        findViewById(R.id.backButton).setOnClickListener(v -> startActivity(new Intent(this, LoginActivity.class)));

        findViewById(R.id.forgotPasswordButton).setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            if (validateEmail(email)) {
                checkEmailAndSendReset(email);
            }
        });
    }

    private void checkEmailAndSendReset(String email) {
        showLoadingDialog();
        auth.signInWithEmailAndPassword(email, "dummy_password")
                .addOnFailureListener(e -> {
                    if (e instanceof FirebaseAuthInvalidUserException) {
                        hideLoadingDialog();
                        showToast("Email chưa được đăng ký");
                    } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                        sendPasswordResetEmail(email);
                    } else {
                        hideLoadingDialog();
                        showToast("Đã xảy ra lỗi, vui lòng thử lại");
                    }
                });
    }

    private void sendPasswordResetEmail(String email) {
        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    hideLoadingDialog();
                    if (task.isSuccessful()) {
                        showToast("Đã gửi email khôi phục mật khẩu");
                        finish();
                    } else {
                        showToast("Không thể gửi email khôi phục");
                    }
                });
    }

    private boolean validateEmail(String email) {
        if (TextUtils.isEmpty(email)) {
            setError(emailEditText, "Vui lòng nhập Email");
            return false;
        }
        if (!Validator.isValidEmail(email)) {
            setError(emailEditText, "Email không hợp lệ");
            return false;
        }
        return true;
    }

    private void showToast(String message) {
        Toast.makeText(this, message, LENGTH_SHORT).show();
    }

    private void setError(EditText editText, String error) {
        editText.setError(error);
        editText.requestFocus();
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
}