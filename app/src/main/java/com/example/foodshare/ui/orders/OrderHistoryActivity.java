package com.example.foodshare.ui.orders;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodshare.R;
import com.example.foodshare.ui.consumer.ConsumerMainActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;
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

    private TabLayout tabsOrders;
    private TextView tvEmptyState;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_history);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        tabsOrders = findViewById(R.id.tabsOrders);
        tabsOrders.addTab(tabsOrders.newTab().setText("Upcoming"));
        tabsOrders.addTab(tabsOrders.newTab().setText("To Pickup"));
        tabsOrders.addTab(tabsOrders.newTab().setText("Completed"));
        tabsOrders.addTab(tabsOrders.newTab().setText("Canceled"));
        bottomNav = findViewById(R.id.bottomNav);

        rvOrderHistory = findViewById(R.id.rvOrderHistory);
        rvOrderHistory.setLayoutManager(new LinearLayoutManager(this));

        orderList = new ArrayList<>();
        adapter = new OrderHistoryAdapter(orderList);
        rvOrderHistory.setAdapter(adapter);

        // Dynamically find or handle empty state text view if you added it to your layout,
        // or safely check to prevent crashes if it hasn't been added to XML yet.
        tvEmptyState = findViewById(R.id.tvEmptyState);

        tabsOrders.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) { selectTab(tab.getPosition()); }
            @Override public void onTabUnselected(TabLayout.Tab tab) { }
            @Override public void onTabReselected(TabLayout.Tab tab) { selectTab(tab.getPosition()); }
        });

        // Setup Bottom Navigation mapping
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_orders);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    Intent intent = new Intent(this, ConsumerMainActivity.class);
                    intent.putExtra(ConsumerMainActivity.EXTRA_SELECTED_TAB, R.id.nav_home);
                    startActivity(intent);
                    finish();
                    return true;
                } else if (id == R.id.nav_orders) {
                    return true;
                } else if (id == R.id.nav_cart) {
                    Intent intent = new Intent(this, ConsumerMainActivity.class);
                    intent.putExtra(ConsumerMainActivity.EXTRA_SELECTED_TAB, R.id.nav_cart);
                    startActivity(intent);
                    finish();
                    return true;
                } else if (id == R.id.nav_profile) {
                    Intent intent = new Intent(this, ConsumerMainActivity.class);
                    intent.putExtra(ConsumerMainActivity.EXTRA_SELECTED_TAB, R.id.nav_profile);
                    startActivity(intent);
                    finish();
                    return true;
                }
                return false;
            });
        }

        selectTab("UPCOMING");
    }

    private void selectTab(String category) {
        if ("UPCOMING".equals(category)) {
            fetchOrders(Arrays.asList("PENDING", "CONFIRMED", "Upcoming"), "No upcoming orders");
        } else if ("READY".equals(category)) {
            fetchOrders(Arrays.asList("READY", "Ready to Pick Up", "READY_FOR_PICKUP"), "No orders ready for pickup");
        } else if ("COMPLETED".equals(category)) {
            fetchOrders(Arrays.asList("COMPLETED"), "No completed orders");
        } else if ("CANCELED".equals(category)) {
            fetchOrders(Arrays.asList("CANCELED"), "No canceled orders");
        }
    }

    private void selectTab(int position) {
        if (position == 1) {
            selectTab("READY");
        } else if (position == 2) {
            selectTab("COMPLETED");
        } else if (position == 3) {
            selectTab("CANCELED");
        } else {
            selectTab("UPCOMING");
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
        if (tabsOrders != null && tabsOrders.getSelectedTabPosition() >= 0) {
            selectTab(tabsOrders.getSelectedTabPosition());
        } else {
            selectTab("UPCOMING");
        }
    }
}
