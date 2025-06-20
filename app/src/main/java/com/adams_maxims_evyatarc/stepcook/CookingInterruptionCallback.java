package com.adams_maxims_evyatarc.stepcook;

/**
 * Interface for activities to handle broadcast events related to cooking interruptions
 */
public interface CookingInterruptionCallback {
    void onCookingInterrupted(InterruptionType type, String message);
    void onCallEnded();
    void onScreenStateChanged(boolean isScreenOn);
    void onHeadphonesStateChanged(boolean areConnected);
    void onInternetStateChanged(boolean isConnected);
    void onPowerStateChanged(boolean isPowerConnected);
}

enum InterruptionType {
    INCOMING_CALL,
    SCREEN_OFF,
    HEADPHONES_DISCONNECTED,
    INTERNET_DISCONNECTED,
    BATTERY_LOW
}