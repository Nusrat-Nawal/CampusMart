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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.campusmart.app.R;
import com.google.firebase.auth.FirebaseAuth; 
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

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
                Toast.makeText(this, "Profile coming soon", Toast.LENGTH_SHORT).show()
        );
    }
    @Override
    protected void onResume() {
        super.onResume();
        loadPosts(); }
    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        ); }
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
}