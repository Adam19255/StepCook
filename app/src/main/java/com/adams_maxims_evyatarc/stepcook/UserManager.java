package com.adams_maxims_evyatarc.stepcook;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Singleton class to manage user data and operations throughout the application.
 * Handles retrieving, storing, and updating user information in Firestore.
 */
public class UserManager {
    private static final String TAG = "UserManager";
    private static final String USERS_COLLECTION = "Users";

    private static UserManager instance;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private User currentUser;

    // Interface for callbacks when user data is loaded
    public interface UserDataCallback {
        void onUserDataLoaded(User user);
        void onError(Exception e);
    }

    // Interface for simpler operations with success/failure
    public interface UserOperationCallback {
        void onSuccess();
        void onError(Exception e);
    }

    // Private constructor to enforce singleton pattern
    private UserManager() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = null;
    }

    // Get the singleton instance
    public static synchronized UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }
        return instance;
    }

    /**
     * Check if a user is currently logged in
     * @return true if a user is logged in, false otherwise
     */
    public boolean isUserLoggedIn() {
        return auth.getCurrentUser() != null;
    }

    /**
     * Get the current Firebase user ID
     * @return User ID or null if no user is logged in
     */
    public String getCurrentUserId() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    /**
     * Get the current cached user data
     * @return User object or null if not loaded yet
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Invalidate the current user cache to force a fresh load from Firestore
     */
    public void invalidateUserCache() {
        currentUser = null;
    }

    /**
     * Load user data from Firestore
     * @param callback Callback to receive the loaded user or error
     */
    public void loadUserData(final UserDataCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onError(new Exception("No user is logged in"));
            return;
        }

        DocumentReference userRef = db.collection(USERS_COLLECTION).document(userId);
        userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        // Create user from document
                        User user = new User();
                        user.setUserId(userId);
                        user.setUserName(document.getString("userName"));
                        user.setEmail(document.getString("email"));

                        // Handle nullable boolean fields with defaults
                        Boolean notificationsEnabled = document.getBoolean("notificationsEnabled");
                        Boolean autoPlayNextStep = document.getBoolean("autoPlayNextStep");
                        user.setNotificationsEnabled(notificationsEnabled != null ? notificationsEnabled : true);
                        user.setAutoPlayNextStep(autoPlayNextStep != null ? autoPlayNextStep : true);

                        Long timerHours = document.getLong("defaultTimerHours");
                        Long timerMinutes = document.getLong("defaultTimerMinutes");
                        user.setDefaultTimerHours(timerHours != null ? timerHours.intValue() : 0);
                        user.setDefaultTimerMinutes(timerMinutes != null ? timerMinutes.intValue() : 0);

                        // Cache the user
                        currentUser = user;

                        // Notify through callback
                        callback.onUserDataLoaded(user);
                    } else {
                        Log.d(TAG, "No user document found");
                        callback.onError(new Exception("User data not found"));
                    }
                } else {
                    Log.d(TAG, "Failed to get user data", task.getException());
                    callback.onError(task.getException());
                }
            }
        });
    }

    /**
     * Update a user preference in Firestore
     * @param preferenceKey The key of the preference to update
     * @param value The new value
     * @param callback Callback for operation result
     */
    public void updateUserPreference(String preferenceKey, Object value, final UserOperationCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onError(new Exception("No user is logged in"));
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put(preferenceKey, value);

        db.collection(USERS_COLLECTION).document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    // Update the cached user data
                    if (currentUser != null) {
                        switch (preferenceKey) {
                            case "notificationsEnabled":
                                currentUser.setNotificationsEnabled((Boolean) value);
                                break;
                            case "autoPlayNextStep":
                                currentUser.setAutoPlayNextStep((Boolean) value);
                                break;
                            case "defaultTimerHours":
                                currentUser.setDefaultTimerHours((Integer) value);
                                break;
                            case "defaultTimerMinutes":
                                currentUser.setDefaultTimerMinutes((Integer) value);
                                break;
                        }
                    }
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error updating preference: " + preferenceKey, e);
                    callback.onError(e);
                });
    }


    /**
     * Update user profile information
     * @param userName The new username
     * @param callback Callback for operation result
     */
    public void updateUserProfile(String userName, final UserOperationCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onError(new Exception("No user is logged in"));
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("userName", userName);

        db.collection(USERS_COLLECTION).document(userId)
                .update(updates)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Update the cached user data
                        if (currentUser != null) {
                            currentUser.setUserName(userName);
                        }
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error updating user profile", e);
                        callback.onError(e);
                    }
                });
    }

    /**
     * Register a new user in Firebase Auth and Firestore
     * @param userName The username for the new user
     * @param email The email for the new user
     * @param password The password for the new user
     * @param callback Callback for operation result
     */
    public void registerUser(final String userName, final String email, String password,
                             final UserOperationCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = auth.getCurrentUser();
                        String userId = firebaseUser.getUid();

                        // Create a new user object
                        User newUser = new User(userId, userName, email, true, true);

                        // Save to Firestore
                        db.collection(USERS_COLLECTION).document(userId)
                                .set(newUser.toMap())
                                .addOnSuccessListener(aVoid -> {
                                    // Cache the new user
                                    currentUser = newUser;
                                    callback.onSuccess();
                                })
                                .addOnFailureListener(e -> {
                                    callback.onError(e);
                                });
                    } else {
                        callback.onError(task.getException());
                    }
                });
    }

    /**
     * Login a user with email and password
     * @param email The email to login with
     * @param password The password to login with
     * @param callback Callback for operation result
     */
    public void loginUser(String email, String password, final UserDataCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    // After successful login, load the user data
                    loadUserData(callback);
                })
                .addOnFailureListener(e -> {
                    callback.onError(e);
                });
    }

    /**
     * Log out the current user
     */
    public void logoutUser() {
        auth.signOut();
        currentUser = null;
    }
}