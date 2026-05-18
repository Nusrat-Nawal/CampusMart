package com.campusmart.app.ui;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.campusmart.app.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WishboardActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private LinearLayout wishboardContainer;
    private MaterialButton buttonOpenNotifications, buttonAddNewWish;
    private static final String TAG = "WishboardActivity";

    // Updated array of colors for the item names
    private int[] itemValueColors = new int[] {
        Color.rgb(204, 153, 255), // light Purple
        Color.rgb(0, 100, 0), // dark Green
        Color.rgb(139, 0, 0), // dark maroon
        Color.rgb(210, 105, 30)  // chocolate colour
    };
    private int colorIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wishboard);

        Toolbar toolbar = findViewById(R.id.toolbarWishboard);
        setSupportActionBar(toolbar);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        wishboardContainer = findViewById(R.id.wishboardContainer);

        buttonOpenNotifications = findViewById(R.id.buttonOpenNotifications);
        buttonAddNewWish = findViewById(R.id.buttonAddNewWish);

        buttonOpenNotifications.setOnClickListener(v -> {
            startActivity(new Intent(WishboardActivity.this, NotificationsActivity.class));
        });

        buttonAddNewWish.setOnClickListener(v -> {
            startActivity(new Intent(WishboardActivity.this, PostWishActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        colorIndex = 0;
        loadWishes();
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
    }

    private void loadWishes() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in to see the wishboard.", Toast.LENGTH_SHORT).show();
            wishboardContainer.removeAllViews();
            TextView tv = new TextView(this);
            tv.setText("Please log in to see the wishboard.");
            tv.setGravity(Gravity.CENTER);
            wishboardContainer.addView(tv);
            return;
        }

        db.collection("wishes")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    wishboardContainer.removeAllViews();
                    if (queryDocumentSnapshots.isEmpty()) {
                        TextView tv = new TextView(this);
                        tv.setText("No wishes posted yet. Be the first!");
                        tv.setGravity(Gravity.CENTER);
                        wishboardContainer.addView(tv);
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
                            cardView.setContentPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
                            cardView.setCardBackgroundColor(Color.WHITE);

                            LinearLayout contentLayout = new LinearLayout(this);
                            contentLayout.setOrientation(LinearLayout.VERTICAL);
                            contentLayout.setLayoutParams(new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                            ));

                            TextView itemNameTv = new TextView(this);
                            String itemNamePrefix = "Item Name: ";
                            String actualItemName = doc.getString("itemName");
                            if (actualItemName == null) actualItemName = "N/A";

                            SpannableString spannableItemName = new SpannableString(itemNamePrefix + actualItemName);
                            spannableItemName.setSpan(new ForegroundColorSpan(Color.BLACK), 0, itemNamePrefix.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            int currentColor = itemValueColors[colorIndex % itemValueColors.length];
                            spannableItemName.setSpan(new ForegroundColorSpan(currentColor), itemNamePrefix.length(), spannableItemName.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            colorIndex++;

                            itemNameTv.setText(spannableItemName);
                            itemNameTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                            itemNameTv.setTypeface(null, Typeface.BOLD);
                            LinearLayout.LayoutParams itemNameParams = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                            itemNameParams.setMargins(0,0,0,dpToPx(4));
                            itemNameTv.setLayoutParams(itemNameParams);
                            contentLayout.addView(itemNameTv);

                            String itemDescription = doc.getString("itemDescription");
                            if (itemDescription != null && !itemDescription.isEmpty() && !"N/A".equalsIgnoreCase(itemDescription)) {
                                TextView itemDescTv = new TextView(this);
                                itemDescTv.setText("Description: " + itemDescription);
                                itemDescTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                                itemDescTv.setTextColor(Color.DKGRAY);
                                LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                                descParams.setMargins(0, 0, 0, dpToPx(8)); 
                                itemDescTv.setLayoutParams(descParams);
                                contentLayout.addView(itemDescTv);
                            }

                            TextView wishTypeTv = new TextView(this);
                            wishTypeTv.setText("Type: " + doc.getString("wishType"));
                            wishTypeTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                            wishTypeTv.setTextColor(Color.DKGRAY);
                            contentLayout.addView(wishTypeTv);

                            if ("Rent".equalsIgnoreCase(doc.getString("wishType"))) {
                                TextView rentDurationTv = new TextView(this);
                                rentDurationTv.setText("Rent Duration: " + doc.getString("rentDuration"));
                                rentDurationTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                                rentDurationTv.setTextColor(Color.DKGRAY);
                                contentLayout.addView(rentDurationTv);
                            }

                            TextView categoryTv = new TextView(this);
                            categoryTv.setText("Category: " + doc.getString("category"));
                            categoryTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                            categoryTv.setTextColor(Color.DKGRAY);
                            contentLayout.addView(categoryTv);
                            
                            LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                            detailParams.setMargins(0,0,0,dpToPx(2));
                            wishTypeTv.setLayoutParams(detailParams);
                            categoryTv.setLayoutParams(detailParams);

                            TextView userEmailTv = new TextView(this);
                            userEmailTv.setText("Posted by: " + doc.getString("userEmail"));
                            userEmailTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                            userEmailTv.setTextColor(Color.GRAY);
                            LinearLayout.LayoutParams userTimeParams = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                            userTimeParams.setMargins(0,dpToPx(8),0,dpToPx(2));
                            userEmailTv.setLayoutParams(userTimeParams);
                            contentLayout.addView(userEmailTv);
                            
                            com.google.firebase.Timestamp timestamp = doc.getTimestamp("timestamp");
                            if (timestamp != null) {
                                Date date = timestamp.toDate();
                                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
                                TextView timeTv = new TextView(this);
                                timeTv.setText("Posted on: " + sdf.format(date));
                                timeTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                                timeTv.setTextColor(Color.GRAY);
                                contentLayout.addView(timeTv);
                            }

                            cardView.addView(contentLayout);
                            wishboardContainer.addView(cardView);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(WishboardActivity.this, "Error loading wishes: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
