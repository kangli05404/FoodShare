package com.example.foodshare.ui.consumer;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.foodshare.R;
import com.example.foodshare.database.CartDatabase;
import com.example.foodshare.database.CartItem;
import com.example.foodshare.database.CartDao;
import com.example.foodshare.model.Listing;
import com.example.foodshare.util.TimeUtils;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ListingDetailsActivity extends AppCompatActivity {
    private TextView textFoodName, textDescription, textPrice, textQuantity, textDiscountPeriod;
    private TextView textToolbarTitle;
    private ImageView imageListingDetail;
    private MaterialButton buttonAddToCart;
    private FirebaseFirestore db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private String listingId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listing_details);
        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());

        textFoodName = findViewById(R.id.textFoodName);
        textDescription = findViewById(R.id.textDescription);
        textPrice = findViewById(R.id.textPrice);
        textQuantity = findViewById(R.id.textQuantity);
        textDiscountPeriod = findViewById(R.id.textDiscountPeriod);
        imageListingDetail = findViewById(R.id.imageListingDetail);
        buttonAddToCart = findViewById(R.id.buttonAddToCart);
        textToolbarTitle = findViewById(R.id.textToolbarTitle);
        db = FirebaseFirestore.getInstance();

        listingId = getIntent().getStringExtra("listingId");

        if (listingId == null || listingId.isEmpty()) {
            Toast.makeText(this, "Listing not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadListing();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (db != null && !listingId.isEmpty()) loadListing();
    }

    private void loadListing() {
        db.collection("listings").document(listingId).get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        Toast.makeText(this, "Listing no longer available", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    Listing listing = document.toObject(Listing.class);

                    if (listing != null) {
                        listing.setListingId(document.getId());
                        bindListing(listing);
                    }
                })
                .addOnFailureListener(exception ->
                        Toast.makeText(this, "Failed to load listing: " + exception.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void bindListing(Listing listing) {
        textFoodName.setText(listing.getFoodName());
        textDescription.setText(listing.getDescription());
        textQuantity.setText(String.format(Locale.getDefault(), "Available: %d", listing.getAvailableQuantity()));
        textToolbarTitle.setText(listing.getFoodName());

        double discount = TimeUtils.getCurrentDiscountPercent(listing.getDiscountRules());
        double currentPrice = TimeUtils.calculateDiscountedPrice(listing.getOriginalPrice(), discount);

        if (discount > 0) {
            textPrice.setText(String.format(Locale.getDefault(), "RM %.2f (%s%% off)", currentPrice, TimeUtils.formatDiscount(discount)));
        } else {
            textPrice.setText(String.format(Locale.getDefault(), "RM %.2f", listing.getOriginalPrice()));
        }

        String start = TimeUtils.getScheduleStart(listing.getDiscountRules());
        String end = TimeUtils.getScheduleEnd(listing.getDiscountRules());
        textDiscountPeriod.setText("Available between " + start + " - " + end);

        if (listing.getImageUrl() != null && !listing.getImageUrl().isEmpty()) {
            Glide.with(this).load(listing.getImageUrl()).placeholder(R.drawable.magic_box_01).into(imageListingDetail);
        }

        if (listing.getAvailableQuantity() <= 0) {
            buttonAddToCart.setEnabled(false);
            buttonAddToCart.setText("Sold Out");
        } else if (!TimeUtils.isWithinDiscountRules(listing.getDiscountRules())) {
            buttonAddToCart.setEnabled(false);
            buttonAddToCart.setText("Not Available");
        } else {
            buttonAddToCart.setEnabled(true);
            buttonAddToCart.setText("Add to Cart");
        }

        buttonAddToCart.setOnClickListener(view -> validateAndAddToCart());
    }

    private void validateAndAddToCart() {
        db.collection("listings").document(listingId).get()
                .addOnSuccessListener(document -> {
                    Listing listing = document.toObject(Listing.class);

                    if (!document.exists() || listing == null) {
                        Toast.makeText(this, "Listing no longer available.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    listing.setListingId(document.getId());

                    if (listing.getAvailableQuantity() <= 0) {
                        Toast.makeText(this, "This box is sold out.", Toast.LENGTH_SHORT).show();
                        bindListing(listing);
                        return;
                    }

                    if (!TimeUtils.isWithinDiscountRules(listing.getDiscountRules())) {
                        Toast.makeText(this, "This listing is not available at this time.", Toast.LENGTH_SHORT).show();
                        bindListing(listing);
                        return;
                    }

                    double discountAtAddTime = TimeUtils.getCurrentDiscountPercent(listing.getDiscountRules());
                    double priceToStore = TimeUtils.calculateDiscountedPrice(listing.getOriginalPrice(), discountAtAddTime);

                    executor.execute(() -> {
                        CartDao cartDao = CartDatabase.getInstance(getApplicationContext()).cartDao();
                        CartItem existing = cartDao.getByListingId(listing.getListingId());

                        if (existing != null) {
                            existing.quantity += 1;
                            cartDao.update(existing);
                        } else {
                            CartItem newItem = new CartItem(
                                    listing.getListingId(),
                                    listing.getVendorId(),
                                    listing.getFoodName(),
                                    priceToStore,
                                    1,
                                    listing.getImageUrl()
                            );
                            cartDao.insert(newItem);
                        }
                        runOnUiThread(() -> Toast.makeText(this, "Added to cart", Toast.LENGTH_SHORT).show());
                    });
                });
    }
}