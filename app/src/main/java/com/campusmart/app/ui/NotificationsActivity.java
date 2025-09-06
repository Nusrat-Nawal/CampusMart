package com.campusmart.app.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.campusmart.app.R;
import com.campusmart.app.adapter.NotificationAdapter;
import com.campusmart.app.model.Notification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity implements NotificationAdapter.OnNotificationClickListener {

    private static final String TAG = "NotificationsActivity";

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private RecyclerView recyclerViewNotifications;
    private NotificationAdapter notificationAdapter;
    private List<Notification> notificationList;
    private TextView textViewNoNotifications;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        Toolbar toolbar = findViewById(R.id.toolbarNotifications);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Notifications");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Enable back arrow
        }

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        recyclerViewNotifications = findViewById(R.id.recyclerViewNotifications);
        textViewNoNotifications = findViewById(R.id.textViewNoNotifications);

        notificationList = new ArrayList<>();
        notificationAdapter = new NotificationAdapter(this, notificationList, this);
        recyclerViewNotifications.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewNotifications.setAdapter(notificationAdapter);

        loadNotifications();
    }

    private void loadNotifications() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to view notifications.", Toast.LENGTH_SHORT).show();
            textViewNoNotifications.setVisibility(View.VISIBLE);
            recyclerViewNotifications.setVisibility(View.GONE);
            return;
        }

        db.collection("notifications")
                .whereEqualTo("notifiedUserId", currentUser.getUid())
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    notificationList.clear();
                    if (queryDocumentSnapshots.isEmpty()) {
                        textViewNoNotifications.setVisibility(View.VISIBLE);
                        recyclerViewNotifications.setVisibility(View.GONE);
                    } else {
                        textViewNoNotifications.setVisibility(View.GONE);
                        recyclerViewNotifications.setVisibility(View.VISIBLE);
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            Notification notification = doc.toObject(Notification.class);
                            if (notification != null) {
                                notification.setId(doc.getId()); // Set Firestore document ID
                                notificationList.add(notification);
                            }
                        }
                        notificationAdapter.notifyDataSetChanged(); // Use notifyDataSetChanged after bulk update
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading notifications", e);
                    Toast.makeText(NotificationsActivity.this, "Error loading notifications: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    textViewNoNotifications.setVisibility(View.VISIBLE);
                    textViewNoNotifications.setText("Failed to load notifications.");
                    recyclerViewNotifications.setVisibility(View.GONE);
                });
    }

    @Override
    public void onNotificationClick(Notification notification) {
        // Mark notification as read in Firestore if it's not already read
        if (!notification.isRead()) {
            db.collection("notifications").document(notification.getId())
                    .update("isRead", true)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Notification marked as read: " + notification.getId());
                        notification.setRead(true); // Update local model
                        // Find the item in the list and notify adapter for visual update if necessary (though adapter handles it optimistically)
                        int itemPosition = notificationList.indexOf(notification);
                        if (itemPosition != -1) {
                            notificationAdapter.notifyItemChanged(itemPosition);
                        }
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Error marking notification as read", e));
        }
        // You could also navigate to the specific post or wish here if you want
        // For now, just a Toast
        // Toast.makeText(this, "Notification clicked: " + notification.getMessage(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed(); // Handle back arrow click
        return true;
    }

    // Consider adding a swipe-to-refresh or a "Mark all as read" button in the future
}
