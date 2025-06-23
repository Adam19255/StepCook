package com.adams_maxims_evyatarc.stepcook;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class TimerForegroundService extends Service {
    private static final String TAG = "TimerForegroundService";
    private static final int NOTIFICATION_ID = 2001;
    private static final String CHANNEL_ID = "timer_channel";

    private final IBinder binder = new TimerBinder();
    private CountDownTimer countDownTimer;
    private long remainingTimeInMillis = 0;
    private boolean isTimerRunning = false;
    private String currentStepDescription = "";
    private int currentStepNumber = 0;

    // Callback interface for timer events
    public interface TimerCallback {
        void onTimerTick(long remainingMillis);
        void onTimerFinished();
    }

    private TimerCallback timerCallback;

    public class TimerBinder extends Binder {
        TimerForegroundService getService() {
            return TimerForegroundService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        Log.d(TAG, "Timer service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, createNotification("Timer ready", ""));
        return START_STICKY; // Restart if killed by system
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Recipe Timer",
                NotificationManager.IMPORTANCE_LOW // Low importance to avoid sound/vibration
        );
        channel.setDescription("Shows active recipe timer");

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification(String title, String content) {
        Intent notificationIntent = new Intent(this, RecipeDetailActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.drawable.all_cook_svg)
                .setContentIntent(pendingIntent)
                .setOngoing(true) // Makes notification persistent
                .setSilent(true) // No sound/vibration
                .build();
    }

    public void startTimer(long millis, String stepDescription, int stepNumber) {
        this.remainingTimeInMillis = millis;
        this.currentStepDescription = stepDescription;
        this.currentStepNumber = stepNumber;

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new CountDownTimer(millis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingTimeInMillis = millisUntilFinished;
                isTimerRunning = true;

                // Update notification
                long minutes = millisUntilFinished / 60000;
                long seconds = (millisUntilFinished / 1000) % 60;
                String timeText = String.format("%02d:%02d", minutes, seconds);
                String title = "Step " + currentStepNumber + " Timer";

                Notification notification = createNotification(title, timeText + " remaining");
                NotificationManager notificationManager = getSystemService(NotificationManager.class);
                if (notificationManager != null) {
                    notificationManager.notify(NOTIFICATION_ID, notification);
                }

                // Notify callback
                if (timerCallback != null) {
                    timerCallback.onTimerTick(millisUntilFinished);
                }
            }

            @Override
            public void onFinish() {
                remainingTimeInMillis = 0;
                isTimerRunning = false;

                // Update notification
                String title = "Step " + currentStepNumber + " Complete!";
                Notification notification = createNotification(title, "Timer finished");
                NotificationManager notificationManager = getSystemService(NotificationManager.class);
                if (notificationManager != null) {
                    notificationManager.notify(NOTIFICATION_ID, notification);
                }

                // Notify callback
                if (timerCallback != null) {
                    timerCallback.onTimerFinished();
                }

                Log.d(TAG, "Timer finished for step " + currentStepNumber);
            }
        };

        countDownTimer.start();
        Log.d(TAG, "Timer started for " + millis + "ms, step " + stepNumber);
    }

    public void pauseTimer() {
        if (countDownTimer != null && isTimerRunning) {
            countDownTimer.cancel();
            isTimerRunning = false;

            // Update notification to show paused state
            long minutes = remainingTimeInMillis / 60000;
            long seconds = (remainingTimeInMillis / 1000) % 60;
            String timeText = String.format("%02d:%02d", minutes, seconds);
            String title = "Step " + currentStepNumber + " Timer (Paused)";

            Notification notification = createNotification(title, timeText + " remaining (paused)");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.notify(NOTIFICATION_ID, notification);
            }

            Log.d(TAG, "Timer paused with " + remainingTimeInMillis + "ms remaining");
        }
    }

    public void resumeTimer() {
        if (remainingTimeInMillis > 0 && !isTimerRunning) {
            startTimer(remainingTimeInMillis, currentStepDescription, currentStepNumber);
            Log.d(TAG, "Timer resumed with " + remainingTimeInMillis + "ms remaining");
        }
    }

    public void stopTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        isTimerRunning = false;
        remainingTimeInMillis = 0;

        // Update notification to show stopped state
        Notification notification = createNotification("Timer stopped", "No active timer");
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, notification);
        }

        Log.d(TAG, "Timer stopped");
    }

    public void setTimerCallback(TimerCallback callback) {
        this.timerCallback = callback;
    }

    public long getRemainingTime() {
        return remainingTimeInMillis;
    }

    public boolean isTimerRunning() {
        return isTimerRunning;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        Log.d(TAG, "Timer service destroyed");
    }
}