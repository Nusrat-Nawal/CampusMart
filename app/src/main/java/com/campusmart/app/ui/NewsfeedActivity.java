package com.campusmart.app.ui;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View; // Import View
import android.view.ViewGroup;
import android.widget.AdapterView; // Import this
import android.widget.ArrayAdapter; // Import this
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner; // Import Spinner
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.campusmart.app.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query; // Import Query

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class NewsfeedActivity extends AppCompatActivity {

    private static final String TAG = "NewsfeedActivity";
    private FirebaseFirestore db;
    private LinearLayout newsfeedContainer;
    private Spinner categorySpinner; // Declare the spinner
    private String selectedCategory = "All"; // Variable to hold the currently selected category

    // Define categories as constants for consistency
    // Make sure these match the EXACT values you store in Firestore's "category" field
    private static final String CATEGORY_ALL = "All";
    private static final String CATEGORY_ELECTRONICS = "Electronics & Stationary";
    private static final String CATEGORY_COSMETICS = "Cosmetics";
    private static final String CATEGORY_ACCESSORIES = "Accessories";
    // Add any other categories you have in your Firestore here

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_newsfeed); // Ensure this ID is correct

        db = FirebaseFirestore.getInstance();
        newsfeedContainer = findViewById(R.id.newsfeedContainer); // Ensure this ID is correct
        categorySpinner = findViewById(R.id.categorySpinner); // Find the spinner

        // --- Spinner Setup ---
        setupCategorySpinner();
        // --- End Spinner Setup ---

        // Button click listeners for navigation
        findViewById(R.id.buttonPostItem).setOnClickListener(v ->
                startActivity(new Intent(this, PostItemActivity.class))
        );

        findViewById(R.id.buttonWishboard).setOnClickListener(v ->
                startActivity(new Intent(this, WishboardActivity.class))
        );

        findViewById(R.id.buttonProfile).setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class))
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload posts whenever the activity resumes, using the last selected category
        loadPosts(selectedCategory);
    }

    // Helper function to convert DP to pixels for layout parameters
    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
    }

    // --- NEW METHOD: Setup Spinner ---
    private void setupCategorySpinner() {
        // Define the categories that will appear in the spinner
        // Ensure these strings match your Firestore category field values EXACTLY
        String[] categories = {CATEGORY_ALL, CATEGORY_ELECTRONICS, CATEGORY_COSMETICS, CATEGORY_ACCESSORIES};

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        categorySpinner.setAdapter(adapter);

        // Set an OnItemSelectedListener to the spinner to handle user selections
        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                // Get the selected category string from the adapter
                selectedCategory = (String) parentView.getItemAtPosition(position);
                Log.d(TAG, "Selected category: " + selectedCategory);
                // Reload posts based on the new selection
                loadPosts(selectedCategory);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // Do nothing when nothing is selected
            }
        });
    }
    // --- END NEW METHOD ---

    // --- MODIFIED METHOD: loadPosts ---
    // This method now accepts a 'category' parameter and uses it for filtering
    private void loadPosts(String category) {
        // Ensure user is authenticated before loading posts
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Log.e(TAG, "User is NOT authenticated. Cannot load posts.");
            Toast.makeText(this, "Error: Not authenticated. Please log in again to see posts.", Toast.LENGTH_LONG).show();
            newsfeedContainer.removeAllViews(); // Clear existing views
            // Display a message that user needs to log in
            TextView tv = new TextView(this);
            tv.setText("Please log in to see the newsfeed.");
            tv.setGravity(Gravity.CENTER);
            tv.setTextSize(18);
            newsfeedContainer.addView(tv);
            return;
        } else {
            Log.d(TAG, "User is authenticated: " + FirebaseAuth.getInstance().getCurrentUser().getUid());
        }

        // --- Step 1: Check for sold/rented products first ---
        // This prevents displaying items that are no longer available.
        db.collection("orders")
                .whereEqualTo("status", "accepted") // Assuming "accepted" orders mean the product is sold/rented
                .get()
                .addOnSuccessListener(orderSnapshots -> {
                    Set<String> soldProductIds = new HashSet<>();
                    for (DocumentSnapshot orderDoc : orderSnapshots) {
                        // Add the productId of accepted orders to a set
                        soldProductIds.add(orderDoc.getString("productId"));
                    }

                    // --- Step 2: Fetch posts with category filtering ---
                    Query query = db.collection("posts");

                    // Apply the category filter ONLY if the selected category is NOT "All"
                    if (!category.equals(CATEGORY_ALL)) {
                        // !!! IMPORTANT !!!
                        // Ensure your Firestore documents in the 'posts' collection
                        // have a field named "category" that EXACTLY matches the string values
                        // in your CATEGORY constants (case-sensitive, spelling).
                        query = query.whereEqualTo("category", category);
                    }

                    // Add ordering (newest first)
                    query = query.orderBy("timestamp", Query.Direction.DESCENDING);

                    // Execute the filtered and ordered query for posts
                    query.get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                newsfeedContainer.removeAllViews(); // Clear previous posts before adding new ones
                                if (queryDocumentSnapshots.isEmpty()) {
                                    // Display a message if no posts are found for the selected category
                                    TextView tv = new TextView(this);
                                    tv.setText("No posts yet in this category. Be the first to post!");
                                    tv.setGravity(Gravity.CENTER);
                                    tv.setTextSize(18);
                                    newsfeedContainer.addView(tv);
                                } else {
                                    // Iterate through each document (post) returned by the query
                                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                                        // Extract data directly from the DocumentSnapshot
                                        String name = doc.getString("name");
                                        String price = doc.getString("price");
                                        String type = doc.getString("type");
                                        String postCategory = doc.getString("category"); // Get the category from Firestore
                                        String rentDuration = doc.getString("rentDuration");
                                        String authorId = doc.getString("authorId"); // Get authorId for showSellerInfo
                                        String imageBase64 = doc.getString("imageBase64"); // For image loading

                                        // Check if this product has already been sold or rented
                                        final String postId = doc.getId(); // Get the document ID for the current post
                                        boolean isSold = soldProductIds.contains(postId); // Check if its ID is in the set of sold products

                                        // --- Dynamically Create UI Elements for each post ---
                                        CardView cardView = new CardView(this);
                                        LinearLayout.LayoutParams cardLayoutParams = new LinearLayout.LayoutParams(
                                                LinearLayout.LayoutParams.MATCH_PARENT,
                                                LinearLayout.LayoutParams.WRAP_CONTENT
                                        );
                                        cardLayoutParams.setMargins(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(12));
                                        cardView.setLayoutParams(cardLayoutParams);
                                        cardView.setRadius(dpToPx(8));
                                        cardView.setCardElevation(dpToPx(4));
                                        cardView.setContentPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));
                                        cardView.setCardBackgroundColor(Color.WHITE);

                                        LinearLayout postLayout = new LinearLayout(this);
                                        postLayout.setOrientation(LinearLayout.HORIZONTAL);
                                        postLayout.setLayoutParams(new LinearLayout.LayoutParams(
                                                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                                        postLayout.setGravity(Gravity.CENTER_VERTICAL);

                                        ImageView imageView = new ImageView(this);
                                        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(
                                                0, dpToPx(160));
                                        imageParams.weight = 1.0f;
                                        imageParams.setMargins(0,0,dpToPx(12),0);
                                        imageView.setLayoutParams(imageParams);
                                        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                                        imageView.setBackgroundColor(Color.LTGRAY);

                                        // Load image from Base64 string
                                        if (imageBase64 != null && !imageBase64.isEmpty()) {
                                            try {
                                                byte[] imageBytes = Base64.decode(imageBase64, Base64.DEFAULT);
                                                Glide.with(NewsfeedActivity.this)
                                                        .asBitmap()
                                                        .load(imageBytes)
                                                        .placeholder(R.drawable.ic_launcher_background) // Default placeholder
                                                        .error(R.drawable.ic_launcher_foreground)     // Error image
                                                        .into(imageView);
                                            } catch (IllegalArgumentException e) {
                                                Log.e(TAG, "Error decoding Base64 string for post: " + doc.getId(), e);
                                                imageView.setImageResource(R.drawable.ic_launcher_foreground); // Show error image on decode error
                                            }
                                        } else {
                                            imageView.setImageResource(R.drawable.ic_launcher_background); // Default placeholder if no image
                                        }
                                        postLayout.addView(imageView);

                                        LinearLayout textInfoLayout = new LinearLayout(this);
                                        textInfoLayout.setOrientation(LinearLayout.VERTICAL);
                                        LinearLayout.LayoutParams textInfoParams = new LinearLayout.LayoutParams(
                                                0, LinearLayout.LayoutParams.WRAP_CONTENT);
                                        textInfoParams.weight = 1.5f;
                                        textInfoLayout.setLayoutParams(textInfoParams);

                                        // --- Populate Text Views ---
                                        TextView tvName = new TextView(this);
                                        tvName.setText((name != null ? name : "N/A"));
                                        tvName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                                        tvName.setTextColor(Color.BLACK);
                                        tvName.setTypeface(null, Typeface.BOLD);
                                        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                                        nameParams.setMargins(0,0,0,dpToPx(4));
                                        tvName.setLayoutParams(nameParams);
                                        textInfoLayout.addView(tvName);

                                        // Display the category fetched from Firestore
                                        TextView tvCategory = new TextView(this);
                                        tvCategory.setText("Category: " + (postCategory != null ? postCategory : "N/A"));
                                        tvCategory.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                                        tvCategory.setTextColor(Color.DKGRAY);
                                        textInfoLayout.addView(tvCategory);

                                        TextView tvType = new TextView(this);
                                        tvType.setText("Type: " + (type != null ? type : "N/A"));
                                        tvType.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                                        tvType.setTextColor(Color.DKGRAY);
                                        textInfoLayout.addView(tvType);

                                        TextView tvPrice = new TextView(this);
                                        tvPrice.setText("Price: " + (price != null ? price : "0") + " Tk");
                                        tvPrice.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                                        tvPrice.setTextColor(Color.rgb(0, 100, 0)); // Dark Green
                                        tvPrice.setTypeface(null, Typeface.BOLD);
                                        LinearLayout.LayoutParams priceParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                                        priceParams.setMargins(0,dpToPx(4),0,0);
                                        tvPrice.setLayoutParams(priceParams);
                                        textInfoLayout.addView(tvPrice);

                                        // Display rent duration if applicable
                                        if (type != null && type.equalsIgnoreCase("Rent") && rentDuration != null && !rentDuration.isEmpty() && !rentDuration.equals("N/A")) {
                                            TextView tvRentDuration = new TextView(this);
                                            tvRentDuration.setText("Rent Duration: " + rentDuration);
                                            tvRentDuration.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                                            tvRentDuration.setTextColor(Color.DKGRAY);
                                            textInfoLayout.addView(tvRentDuration);
                                        }
                                        // --- End Populate Text Views ---

                                        // --- Configure Action Button (Buy/Rent or Not Available) ---
                                        Button actionButton = new Button(this);
                                        final String postType = type; // Store type for the listener

                                        if (isSold) { // Check if the product is sold/rented based on the initial query
                                            actionButton.setText("Not Available");
                                            actionButton.setEnabled(false); // Disable the button
                                            actionButton.setBackgroundColor(Color.GRAY); // Change background color
                                        } else {
                                            // Set button text based on post type
                                            if ("Rent".equalsIgnoreCase(postType)) {
                                                actionButton.setText("Rent");
                                            } else {
                                                actionButton.setText("Buy");
                                            }
                                            actionButton.setBackgroundResource(R.drawable.button_background); // Your custom button background
                                            // Set click listener for Buy/Rent button
                                            actionButton.setOnClickListener(v -> {
                                                Log.d(TAG, "Attempting to show seller info for post: " + doc.getId());
                                                // authorId is needed to fetch seller details
                                                if (authorId != null && !authorId.isEmpty()) {
                                                    showSellerInfo(authorId, doc.getId(), postType);
                                                } else {
                                                    Log.e(TAG, "Post with ID " + doc.getId() + " is missing an authorId.");
                                                    Toast.makeText(NewsfeedActivity.this, "Seller information not available.", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        }

                                        actionButton.setTextColor(Color.BLACK);
                                        actionButton.setTypeface(null, Typeface.BOLD);

                                        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                                                dpToPx(100),
                                                dpToPx(40)
                                        );
                                        buttonParams.setMargins(0, dpToPx(8), 0, 0);
                                        actionButton.setLayoutParams(buttonParams);
                                        textInfoLayout.addView(actionButton);
                                        // --- End Action Button Configuration ---

                                        postLayout.addView(textInfoLayout); // Add text layout to the horizontal post layout
                                        cardView.addView(postLayout);       // Add the post layout to the card view
                                        newsfeedContainer.addView(cardView); // Add the created card view to the main newsfeed container
                                    }
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error loading posts from Firestore: " + e.getMessage(), e);
                                Toast.makeText(NewsfeedActivity.this, "Error loading posts: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading orders from Firestore: " + e.getMessage(), e);
                    Toast.makeText(NewsfeedActivity.this, "Error checking product availability: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    // Fallback mechanism: If fetching orders fails, load posts without checking availability.
                    loadPostsWithoutAvailabilityCheck();
                });
    }

    // This method is called as a fallback if fetching orders (for availability check) fails.
    // It loads all posts without checking if they are sold/rented.
    private void loadPostsWithoutAvailabilityCheck() {
        db.collection("posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    newsfeedContainer.removeAllViews(); // Clear existing views
                    if (queryDocumentSnapshots.isEmpty()) {
                        // Display message if no posts are found
                        TextView tv = new TextView(this);
                        tv.setText("No posts yet. Be the first to post!");
                        tv.setGravity(Gravity.CENTER);
                        tv.setTextSize(18);
                        newsfeedContainer.addView(tv);
                    } else {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            // --- Dynamically Create UI Elements for each post ---
                            // This part is a repetition of the UI creation logic from loadPosts().
                            // For this direct-UI approach, we repeat the view creation.

                            CardView cardView = new CardView(this);
                            LinearLayout.LayoutParams cardLayoutParams = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                            );
                            cardLayoutParams.setMargins(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(12));
                            cardView.setLayoutParams(cardLayoutParams);
                            cardView.setRadius(dpToPx(8));
                            cardView.setCardElevation(dpToPx(4));
                            cardView.setContentPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));
                            cardView.setCardBackgroundColor(Color.WHITE);

                            LinearLayout postLayout = new LinearLayout(this);
                            postLayout.setOrientation(LinearLayout.HORIZONTAL);
                            postLayout.setLayoutParams(new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            postLayout.setGravity(Gravity.CENTER_VERTICAL);

                            String imageBase64 = doc.getString("imageBase64");
                            ImageView imageView = new ImageView(this);
                            LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(
                                    0, dpToPx(160));
                            imageParams.weight = 1.0f;
                            imageParams.setMargins(0,0,dpToPx(12),0);
                            imageView.setLayoutParams(imageParams);
                            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            imageView.setBackgroundColor(Color.LTGRAY);

                            if (imageBase64 != null && !imageBase64.isEmpty()) {
                                try {
                                    byte[] imageBytes = Base64.decode(imageBase64, Base64.DEFAULT);
                                    Glide.with(NewsfeedActivity.this)
                                            .asBitmap()
                                            .load(imageBytes)
                                            .placeholder(R.drawable.ic_launcher_background)
                                            .error(R.drawable.ic_launcher_foreground)
                                            .into(imageView);
                                } catch (IllegalArgumentException e) {
                                    Log.e(TAG, "Error decoding Base64 string for post: " + doc.getId(), e);
                                    imageView.setImageResource(R.drawable.ic_launcher_foreground);
                                }
                            } else {
                                imageView.setImageResource(R.drawable.ic_launcher_background);
                            }
                            postLayout.addView(imageView);

                            LinearLayout textInfoLayout = new LinearLayout(this);
                            textInfoLayout.setOrientation(LinearLayout.VERTICAL);
                            LinearLayout.LayoutParams textInfoParams = new LinearLayout.LayoutParams(
                                    0, LinearLayout.LayoutParams.WRAP_CONTENT);
                            textInfoParams.weight = 1.5f;
                            textInfoLayout.setLayoutParams(textInfoParams);

                            String name = doc.getString("name");
                            String price = doc.getString("price");
                            String type = doc.getString("type");
                            String category = doc.getString("category");
                            String rentDuration = doc.getString("rentDuration");

                            TextView tvName = new TextView(this);
                            tvName.setText((name != null ? name : "N/A"));
                            tvName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                            tvName.setTextColor(Color.BLACK);
                            tvName.setTypeface(null, Typeface.BOLD);
                            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                            nameParams.setMargins(0,0,0,dpToPx(4));
                            tvName.setLayoutParams(nameParams);
                            textInfoLayout.addView(tvName);

                            TextView tvCategory = new TextView(this);
                            tvCategory.setText("Category: " + (category != null ? category : "N/A"));
                            tvCategory.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                            tvCategory.setTextColor(Color.DKGRAY);
                            textInfoLayout.addView(tvCategory);

                            TextView tvType = new TextView(this);
                            tvType.setText("Type: " + (type != null ? type : "N/A"));
                            tvType.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                            tvType.setTextColor(Color.DKGRAY);
                            textInfoLayout.addView(tvType);

                            TextView tvPrice = new TextView(this);
                            tvPrice.setText("Price: " + (price != null ? price : "0") + " Tk");
                            tvPrice.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                            tvPrice.setTextColor(Color.rgb(0, 100, 0)); // Dark Green
                            tvPrice.setTypeface(null, Typeface.BOLD);
                            LinearLayout.LayoutParams priceParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                            priceParams.setMargins(0,dpToPx(4),0,0);
                            tvPrice.setLayoutParams(priceParams);
                            textInfoLayout.addView(tvPrice);

                            if (type != null && type.equalsIgnoreCase("Rent") && rentDuration != null && !rentDuration.isEmpty() && !rentDuration.equals("N/A")) {
                                TextView tvRentDuration = new TextView(this);
                                tvRentDuration.setText("Rent Duration: " + rentDuration);
                                tvRentDuration.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                                tvRentDuration.setTextColor(Color.DKGRAY);
                                textInfoLayout.addView(tvRentDuration);
                            }

                            Button actionButton = new Button(this);
                            final String postType = type;
                            // In this fallback, we don't know availability, so set a generic button
                            actionButton.setText("View Details"); // Or Buy/Rent if you prefer
                            actionButton.setBackgroundResource(R.drawable.button_background);
                            actionButton.setTextColor(Color.BLACK);
                            actionButton.setTypeface(null, Typeface.BOLD);

                            LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                                    dpToPx(100),
                                    dpToPx(40)
                            );
                            buttonParams.setMargins(0, dpToPx(8), 0, 0);
                            actionButton.setLayoutParams(buttonParams);
                            textInfoLayout.addView(actionButton);

                            actionButton.setOnClickListener(v -> {
                                Log.d(TAG, "Attempting to show seller info for post: " + doc.getId());
                                String sellerId = doc.getString("authorId");
                                if (sellerId != null && !sellerId.isEmpty()) {
                                    showSellerInfo(sellerId, doc.getId(), postType);
                                } else {
                                    Log.e(TAG, "Post with ID " + doc.getId() + " is missing an authorId.");
                                    Toast.makeText(NewsfeedActivity.this, "Seller information not available.", Toast.LENGTH_SHORT).show();
                                }
                            });

                            postLayout.addView(textInfoLayout);
                            cardView.addView(postLayout);
                            newsfeedContainer.addView(cardView);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading posts (fallback) from Firestore: " + e.getMessage(), e);
                    Toast.makeText(NewsfeedActivity.this, "Error loading posts.", Toast.LENGTH_LONG).show();
                });
    }

    // This method is called when an action button is clicked on a post.
    // It fetches seller details and displays them in an AlertDialog.
    private void showSellerInfo(String sellerId, String postId, String postType) {
        Log.d(TAG, "Fetching seller info for userId: " + sellerId);
        db.collection("users").document(sellerId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        String email = documentSnapshot.getString("email");
                        String phoneNumber = documentSnapshot.getString("phoneNumber");
                        String address = documentSnapshot.getString("university"); // Assuming "university" is used for address
                        final String sellerName = name;

                        // Display seller info in an AlertDialog
                        new AlertDialog.Builder(this)
                                .setTitle("Seller Information")
                                .setMessage("Seller Name: " + sellerName + "\n" +
                                        "Seller Phone: " + phoneNumber + "\n" +
                                        "Seller Email: " + email + "\n" +
                                        "Seller Address: " + address)
                                .setPositiveButton("OK", null) // Dismiss dialog
                                .setNegativeButton("Confirm Order", (dialog, which) -> {
                                    // If user confirms, proceed to create order
                                    createOrder(postId, sellerId, postType, sellerName);
                                })
                                .show();
                    } else {
                        Log.e(TAG, "Seller document not found for userId: " + sellerId);
                        Toast.makeText(NewsfeedActivity.this, "Seller not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching seller info for userId: " + sellerId, e);
                    Toast.makeText(NewsfeedActivity.this, "Failed to get seller info.", Toast.LENGTH_SHORT).show();
                });
    }

    // This method is called when the "Confirm Order" button is clicked in the AlertDialog.
    // It creates a new document in the "orders" collection in Firestore.
    private void createOrder(String postId, String sellerId, String orderType, String sellerName) {
        String buyerId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Prevent users from ordering their own products
        if(buyerId.equals(sellerId)) {
            Toast.makeText(this, "You cannot order your own product.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Fetch buyer's details to include in the order
        db.collection("users").document(buyerId).get().addOnSuccessListener(buyerDoc -> {
            if (buyerDoc.exists()) {
                String buyerName = buyerDoc.getString("name");

                // Fetch post details to include in the order
                db.collection("posts").document(postId).get().addOnSuccessListener(postDoc -> {
                    if (postDoc.exists()) {
                        String productName = postDoc.getString("name");

                        // Create the order data map
                        Map<String, Object> order = new HashMap<>();
                        order.put("productId", postId);
                        order.put("productName", productName);
                        order.put("buyerId", buyerId);
                        order.put("buyerName", buyerName);
                        // Removed contact number fields as per user request
                        order.put("sellerId", sellerId);
                        order.put("sellerName", sellerName);
                        order.put("orderType", orderType);
                        order.put("status", "pending"); // Initial status
                        // Use server timestamp for accurate ordering
                        order.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());

                        // Add the order to the "orders" collection
                        db.collection("orders")
                                .add(order)
                                .addOnSuccessListener(documentReference -> {
                                    Toast.makeText(NewsfeedActivity.this, "Order placed successfully!", Toast.LENGTH_SHORT).show();
                                    // Optionally, you might want to update the post's status or mark it as sold here,
                                    // but the current logic relies on checking "accepted" orders.
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(NewsfeedActivity.this, "Failed to place order.", Toast.LENGTH_SHORT).show();
                                    Log.e(TAG, "Error creating order", e);
                                });
                    } else {
                        Toast.makeText(NewsfeedActivity.this, "Failed to place order. Product not found.", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error creating order: post not found with id " + postId);
                    }
                }).addOnFailureListener(e -> {
                    Toast.makeText(NewsfeedActivity.this, "Failed to place order.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error getting post details for order", e);
                });
            } else {
                Toast.makeText(NewsfeedActivity.this, "Failed to place order. Buyer not found.", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error creating order: buyer not found with id " + buyerId);
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(NewsfeedActivity.this, "Failed to place order.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error getting buyer details for order", e);
        });
    }
}