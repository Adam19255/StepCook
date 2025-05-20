package com.adams_maxims_evyatarc.stepcook;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    private EditText userName;
    private EditText userEmail;
    private EditText userPassword;
    private EditText confirmPassword;
    private Button signupButton;
    private ImageView backButton;
    private ProgressBar progressBar;

    private UserManager userManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        userName = findViewById(R.id.userName);
        userEmail = findViewById(R.id.userEmail);
        userPassword = findViewById(R.id.userPassword);
        confirmPassword = findViewById(R.id.confirmPassword);
        signupButton = findViewById(R.id.signupButton);
        backButton = findViewById(R.id.backButton);
        progressBar = findViewById(R.id.registerProgressBar);

        userManager = UserManager.getInstance();

        signupButton.setOnClickListener(v -> validate());

        backButton.setOnClickListener(v -> finish());
    }

    private void validate(){
        String txt_user = userName.getText().toString();
        String txt_email = userEmail.getText().toString();
        String txt_pass = userPassword.getText().toString();
        String txt_confirm_pass = confirmPassword.getText().toString();

        if (txt_user.isEmpty()) {
            userName.setError("Please enter a name");
            userName.requestFocus();
            return;
        }
        if (txt_email.isEmpty()) {
            userEmail.setError("Please enter an email");
            userEmail.requestFocus();
            return;
        }
        if (txt_pass.isEmpty()) {
            userPassword.setError("Please enter a password");
            userPassword.requestFocus();
            return;
        }
        if (txt_confirm_pass.isEmpty()) {
            confirmPassword.setError("Please confirm password");
            confirmPassword.requestFocus();
        }
        else if (txt_pass.length() < 6) {
            userPassword.setError("Password is too short, minimum 6 chars");
            userPassword.requestFocus();
        }
        else if (!txt_pass.equals(txt_confirm_pass)) {
            confirmPassword.setError("Passwords don't match");
            confirmPassword.requestFocus();
        }
        else {
            // Show loading state
            setLoading(true);

            userManager.registerUser(txt_user, txt_email, txt_pass, new UserManager.UserOperationCallback() {
                @Override
                public void onSuccess() {
                    // Hide loading state
                    setLoading(false);

                    Toast.makeText(RegisterActivity.this, "Registered successfully!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                    finish();
                }

                @Override
                public void onError(Exception e) {
                    // Hide loading state
                    setLoading(false);

                    Toast.makeText(RegisterActivity.this, "Registration failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            signupButton.setText("");
            signupButton.setEnabled(false);
            progressBar.setVisibility(View.VISIBLE);
        } else {
            signupButton.setText(R.string.register_text);
            signupButton.setEnabled(true);
            progressBar.setVisibility(View.GONE);
        }
    }
}