package com.adams_maxims_evyatarc.stepcook;

import android.app.AlertDialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatButton;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class EditProfileDialog {

    private Context context;
    private AlertDialog dialog;
    private ImageView closeButton;
    private EditText userNameField;
    private EditText currentPasswordField;
    private EditText newPasswordField;
    private EditText confirmPasswordField;
    private AppCompatButton saveButton;
    private AppCompatButton cancelButton;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String currentUserName;

    public EditProfileDialog(Context context) {
        this.context = context;
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    public void show() {
        // Create the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.CustomAlertDialog);

        // Inflate the custom layout
        View dialogView = LayoutInflater.from(context).inflate(R.layout.edit_profile_dialog_layout, null);
        builder.setView(dialogView);

        // Get references to views
        closeButton = dialogView.findViewById(R.id.closeButton);
        userNameField = dialogView.findViewById(R.id.userName);
        currentPasswordField = dialogView.findViewById(R.id.currentPassword);
        newPasswordField = dialogView.findViewById(R.id.newPassword);
        confirmPasswordField = dialogView.findViewById(R.id.confirmPassword);
        saveButton = dialogView.findViewById(R.id.save_button);
        cancelButton = dialogView.findViewById(R.id.cancel_button);

        // Load current user data
        loadUserData();

        // Set click listeners
        saveButton.setOnClickListener(v -> saveChanges());
        closeButton.setOnClickListener(v -> dialog.dismiss());
        cancelButton.setOnClickListener(v -> dialog.dismiss());

        // Create and show the dialog
        dialog = builder.create();

        // Remove default background
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialog.show();
    }

    private void loadUserData() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();

            db.collection("Users").document(userId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            currentUserName = documentSnapshot.getString("userName");
                            userNameField.setText(currentUserName);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Failed to load user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void saveChanges() {
        String newUserName = userNameField.getText().toString().trim();
        String currentPassword = currentPasswordField.getText().toString().trim();
        String newPassword = newPasswordField.getText().toString().trim();
        String confirmPassword = confirmPasswordField.getText().toString().trim();

        boolean nameChanged = !TextUtils.isEmpty(newUserName) && !newUserName.equals(currentUserName);
        boolean passwordChanged = !TextUtils.isEmpty(currentPassword) && !TextUtils.isEmpty(newPassword);

        // Validate inputs
        if (nameChanged && TextUtils.isEmpty(newUserName)) {
            Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        if (passwordChanged) {
            if (newPassword.length() < 6) {
                Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                Toast.makeText(context, "New passwords don't match", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPassword.equals(currentPassword)) {
                Toast.makeText(context, "New password cannot be the same as current password", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        // If password change is requested, we need to reauthenticate first
        if (passwordChanged) {
            // Get email from current user
            String email = user.getEmail();
            if (email == null) {
                Toast.makeText(context, "Cannot retrieve user email", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create credential for reauthentication
            AuthCredential credential = EmailAuthProvider.getCredential(email, currentPassword);

            // Reauthenticate
            user.reauthenticate(credential)
                    .addOnSuccessListener(aVoid -> {
                        // Update password after successful reauthentication
                        user.updatePassword(newPassword)
                                .addOnSuccessListener(aVoid1 -> {
                                    Toast.makeText(context, "Password updated successfully", Toast.LENGTH_SHORT).show();

                                    // If name is also changed, update it
                                    if (nameChanged) {
                                        updateUserName(user.getUid(), newUserName);
                                    } else {
                                        dialog.dismiss();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(context, "Failed to update password: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Current password is incorrect", Toast.LENGTH_SHORT).show();
                    });
        }
        // If only name is changed, update it directly
        else if (nameChanged) {
            updateUserName(user.getUid(), newUserName);
        }
        // Nothing to change
        else {
            Toast.makeText(context, "No changes to save", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        }
    }

    private void updateUserName(String userId, String newUserName) {
        db.collection("Users").document(userId)
                .update("userName", newUserName)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "Username updated successfully", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to update username: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}