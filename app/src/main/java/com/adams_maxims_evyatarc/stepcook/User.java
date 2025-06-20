package com.adams_maxims_evyatarc.stepcook;

/**
 * Model class representing a user in the application.
 * Stores all user-related data retrieved from Firestore.
 */
public class User {
    private String userId;
    private String userName;
    private String email;
    private boolean notificationsEnabled;
    private boolean autoPlayNextStep;

    private int defaultTimerHours = 0;
    private int defaultTimerMinutes = 5;

    // Default constructor required for Firestore
    public User() {
        // Initialize with default values
        this.notificationsEnabled = true;
        this.autoPlayNextStep = true;
    }

    // Constructor with all fields
    public User(String userId, String userName, String email,
                boolean notificationsEnabled, boolean autoPlayNextStep) {
        this.userId = userId;
        this.userName = userName;
        this.email = email;
        this.notificationsEnabled = notificationsEnabled;
        this.autoPlayNextStep = autoPlayNextStep;
    }

    // Getters and setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    public boolean isAutoPlayNextStep() {
        return autoPlayNextStep;
    }

    public void setAutoPlayNextStep(boolean autoPlayNextStep) {
        this.autoPlayNextStep = autoPlayNextStep;
    }
    public int getDefaultTimerHours() {
        return defaultTimerHours;
    }

    public void setDefaultTimerHours(int defaultTimerHours) {
        this.defaultTimerHours = defaultTimerHours;
    }

    public int getDefaultTimerMinutes() {
        return defaultTimerMinutes;
    }

    public void setDefaultTimerMinutes(int defaultTimerMinutes) {
        this.defaultTimerMinutes = defaultTimerMinutes;
    }
    /**
     * Creates a Map of user data suitable for saving to Firestore
     * @return Map containing user data fields
     */
    public java.util.Map<String, Object> toMap() {
        java.util.Map<String, Object> userData = new java.util.HashMap<>();
        userData.put("userName", userName);
        userData.put("email", email);
        userData.put("notificationsEnabled", notificationsEnabled);
        userData.put("autoPlayNextStep", autoPlayNextStep);
        userData.put("defaultTimerHours", defaultTimerHours);
        userData.put("defaultTimerMinutes", defaultTimerMinutes);
        return userData;
    }

}