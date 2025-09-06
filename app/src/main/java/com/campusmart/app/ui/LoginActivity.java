package com.campusmart.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.campusmart.app.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private Button loginButton, signUpButton;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        signUpButton = findViewById(R.id.signUpButton);

        loginButton.setOnClickListener(v -> loginUser());
        signUpButton.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, SignupActivity.class)));
    }

    private void loginUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please enter email and password.", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Toast.makeText(LoginActivity.this, "Authentication successful.", Toast.LENGTH_SHORT).show();
                        // Navigate to NewsfeedActivity
                        startActivity(new Intent(LoginActivity.this, NewsfeedActivity.class));
                        finish(); // Finish LoginActivity so user can't go back to it
                    } else {
                        Toast.makeText(LoginActivity.this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // User is already signed in, navigate to NewsfeedActivity
            startActivity(new Intent(LoginActivity.this, NewsfeedActivity.class));
            finish(); // Finish LoginActivity so user can't go back to it
        }
    }
}