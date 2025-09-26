package com.campusmart.app.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.campusmart.app.R;
import com.campusmart.app.api.MlKitOcrService;
import com.campusmart.app.models.User;
import com.campusmart.app.utils.Constants;
import com.campusmart.app.utils.StringMatcher;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
// import com.google.gson.JsonObject; // No longer needed for direct parsing
// import com.google.gson.JsonParser; // No longer needed for direct parsing
// import com.google.gson.JsonSyntaxException; // No longer needed for direct parsing
import com.google.mlkit.vision.text.Text;

import java.io.IOException;

public class SignupActivity extends AppCompatActivity {

    private EditText nameEditText, emailEditText, studentIdEditText, universityEditText,
            phoneEditText, addressEditText, passwordEditText;
    private Button uploadIdButton, signupButton;
    private TextView selectedImageTextView;
    private ImageView idPreviewImageView;

    private Uri studentIdImageUri;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private MlKitOcrService ocrService;

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
        ocrService = new MlKitOcrService();

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

        if (studentIdImageUri == null) {
            handleSignupError("Please upload a photo of your student ID.");
            return;
        }

        try {
            ocrService.recognizeText(this, studentIdImageUri)
                .addOnSuccessListener(new OnSuccessListener<Text>() {
                    @Override
                    public void onSuccess(Text visionText) {
                        runOnUiThread(() -> {
                            String extractedFullText = visionText.getText();
                            Log.d("SignupActivity_OCR", "ML Kit Extracted Full Text:\n" + extractedFullText);

                            String parsedName = "";
                            String parsedId = "";
                            String parsedUniversity = "";

                            // --- Start of new parsing logic ---
                            String[] lines = extractedFullText.split("\n");
                            for (String line : lines) {
                                String trimmedLine = line.trim();

                                // Attempt to find University Name
                                if (parsedUniversity.isEmpty() && trimmedLine.toUpperCase().contains("SOUTHEAST UNIVERSITY")) {
                                    parsedUniversity = "Southeast University"; 
                                } else if (parsedUniversity.isEmpty() && trimmedLine.toUpperCase().contains("UNIVERSITY")) {
                                   // Fallback, might need refinement
                                    if (trimmedLine.length() > 10 && trimmedLine.length() < 50) { // Avoid grabbing random lines
                                     parsedUniversity = trimmedLine;
                                    }
                                }

                                // Attempt to find Student ID
                                if (parsedId.isEmpty()) {
                                    if (trimmedLine.startsWith(":") && trimmedLine.matches(":\\s*\\d{10,}.*")) {
                                        parsedId = trimmedLine.replaceAll(".*:\\s*", "").replaceAll("[^0-9]", "");
                                    } else if (trimmedLine.matches("\\d{10,}")) { 
                                        parsedId = trimmedLine.replaceAll("[^0-9]", "");
                                    } else if (trimmedLine.toUpperCase().contains("ID") && trimmedLine.matches(".*\\d{5,}.*")) {
                                        // Look for lines with "ID" and some numbers
                                        parsedId = trimmedLine.replaceAll("[^0-9]", "");
                                    }
                                }
                                
                                // Attempt to find Name
                                if (parsedName.isEmpty() &&
                                    trimmedLine.matches("^[A-Za-zÀ-ÖØ-öø-ÿ.]+\\s[A-Za-zÀ-ÖØ-öø-ÿ\\s.]*[A-Za-zÀ-ÖØ-öø-ÿ.]+$") &&
                                    trimmedLine.split("\\s+").length >= 2 &&  trimmedLine.split("\\s+").length <= 5 && // 2 to 5 words
                                    !trimmedLine.toUpperCase().contains("UNIVERSITY") &&
                                    !trimmedLine.toUpperCase().contains("PROGRAM") &&
                                    !trimmedLine.toUpperCase().contains("BATCH") &&
                                    !trimmedLine.toUpperCase().contains("CODE") &&
                                    !trimmedLine.toUpperCase().contains("BLOOD") &&
                                    !trimmedLine.toUpperCase().contains("GROUP") &&
                                    !trimmedLine.toUpperCase().contains("DATE") &&
                                    !trimmedLine.toUpperCase().contains("BIRTH") &&
                                    !trimmedLine.toUpperCase().matches(".*(STUDENT|CARD|IDENTIFICATION).*") &&
                                    !trimmedLine.matches(".*\\d.*")) { 
                                    parsedName = trimmedLine;
                                }
                            }
                            // --- End of new parsing logic ---

                            Log.d("SignupActivity_OCR", "Parsed from ML Kit - Name: '" + parsedName + "', ID: '" + parsedId + "', University: '" + parsedUniversity + "'");

                            String userEnteredName = nameEditText.getText().toString().trim();
                            String userEnteredId = studentIdEditText.getText().toString().trim();
                            String userEnteredUniversity = universityEditText.getText().toString().trim();

                            Log.d("SignupActivity_OCR", "User Entered (trimmed) - Name: '" + userEnteredName + "', ID: '" + userEnteredId + "', University: '" + userEnteredUniversity + "'");

                            boolean nameMatch = StringMatcher.compareWithLevenshtein(userEnteredName, parsedName, 2);
                            boolean idMatch = StringMatcher.compare(userEnteredId, parsedId);
                            boolean universityMatch = StringMatcher.compareWithLevenshtein(userEnteredUniversity, parsedUniversity, 2);

                            Log.d("SignupActivity_OCR", "Match Results - Name: " + nameMatch + ", ID: " + idMatch + ", University: " + universityMatch);

                            if (nameMatch && idMatch && universityMatch) {
                                createFirebaseUser(userEnteredName, userEnteredId, userEnteredUniversity);
                            } else {
                                String validationMessage = "Verification Failed. Please check your entries against the ID card.";
                                boolean anyFieldParsed = !parsedName.isEmpty() || !parsedId.isEmpty() || !parsedUniversity.isEmpty();

                                if (!anyFieldParsed && !extractedFullText.isEmpty()) {
                                     showValidationErrorWithRawText("Could not automatically extract all details from the ID. Please verify manually.\n\nRaw extracted text:\n" + extractedFullText);
                                } else {
                                    if (!nameMatch) validationMessage += "\n\nName mismatch. (Entered: '" + userEnteredName + "', Found on ID: '" + (parsedName.isEmpty() ? "Not found" : parsedName) + "')";
                                    if (!idMatch) validationMessage += "\nID mismatch. (Entered: '" + userEnteredId + "', Found on ID: '" + (parsedId.isEmpty() ? "Not found" : parsedId) + "')";
                                    if (!universityMatch) validationMessage += "\nUniversity mismatch. (Entered: '" + userEnteredUniversity + "', Found on ID: '" + (parsedUniversity.isEmpty() ? "Not found" : parsedUniversity) + "')";
                                    showCustomValidationError(validationMessage);
                                }
                            }
                        });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        runOnUiThread(() -> handleSignupError("Image verification failed: " + e.getMessage()));
                    }
                });
        } catch (IOException e) {
            runOnUiThread(() -> handleSignupError("Failed to load image for OCR: " + e.getMessage()));
        }
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
        User newUser = new User(uid, name, emailEditText.getText().toString(), studentId,
                addressEditText.getText().toString(), phoneEditText.getText().toString(), university);
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

