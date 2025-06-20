package com.adams_maxims_evyatarc.stepcook;

/**
 * Interface for handling broadcast events
 */
public interface BroadcastEventListener {
    void onIncomingCall(String phoneNumber);
    void onCallEnded();
    void onScreenOn();
    void onScreenOff();
    void onHeadphonesConnected();
    void onHeadphonesDisconnected();
    void onInternetConnected();
    void onInternetDisconnected();
    void onPowerConnected();
    void onPowerDisconnected();
    void onBatteryLow();
    void onBatteryOkay();
}