package com.example.timphongtro.Activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.timphongtro.Entity.User;
import com.example.timphongtro.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends AppCompatActivity {

    private TextView txtViewName, txtViewEmail, txtViewPassword, txtViewLogin;
    private Button btnRegister;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        txtViewName = findViewById(R.id.txtname);
        txtViewEmail = findViewById(R.id.txtemail);
        txtViewPassword = findViewById(R.id.txtpassword);
        btnRegister = findViewById(R.id.btnDangky);
        txtViewLogin = findViewById(R.id.txtviewDangnhap);
        mAuth = FirebaseAuth.getInstance();

        txtViewLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(i);
            }
        });

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = txtViewName.getText().toString();
                String email = txtViewEmail.getText().toString();
                String password = txtViewPassword.getText().toString();
                if (TextUtils.isEmpty(name)) {
                    Toast.makeText(getApplicationContext(), "Vui lòng nhập Họ tên.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(getApplicationContext(), "Vui lòng nhập Email.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(password)) {
                    Toast.makeText(getApplicationContext(), "Vui lòng nhập Mật khẩu.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (password.length() < 6) {
                    Toast.makeText(getApplicationContext(), "Mật khẩu phải có ít nhất 6 ký tự.", Toast.LENGTH_SHORT).show();
                    return;
                }
                mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(getApplicationContext(), "Đăng ký thành công.", Toast.LENGTH_SHORT).show();
                            btnRegister.setEnabled(false);
                            FirebaseDatabase database = FirebaseDatabase.getInstance();
                            mDatabase = database.getReference();
                            FirebaseUser currentUser = mAuth.getCurrentUser();

                            String uid = currentUser.getUid();
                            String email = currentUser.getEmail();
                            String name = txtViewName.getText().toString();
                            String phone = "";
                            String permission = "user";

                            User user = new User(email, uid, name, phone, permission);
                            mDatabase.child("users").child(uid).setValue(user);
                            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                            startActivity(intent);
                        } else {
                            Toast.makeText(getApplicationContext(), "Đăng ký thất bại.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }
}