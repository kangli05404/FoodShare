package com.example.foodshare.ui.vendor;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.foodshare.R;
import com.example.foodshare.ui.auth.LoginActivity;
import com.example.foodshare.ui.profile.ProfileActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class VendorDashboardActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    private TextView textVendorWelcome;
    private TextView textActiveListings;
    private TextView textTodayOrders;

    private Button buttonCreateListing;
    private Button buttonManageListings;
    private Button buttonVendorOrders;
    private Button buttonVendorProfile;
    private Button buttonVendorLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vendor_dashboard);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        initViews();
        setupListeners();
        loadDashboardData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardData();
    }

    private void initViews() {
        textVendorWelcome = findViewById(R.id.textVendorWelcome);
        textActiveListings = findViewById(R.id.textActiveListings);
        textTodayOrders = findViewById(R.id.textTodayOrders);

        buttonCreateListing = findViewById(R.id.buttonCreateListing);
        buttonManageListings = findViewById(R.id.buttonManageListings);
        buttonVendorOrders = findViewById(R.id.buttonVendorOrders);
        buttonVendorProfile = findViewById(R.id.buttonVendorProfile);
        buttonVendorLogout = findViewById(R.id.buttonVendorLogout);
    }

    private void setupListeners() {
        buttonCreateListing.setOnClickListener(view -> {
            Intent intent = new Intent(VendorDashboardActivity.this, CreateListingActivity.class);
            startActivity(intent);
        });

        buttonManageListings.setOnClickListener(view -> {
            Intent intent = new Intent(VendorDashboardActivity.this, ManageListingsActivity.class);
            startActivity(intent);
        });

        buttonVendorOrders.setOnClickListener(view -> {
            Toast.makeText(this, R.string.vendor_orders_coming, Toast.LENGTH_SHORT).show();
        });

        buttonVendorProfile.setOnClickListener(view -> {
            Intent intent = new Intent(VendorDashboardActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        buttonVendorLogout.setOnClickListener(view -> logout());
    }

    private void loadDashboardData() {
        if (firebaseAuth.getCurrentUser() == null) {
            logout();
            return;
        }

        String vendorId = firebaseAuth.getCurrentUser().getUid();

        String email = firebaseAuth.getCurrentUser().getEmail();
        if (email != null && !email.isEmpty()) {
            String displayName = email.split("@")[0];
            textVendorWelcome.setText(String.format(getString(R.string.welcome_message), displayName));
        }

        loadActiveListingsCount(vendorId);
        loadTodayOrdersCount(vendorId);
    }

    private void loadActiveListingsCount(String vendorId) {
        firestore.collection("listings")
                .whereEqualTo("vendorId", vendorId)
                .whereEqualTo("status", "ACTIVE")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    textActiveListings.setText(String.valueOf(count));
                })
                .addOnFailureListener(e -> {
                    textActiveListings.setText("0");
                    Toast.makeText(this, R.string.failed_to_load_listings, Toast.LENGTH_SHORT).show();
                });
    }

    private void loadTodayOrdersCount(String vendorId) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        String today = sdf.format(calendar.getTime());

        firestore.collection("orders")
                .whereEqualTo("vendorId", vendorId)
                .whereEqualTo("createdDate", today)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    textTodayOrders.setText(String.valueOf(count));
                })
                .addOnFailureListener(e -> {
                    textTodayOrders.setText("0");
                });
    }

    private void logout() {
        firebaseAuth.signOut();
        Intent intent = new Intent(VendorDashboardActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}