package com.example.timphongtro.Activities;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.timphongtro.R;
import com.example.timphongtro.Utils.AuthUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashScreen extends AppCompatActivity {
    private static final int SPLASH_TIMER = 3000;
    private ImageView logoImage;
    private TextView appName;
    private boolean navigationDone = false;
    private long startTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        startTime = System.currentTimeMillis();
        
        initializeViews();
        startAnimations();
        checkUserAndNavigate();
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

    private void checkUserAndNavigate() {
        proceedWithUserCheck();
    }

    private void proceedWithUserCheck() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            AuthUtils.checkUserExistsOnStartup(this, currentUser, exists -> {
                if (!navigationDone) {
                    long elapsedTime = System.currentTimeMillis() - startTime;
                    long remainingTime = Math.max(SPLASH_TIMER - elapsedTime, 500);
                    
                    new Handler().postDelayed(() -> {
                        if (!navigationDone) {
                            navigateToMain();
                        }
                    }, remainingTime);
                }
            });
        } else {
            long elapsedTime = System.currentTimeMillis() - startTime;
            long remainingTime = Math.max(SPLASH_TIMER - elapsedTime, 500);
            
            new Handler().postDelayed(() -> {
                if (!navigationDone) {
                    navigateToMain();
                }
            }, remainingTime);
        }
    }

    private void navigateToMain() {
        navigationDone = true;
        Intent intent = new Intent(SplashScreen.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}