package com.example.foodshare.ui.orders;

import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodshare.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ConsumerOrdersFragment extends Fragment {

    private RecyclerView rvOrderHistory;
    private OrderHistoryAdapter adapter;
    private List<DocumentSnapshot> orderList;
    private TabLayout tabsOrders;
    private TextView tvEmptyState;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.activity_order_history, container, false);
        View oldBottomNav = root.findViewById(R.id.bottomNav);
        if (oldBottomNav != null) oldBottomNav.setVisibility(View.GONE);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        tabsOrders = view.findViewById(R.id.tabsOrders);
        tabsOrders.addTab(tabsOrders.newTab().setCustomView(createTabView("Upcoming")));
        tabsOrders.addTab(tabsOrders.newTab().setCustomView(createTabView("To Pickup")));
        tabsOrders.addTab(tabsOrders.newTab().setCustomView(createTabView("Completed")));
        tabsOrders.addTab(tabsOrders.newTab().setCustomView(createTabView("Canceled")));
        updateTabAppearance(0);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        rvOrderHistory = view.findViewById(R.id.rvOrderHistory);
        rvOrderHistory.setLayoutManager(new LinearLayoutManager(requireContext()));

        orderList = new ArrayList<>();
        adapter = new OrderHistoryAdapter(orderList);
        rvOrderHistory.setAdapter(adapter);

        tabsOrders.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                updateTabAppearance(tab.getPosition());
                selectTab(tab.getPosition());
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) { }
            @Override public void onTabReselected(TabLayout.Tab tab) {
                updateTabAppearance(tab.getPosition());
                selectTab(tab.getPosition());
            }
        });
        selectTab("UPCOMING");
    }

    private void selectTab(String category) {
        if ("UPCOMING".equals(category)) {
            fetchOrders(Arrays.asList("PENDING", "CONFIRMED", "Upcoming"), "No upcoming orders");
        } else if ("READY".equals(category)) {
            fetchOrders(Arrays.asList("READY", "Ready to Pick Up", "READY_FOR_PICKUP"),
                    "No orders ready for pickup");
        } else if ("COMPLETED".equals(category)) {
            fetchOrders(Collections.singletonList("COMPLETED"), "No completed orders");
        } else {
            fetchOrders(Collections.singletonList("CANCELED"), "No canceled orders");
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

    private TextView createTabView(String title) {
        TextView tab = new TextView(requireContext());
        tab.setText(title);
        tab.setGravity(android.view.Gravity.CENTER);
        tab.setTextSize(13f);
        tab.setPadding(dpToPx(4), dpToPx(10), dpToPx(4), dpToPx(10));
        return tab;
    }

    private void updateTabAppearance(int selectedPosition) {
        for (int i = 0; i < tabsOrders.getTabCount(); i++) {
            TabLayout.Tab tab = tabsOrders.getTabAt(i);
            if (tab == null || tab.getCustomView() == null) continue;
            TextView view = (TextView) tab.getCustomView();
            boolean selected = i == selectedPosition;
            view.setTextColor(ContextCompat.getColor(requireContext(),
                    selected ? R.color.foodshare_dark_green : R.color.foodshare_text_secondary));
            view.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
            GradientDrawable background = new GradientDrawable();
            background.setColor(ContextCompat.getColor(requireContext(),
                    selected ? R.color.consumer_lime : R.color.foodshare_surface));
            background.setCornerRadius(dpToPx(8));
            view.setBackground(background);
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void fetchOrders(List<String> statuses, String emptyMessage) {
        String userId = mAuth.getCurrentUser() == null
                ? "GUEST_USER" : mAuth.getCurrentUser().getUid();
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
                        if (t1 != null && t2 != null) return t1.compareTo(t2);
                        return 0;
                    });
                    orderList.addAll(fetchedDocs);
                    adapter.notifyDataSetChanged();
                    if (tvEmptyState != null) {
                        boolean empty = orderList.isEmpty();
                        tvEmptyState.setText(emptyMessage);
                        tvEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
                        rvOrderHistory.setVisibility(empty ? View.GONE : View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) Toast.makeText(requireContext(),
                            "Error loading orders: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
