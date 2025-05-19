package com.adams_maxims_evyatarc.stepcook;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.SwitchCompat;

public class SettingsActivity extends AppCompatActivity implements DialogManager.ProfileUpdateListener {
    private static final String TAG = "SettingsActivity";
    private ImageView backButton;
    private AppCompatButton editProfileButton;
    private SwitchCompat notificationSwitch;
    private SwitchCompat autoPlayNextStepSwitch;
    private RelativeLayout aboutUsButton;
    private RelativeLayout logoutButton;
    private TextView userNameTextView;
    private TextView userEmailTextView;


    private UserManager userManager;
    private DialogManager dialogManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Force Light Mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        backButton = findViewById(R.id.backButton);
        editProfileButton = findViewById(R.id.editProfileButton);
        notificationSwitch = findViewById(R.id.notificationSwitch);
        autoPlayNextStepSwitch = findViewById(R.id.autoPlayNextStepSwitch);
        aboutUsButton = findViewById(R.id.aboutUsButton);
        logoutButton = findViewById(R.id.logoutButton);
        userNameTextView = findViewById(R.id.userNameTextView);
        userEmailTextView = findViewById(R.id.userEmailTextView);

        userManager = UserManager.getInstance();
        dialogManager = new DialogManager(this);

        // Register this activity as the profile update listener
        dialogManager.setProfileUpdateListener(this);

        // Load user data
        loadUserData();

        backButton.setOnClickListener(v -> finish());

        editProfileButton.setOnClickListener(v -> dialogManager.showEditProfileDialog());

        notificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateUserPreference("notificationsEnabled", isChecked);
        });

        autoPlayNextStepSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateUserPreference("autoPlayNextStep", isChecked);
        });

        aboutUsButton.setOnClickListener(v -> dialogManager.showAboutDialog());

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                userManager.logoutUser();
                Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                // Clear the entire activity stack and make LoginActivity the new root
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    public void onProfileUpdated() {
        // Refresh user data when profile is updated
        refreshUserData();
    }

    private void refreshUserData() {
        // Clear cached user data and reload from Firestore
        userManager.invalidateUserCache();
        loadUserData();
    }

    private void loadUserData() {
        // First check if we already have the user data cached
        User currentUser = userManager.getCurrentUser();

        if (currentUser != null) {
            // Use cached user data
            displayUserData(currentUser);
        } else {
            // Load user data from Firestore
            userManager.loadUserData(new UserManager.UserDataCallback() {
                @Override
                public void onUserDataLoaded(User user) {
                    displayUserData(user);
                }

                @Override
                public void onError(Exception e) {
                    Log.d(TAG, "Failed to load user data", e);
                    Toast.makeText(SettingsActivity.this, "Failed to load user data", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void displayUserData(User user) {
        userNameTextView.setText(user.getUserName());
        userEmailTextView.setText(user.getEmail());
        notificationSwitch.setChecked(user.isNotificationsEnabled());
        autoPlayNextStepSwitch.setChecked(user.isAutoPlayNextStep());
    }

    private void updateUserPreference(String preferenceKey, boolean value) {
        userManager.updateUserPreference(preferenceKey, value, new UserManager.UserOperationCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Preference " + preferenceKey + " updated successfully");
            }

            @Override
            public void onError(Exception e) {
                Log.w(TAG, "Error updating preference", e);
                Toast.makeText(SettingsActivity.this, "Failed to update setting", Toast.LENGTH_SHORT).show();

                // Revert the switch state if update fails
                if (preferenceKey.equals("notificationsEnabled")) {
                    notificationSwitch.setChecked(!value);
                } else if (preferenceKey.equals("autoPlayNextStep")) {
                    autoPlayNextStepSwitch.setChecked(!value);
                }
            }
        });
    }
}