package com.adams_maxims_evyatarc.stepcook;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;

public class StepCookBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "StepCookBroadcastReceiver";

    private BroadcastEventListener listener;
    private Context context;
    private boolean isRegistered = false;

    public StepCookBroadcastReceiver(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null || listener == null) return;

        Log.d(TAG, "Received broadcast: " + action);

        switch (action) {
            case TelephonyManager.ACTION_PHONE_STATE_CHANGED:
                handlePhoneStateChange(intent);
                break;

            case Intent.ACTION_SCREEN_ON:
                listener.onScreenOn();
                break;

            case Intent.ACTION_SCREEN_OFF:
                listener.onScreenOff();
                break;

            case Intent.ACTION_HEADSET_PLUG:
                handleHeadsetPlug(intent);
                break;

            case ConnectivityManager.CONNECTIVITY_ACTION:
                handleConnectivityChange();
                break;

            case Intent.ACTION_POWER_CONNECTED:
                listener.onPowerConnected();
                break;

            case Intent.ACTION_POWER_DISCONNECTED:
                listener.onPowerDisconnected();
                break;

            case Intent.ACTION_BATTERY_LOW:
                listener.onBatteryLow();
                break;

            case Intent.ACTION_BATTERY_OKAY:
                listener.onBatteryOkay();
                break;
        }
    }

    private void handlePhoneStateChange(Intent intent) {
        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        String phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);

        if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
            Log.d(TAG, "Incoming call detected");
            listener.onIncomingCall(phoneNumber != null ? phoneNumber : "Unknown");
        } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
            Log.d(TAG, "Call ended");
            listener.onCallEnded();
        }
    }

    private void handleHeadsetPlug(Intent intent) {
        int state = intent.getIntExtra("state", -1);
        boolean isConnected = (state == 1);

        Log.d(TAG, "Headset " + (isConnected ? "connected" : "disconnected"));

        if (isConnected) {
            listener.onHeadphonesConnected();
        } else {
            listener.onHeadphonesDisconnected();
        }
    }

    private void handleConnectivityChange() {
        boolean isConnected = isInternetConnected();

        if (isConnected) {
            listener.onInternetConnected();
        } else {
            listener.onInternetDisconnected();
        }
    }

    private boolean isInternetConnected() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network activeNetwork = cm.getActiveNetwork();
            if (activeNetwork == null) return false;

            NetworkCapabilities capabilities = cm.getNetworkCapabilities(activeNetwork);
            return capabilities != null && (
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            );
        } else {
            // For older Android versions
            android.net.NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
    }

    public void setListener(BroadcastEventListener listener) {
        this.listener = listener;
    }

    public void registerReceiver() {
        if (isRegistered) {
            Log.w(TAG, "Receiver already registered");
            return;
        }

        IntentFilter filter = new IntentFilter();

        // Phone state
        filter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);

        // Screen state
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);

        // Power state
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);

        // Headphones
        filter.addAction(Intent.ACTION_HEADSET_PLUG);

        // Connectivity
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

        // Battery
        filter.addAction(Intent.ACTION_BATTERY_LOW);
        filter.addAction(Intent.ACTION_BATTERY_OKAY);

        context.registerReceiver(this, filter);
        isRegistered = true;

        Log.d(TAG, "BroadcastReceiver registered successfully");
    }

    public void unregisterReceiver() {
        if (!isRegistered) {
            Log.w(TAG, "Receiver not registered");
            return;
        }

        try {
            context.unregisterReceiver(this);
            isRegistered = false;
            Log.d(TAG, "BroadcastReceiver unregistered successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error unregistering receiver", e);
        }
    }
}