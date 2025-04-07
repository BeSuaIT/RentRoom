package com.example.timphongtro.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import com.example.timphongtro.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SplashScreen extends AppCompatActivity {
    private static final int SPLASH_TIMER = 4000;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        firebaseAuth = FirebaseAuth.getInstance();

        new Handler().postDelayed(this::checkUserAndNavigate, SPLASH_TIMER);
    }

    private void checkUserAndNavigate() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser != null) {
            // Kiểm tra xem UID còn tồn tại trong database không
            DatabaseReference userRef = FirebaseDatabase.getInstance()
                    .getReference("Users")
                    .child(currentUser.getUid());

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Intent intent;

                    if (snapshot.exists()) {
                        // UID vẫn tồn tại trong database
                        intent = new Intent(SplashScreen.this, MainActivity.class);
                    } else {
                        // UID không còn tồn tại, buộc đăng xuất
                        firebaseAuth.signOut();
                        intent = new Intent(SplashScreen.this, LoginActivity.class);
                    }

                    startActivity(intent);
                    finish();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // Nếu có lỗi khi kiểm tra, cho đăng xuất để an toàn
                    firebaseAuth.signOut();
                    Intent intent = new Intent(SplashScreen.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                }
            });
        } else {
            // Chưa đăng nhập
            Intent intent = new Intent(SplashScreen.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }
}