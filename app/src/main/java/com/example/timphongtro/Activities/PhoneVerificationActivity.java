package com.example.timphongtro.Activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.timphongtro.R;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.concurrent.TimeUnit;

public class PhoneVerificationActivity extends AppCompatActivity {
    private EditText[] otpEdits = new EditText[6];
    private String verificationId;
    private FirebaseAuth auth;
    private String newPhoneNumber;
    private DatabaseReference userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_verification);

        auth = FirebaseAuth.getInstance();
        newPhoneNumber = getIntent().getStringExtra("phone");

        initViews();
        setupOTPInputs();
        sendVerificationCode();
    }

    private void initViews() {
        otpEdits[0] = findViewById(R.id.otpEdit1);
        otpEdits[1] = findViewById(R.id.otpEdit2);
        otpEdits[2] = findViewById(R.id.otpEdit3);
        otpEdits[3] = findViewById(R.id.otpEdit4);
        otpEdits[4] = findViewById(R.id.otpEdit5);
        otpEdits[5] = findViewById(R.id.otpEdit6);

        TextView phoneNumberText = findViewById(R.id.phoneNumberText);
        phoneNumberText.setText("Mã OTP sẽ được gửi đến: " + newPhoneNumber);

        findViewById(R.id.backButton).setOnClickListener(v -> finish());
        findViewById(R.id.verifyButton).setOnClickListener(v -> verifyCode());
        findViewById(R.id.resendButton).setOnClickListener(v -> resendVerificationCode());
    }

    private void setupOTPInputs() {
        for (int i = 0; i < 6; i++) {
            int finalI = i;
            otpEdits[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1 && finalI < 5) {
                        otpEdits[finalI + 1].requestFocus();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void sendVerificationCode() {
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber("+84" + newPhoneNumber.substring(1)) // Convert 0xxx to +84xxx
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        signInWithPhoneAuthCredential(credential);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        Toast.makeText(PhoneVerificationActivity.this,
                                "Lỗi gửi mã: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCodeSent(@NonNull String vId,
                                           @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        verificationId = vId;
                    }
                })
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void verifyCode() {
        StringBuilder code = new StringBuilder();
        for (EditText edit : otpEdits) {
            code.append(edit.getText().toString());
        }

        if (code.length() != 6) {
            Toast.makeText(this, "Vui lòng nhập đủ mã OTP", Toast.LENGTH_SHORT).show();
            return;
        }

        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code.toString());
        signInWithPhoneAuthCredential(credential);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        auth.getCurrentUser().updatePhoneNumber(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        updatePhoneNumberInDatabase();
                    } else {
                        Toast.makeText(PhoneVerificationActivity.this,
                                "Xác thực thất bại: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updatePhoneNumberInDatabase() {
        userRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(auth.getCurrentUser().getUid());

        userRef.child("phone").setValue(newPhoneNumber)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Cập nhật số điện thoại thành công",
                            Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi cập nhật database: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
    }

    private void resendVerificationCode() {
        sendVerificationCode();
    }
}