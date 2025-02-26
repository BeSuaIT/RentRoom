package com.example.timphongtro.Activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.timphongtro.Entity.User;
import com.example.timphongtro.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateInformationUserActivity extends AppCompatActivity {

    FirebaseDatabase database;
    private FirebaseUser mUser;
    DatabaseReference userRef;
    private String name, email, phone;
    private EditText txtname, txtemail, txtphone;
    private ImageView imageViewBack;
    private LinearLayout linearEmail;
    private User user;
    private Button btnCapnhat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_updateinformationuser);

        txtname = findViewById(R.id.txtname);
        txtemail = findViewById(R.id.txtemail);
        imageViewBack = findViewById(R.id.imageViewBack);
        btnCapnhat = findViewById(R.id.btnCapnhat);
        mUser = FirebaseAuth.getInstance().getCurrentUser();
        txtphone = findViewById(R.id.txtphone);
        linearEmail = findViewById(R.id.linearEmail);
        imageViewBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        database = FirebaseDatabase.getInstance();
        userRef = database.getReference("users/" + mUser.getUid());

        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    user = snapshot.getValue(User.class);
                    if(user!=null) {
                        email = user.getEmail();
                        phone = user.getPhone();
                        name = user.getName();
                        txtname.setText(name);
                        txtemail.setText(email);
                        txtphone.setText(phone);
                        txtemail.setEnabled(false);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
        btnCapnhat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!TextUtils.isEmpty(txtname.getText().toString()) && !TextUtils.isEmpty(txtphone.getText().toString())){
                        String regex = "^\\d{10}$";
                        Pattern pattern = Pattern.compile(regex);
                        Matcher matcher = pattern.matcher(txtphone.getText().toString());
                        if (matcher.matches()) {
                            User user = new User(email, mUser.getUid(), txtname.getText().toString(), txtphone.getText().toString(), "user");
                            DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("users/" + mUser.getUid());
                            databaseRef.setValue(user)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            // Cập nhật thành công
                                            Toast.makeText(getApplicationContext(), "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                                            finish();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            // Có lỗi xảy ra khi cập nhật
                                        }
                                    });
                        } else {
                            Toast.makeText(getApplicationContext(),"Vui lòng nhập đúng định dạng số điện thoại",Toast.LENGTH_SHORT).show();
                    }
                }
                else{
                    Toast.makeText(getApplicationContext(),"Vui lòng nhập đầy đủ các trường thông tin",Toast.LENGTH_SHORT).show();

                }
            }
        });

        linearEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(),"Không được chỉnh sửa trường email",Toast.LENGTH_SHORT).show();
            }
        });
    }
}