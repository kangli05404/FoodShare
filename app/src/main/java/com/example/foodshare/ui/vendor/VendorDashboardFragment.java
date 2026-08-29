package com.example.foodshare.ui.vendor;

import android.content.Intent;
import android.os.Bundle;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class VendorDashboardFragment extends Fragment {
    private TextView textVendorWelcome;
    private TextView textActiveListings;
    private TextView textTodayOrders;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.activity_vendor_dashboard, container, false);

        View oldNavigation = root.findViewById(R.id.vendorBottomNavigation);

        if (oldNavigation != null) {
            oldNavigation.setVisibility(View.GONE);
        }

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        textVendorWelcome = view.findViewById(R.id.textVendorWelcome);
        textActiveListings = view.findViewById(R.id.textActiveListings);
        textTodayOrders = view.findViewById(R.id.textTodayOrders);

        Button createListing = view.findViewById(R.id.buttonCreateListing);
        createListing.setOnClickListener(view1 ->
                startActivity(new Intent(requireContext(), CreateListingActivity.class)));

        loadDashboardData();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (textActiveListings != null) {
            loadDashboardData();
        }
    }

    private void loadDashboardData() {
        if (firebaseAuth.getCurrentUser() == null) {
            ((VendorMainActivity) requireActivity()).logout();
            return;
        }

        String vendorId = firebaseAuth.getCurrentUser().getUid();
        String email = firebaseAuth.getCurrentUser().getEmail();

        if (email != null && !email.isEmpty()) {
            String displayName = email.split("@")[0];
            textVendorWelcome.setText(String.format(getString(R.string.welcome_message), displayName));
        }

        firestore.collection("listings")
                .whereEqualTo("vendorId", vendorId)
                .get()
                .addOnSuccessListener(result -> textActiveListings.setText(String.valueOf(result.size())))
                .addOnFailureListener(exception -> {
                    textActiveListings.setText("0");

                    if (isAdded()) {
                        Toast.makeText(requireContext(), R.string.failed_to_load_listings, Toast.LENGTH_SHORT).show();
                    }
                });

        java.util.Calendar calendar = java.util.Calendar.getInstance();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());

        firestore.collection("orders")
                .whereEqualTo("vendorId", vendorId)
                .whereEqualTo("createdDate", sdf.format(calendar.getTime()))
                .get()
                .addOnSuccessListener(result -> textTodayOrders.setText(String.valueOf(result.size())))
                .addOnFailureListener(exception -> textTodayOrders.setText("0"));
    }
}