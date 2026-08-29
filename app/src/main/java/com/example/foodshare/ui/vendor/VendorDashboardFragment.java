package com.example.foodshare.ui.vendor;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.foodshare.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class VendorDashboardFragment extends Fragment {
    private TextView textVendorWelcome, textActiveListings, textTodayOrders, textTodayRevenue;
    private TextView textTotalRevenue, textTotalCustomers;
    private TextView textWeeklyRevenue, textCompletedOrders, textAverageOrder, textCancellationRate;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private ListenerRegistration ordersListener;

    private final Handler midnightHandler = new Handler(Looper.getMainLooper());

    private final Runnable midnightRefresh = new Runnable() {
        @Override
        public void run() {
            listenForOrders();
            scheduleMidnightRefresh();
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_vendor_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        textVendorWelcome = view.findViewById(R.id.textVendorWelcome);
        textActiveListings = view.findViewById(R.id.textActiveListings);
        textTodayOrders = view.findViewById(R.id.textTodayOrders);
        textTodayRevenue = view.findViewById(R.id.textTodayRevenue);
        textTotalRevenue = view.findViewById(R.id.textTotalRevenue);
        textTotalCustomers = view.findViewById(R.id.textTotalCustomers);
        textWeeklyRevenue = view.findViewById(R.id.textWeeklyRevenue);
        textCompletedOrders = view.findViewById(R.id.textCompletedOrders);
        textAverageOrder = view.findViewById(R.id.textAverageOrder);
        textCancellationRate = view.findViewById(R.id.textCancellationRate);

        Button buttonCreateListing = view.findViewById(R.id.buttonCreateListing);
        buttonCreateListing.setOnClickListener(v -> startActivity(new Intent(requireContext(), CreateListingActivity.class)));
    }

    @Override
    public void onStart() {
        super.onStart();

        if (firebaseAuth.getCurrentUser() == null) {
            ((VendorMainActivity) requireActivity()).logout();
            return;
        }

        loadWelcomeMessage();
        loadListings();
        listenForOrders();
        scheduleMidnightRefresh();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (firestore != null && firebaseAuth != null && firebaseAuth.getCurrentUser() != null) loadListings();
    }

    @Override
    public void onStop() {
        super.onStop();

        if (ordersListener != null) {
            ordersListener.remove();
            ordersListener = null;
        }

        midnightHandler.removeCallbacks(midnightRefresh);
    }

    private void loadWelcomeMessage() {
        String email = firebaseAuth.getCurrentUser().getEmail();
        if (email == null || email.isEmpty()) return;

        String displayName = email.split("@")[0];
        textVendorWelcome.setText(String.format(getString(R.string.welcome_message), displayName));
    }

    private void loadListings() {
        if (firebaseAuth.getCurrentUser() == null) return;

        String vendorId = firebaseAuth.getCurrentUser().getUid();

        firestore.collection("listings")
                .whereEqualTo("vendorId", vendorId)
                .get()
                .addOnSuccessListener(result -> {
                    if (!isAdded()) return;
                    textActiveListings.setText(String.valueOf(result.size()));
                })
                .addOnFailureListener(exception -> {
                    if (!isAdded()) return;

                    textActiveListings.setText("0");
                    Toast.makeText(requireContext(), R.string.failed_to_load_listings, Toast.LENGTH_SHORT).show();
                });
    }

    private void listenForOrders() {
        if (firebaseAuth.getCurrentUser() == null) return;

        if (ordersListener != null) {
            ordersListener.remove();
            ordersListener = null;
        }

        String vendorId = firebaseAuth.getCurrentUser().getUid();

        ordersListener = firestore.collection("orders")
                .whereEqualTo("vendorId", vendorId)
                .addSnapshotListener((snapshot, error) -> {
                    if (!isAdded()) return;

                    if (error != null) {
                        resetOrderStatistics();
                        Toast.makeText(requireContext(), "Failed to load dashboard orders.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int todayOrders = 0;
                    int completedOrders = 0;
                    int canceledOrders = 0;
                    int totalOrders = 0;

                    double todayRevenue = 0;
                    double totalRevenue = 0;
                    double weeklyRevenue = 0;
                    double completedRevenue = 0;

                    Set<String> customers = new HashSet<>();
                    Calendar sevenDaysAgo = getSevenDaysAgo();

                    if (snapshot != null) {
                        for (DocumentSnapshot order : snapshot.getDocuments()) {
                            totalOrders++;

                            String status = order.getString("status");
                            String payment = order.getString("payment");
                            String userId = order.getString("userId");
                            Timestamp timestamp = getOrderTimestamp(order);
                            Double total = getOrderTotal(order);

                            boolean canceled = status != null && status.equalsIgnoreCase("CANCELED");
                            boolean completed = status != null && status.equalsIgnoreCase("COMPLETED");
                            boolean paid = payment != null && payment.equalsIgnoreCase("Completed");

                            if (isToday(timestamp)) {
                                todayOrders++;
                                if (!canceled && paid) todayRevenue += total;
                            }

                            if (canceled) {
                                canceledOrders++;
                            } else {
                                if (userId != null && !userId.isEmpty()) customers.add(userId);
                                if (paid) totalRevenue += total;

                                if (paid && timestamp != null && !timestamp.toDate().before(sevenDaysAgo.getTime())) {
                                    weeklyRevenue += total;
                                }
                            }

                            if (completed) {
                                completedOrders++;
                                completedRevenue += total;
                            }
                        }
                    }

                    double averageOrder = completedOrders > 0 ? completedRevenue / completedOrders : 0;
                    double cancellationRate = totalOrders > 0 ? canceledOrders * 100.0 / totalOrders : 0;

                    updateDashboard(
                            todayOrders,
                            todayRevenue,
                            totalRevenue,
                            customers.size(),
                            weeklyRevenue,
                            completedOrders,
                            averageOrder,
                            cancellationRate
                    );
                });
    }

    private Double getOrderTotal(DocumentSnapshot order) {
        Double total = order.getDouble("totalNetPrice");
        if (total == null) total = order.getDouble("totalAmount");
        return total == null ? 0.0 : total;
    }

    private Timestamp getOrderTimestamp(DocumentSnapshot order) {
        Timestamp timestamp = order.getTimestamp("timestamp");
        if (timestamp == null) timestamp = order.getTimestamp("createdAt");
        return timestamp;
    }

    private boolean isToday(Timestamp timestamp) {
        if (timestamp == null) return false;

        Calendar orderDate = Calendar.getInstance();
        orderDate.setTime(timestamp.toDate());

        Calendar today = Calendar.getInstance();

        return orderDate.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                && orderDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR);
    }

    private Calendar getSevenDaysAgo() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -6);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    private void scheduleMidnightRefresh() {
        midnightHandler.removeCallbacks(midnightRefresh);

        Calendar midnight = Calendar.getInstance();
        midnight.add(Calendar.DAY_OF_YEAR, 1);
        midnight.set(Calendar.HOUR_OF_DAY, 0);
        midnight.set(Calendar.MINUTE, 0);
        midnight.set(Calendar.SECOND, 1);
        midnight.set(Calendar.MILLISECOND, 0);

        long delay = midnight.getTimeInMillis() - System.currentTimeMillis();
        midnightHandler.postDelayed(midnightRefresh, delay);
    }

    private void updateDashboard(int todayOrders, double todayRevenue, double totalRevenue, int totalCustomers,
                                 double weeklyRevenue, int completedOrders, double averageOrder, double cancellationRate) {
        textTodayOrders.setText(String.valueOf(todayOrders));
        textTodayRevenue.setText(String.format(Locale.getDefault(), "RM %.2f", todayRevenue));
        textTotalRevenue.setText(String.format(Locale.getDefault(), "RM %.2f", totalRevenue));
        textTotalCustomers.setText(String.valueOf(totalCustomers));
        textWeeklyRevenue.setText(String.format(Locale.getDefault(), "RM %.2f", weeklyRevenue));
        textCompletedOrders.setText(String.valueOf(completedOrders));
        textAverageOrder.setText(String.format(Locale.getDefault(), "RM %.2f", averageOrder));
        textCancellationRate.setText(String.format(Locale.getDefault(), "%.1f%%", cancellationRate));
    }

    private void resetOrderStatistics() {
        textTodayOrders.setText("0");
        textTodayRevenue.setText("RM 0.00");
        textTotalRevenue.setText("RM 0.00");
        textTotalCustomers.setText("0");
        textWeeklyRevenue.setText("RM 0.00");
        textCompletedOrders.setText("0");
        textAverageOrder.setText("RM 0.00");
        textCancellationRate.setText("0.0%");
    }
}