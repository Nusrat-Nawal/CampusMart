package com.campusmart.app.ui;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.campusmart.app.R;
import com.campusmart.app.adapters.OrdersAdapter;
import com.campusmart.app.models.Order;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class MyOrdersActivity extends AppCompatActivity {

    private static final String TAG = "MyOrdersActivity";
    private FirebaseFirestore db;
    private RecyclerView ordersRecyclerView;
    private OrdersAdapter ordersAdapter;
    private List<Order> orderList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_orders);

        db = FirebaseFirestore.getInstance();
        ordersRecyclerView = findViewById(R.id.ordersRecyclerView);
        ordersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        orderList = new ArrayList<>();
        ordersAdapter = new OrdersAdapter(orderList);
        ordersRecyclerView.setAdapter(ordersAdapter);

        loadOrders();
    }

    private void loadOrders() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("orders")
                .whereEqualTo("buyerId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    orderList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Order order = doc.toObject(Order.class);
                        orderList.add(order);
                    }
                    ordersAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading orders", e);
                    Toast.makeText(MyOrdersActivity.this, "Error loading orders.", Toast.LENGTH_SHORT).show();
                });
    }
}
