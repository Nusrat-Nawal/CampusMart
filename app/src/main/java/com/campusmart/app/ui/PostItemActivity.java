package com.campusmart.app.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.campusmart.app.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PostItemActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private EditText editTextName, editTextPrice, editTextRentDuration;
    private Spinner spinnerType, spinnerCategory;
    private Button buttonProceedToPayment, buttonUploadImage;
    private ImageView imageViewPreview;
    private Uri selectedImageUri;

    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<Intent> paymentActivityResultLauncher;

    private String validatedName, validatedPrice, validatedCategory, validatedType, validatedRentDuration;
    private static final int MAX_IMAGE_SIZE_KB = 150;
    private static final String TAG = "PostItemActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_item);

        db = FirebaseFirestore.getInstance();

        editTextName = findViewById(R.id.editTextName);
        editTextPrice = findViewById(R.id.editTextPrice);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        imageViewPreview = findViewById(R.id.imageViewPreview);
        buttonUploadImage = findViewById(R.id.buttonUploadImage);
        spinnerType = findViewById(R.id.spinnerType);
        editTextRentDuration = findViewById(R.id.editTextRentDuration);
        buttonProceedToPayment = findViewById(R.id.buttonProceedToPayment);

        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                    selectedImageUri = result.getData().getData();
                    try {
                        InputStream is = getContentResolver().openInputStream(selectedImageUri);
                        if (is != null) {
                            int imageSizeBytes = is.available();
                            is.close();
                            if (imageSizeBytes / 1024 > MAX_IMAGE_SIZE_KB) {
                                Toast.makeText(this, "Image too large. Max size: " + MAX_IMAGE_SIZE_KB + "KB", Toast.LENGTH_LONG).show();
                                selectedImageUri = null;
                                imageViewPreview.setImageResource(android.R.color.darker_gray);
                                return;
                            }
                        }
                        Glide.with(this).load(selectedImageUri).into(imageViewPreview);
                    } catch (IOException e) {
                        Toast.makeText(this, "Error checking image size: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        selectedImageUri = null;
                        imageViewPreview.setImageResource(android.R.color.darker_gray);
                    }
                }
            });

        paymentActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    boolean paymentConfirmed = result.getData().getBooleanExtra(PaymentActivity.EXTRA_PAYMENT_CONFIRMED, false);
                    if (paymentConfirmed) {
                        Toast.makeText(PostItemActivity.this, "Payment confirmed. Posting item...", Toast.LENGTH_SHORT).show();
                        processAndSavePostWithBase64Image(validatedName, validatedPrice, validatedCategory, validatedType, validatedRentDuration);
                    } else {
                        Toast.makeText(PostItemActivity.this, "Payment not completed or cancelled.", Toast.LENGTH_LONG).show();
                        buttonProceedToPayment.setVisibility(View.VISIBLE);
                    }
                } else {
                    Toast.makeText(PostItemActivity.this, "Payment cancelled or failed.", Toast.LENGTH_LONG).show();
                    buttonProceedToPayment.setVisibility(View.VISIBLE);
                }
            });

        ArrayAdapter<CharSequence> categoryAdapter = ArrayAdapter.createFromResource(this,
                R.array.post_item_categories, android.R.layout.simple_spinner_item);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(this,
                R.array.post_item_types, android.R.layout.simple_spinner_item);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(typeAdapter);

        spinnerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedType = parent.getItemAtPosition(position).toString();
                if (selectedType.equalsIgnoreCase("Rent")) {
                    editTextRentDuration.setVisibility(View.VISIBLE);
                } else {
                    editTextRentDuration.setVisibility(View.GONE);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                editTextRentDuration.setVisibility(View.GONE);
            }
        });

        buttonUploadImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        buttonProceedToPayment.setOnClickListener(v -> {
            String name = editTextName.getText().toString().trim();
            String price = editTextPrice.getText().toString().trim();
            String category = spinnerCategory.getSelectedItem().toString();
            String type = spinnerType.getSelectedItem().toString();
            String rentDuration = "";

            boolean isValid = true;
            if (name.isEmpty()) { editTextName.setError("Item name required"); isValid = false; }
            if (price.isEmpty()) { editTextPrice.setError("Price required"); isValid = false; }
            else {
                try {
                    double priceValue = Double.parseDouble(price);
                    if (priceValue <= 0) { editTextPrice.setError("Price must be positive"); isValid = false;}
                } catch (NumberFormatException e) {
                    editTextPrice.setError("Invalid price format"); isValid = false;
                }
            }

            if (category.equalsIgnoreCase("Select Category")) { Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show(); isValid = false; }
            if (type.equalsIgnoreCase("Select Type")) { Toast.makeText(this, "Please select an item type", Toast.LENGTH_SHORT).show(); isValid = false; }
            if (selectedImageUri == null) { Toast.makeText(this, "Please select an image (max " + MAX_IMAGE_SIZE_KB + "KB)", Toast.LENGTH_SHORT).show(); isValid = false; }
            
            if (type.equalsIgnoreCase("Rent")) {
                rentDuration = editTextRentDuration.getText().toString().trim();
                if (rentDuration.isEmpty()) { editTextRentDuration.setError("Enter rent duration"); isValid = false; }
            }

            if (!isValid) {
                Toast.makeText(this, "Please fill all required details correctly.", Toast.LENGTH_LONG).show();
                return;
            }

            this.validatedName = name; 
            this.validatedPrice = price; 
            this.validatedCategory = category;
            this.validatedType = type; 
            this.validatedRentDuration = rentDuration;

            Intent paymentIntent = new Intent(PostItemActivity.this, PaymentActivity.class);
            paymentIntent.putExtra(PaymentActivity.EXTRA_ITEM_PRICE, this.validatedPrice);
            paymentActivityResultLauncher.launch(paymentIntent);
            
            buttonProceedToPayment.setVisibility(View.GONE); 
        });
    }

    private String convertUriToBase64(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                Toast.makeText(this, "Failed to open image stream.", Toast.LENGTH_SHORT).show();
                return null;
            }
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();

            if (bitmap == null) {
                 Toast.makeText(this, "Failed to decode image.", Toast.LENGTH_SHORT).show();
                 return null;
            }

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream); 
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            byteArrayOutputStream.close();

            if (byteArray.length / 1024 > MAX_IMAGE_SIZE_KB) {
                 Toast.makeText(this, "Compressed image still too large (" + byteArray.length / 1024 + "KB). Please use a smaller image.", Toast.LENGTH_LONG).show();
                 return null;
            }
            return Base64.encodeToString(byteArray, Base64.DEFAULT);
        } catch (IOException e) {
            Toast.makeText(this, "Error converting image to Base64: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return null;
        }
    }

    private void processAndSavePostWithBase64Image(String name, String price, String category, String type, String rentDuration) {
        if (selectedImageUri == null) {
            Toast.makeText(this, "No image selected. Please go back and select an image.", Toast.LENGTH_LONG).show();
            buttonProceedToPayment.setVisibility(View.VISIBLE);
            return;
        }
        buttonProceedToPayment.setEnabled(false);
        Toast.makeText(this, "Processing image...", Toast.LENGTH_SHORT).show();

        String imageBase64 = convertUriToBase64(selectedImageUri);

        if (imageBase64 == null) {
            buttonProceedToPayment.setEnabled(true);
            buttonProceedToPayment.setVisibility(View.VISIBLE);
            return;
        }
        
        if (imageBase64.length() > 700 * 1024) { 
             Toast.makeText(this, "Image data is too large after encoding. Try a smaller image.", Toast.LENGTH_LONG).show();
             buttonProceedToPayment.setEnabled(true);
             buttonProceedToPayment.setVisibility(View.VISIBLE);
             return;
        }
        savePostToFirestore(name, price, category, type, rentDuration, imageBase64);
    }

    private void savePostToFirestore(String name, String price, String category, String type, String rentDuration, String imageBase64) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Authentication error. Please log in again.", Toast.LENGTH_LONG).show();
            buttonProceedToPayment.setEnabled(true); 
            buttonProceedToPayment.setVisibility(View.VISIBLE); 
            return; 
        }
        String currentUserId = currentUser.getUid(); 

        Map<String, Object> post = new HashMap<>();
        post.put("name", name);
        post.put("price", price);
        post.put("category", category);
        post.put("type", type);
        post.put("imageBase64", imageBase64); 
        post.put("timestamp", FieldValue.serverTimestamp());
        post.put("authorId", currentUserId);

        if (type.equalsIgnoreCase("Rent")) {
            post.put("rentDuration", rentDuration);
        } else {
            post.put("rentDuration", "N/A");
        }

        db.collection("posts")
            .add(post)
            .addOnSuccessListener(documentReference -> {
                Toast.makeText(PostItemActivity.this, "Item posted successfully!", Toast.LENGTH_SHORT).show();
                String newPostId = documentReference.getId();
                // Call method to check for wish matches and create notifications
                checkForMatchesAndCreateNotifications(newPostId, name, currentUserId);
                finish();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(PostItemActivity.this, "Error posting item: " + e.getMessage(), Toast.LENGTH_LONG).show();
                Log.e(TAG, "Error posting item to Firestore", e); 
                buttonProceedToPayment.setEnabled(true);
                buttonProceedToPayment.setVisibility(View.VISIBLE); 
            });
    }

    private void checkForMatchesAndCreateNotifications(String newPostId, String newPostName, String newPostAuthorId) {
        db.collection("wishes")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (queryDocumentSnapshots.isEmpty()) {
                    Log.d(TAG, "No wishes found to check against.");
                    return;
                }
                for (DocumentSnapshot wishDocument : queryDocumentSnapshots) {
                    String wishUserId = wishDocument.getString("userId");
                    String wishItemName = wishDocument.getString("itemName");
                    String wishId = wishDocument.getId();

                    if (wishUserId == null || wishItemName == null) {
                        Log.w(TAG, "Skipping wish due to missing userId or itemName: " + wishId);
                        continue;
                    }

                    // Don't notify the user if they posted the item that matches their own wish
                    if (wishUserId.equals(newPostAuthorId)) {
                        Log.d(TAG, "Skipping notification for self-posted item for wish: " + wishId);
                        continue;
                    }

                    // Case-insensitive matching: if wish item name contains new post name, or vice-versa
                    boolean namesMatch = wishItemName.toLowerCase(Locale.ROOT).contains(newPostName.toLowerCase(Locale.ROOT)) ||
                                         newPostName.toLowerCase(Locale.ROOT).contains(wishItemName.toLowerCase(Locale.ROOT));

                    if (namesMatch) {
                        Log.d(TAG, "Match found! Wish: " + wishItemName + ", New Post: " + newPostName);
                        Map<String, Object> notification = new HashMap<>();
                        notification.put("notifiedUserId", wishUserId); // User who made the wish
                        notification.put("triggeringPostId", newPostId);
                        notification.put("triggeringPostName", newPostName);
                        notification.put("triggeringPostAuthorId", newPostAuthorId); // User who posted the item
                        notification.put("wishId", wishId);
                        notification.put("wishItemName", wishItemName);
                        String message = String.format(Locale.getDefault(),
                                "An item matching your wish '%s' has been posted: '%s'!",
                                wishItemName, newPostName);
                        notification.put("message", message);
                        notification.put("timestamp", FieldValue.serverTimestamp());
                        notification.put("isRead", false);

                        db.collection("notifications")
                            .add(notification)
                            .addOnSuccessListener(docRef -> Log.d(TAG, "Notification created for user " + wishUserId + " for post " + newPostId))
                            .addOnFailureListener(e -> Log.e(TAG, "Failed to create notification for user " + wishUserId, e));
                    }
                }
            })
            .addOnFailureListener(e -> Log.e(TAG, "Error fetching wishes for notification check: ", e));
    }
}
