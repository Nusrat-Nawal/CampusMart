package com.campusmart.app.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.campusmart.app.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class PostWishActivity extends AppCompatActivity {

    private EditText editTextWishItemName, editTextWishItemDescription, editTextWishRentDuration;
    private RadioGroup radioGroupWishType;
    private RadioButton radioButtonWishBuy, radioButtonWishRent;
    private TextInputLayout textInputLayoutWishRentDuration;
    private Spinner spinnerWishCategory;
    private MaterialButton buttonPostWishSubmit;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_wish);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        editTextWishItemName = findViewById(R.id.editTextWishItemName);
        editTextWishItemDescription = findViewById(R.id.editTextWishItemDescription); // New EditText
        radioGroupWishType = findViewById(R.id.radioGroupWishType);
        radioButtonWishBuy = findViewById(R.id.radioButtonWishBuy);
        radioButtonWishRent = findViewById(R.id.radioButtonWishRent);
        textInputLayoutWishRentDuration = findViewById(R.id.textInputLayoutWishRentDuration);
        editTextWishRentDuration = findViewById(R.id.editTextWishRentDuration);
        spinnerWishCategory = findViewById(R.id.spinnerWishCategory);
        buttonPostWishSubmit = findViewById(R.id.buttonPostWishSubmit);

        String[] categories = {"Electronics & Stationary", "Cosmetics", "Accessories"};
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerWishCategory.setAdapter(categoryAdapter);

        radioGroupWishType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioButtonWishRent) {
                textInputLayoutWishRentDuration.setVisibility(View.VISIBLE);
            } else {
                textInputLayoutWishRentDuration.setVisibility(View.GONE);
            }
        });

        buttonPostWishSubmit.setOnClickListener(v -> postWish());
    }

    private void postWish() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to post a wish.", Toast.LENGTH_SHORT).show();
            return;
        }

        String itemName = editTextWishItemName.getText().toString().trim();
        String itemDescription = editTextWishItemDescription.getText().toString().trim();
        String selectedCategory = spinnerWishCategory.getSelectedItem().toString();
        String rentDuration = editTextWishRentDuration.getText().toString().trim();

        if (itemName.isEmpty()) {
            editTextWishItemName.setError("Item name cannot be empty");
            editTextWishItemName.requestFocus();
            return;
        }

        String wishType = radioButtonWishBuy.isChecked() ? "Buy" : "Rent";

        if (wishType.equals("Rent") && rentDuration.isEmpty()) {
            editTextWishRentDuration.setError("Rent duration cannot be empty for rent wishes");
            editTextWishRentDuration.requestFocus();
            return;
        }

        Map<String, Object> wish = new HashMap<>();
        wish.put("userId", currentUser.getUid());
        wish.put("userEmail", currentUser.getEmail());
        wish.put("itemName", itemName);
        wish.put("itemDescription", itemDescription.isEmpty() ? "N/A" : itemDescription);
        wish.put("wishType", wishType);
        wish.put("category", selectedCategory);
        if (wishType.equals("Rent")) {
            wish.put("rentDuration", rentDuration);
        }
        wish.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());

        db.collection("wishes")
                .add(wish)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(PostWishActivity.this, "Wish posted successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(PostWishActivity.this, "Error posting wish: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
