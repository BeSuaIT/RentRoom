package com.example.timphongtro.Activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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

    private TextView txthoten;
    private EditText txtemail;
    private EditText txtpassword;
    private Button btnDangky;
    private FirebaseAuth mAuth;


    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        txthoten = (EditText) findViewById(R.id.txtname);
        txtemail = (EditText) findViewById(R.id.txtemail);
        txtpassword = (EditText) findViewById(R.id.txtpassword);
        btnDangky = (Button) findViewById(R.id.btnDangky);
        TextView txtViewDangnhap = (TextView) findViewById(R.id.txtviewDangnhap);
        mAuth = FirebaseAuth.getInstance();
        txtViewDangnhap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(i);
            }
        });
        btnDangky.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                register();
            }

            private void register() {
                String name;
                String email;
                String password;
                name = txthoten.getText().toString();
                email = txtemail.getText().toString();
                password = txtpassword.getText().toString();
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
                mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(getApplicationContext(), "Đăng ký thành công.", Toast.LENGTH_SHORT).show();
                            btnDangky.setEnabled(false);
                            // Khởi tạo Firebase Realtime Database
                            FirebaseDatabase database = FirebaseDatabase.getInstance();
                            mDatabase = database.getReference();
                            // Lấy người dùng hiện tại đã đăng nhập
                            FirebaseUser currentUser = mAuth.getCurrentUser();
                            String uid="";
                            String email="";
                            String name="";
                            if (currentUser != null) {
                                // Người dùng đã đăng nhập
                                uid = currentUser.getUid(); // Lấy ID người dùng
                                email = currentUser.getEmail(); // Lấy địa chỉ email người dùng
                                name = txthoten.getText().toString(); //Lấy tên người dùng
                                // Thực hiện các hoạt động khác liên quan đến người dùng đã đăng nhập
                            } else {
                                // Người dùng chưa đăng nhập
                            }
                            // Tạo một đối tượng User
                            User user = new User(email,uid,name);
                            // Thêm đối tượng User vào Realtime Database
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