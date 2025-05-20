package com.adams_maxims_evyatarc.stepcook;

import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 3;
    private int timer;
    private TextView appStartText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        appStartText = findViewById(R.id.appStartText);
        appStartText.setText("We will start in " + SPLASH_DURATION);

        new Thread(() -> {
            timer = SPLASH_DURATION;
            while(timer != 0){
                SystemClock.sleep(1000);
                timer--;
                runOnUiThread(() -> appStartText.setText("We will start in " + timer));
            }
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            finish();
        }).start();
    }
}