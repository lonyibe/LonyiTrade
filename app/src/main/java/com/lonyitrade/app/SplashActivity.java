package com.lonyitrade.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import com.lonyitrade.app.utils.SessionManager;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY = 2000; // 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(() -> {
            SessionManager sessionManager = new SessionManager(SplashActivity.this);
            Intent intent;

            // Check if the activity was started from a notification
            Bundle extras = getIntent().getExtras();
            if (extras != null && extras.containsKey("type")) {
                // If logged in, navigate based on notification type
                if (sessionManager.isLoggedIn()) {
                    String type = extras.getString("type");
                    if ("newMessage".equals(type)) {
                        intent = new Intent(SplashActivity.this, ChatActivity.class);
                        // Pass all extras from the notification to the ChatActivity
                        intent.putExtras(extras);
                    } else if ("newReview".equals(type)) {
                        intent = new Intent(SplashActivity.this, ReviewActivity.class);
                        // Pass all extras from the notification to the ReviewActivity
                        intent.putExtras(extras);
                    } else {
                        // Default to MainAppActivity if type is unknown
                        intent = new Intent(SplashActivity.this, MainAppActivity.class);
                    }
                } else {
                    // If not logged in, go to LoginActivity
                    intent = new Intent(SplashActivity.this, LoginActivity.class);
                }
            } else {
                // Normal app start
                if (sessionManager.isLoggedIn()) {
                    intent = new Intent(SplashActivity.this, MainAppActivity.class);
                } else {
                    intent = new Intent(SplashActivity.this, LoginActivity.class);
                }
            }

            startActivity(intent);
            finish();
        }, SPLASH_DELAY);
    }
}