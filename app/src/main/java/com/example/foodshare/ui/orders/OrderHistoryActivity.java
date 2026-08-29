package com.example.foodshare.ui.orders;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodshare.R;
import com.example.foodshare.ui.consumer.CartActivity;
import com.example.foodshare.ui.consumer.ConsumerHomeActivity;
import com.example.foodshare.ui.profile.ProfileActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class OrderHistoryActivity extends AppCompatActivity {

    private RecyclerView rvOrderHistory;
    private OrderHistoryAdapter adapter;
    private List<DocumentSnapshot> orderList;

    private TextView tabUpcoming, tabReady, tabCompleted, tabCanceled, tvEmptyState;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_history);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        tabUpcoming = findViewById(R.id.tabUpcoming);
        tabReady = findViewById(R.id.tabReady);
        tabCompleted = findViewById(R.id.tabCompleted);
        tabCanceled = findViewById(R.id.tabCanceled);
        bottomNav = findViewById(R.id.bottomNav);

        rvOrderHistory = findViewById(R.id.rvOrderHistory);
        rvOrderHistory.setLayoutManager(new LinearLayoutManager(this));

        orderList = new ArrayList<>();
        adapter = new OrderHistoryAdapter(orderList);
        rvOrderHistory.setAdapter(adapter);

        // Dynamically find or handle empty state text view if you added it to your layout,
        // or safely check to prevent crashes if it hasn't been added to XML yet.
        tvEmptyState = findViewById(R.id.tvEmptyState);

        tabUpcoming.setOnClickListener(v -> selectTab("UPCOMING"));
        tabReady.setOnClickListener(v -> selectTab("READY"));
        tabCompleted.setOnClickListener(v -> selectTab("COMPLETED"));
        tabCanceled.setOnClickListener(v -> selectTab("CANCELED"));

        // Setup Bottom Navigation mapping
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_orders);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    startActivity(new Intent(this, ConsumerHomeActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_orders) {
                    return true;
                } else if (id == R.id.nav_cart) {
                    startActivity(new Intent(this, CartActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_profile) {
                    startActivity(new Intent(this, ProfileActivity.class));
                    finish();
                    return true;
                }
                return false;
            });
        }

        selectTab("UPCOMING");
    }

    private void selectTab(String category) {
        resetTabStyle(tabUpcoming);
        resetTabStyle(tabReady);
        resetTabStyle(tabCompleted);
        resetTabStyle(tabCanceled);

        if ("UPCOMING".equals(category)) {
            setActiveTabStyle(tabUpcoming);
            fetchOrders(Arrays.asList("PENDING", "CONFIRMED", "Upcoming"), "No upcoming orders");
        } else if ("READY".equals(category)) {
            setActiveTabStyle(tabReady);
            fetchOrders(Arrays.asList("READY", "Ready to Pick Up", "READY_FOR_PICKUP"), "No orders ready for pickup");
        } else if ("COMPLETED".equals(category)) {
            setActiveTabStyle(tabCompleted);
            fetchOrders(Arrays.asList("COMPLETED"), "No completed orders");
        } else if ("CANCELED".equals(category)) {
            setActiveTabStyle(tabCanceled);
            fetchOrders(Arrays.asList("CANCELED"), "No canceled orders");
        }
    }

    private void resetTabStyle(TextView tab) {
        if (tab != null) {
            tab.setTextColor(Color.parseColor("#888888"));
            tab.setTypeface(null, Typeface.NORMAL);
        }
    }

    private void setActiveTabStyle(TextView tab) {
        if (tab != null) {
            tab.setTextColor(Color.parseColor("#5A774A")); // Matching your app's green theme tint
            tab.setTypeface(null, Typeface.BOLD);
        }
    }

    private void fetchOrders(List<String> statuses, String emptyMessage) {
        String userId = (mAuth.getCurrentUser() != null) ? mAuth.getCurrentUser().getUid() : "GUEST_USER";

        db.collection("orders")
                .whereEqualTo("userId", userId)
                .whereIn("status", statuses)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    orderList.clear();
                    List<DocumentSnapshot> fetchedDocs = queryDocumentSnapshots.getDocuments();

                    Collections.sort(fetchedDocs, (doc1, doc2) -> {
                        Timestamp t1 = doc1.getTimestamp("timestamp");
                        if (t1 == null) t1 = doc1.getTimestamp("createdAt");

                        Timestamp t2 = doc2.getTimestamp("timestamp");
                        if (t2 == null) t2 = doc2.getTimestamp("createdAt");

                        if (t1 != null && t2 != null) {
                            return t1.compareTo(t2);
                        }
                        return 0;
                    });

                    orderList.addAll(fetchedDocs);
                    adapter.notifyDataSetChanged();

                    // Handle visibility of empty state message if the TextView exists in your layout
                    if (tvEmptyState != null) {
                        if (orderList.isEmpty()) {
                            tvEmptyState.setText(emptyMessage);
                            tvEmptyState.setVisibility(View.VISIBLE);
                            rvOrderHistory.setVisibility(View.GONE);
                        } else {
                            tvEmptyState.setVisibility(View.GONE);
                            rvOrderHistory.setVisibility(View.VISIBLE);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading orders: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tabReady != null && tabReady.getTypeface() != null && tabReady.getTypeface().isBold()) {
            selectTab("READY");
        } else if (tabCompleted != null && tabCompleted.getTypeface() != null && tabCompleted.getTypeface().isBold()) {
            selectTab("COMPLETED");
        } else if (tabCanceled != null && tabCanceled.getTypeface() != null && tabCanceled.getTypeface().isBold()) {
            selectTab("CANCELED");
        } else {
            selectTab("UPCOMING");
        }
    }
}