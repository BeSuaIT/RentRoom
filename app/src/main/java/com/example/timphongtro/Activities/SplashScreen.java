package com.example.timphongtro.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import com.example.timphongtro.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SplashScreen extends AppCompatActivity {
    private static final int SPLASH_TIMER = 3000;
    private FirebaseAuth firebaseAuth;
    private ImageView logoImage;
    private TextView appName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        initializeViews();
        startAnimations();
        initializeFirebase();
    }

    private void initializeViews() {
        logoImage = findViewById(R.id.logoImage);
        appName = findViewById(R.id.appName);
    }

    private void startAnimations() {
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);

        logoImage.startAnimation(fadeIn);
        appName.startAnimation(slideUp);
    }

    private void initializeFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
        new Handler().postDelayed(this::checkUserAndNavigate, SPLASH_TIMER);
    }

    private void checkUserAndNavigate() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance()
                    .getReference("Users")
                    .child(currentUser.getUid());

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Intent intent;
                    if (snapshot.exists()) {
                        intent = new Intent(SplashScreen.this, MainActivity.class);
                    } else {
                        firebaseAuth.signOut();
                        intent = new Intent(SplashScreen.this, LoginActivity.class);
                    }
                    startActivity(intent);
                    finish();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    firebaseAuth.signOut();
                    startActivity(new Intent(SplashScreen.this, LoginActivity.class));
                    finish();
                }
            });
        } else {
            startActivity(new Intent(SplashScreen.this, LoginActivity.class));
            finish();
        }
    }
}