package com.example.foodshare.ui.consumer;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.foodshare.R;
import com.example.foodshare.database.CartDatabase;
import com.example.foodshare.database.CartItem;
import com.example.foodshare.model.Listing;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ListingDetailsActivity extends AppCompatActivity {

    private TextView textFoodName, textDescription, textPrice, textQuantity, textDiscountPeriod;
    private MaterialButton buttonAddToCart;
    private FirebaseFirestore db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Listing currentListing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listing_details);

        textFoodName = findViewById(R.id.textFoodName);
        textDescription = findViewById(R.id.textDescription);
        textPrice = findViewById(R.id.textPrice);
        textQuantity = findViewById(R.id.textQuantity);
        textDiscountPeriod = findViewById(R.id.textDiscountPeriod);
        buttonAddToCart = findViewById(R.id.buttonAddToCart);

        db = FirebaseFirestore.getInstance();

        String listingId = getIntent().getStringExtra("listingId");
        if (listingId == null) {
            Toast.makeText(this, "Listing not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadListing(listingId);
    }

    private void loadListing(String listingId) {
        db.collection("listings").document(listingId).get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        Toast.makeText(this, "Listing no longer available", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    currentListing = document.toObject(Listing.class);
                    if (currentListing != null) {
                        currentListing.setListingId(document.getId());
                        bindListing(currentListing);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load listing: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void bindListing(Listing listing) {
        textFoodName.setText(listing.getFoodName());
        textDescription.setText(listing.getDescription());
        textPrice.setText(String.format(Locale.getDefault(), "RM %.2f", listing.getOriginalPrice()));
        textQuantity.setText(String.format(Locale.getDefault(), "Available: %d", listing.getAvailableQuantity()));
        textDiscountPeriod.setText(String.format("Collect between %s - %s",
                listing.getDiscountStart(), listing.getDiscountEnd()));

        buttonAddToCart.setOnClickListener(v -> {
            executor.execute(() -> {
                CartItem cartItem = new CartItem(
                        listing.getListingId(),
                        listing.getVendorId(),
                        listing.getFoodName(),
                        listing.getOriginalPrice(),
                        1,
                        listing.getImageUrl()
                );
                CartDatabase.getInstance(getApplicationContext())
                        .cartDao().insert(cartItem);
            });
            Toast.makeText(this, "Added to cart", Toast.LENGTH_SHORT).show();
        });
    }
}