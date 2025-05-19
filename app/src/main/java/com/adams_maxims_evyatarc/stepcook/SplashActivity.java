package com.adams_maxims_evyatarc.stepcook;

import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2;
    private int timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Force Light Mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Thread(() -> {
            timer = SPLASH_DURATION;
            while(timer != 0){
                SystemClock.sleep(1000);
                timer--;
            }
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            finish();
        }).start();
    }
}