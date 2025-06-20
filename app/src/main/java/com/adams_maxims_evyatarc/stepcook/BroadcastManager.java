package com.adams_maxims_evyatarc.stepcook;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

/**
 * Manager class to handle broadcast events and coordinate responses
 * with the cooking app functionality
 */
public class BroadcastManager implements BroadcastEventListener {
    private static final String TAG = "BroadcastManager";
    private static BroadcastManager instance;

    private Context context;
    private StepCookBroadcastReceiver broadcastReceiver;
    private CookingInterruptionCallback callback;

    private BroadcastManager(Context context) {
        this.context = context.getApplicationContext();
        this.broadcastReceiver = new StepCookBroadcastReceiver(this.context);
        this.broadcastReceiver.setListener(this);
    }

    public static synchronized BroadcastManager getInstance(Context context) {
        if (instance == null) {
            instance = new BroadcastManager(context);
        }
        return instance;
    }

    public static BroadcastManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("BroadcastManager not initialized. Call getInstance(Context) first.");
        }
        return instance;
    }

    public void setCookingInterruptionCallback(CookingInterruptionCallback callback) {
        this.callback = callback;
    }

    public void startListening() {
        broadcastReceiver.registerReceiver();
        Log.d(TAG, "Started listening for broadcast events");
    }

    public void stopListening() {
        broadcastReceiver.unregisterReceiver();
        Log.d(TAG, "Stopped listening for broadcast events");
    }

    // BroadcastEventListener implementations
    @Override
    public void onIncomingCall(String phoneNumber) {
        Log.i(TAG, "Incoming call from: " + phoneNumber);
        String message = "Incoming call detected. Consider pausing your cooking timer.";

        if (callback != null) {
            callback.onCookingInterrupted(InterruptionType.INCOMING_CALL, message);
        } else {
            showToast(message);
        }
    }

    @Override
    public void onCallEnded() {
        Log.d(TAG, "Call ended");
        if (callback != null) {
            callback.onCallEnded();
        }
    }

    @Override
    public void onScreenOn() {
        Log.d(TAG, "Screen turned on");
        if (callback != null) {
            callback.onScreenStateChanged(true);
        }
    }

    @Override
    public void onScreenOff() {
        Log.i(TAG, "Screen turned off");
        String message = "Screen turned off. Your timers are still running in the background.";

        if (callback != null) {
            callback.onCookingInterrupted(InterruptionType.SCREEN_OFF, message);
            callback.onScreenStateChanged(false);
        } else {
            showToast(message);
        }
    }

    @Override
    public void onHeadphonesConnected() {
        Log.d(TAG, "Headphones connected");
        showToast("Headphones connected. Audio guidance available.");
        if (callback != null) {
            callback.onHeadphonesStateChanged(true);
        }
    }

    @Override
    public void onHeadphonesDisconnected() {
        Log.i(TAG, "Headphones disconnected");
        String message = "Headphones disconnected. Audio will play through speaker.";

        if (callback != null) {
            callback.onCookingInterrupted(InterruptionType.HEADPHONES_DISCONNECTED, message);
            callback.onHeadphonesStateChanged(false);
        } else {
            showToast(message);
        }
    }

    @Override
    public void onInternetConnected() {
        Log.d(TAG, "Internet connected");
        showToast("Internet connection restored.");
        if (callback != null) {
            callback.onInternetStateChanged(true);
        }
    }

    @Override
    public void onInternetDisconnected() {
        Log.w(TAG, "Internet disconnected");
        String message = "Internet connection lost. Some features may be limited.";

        if (callback != null) {
            callback.onCookingInterrupted(InterruptionType.INTERNET_DISCONNECTED, message);
            callback.onInternetStateChanged(false);
        } else {
            showToast(message);
        }
    }

    @Override
    public void onPowerConnected() {
        Log.d(TAG, "Power connected");
        showToast("Device is now charging.");
        if (callback != null) {
            callback.onPowerStateChanged(true);
        }
    }

    @Override
    public void onPowerDisconnected() {
        Log.d(TAG, "Power disconnected");
        if (callback != null) {
            callback.onPowerStateChanged(false);
        }
    }

    @Override
    public void onBatteryLow() {
        Log.w(TAG, "Battery low");
        String message = "Battery is low. Consider connecting to power for longer cooking sessions.";

        if (callback != null) {
            callback.onCookingInterrupted(InterruptionType.BATTERY_LOW, message);
        } else {
            showToast(message);
        }
    }

    @Override
    public void onBatteryOkay() {
        Log.d(TAG, "Battery okay");
        showToast("Battery level is now okay.");
    }

    private void showToast(String message) {
        if (context != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }

    // Utility methods
    public boolean isDeviceReadyForCooking() {
        boolean batteryOk = !SystemStateUtils.isBatteryLow(context) || SystemStateUtils.isPowerConnected(context);
        boolean displayOk = SystemStateUtils.isScreenOn(context) || SystemStateUtils.isPowerConnected(context);
        boolean internetOk = SystemStateUtils.isInternetConnected(context);

        return batteryOk && displayOk && internetOk;
    }

    public String getDeviceStatusSummary() {
        StringBuilder summary = new StringBuilder();

        summary.append("Device Status:\n");
        summary.append("• Screen: ").append(SystemStateUtils.isScreenOn(context) ? "On" : "Off").append("\n");
        summary.append("• Power: ").append(SystemStateUtils.isPowerConnected(context) ? "Connected" : "Disconnected").append("\n");
        summary.append("• Battery: ").append(SystemStateUtils.getBatteryLevel(context)).append("%\n");
        summary.append("• Headphones: ").append(SystemStateUtils.areHeadphonesConnected(context) ? "Connected" : "Disconnected").append("\n");
        summary.append("• Internet: ").append(SystemStateUtils.isInternetConnected(context) ? "Connected" : "Disconnected");

        return summary.toString();
    }
}