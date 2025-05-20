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

public class SplashScreen extends AppCompatActivity {
    private static final int SPLASH_TIMER = 3000;
    private ImageView logoImage;
    private TextView appName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

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
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(SplashScreen.this, MainActivity.class);
            startActivity(intent);
            finish();
        }, SPLASH_TIMER);
    }
}