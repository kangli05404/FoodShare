package com.example.foodshare.ui.vendor;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.foodshare.R;
import com.example.foodshare.util.TimeUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class VendorListingsFragment extends Fragment {
    private LinearLayout layoutListings;
    private TextView textEmptyListings;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable android.os.Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.activity_manage_listings, container, false);

        View oldNavigation = root.findViewById(R.id.vendorBottomNavigation);
        if (oldNavigation != null) oldNavigation.setVisibility(View.GONE);

        ImageButton back = root.findViewById(R.id.buttonBack);
        if (back != null) back.setVisibility(View.GONE);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable android.os.Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        layoutListings = view.findViewById(R.id.layoutListings);
        textEmptyListings = view.findViewById(R.id.textEmptyListings);

        View buttonCreateListing = view.findViewById(R.id.buttonCreateListing);
        buttonCreateListing.setOnClickListener(v -> startActivity(new Intent(requireContext(), CreateListingActivity.class)));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (layoutListings != null) loadListings();
    }

    private void loadListings() {
        if (firebaseAuth.getCurrentUser() == null) return;

        clearListingViews();
        textEmptyListings.setVisibility(View.GONE);

        firestore.collection("listings")
                .whereEqualTo("vendorId", firebaseAuth.getCurrentUser().getUid())
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(result -> {
                    clearListingViews();
                    List<DocumentSnapshot> listings = result.getDocuments();
                    updateListingsBadge(listings.size());

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
                    clearListingViews();
                    updateListingsBadge(0);
                    textEmptyListings.setText("Failed to load listings.");
                    textEmptyListings.setVisibility(View.VISIBLE);
                });
    }

    private void updateListingsBadge(int count) {
        if (!isAdded()) return;
        if (requireActivity() instanceof VendorMainActivity) {
            ((VendorMainActivity) requireActivity()).updateListingsBadge(count);
        }
    }

    private void clearListingViews() {
        while (layoutListings.getChildCount() > 1) {
            layoutListings.removeViewAt(1);
        }
    }

    private void addListingView(DocumentSnapshot document) {
        View listingView = LayoutInflater.from(requireContext()).inflate(R.layout.item_listing, layoutListings, false);

        android.widget.ImageView image = listingView.findViewById(R.id.imageListing);
        TextView foodName = listingView.findViewById(R.id.textFoodName);
        TextView category = listingView.findViewById(R.id.textCategory);
        TextView description = listingView.findViewById(R.id.textDescription);
        TextView price = listingView.findViewById(R.id.textPrice);
        TextView quantity = listingView.findViewById(R.id.textQuantity);
        TextView period = listingView.findViewById(R.id.textDiscountPeriod);
        TextView rules = listingView.findViewById(R.id.textDiscountRules);
        Button edit = listingView.findViewById(R.id.buttonEditListing);
        Button delete = listingView.findViewById(R.id.buttonDeleteListing);

        String id = document.getId();
        String name = stringValue(document, "foodName");
        String categoryValue = stringValue(document, "category");
        String descriptionValue = stringValue(document, "description");

        foodName.setText(name.isEmpty() ? "Unnamed Box" : name);
        category.setText(categoryValue.isEmpty() ? "Uncategorized" : categoryValue);
        description.setText(descriptionValue.isEmpty() ? "No description" : descriptionValue);
        price.setText(String.format(Locale.getDefault(), "RM %.2f", numberValue(document, "originalPrice")));

        int total = intValue(document, "quantity");
        int available = intValue(document, "availableQuantity");
        quantity.setText("Available: " + available + " / " + total);

        List<Map<String, Object>> discountRules = getDiscountRules(document);
        String start = TimeUtils.getScheduleStart(discountRules);
        String end = TimeUtils.getScheduleEnd(discountRules);

        period.setText(start.isEmpty() || end.isEmpty()
                ? "Availability: Not set"
                : "Availability: " + TimeUtils.formatDisplayTime(start)
                + " - " + TimeUtils.formatDisplayTime(end));
        rules.setText(formatDiscountRules(discountRules));

        edit.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), EditListingActivity.class);
            intent.putExtra("listingId", id);
            startActivity(intent);
        });

        delete.setOnClickListener(v -> new AlertDialog.Builder(requireContext())
                .setTitle("Delete Listing?")
                .setMessage("Are you sure you want to delete \"" + name + "\"?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) ->
                        firestore.collection("listings").document(id).delete()
                                .addOnSuccessListener(unused -> loadListings()))
                .show());

        String imageUrl = stringValue(document, "imageUrl");

        if (!imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_food_placeholder)
                    .into(image);
        } else {
            image.setImageResource(R.drawable.ic_food_placeholder);
        }

        layoutListings.addView(listingView);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getDiscountRules(DocumentSnapshot document) {
        return (List<Map<String, Object>>) document.get("discountRules");
    }

    private String formatDiscountRules(List<Map<String, Object>> values) {
        if (values == null || values.isEmpty()) return "Discount Rules: None";

        StringBuilder result = new StringBuilder("Discount Rules:");

        for (Map<String, Object> value : values) {
            Object discountObject = value.get("discountPercent");
            double discount = discountObject instanceof Number ? ((Number) discountObject).doubleValue() : 0;

            result.append("\n")
                    .append(TimeUtils.formatDisplayTime(stringValue(value.get("startTime"))))
                    .append(" - ")
                    .append(TimeUtils.formatDisplayTime(stringValue(value.get("endTime"))))
                    .append(" : ")
                    .append(TimeUtils.formatDiscount(discount))
                    .append("%");
        }

        return result.toString();
    }

    private String stringValue(Object value) {
        return value instanceof String ? (String) value : "";
    }

    private String stringValue(DocumentSnapshot document, String field) {
        String value = document.getString(field);
        return value == null ? "" : value;
    }

    private double numberValue(DocumentSnapshot document, String field) {
        Double decimal = document.getDouble(field);
        if (decimal != null) return decimal;

        Long whole = document.getLong(field);
        return whole == null ? 0 : whole.doubleValue();
    }

    private int intValue(DocumentSnapshot document, String field) {
        Long value = document.getLong(field);
        return value == null ? 0 : value.intValue();
    }
}
