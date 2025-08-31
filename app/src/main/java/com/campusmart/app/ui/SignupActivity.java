package com.campusmart.app.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.campusmart.app.R;
import com.campusmart.app.api.GeminiApi;
import com.campusmart.app.models.User;
import com.campusmart.app.utils.Constants;
import com.campusmart.app.utils.StringMatcher;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class SignupActivity extends AppCompatActivity {

    private EditText nameEditText, emailEditText, studentIdEditText, universityEditText,
            phoneEditText, addressEditText, passwordEditText;
    private Button uploadIdButton, signupButton;
    private TextView selectedImageTextView;
    private ImageView idPreviewImageView;

    private Uri studentIdImageUri;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GeminiApi geminiApi;

    // Activity Result Launcher for image picking
    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    studentIdImageUri = uri;
                    selectedImageTextView.setText("Image selected");
                    idPreviewImageView.setVisibility(View.VISIBLE);
                    Glide.with(this).load(studentIdImageUri).into(idPreviewImageView);
                } else {
                    studentIdImageUri = null;
                    selectedImageTextView.setText(R.string.image_id_placeholder);
                    idPreviewImageView.setVisibility(View.GONE);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        geminiApi = new GeminiApi(this);

        initViews();

        uploadIdButton.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        signupButton.setOnClickListener(v -> {
            if (validateInputs()) {
                startSignupProcess();
            }
        });
    }

    private void initViews() {
        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        studentIdEditText = findViewById(R.id.studentIdEditText);
        universityEditText = findViewById(R.id.universityEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        addressEditText = findViewById(R.id.addressEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        uploadIdButton = findViewById(R.id.uploadIdButton);
        signupButton = findViewById(R.id.signupButton);
        selectedImageTextView = findViewById(R.id.selectedImageTextView);
        idPreviewImageView = findViewById(R.id.idPreviewImageView);
    }

    private boolean validateInputs() {
        if (TextUtils.isEmpty(nameEditText.getText()) || TextUtils.isEmpty(emailEditText.getText()) ||
                TextUtils.isEmpty(studentIdEditText.getText()) || TextUtils.isEmpty(universityEditText.getText()) ||
                TextUtils.isEmpty(phoneEditText.getText()) || TextUtils.isEmpty(addressEditText.getText()) ||
                TextUtils.isEmpty(passwordEditText.getText())) {
            Toast.makeText(this, "Please fill out all fields.", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (passwordEditText.getText().toString().length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (studentIdImageUri == null) {
            Toast.makeText(this, "Please upload a photo of your student ID.", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void startSignupProcess() {
        signupButton.setEnabled(false);

        // Step 1: Call Gemini API for text extraction
        geminiApi.extractTextFromImage(studentIdImageUri, new GeminiApi.GeminiResponseCallback() {
            @Override
            public void onSuccess(String jsonResult) {
                runOnUiThread(() -> {
                    try {
                        // Step 2 & 3: Parse and validate
                        JsonObject extractedData = JsonParser.parseString(jsonResult).getAsJsonObject();
                        String extractedName = extractedData.has("name") ? extractedData.get("name").getAsString() : "";
                        String extractedId = extractedData.has("studentId") ? extractedData.get("studentId").getAsString() : "";
                        String extractedUniversity = extractedData.has("university") ? extractedData.get("university").getAsString() : "";

                        String userEnteredName = nameEditText.getText().toString();
                        String userEnteredId = studentIdEditText.getText().toString();
                        String userEnteredUniversity = universityEditText.getText().toString();

                        boolean nameMatch = StringMatcher.compareWithLevenshtein(userEnteredName, extractedName, 2);
                        boolean idMatch = StringMatcher.compare(userEnteredId, extractedId);
                        boolean universityMatch = StringMatcher.compareWithLevenshtein(userEnteredUniversity, extractedUniversity, 2);

                        if (nameMatch && idMatch && universityMatch) {
                            // Step 4: All fields match, proceed with Firebase registration
                            createFirebaseUser(userEnteredName, userEnteredId, userEnteredUniversity);
                        } else {
                            // Validation failed, show error
                            showValidationError(extractedName, extractedId, extractedUniversity);
                        }
                    } catch (JsonSyntaxException e) {
                        handleSignupError("Failed to parse Gemini API response. Please try again or with a clearer image.");
                    }
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> handleSignupError("Image verification failed: " + errorMessage));
            }
        });
    }

    private void createFirebaseUser(String name, String studentId, String university) {
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            updateUserProfile(user, name, studentId, university);
                        }
                    } else {
                        handleSignupError("Authentication failed: " + task.getException().getMessage());
                    }
                });
    }

    private void updateUserProfile(FirebaseUser user, String name, String studentId, String university) {
        // Update Firebase Auth profile
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build();

        user.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        saveUserProfileToFirestore(user.getUid(), name, studentId, university);
                    } else {
                        handleSignupError("Failed to update user profile.");
                    }
                });
    }

    private void saveUserProfileToFirestore(String uid, String name, String studentId, String university) {
        // Note: studentIdImageUrl is no longer part of the User model
        User newUser = new User(uid, name, emailEditText.getText().toString(), studentId,
                addressEditText.getText().toString(), phoneEditText.getText().toString(), university);

        // Mark as verified since all checks passed.
        newUser.setVerified(true);

        db.collection(Constants.FIRESTORE_USERS_COLLECTION)
                .document(uid)
                .set(newUser)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(SignupActivity.this, "Account created successfully!", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    handleSignupError("Failed to save user profile: " + e.getMessage());
                });
    }

    private void showValidationError(String extractedName, String extractedId, String extractedUniversity) {
        signupButton.setEnabled(true);
        String message = "The details from your ID do not match the entered information. Please check and try again.\n\n" +
                "**Entered:**\n" +
                "Name: " + nameEditText.getText().toString() + "\n" +
                "ID: " + studentIdEditText.getText().toString() + "\n" +
                "University: " + universityEditText.getText().toString() + "\n\n" +
                "**Extracted from ID:**\n" +
                "Name: " + extractedName + "\n" +
                "ID: " + extractedId + "\n" +
                "University: " + extractedUniversity;

        new AlertDialog.Builder(this)
                .setTitle(R.string.validation_error_dialog_title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void handleSignupError(String message) {
        signupButton.setEnabled(true);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}