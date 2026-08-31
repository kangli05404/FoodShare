package com.example.foodshare.ui.vendor;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodshare.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VendorOrdersFragment extends Fragment {
    private RecyclerView recyclerOrders;
    private TextView tabUpcoming;
    private TextView tabReady;
    private TextView tabCompleted;
    private TextView tabCanceled;
    private TextView textEmptyOrders;
    private VendorOrderAdapter adapter;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private ListenerRegistration ordersListener;

    private final List<DocumentSnapshot> allOrders = new ArrayList<>();
    private final List<DocumentSnapshot> displayedOrders = new ArrayList<>();
    private String currentTab = "UPCOMING";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_vendor_orders, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        recyclerOrders = view.findViewById(R.id.recyclerOrders);
        tabUpcoming = view.findViewById(R.id.tabUpcoming);
        tabReady = view.findViewById(R.id.tabReady);
        tabCompleted = view.findViewById(R.id.tabCompleted);
        tabCanceled = view.findViewById(R.id.tabCanceled);
        textEmptyOrders = view.findViewById(R.id.textEmptyOrders);

        recyclerOrders.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new VendorOrderAdapter(displayedOrders, this::handleOrderAction);
        recyclerOrders.setAdapter(adapter);

        tabUpcoming.setOnClickListener(v -> selectTab("UPCOMING"));
        tabReady.setOnClickListener(v -> selectTab("READY"));
        tabCompleted.setOnClickListener(v -> selectTab("COMPLETED"));
        tabCanceled.setOnClickListener(v -> selectTab("CANCELED"));

        selectTab("UPCOMING");
    }

    @Override
    public void onStart() {
        super.onStart();
        listenForOrders();
    }

    @Override
    public void onStop() {
        super.onStop();

        if (ordersListener != null) {
            ordersListener.remove();
            ordersListener = null;
        }
    }

    private void listenForOrders() {
        if (firebaseAuth.getCurrentUser() == null) {
            textEmptyOrders.setText("Please log in to view orders.");
            textEmptyOrders.setVisibility(View.VISIBLE);
            return;
        }

        if (ordersListener != null) {
            ordersListener.remove();
        }

        String vendorId = firebaseAuth.getCurrentUser().getUid();

        ordersListener = firestore.collection("orders")
                .whereEqualTo("vendorId", vendorId)
                .addSnapshotListener((snapshot, error) -> {
                    if (!isAdded()) return;

                    if (error != null) {
                        if (requireActivity() instanceof VendorMainActivity) {
                            ((VendorMainActivity) requireActivity()).updateOrdersBadge(0);
                        }
                        Toast.makeText(requireContext(), "Failed to load orders: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    allOrders.clear();

                    if (snapshot != null) {
                        allOrders.addAll(snapshot.getDocuments());
                    }

                    int upcomingOrderCount = 0;
                    for (DocumentSnapshot order : allOrders) {
                        if (isUpcomingStatus(order.getString("status"))) {
                            upcomingOrderCount++;
                        }
                    }

                    if (requireActivity() instanceof VendorMainActivity) {
                        ((VendorMainActivity) requireActivity()).updateOrdersBadge(upcomingOrderCount);
                    }

                    Collections.sort(allOrders, (order1, order2) -> {
                        Timestamp time1 = getOrderTimestamp(order1);
                        Timestamp time2 = getOrderTimestamp(order2);

                        if (time1 == null && time2 == null) return 0;
                        if (time1 == null) return 1;
                        if (time2 == null) return -1;

                        return time2.compareTo(time1);
                    });

                    showOrdersForCurrentTab();
                });
    }

    private Timestamp getOrderTimestamp(DocumentSnapshot document) {
        Timestamp timestamp = document.getTimestamp("timestamp");

        if (timestamp == null) {
            timestamp = document.getTimestamp("createdAt");
        }

        return timestamp;
    }

    private void selectTab(String tab) {
        currentTab = tab;

        resetTabStyle(tabUpcoming);
        resetTabStyle(tabReady);
        resetTabStyle(tabCompleted);
        resetTabStyle(tabCanceled);

        if ("UPCOMING".equals(tab)) {
            setActiveTabStyle(tabUpcoming);
        } else if ("READY".equals(tab)) {
            setActiveTabStyle(tabReady);
        } else if ("COMPLETED".equals(tab)) {
            setActiveTabStyle(tabCompleted);
        } else if ("CANCELED".equals(tab)) {
            setActiveTabStyle(tabCanceled);
        }

        showOrdersForCurrentTab();
    }

    private void resetTabStyle(TextView tab) {
        tab.setBackgroundResource(R.drawable.bg_order_tab_inactive);
        tab.setTextColor(tab.getContext().getColor(R.color.foodshare_text_secondary));
        tab.setTypeface(null, Typeface.NORMAL);
    }

    private void setActiveTabStyle(TextView tab) {
        tab.setBackgroundResource(R.drawable.bg_order_tab_active);
        tab.setTextColor(tab.getContext().getColor(R.color.foodshare_dark_green));
        tab.setTypeface(null, Typeface.BOLD);
    }

    private void showOrdersForCurrentTab() {
        if (adapter == null) return;

        displayedOrders.clear();

        for (DocumentSnapshot order : allOrders) {
            String status = order.getString("status");

            if (matchesCurrentTab(status)) {
                displayedOrders.add(order);
            }
        }

        adapter.notifyDataSetChanged();

        if (displayedOrders.isEmpty()) {
            textEmptyOrders.setText(getEmptyMessage());
            textEmptyOrders.setVisibility(View.VISIBLE);
            recyclerOrders.setVisibility(View.GONE);
        } else {
            textEmptyOrders.setVisibility(View.GONE);
            recyclerOrders.setVisibility(View.VISIBLE);
        }
    }

    private boolean matchesCurrentTab(String status) {
        if ("UPCOMING".equals(currentTab)) {
            return isUpcomingStatus(status);
        }

        if ("READY".equals(currentTab)) {
            return isReadyStatus(status);
        }

        if ("COMPLETED".equals(currentTab)) {
            return status != null && status.equalsIgnoreCase("COMPLETED");
        }

        if ("CANCELED".equals(currentTab)) {
            return status != null && status.equalsIgnoreCase("CANCELED");
        }

        return false;
    }

    private boolean isUpcomingStatus(String status) {
        if (status == null) return false;

        return status.equalsIgnoreCase("Upcoming")
                || status.equalsIgnoreCase("PENDING")
                || status.equalsIgnoreCase("CONFIRMED");
    }

    private boolean isReadyStatus(String status) {
        if (status == null) return false;

        return status.equalsIgnoreCase("READY")
                || status.equalsIgnoreCase("Ready to Pick Up")
                || status.equalsIgnoreCase("READY_FOR_PICKUP");
    }

    private String getEmptyMessage() {
        if ("UPCOMING".equals(currentTab)) {
            return "No upcoming orders.";
        }

        if ("READY".equals(currentTab)) {
            return "No orders ready for pickup.";
        }

        if ("COMPLETED".equals(currentTab)) {
            return "No completed orders.";
        }

        return "No cancelled orders.";
    }

    private void handleOrderAction(DocumentSnapshot order) {
        String status = order.getString("status");

        if (isUpcomingStatus(status)) {
            markOrderReady(order.getId());
        } else if (isReadyStatus(status)) {
            showCompleteOrderDialog(order);
        }
    }

    private void markOrderReady(String orderId) {
        updateOrderStatus(orderId, "READY");
    }

    private void showCompleteOrderDialog(DocumentSnapshot order) {
        String itemName = order.getString("itemName");

        if (itemName == null || itemName.isEmpty()) {
            itemName = "this order";
        }

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_complete_order, null);
        TextView message = dialogView.findViewById(R.id.completeOrderMessage);
        message.setText("Confirm that the customer has collected " + itemName + ".");

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Complete", (buttonDialog, which) -> updateOrderStatus(order.getId(), "COMPLETED"))
                .create();

        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(requireContext().getColor(R.color.foodshare_green));
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor(requireContext().getColor(R.color.foodshare_text_secondary));
        });
        dialog.show();
    }

    private void updateOrderStatus(String orderId, String newStatus) {
        DocumentReference orderRef = firestore.collection("orders").document(orderId);

        firestore.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(orderRef);

            if (!snapshot.exists()) {
                throw new FirebaseFirestoreException("Order not found.", FirebaseFirestoreException.Code.NOT_FOUND);
            }

            String currentStatus = snapshot.getString("status");

            if ("READY".equals(newStatus) && !isUpcomingStatus(currentStatus)) {
                throw new FirebaseFirestoreException("This order is no longer upcoming.", FirebaseFirestoreException.Code.ABORTED);
            }

            if ("COMPLETED".equals(newStatus) && !isReadyStatus(currentStatus)) {
                throw new FirebaseFirestoreException("This order is no longer ready for pickup.", FirebaseFirestoreException.Code.ABORTED);
            }

            transaction.update(orderRef, "status", newStatus);
            return null;
        }).addOnSuccessListener(unused -> {
            if (!isAdded()) return;

            if ("READY".equals(newStatus)) {
                showStatusMessage("Order marked as ready for pickup.");
            } else {
                showStatusMessage("Order completed.");
            }
        }).addOnFailureListener(exception -> {
            if (!isAdded()) return;
            Toast.makeText(requireContext(), "Unable to update order: " + exception.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    private void showStatusMessage(String message) {
        if (!isAdded() || recyclerOrders == null) return;

        Snackbar snackbar = Snackbar.make(recyclerOrders, message, Snackbar.LENGTH_SHORT);
        View anchor = requireActivity().findViewById(R.id.vendorBottomNavigation);
        if (anchor != null) snackbar.setAnchorView(anchor);

        View snackbarView = snackbar.getView();
        snackbarView.setBackground(ContextCompat.getDrawable(requireContext(),
                R.drawable.bg_cart_snackbar));
        snackbarView.setElevation(8f);

        TextView text = snackbarView.findViewById(
                com.google.android.material.R.id.snackbar_text);
        if (text != null) {
            text.setTextColor(ContextCompat.getColor(requireContext(), R.color.foodshare_surface));
            text.setTextSize(15f);
            text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }

        snackbar.show();
    }
}
