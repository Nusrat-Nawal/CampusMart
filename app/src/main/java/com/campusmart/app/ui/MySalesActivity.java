package com.campusmart.app.ui;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.campusmart.app.R;
import com.campusmart.app.adapters.SalesAdapter;
import com.campusmart.app.models.Order;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class MySalesActivity extends AppCompatActivity {

    private static final String TAG = "MySalesActivity";
    private FirebaseFirestore db;
    private RecyclerView salesRecyclerView;
    private SalesAdapter salesAdapter;
    private List<Order> orderList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_sales);

        db = FirebaseFirestore.getInstance();
        salesRecyclerView = findViewById(R.id.salesRecyclerView);
        salesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        orderList = new ArrayList<>();
        salesAdapter = new SalesAdapter(orderList, this::updateOrderStatus);
        salesRecyclerView.setAdapter(salesAdapter);

        loadSales();
    }

    private void loadSales() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("orders")
                .whereEqualTo("sellerId", currentUserId)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    orderList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Order order = doc.toObject(Order.class);
                        order.setOrderId(doc.getId());
                        orderList.add(order);
                    }
                    salesAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading sales", e);
                    Toast.makeText(MySalesActivity.this, "Error loading sales.", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateOrderStatus(String orderId, String newStatus) {
        db.collection("orders").document(orderId)
                .update("status", newStatus)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(MySalesActivity.this, "Order " + newStatus, Toast.LENGTH_SHORT).show();
                    loadSales(); // Refresh the list
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MySalesActivity.this, "Failed to update order status.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error updating order status", e);
                });
    }
}
