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
    private TextView textListingCount;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable android.os.Bundle savedInstanceState) {
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
        textListingCount = view.findViewById(R.id.textListingCount);
        loadListings();
    }

    private void loadListings() {
        if (firebaseAuth.getCurrentUser() == null) return;
        layoutListings.removeAllViews();
        textEmptyListings.setVisibility(View.GONE);
        textListingCount.setText("Loading...");
        String vendorId = firebaseAuth.getCurrentUser().getUid();
        firestore.collection("listings")
                .whereEqualTo("vendorId", vendorId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(result -> {
                    List<DocumentSnapshot> listings = result.getDocuments();
                    textListingCount.setText(listings.size() +
                            (listings.size() == 1 ? " Listing" : " Listings"));
                    if (listings.isEmpty()) {
                        textEmptyListings.setVisibility(View.VISIBLE);
                    } else {
                        for (DocumentSnapshot document : listings) addListingView(document);
                    }
                })
                .addOnFailureListener(e -> {
                    textListingCount.setText("0 Listings");
                    textEmptyListings.setVisibility(View.VISIBLE);
                    textEmptyListings.setText("Failed to load listings.");
                });
    }

    private void addListingView(DocumentSnapshot document) {
        View listingView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_listing, layoutListings, false);
        android.widget.ImageView image = listingView.findViewById(R.id.imageListing);
        TextView foodName = listingView.findViewById(R.id.textFoodName);
        TextView description = listingView.findViewById(R.id.textDescription);
        TextView price = listingView.findViewById(R.id.textPrice);
        TextView quantity = listingView.findViewById(R.id.textQuantity);
        TextView period = listingView.findViewById(R.id.textDiscountPeriod);
        TextView rules = listingView.findViewById(R.id.textDiscountRules);
        TextView status = listingView.findViewById(R.id.textStatus);
        Button edit = listingView.findViewById(R.id.buttonEditListing);
        Button delete = listingView.findViewById(R.id.buttonDeleteListing);

        String id = document.getId();
        String name = stringValue(document, "foodName");
        foodName.setText(name);
        String details = stringValue(document, "description");
        description.setText(details.isEmpty() ? "No description" : details);
        price.setText(String.format(Locale.getDefault(), "RM %.2f", numberValue(document, "originalPrice")));
        int total = intValue(document, "quantity");
        quantity.setText("Available: " + intValue(document, "availableQuantity") + " / " + total);
        String start = stringValue(document, "discountStart");
        String end = stringValue(document, "discountEnd");
        period.setText(start.isEmpty() || end.isEmpty() ? "Discount Period: Not set" :
                "Discount Period: " + start + " - " + end);
        rules.setText(discountRules(document));
        String state = stringValue(document, "status");
        status.setText(state.isEmpty() ? "UNKNOWN" : state);

        edit.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), EditListingActivity.class);
            intent.putExtra("listingId", id);
            startActivity(intent);
        });
        delete.setOnClickListener(v -> new AlertDialog.Builder(requireContext())
                .setTitle("Delete Listing?")
                .setMessage("Are you sure you want to delete \"" + name + "\"?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> firestore.collection("listings")
                        .document(id).delete().addOnSuccessListener(unused -> loadListings()))
                .show());

        String imageUrl = stringValue(document, "imageUrl");
        if (!imageUrl.isEmpty()) Glide.with(this).load(imageUrl)
                .placeholder(R.drawable.magic_box_01).into(image);
        else image.setImageResource(R.drawable.magic_box_01);
        layoutListings.addView(listingView);
    }

    private String stringValue(DocumentSnapshot d, String field) {
        String value = d.getString(field);
        return value == null ? "" : value;
    }

    private double numberValue(DocumentSnapshot d, String field) {
        Double decimal = d.getDouble(field);
        if (decimal != null) return decimal;
        Long whole = d.getLong(field);
        return whole == null ? 0 : whole.doubleValue();
    }

    private int intValue(DocumentSnapshot d, String field) {
        Long value = d.getLong(field);
        return value == null ? 0 : value.intValue();
    }

    @SuppressWarnings("unchecked")
    private String discountRules(DocumentSnapshot d) {
        List<Map<String, Object>> values = (List<Map<String, Object>>) d.get("discountRules");
        if (values == null || values.isEmpty()) return "Discount Rules: None";
        StringBuilder result = new StringBuilder("Discount Rules:");
        for (Map<String, Object> value : values) {
            result.append("\n").append(String.valueOf(value.get("startTime")))
                    .append(" - ").append(String.valueOf(value.get("endTime")))
                    .append(" : ").append(String.valueOf(value.get("discountPercent"))).append("%");
        }
        return result.toString();
    }
}
