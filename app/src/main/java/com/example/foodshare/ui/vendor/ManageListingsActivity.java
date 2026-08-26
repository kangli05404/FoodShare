package com.example.foodshare.ui.vendor;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.foodshare.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ManageListingsActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    private LinearLayout layoutListings;
    private TextView textEmptyListings;
    private TextView textListingCount;
    private ImageButton buttonBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_listings);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        layoutListings = findViewById(R.id.layoutListings);
        textEmptyListings = findViewById(R.id.textEmptyListings);
        textListingCount = findViewById(R.id.textListingCount);
        buttonBack = findViewById(R.id.buttonBack);

        buttonBack.setOnClickListener(view -> finish());

        loadListings();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void loadListings() {
        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(ManageListingsActivity.this, "Please log in again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        layoutListings.removeAllViews();
        textEmptyListings.setVisibility(View.GONE);
        textListingCount.setText("Loading...");

        String vendorId = firebaseAuth.getCurrentUser().getUid();

        firestore.collection("listings")
                .whereEqualTo("vendorId", vendorId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<DocumentSnapshot> listings = queryDocumentSnapshots.getDocuments();
                    int count = listings.size();
                    textListingCount.setText(count + (count == 1 ? " Listing" : " Listings"));

                    if (listings.isEmpty()) {
                        textEmptyListings.setVisibility(View.VISIBLE);
                        return;
                    }

                    textEmptyListings.setVisibility(View.GONE);
                    for (DocumentSnapshot document : listings) {
                        addListingView(document);
                    }
                })
                .addOnFailureListener(exception -> {
                    textListingCount.setText("0 Listings");
                    textEmptyListings.setVisibility(View.VISIBLE);
                    textEmptyListings.setText("Failed to load listings.");
                    Toast.makeText(ManageListingsActivity.this, "Failed to load listings: " + exception.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void addListingView(DocumentSnapshot document) {
        LayoutInflater inflater = LayoutInflater.from(ManageListingsActivity.this);
        View listingView = inflater.inflate(R.layout.item_listing, layoutListings, false);

        android.widget.ImageView imageListing = listingView.findViewById(R.id.imageListing);
        TextView textFoodName = listingView.findViewById(R.id.textFoodName);
        TextView textDescription = listingView.findViewById(R.id.textDescription);
        TextView textPrice = listingView.findViewById(R.id.textPrice);
        TextView textQuantity = listingView.findViewById(R.id.textQuantity);
        TextView textDiscountPeriod = listingView.findViewById(R.id.textDiscountPeriod);
        TextView textDiscountRules = listingView.findViewById(R.id.textDiscountRules);
        TextView textStatus = listingView.findViewById(R.id.textStatus);
        Button buttonEdit = listingView.findViewById(R.id.buttonEditListing);
        Button buttonDelete = listingView.findViewById(R.id.buttonDeleteListing);

        String listingId = document.getId();

        String foodName = getStringValue(document, "foodName");
        textFoodName.setText(foodName);

        String description = getStringValue(document, "description");
        if (description.isEmpty()) {
            textDescription.setText("No description");
        } else {
            textDescription.setText(description);
        }

        double originalPrice = getDoubleValue(document, "originalPrice");
        textPrice.setText(String.format(Locale.getDefault(), "RM %.2f", originalPrice));

        int quantity = getIntValue(document, "quantity");
        int availableQuantity = getIntValue(document, "availableQuantity");
        textQuantity.setText("Available: " + availableQuantity + " / " + quantity);

        String discountStart = getStringValue(document, "discountStart");
        String discountEnd = getStringValue(document, "discountEnd");
        if (!discountStart.isEmpty() && !discountEnd.isEmpty()) {
            textDiscountPeriod.setText("Discount Period: " + discountStart + " - " + discountEnd);
        } else {
            textDiscountPeriod.setText("Discount Period: Not set");
        }

        String discountRules = getDiscountRulesText(document);
        textDiscountRules.setText(discountRules);

        String status = getStringValue(document, "status");
        if (status.isEmpty()) {
            status = "UNKNOWN";
        }
        textStatus.setText(status);

        buttonEdit.setOnClickListener(view -> openEditListing(listingId));
        buttonDelete.setOnClickListener(view -> confirmDeleteListing(listingId, foodName));

        String imageName = getStringValue(document, "imageName");
        setMagicBoxImage(imageListing, imageName);

        layoutListings.addView(listingView);
    }

    private void openEditListing(String listingId) {
        Intent intent = new Intent(ManageListingsActivity.this, EditListingActivity.class);
        intent.putExtra("listingId", listingId);
        startActivity(intent);
    }

    private void confirmDeleteListing(String listingId, String foodName) {
        new AlertDialog.Builder(ManageListingsActivity.this)
                .setTitle("Delete Listing?")
                .setMessage("Are you sure you want to delete \"" + foodName + "\"?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> deleteListing(listingId))
                .show();
    }

    private void deleteListing(String listingId) {
        firestore.collection("listings").document(listingId)
                .delete()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(ManageListingsActivity.this, "Listing deleted successfully.", Toast.LENGTH_SHORT).show();
                    loadListings();
                })
                .addOnFailureListener(exception -> {
                    Toast.makeText(ManageListingsActivity.this, "Failed to delete listing: " + exception.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void setMagicBoxImage(android.widget.ImageView imageView, String imageName) {
        int imageResource = 0;
        switch (imageName) {
            case "magic_box_01":
                imageResource = R.drawable.magic_box_01;
                break;
            case "magic_box_02":
                imageResource = R.drawable.magic_box_02;
                break;
            case "magic_box_03":
                imageResource = R.drawable.magic_box_03;
                break;
            case "magic_box_04":
                imageResource = R.drawable.magic_box_04;
                break;
            case "magic_box_05":
                imageResource = R.drawable.magic_box_05;
                break;
        }

        if (imageResource != 0) {
            imageView.setImageResource(imageResource);
        } else {
            imageView.setImageResource(R.drawable.magic_box_01);
        }
    }

    private String getDiscountRulesText(DocumentSnapshot document) {
        List<Map<String, Object>> rules = (List<Map<String, Object>>) document.get("discountRules");
        if (rules == null || rules.isEmpty()) {
            return "Discount Rules: None";
        }

        StringBuilder builder = new StringBuilder();
        builder.append("Discount Rules:");
        for (Map<String, Object> rule : rules) {
            String startTime = getMapString(rule, "startTime");
            String endTime = getMapString(rule, "endTime");
            double discount = getMapDouble(rule, "discountPercent");
            builder.append("\n" + startTime + " - " + endTime + " : " + formatDiscount(discount) + "%");
        }
        return builder.toString();
    }

    private String getStringValue(DocumentSnapshot document, String field) {
        String value = document.getString(field);
        return value == null ? "" : value;
    }

    private double getDoubleValue(DocumentSnapshot document, String field) {
        Number number = document.getDouble(field);
        return number == null ? 0 : number.doubleValue();
    }

    private int getIntValue(DocumentSnapshot document, String field) {
        Long value = document.getLong(field);
        return value == null ? 0 : value.intValue();
    }

    private String getMapString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? "" : value.toString();
    }

    private double getMapDouble(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            return 0;
        }
    }

    private String formatDiscount(double discount) {
        if (discount == (int) discount) {
            return String.valueOf((int) discount);
        }
        return String.format(Locale.getDefault(), "%.2f", discount);
    }
}