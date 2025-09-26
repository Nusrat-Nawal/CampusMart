package com.campusmart.app.ui;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class NewsfeedActivity extends AppCompatActivity {

    FirebaseFirestore db;
    LinearLayout newsfeedContainer;
    private static final String TAG = "NewsfeedActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_newsfeed);

        db = FirebaseFirestore.getInstance();
        newsfeedContainer = findViewById(R.id.newsfeedContainer);


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
        loadPosts();
    }
    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
    }

    private void loadPosts() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Log.e(TAG, "User is NOT authenticated. Cannot load posts.");
            Toast.makeText(this, "Error: Not authenticated. Please log in again to see posts.", Toast.LENGTH_LONG).show();
            newsfeedContainer.removeAllViews();
            TextView tv = new TextView(this);
            tv.setText("Please log in to see the newsfeed.");
            tv.setGravity(Gravity.CENTER);
            tv.setTextSize(18);
            newsfeedContainer.addView(tv);
            return;
        } else {
            Log.d(TAG, "User is authenticated: " + FirebaseAuth.getInstance().getCurrentUser().getUid());
        }

        db.collection("orders")
                .whereEqualTo("status", "accepted")
                .get()
                .addOnSuccessListener(orderSnapshots -> {
                    Set<String> soldProductIds = new HashSet<>();
                    for (DocumentSnapshot orderDoc : orderSnapshots) {
                        soldProductIds.add(orderDoc.getString("productId"));
                    }

                    db.collection("posts")
                            .orderBy("timestamp", Query.Direction.DESCENDING)
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                newsfeedContainer.removeAllViews();
                                if (queryDocumentSnapshots.isEmpty()) {
                                    TextView tv = new TextView(this);
                                    tv.setText("No posts yet. Be the first to post!");
                                    tv.setGravity(Gravity.CENTER);
                                    tv.setTextSize(18);
                                    newsfeedContainer.addView(tv);
                                } else {
                                    for (DocumentSnapshot doc : queryDocumentSnapshots) {

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
                                                0,
                                                dpToPx(160) //
                                        );
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
                                                0,
                                                LinearLayout.LayoutParams.WRAP_CONTENT
                                        );
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
                                        final String postId = doc.getId();

                                        if (soldProductIds.contains(postId)) {
                                            actionButton.setText("Not Available");
                                            actionButton.setEnabled(false);
                                            actionButton.setBackgroundColor(Color.GRAY);
                                        } else {
                                            if ("Rent".equalsIgnoreCase(postType)) {
                                                actionButton.setText("Rent");
                                            } else {
                                                actionButton.setText("Buy");
                                            }
                                            actionButton.setBackgroundResource(R.drawable.button_background);
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

                                        postLayout.addView(textInfoLayout);
                                        cardView.addView(postLayout);
                                        newsfeedContainer.addView(cardView);
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
                    // Fallback to loading posts without checking availability
                    loadPostsWithoutAvailabilityCheck();
                });
    }

    private void loadPostsWithoutAvailabilityCheck() {
        db.collection("posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    newsfeedContainer.removeAllViews();
                    if (queryDocumentSnapshots.isEmpty()) {
                        TextView tv = new TextView(this);
                        tv.setText("No posts yet. Be the first to post!");
                        tv.setGravity(Gravity.CENTER);
                        tv.setTextSize(18);
                        newsfeedContainer.addView(tv);
                    } else {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {

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
                                    0,
                                    dpToPx(160) //
                            );
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
                                    0,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                            );
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
                            if ("Rent".equalsIgnoreCase(postType)) {
                                actionButton.setText("Rent");
                            } else {
                                actionButton.setText("Buy");
                            }
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
                    Log.e(TAG, "Error loading posts from Firestore: " + e.getMessage(), e);
                    Toast.makeText(NewsfeedActivity.this, "Error loading posts: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void showSellerInfo(String sellerId, String postId, String postType) {
        Log.d(TAG, "Fetching seller info for userId: " + sellerId);
        db.collection("users").document(sellerId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        String email = documentSnapshot.getString("email");
                        String phoneNumber = documentSnapshot.getString("phoneNumber");
                        String address = documentSnapshot.getString("university");
                        final String sellerName = name;

                        new AlertDialog.Builder(this)
                                .setTitle("Seller Information")
                                .setMessage("Name: " + name + "\n" +
                                        "Email: " + email + "\n" +
                                        "Phone: " + phoneNumber + "\n" +
                                        "Address: " + address)
                                .setPositiveButton("OK", null)
                                .setNegativeButton("Confirm Order", (dialog, which) -> {
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

    private void createOrder(String postId, String sellerId, String orderType, String sellerName) {
        String buyerId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        if(buyerId.equals(sellerId)) {
            Toast.makeText(this, "You cannot order your own product.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(buyerId).get().addOnSuccessListener(buyerDoc -> {
            if (buyerDoc.exists()) {
                String buyerName = buyerDoc.getString("name");

                db.collection("posts").document(postId).get().addOnSuccessListener(postDoc -> {
                    if (postDoc.exists()) {
                        String productName = postDoc.getString("name");

                        Map<String, Object> order = new HashMap<>();
                        order.put("productId", postId);
                        order.put("productName", productName);
                        order.put("buyerId", buyerId);
                        order.put("buyerName", buyerName);
                        order.put("sellerId", sellerId);
                        order.put("sellerName", sellerName);
                        order.put("orderType", orderType);
                        order.put("status", "pending");
                        order.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());

                        db.collection("orders")
                                .add(order)
                                .addOnSuccessListener(documentReference -> {
                                    Toast.makeText(NewsfeedActivity.this, "Order placed successfully!", Toast.LENGTH_SHORT).show();
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
