package com.adams_maxims_evyatarc.stepcook;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText userEmail;
    private EditText userPass;
    private Button loginButton;
    private TextView signupRedirect;
    private ProgressBar progressBar;

    private UserManager userManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        userEmail = findViewById(R.id.userEmail);
        userPass = findViewById(R.id.userPassword);
        loginButton = findViewById(R.id.loginButton);
        signupRedirect = findViewById(R.id.signupRedirect);
        progressBar = findViewById(R.id.loginProgressBar);

        userManager = UserManager.getInstance();

        if (userManager.isUserLoggedIn()) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        }

        loginButton.setOnClickListener(v -> validate());

        signupRedirect.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
    }

    private void validate(){
        String email = userEmail.getText().toString();
        String password = userPass.getText().toString();

        if (email.isEmpty()) {
            userEmail.setError("Please enter an email");
            userEmail.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            userPass.setError("Please enter a password");
            userPass.requestFocus();
        }
        else if (password.length() < 6) {
            userPass.setError("Password is too short, minimum 6 chars");
            userPass.requestFocus();
        }
        else{
            // Show loading state
            setLoading(true);

            userManager.loginUser(email, password, new UserManager.UserDataCallback() {
                @Override
                public void onUserDataLoaded(User user) {
                    // Hide loading state
                    setLoading(false);

                    Toast.makeText(LoginActivity.this, "Welcome " + user.getUserName() + "!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                }

                @Override
                public void onError(Exception e) {
                    // Hide loading state
                    setLoading(false);

                    Toast.makeText(LoginActivity.this, "Login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            loginButton.setText("");
            loginButton.setEnabled(false);
            progressBar.setVisibility(View.VISIBLE);
        } else {
            loginButton.setText(R.string.login_text);
            loginButton.setEnabled(true);
            progressBar.setVisibility(View.GONE);
        }
    }
}