    // This method is kept for reference if direct JSON parsing was ever to be restored or for other uses.
    private void showValidationError(String extractedName, String extractedId, String extractedUniversity) {
        signupButton.setEnabled(true);
        String message = "The details from your ID do not match the entered information. Please check and try again.\n\n" +
                "**Entered:**\n" +
                "Name: " + nameEditText.getText().toString() + "\n" +
                "ID: " + studentIdEditText.getText().toString() + "\n" +
                "University: " + universityEditText.getText().toString() + "\n\n" +
                "**Extracted from ID (Attempted):**\n" +
                "Name: " + (TextUtils.isEmpty(extractedName) ? "Not found" : extractedName) + "\n" +
                "ID: " + (TextUtils.isEmpty(extractedId) ? "Not found" : extractedId) + "\n" +
                "University: " + (TextUtils.isEmpty(extractedUniversity) ? "Not found" : extractedUniversity);

        new AlertDialog.Builder(this)
                .setTitle(R.string.validation_error_dialog_title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
    
    private void showValidationErrorWithRawText(String message) {
        signupButton.setEnabled(true);
        new AlertDialog.Builder(this)
                .setTitle("ID Verification Issue")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void showCustomValidationError(String message) {
        signupButton.setEnabled(true);
        new AlertDialog.Builder(this)
                .setTitle("ID Verification Issue")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void handleSignupError(String message) {
        signupButton.setEnabled(true);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
