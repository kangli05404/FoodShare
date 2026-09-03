package com.example.foodshare.ui.vendor;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.foodshare.R;
import com.example.foodshare.util.CloudinaryUploader;
import com.example.foodshare.util.ListingValidator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class CreateListingActivity extends AppCompatActivity {
    private static final String[] CATEGORIES = {
            "Vegetarian", "Bakery & Pastry", "Rice & Noodles", "Meat", "Seafood",
            "Dessert & Snacks", "Drinks", "Mixed Food", "Halal", "Others"
    };

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private EditText editFoodName, editDescription, editPrice, editQuantity;
    private AutoCompleteTextView editCategory;
    private ImageView imageListing;
    private Button buttonChooseImage, buttonAddDiscountRule, buttonCreateListing, buttonCancel;
    private LinearLayout layoutDiscountRules;
    private ListingImageHelper imageHelper;
    private DiscountRuleManager discountRuleManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_listing);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        initViews();
        setupCategoryDropdown();

        imageHelper = new ListingImageHelper(this, imageListing);
        discountRuleManager = new DiscountRuleManager(this, layoutDiscountRules);

        setupListeners();
    }

    private void initViews() {
        editFoodName = findViewById(R.id.editFoodName);
        editCategory = findViewById(R.id.editCategory);
        editDescription = findViewById(R.id.editDescription);
        editPrice = findViewById(R.id.editPrice);
        editQuantity = findViewById(R.id.editQuantity);
        imageListing = findViewById(R.id.imageListing);
        buttonChooseImage = findViewById(R.id.buttonChooseImage);
        buttonAddDiscountRule = findViewById(R.id.buttonAddDiscountRule);
        buttonCreateListing = findViewById(R.id.buttonCreateListing);
        buttonCancel = findViewById(R.id.buttonCancel);
        layoutDiscountRules = findViewById(R.id.layoutDiscountRules);
    }

    private void setupCategoryDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, CATEGORIES);
        editCategory.setAdapter(adapter);
        editCategory.setOnClickListener(view -> editCategory.showDropDown());
    }

    private void setupListeners() {
        ImageButton buttonBack = findViewById(R.id.buttonBack);

        buttonBack.setOnClickListener(view -> {
            if (hasUnsavedChanges()) showDiscardChangesDialog();
            else finish();
        });

        buttonCancel.setOnClickListener(view -> {
            if (hasUnsavedChanges()) showDiscardChangesDialog();
            else finish();
        });

        buttonChooseImage.setOnClickListener(view -> imageHelper.showImageSourceDialog());
        buttonAddDiscountRule.setOnClickListener(view -> discountRuleManager.showAddRuleDialog());
        buttonCreateListing.setOnClickListener(view -> checkStoreLocationBeforeCreate());
    }

    private boolean hasUnsavedChanges() {
        String foodName = editFoodName.getText().toString().trim();
        String category = editCategory.getText().toString().trim();
        String description = editDescription.getText().toString().trim();
        String price = editPrice.getText().toString().trim();
        String quantity = editQuantity.getText().toString().trim();

        return !foodName.isEmpty() || !category.isEmpty() || !description.isEmpty() || !price.isEmpty()
                || !quantity.isEmpty() || imageHelper.hasSelectedImage() || discountRuleManager.hasAnyData();
    }

    private void showDiscardChangesDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.discard_changes_title)
                .setMessage(R.string.discard_changes_message)
                .setPositiveButton(R.string.discard, (dialog, which) -> finish())
                .setNegativeButton(R.string.stay, null)
                .show();
    }

    private void checkStoreLocationBeforeCreate() {
        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(this, R.string.login_again, Toast.LENGTH_LONG).show();
            return;
        }

        String vendorId = firebaseAuth.getCurrentUser().getUid();
        buttonCreateListing.setEnabled(false);

        firestore.collection("users").document(vendorId).get()
                .addOnSuccessListener(document -> {
                    buttonCreateListing.setEnabled(true);

                    if (!document.exists()) {
                        Toast.makeText(this, "Vendor profile could not be found.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    Double latitude = document.getDouble("storeLatitude");
                    Double longitude = document.getDouble("storeLongitude");
                    String address = document.getString("storeAddress");

                    if (latitude == null || longitude == null || address == null || address.trim().isEmpty()) {
                        showStoreLocationRequiredDialog();
                        return;
                    }

                    createListing();
                })
                .addOnFailureListener(exception -> {
                    buttonCreateListing.setEnabled(true);
                    Toast.makeText(this, "Unable to check store location: " + exception.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void showStoreLocationRequiredDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Store Location Required")
                .setMessage("You must set your store location before creating a listing.")
                .setPositiveButton("Set Location", (dialog, which) -> startActivity(new Intent(this, StoreLocationActivity.class)))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void createListing() {
        String foodName = editFoodName.getText().toString().trim();
        String category = editCategory.getText().toString().trim();
        String description = editDescription.getText().toString().trim();
        String priceText = editPrice.getText().toString().trim();
        String quantityText = editQuantity.getText().toString().trim();

        if (foodName.isEmpty()) {
            editFoodName.setError("Please enter a box name");
            editFoodName.requestFocus();
            return;
        }

        if (foodName.length() < 2) {
            editFoodName.setError("Box name must be at least 2 characters");
            editFoodName.requestFocus();
            return;
        }

        if (category.isEmpty()) {
            editCategory.setError("Please select a category");
            editCategory.requestFocus();
            return;
        }

        String priceError = ListingValidator.validatePrice(priceText);
        if (priceError != null) {
            editPrice.setError(priceError);
            editPrice.requestFocus();
            return;
        }

        String quantityError = ListingValidator.validateQuantity(quantityText);
        if (quantityError != null) {
            editQuantity.setError(quantityError);
            editQuantity.requestFocus();
            return;
        }

        Uri selectedImageUri = imageHelper.getSelectedImageUri();

        if (selectedImageUri == null) {
            Toast.makeText(this, R.string.choose_image_error, Toast.LENGTH_LONG).show();
            return;
        }

        if (!discountRuleManager.hasRules()) {
            Toast.makeText(this, "Please add at least one discount rule.", Toast.LENGTH_LONG).show();
            return;
        }

        if (!discountRuleManager.hasContinuousCoverage()) {
            Toast.makeText(this, "Discount rules must connect without any time gaps.", Toast.LENGTH_LONG).show();
            return;
        }

        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(this, R.string.login_again, Toast.LENGTH_LONG).show();
            return;
        }

        double price = Double.parseDouble(priceText);
        int quantity = Integer.parseInt(quantityText);
        String vendorId = firebaseAuth.getCurrentUser().getUid();
        String listingId = firestore.collection("listings").document().getId();

        buttonCreateListing.setEnabled(false);
        buttonCreateListing.setText(R.string.creating_listing);
        buttonCancel.setEnabled(false);

        Map<String, Object> listing = new HashMap<>();
        listing.put("listingId", listingId);
        listing.put("vendorId", vendorId);
        listing.put("foodName", foodName);
        listing.put("category", category);
        listing.put("description", description);
        listing.put("originalPrice", price);
        listing.put("quantity", quantity);
        listing.put("availableQuantity", quantity);
        listing.put("imageName", "");
        listing.put("discountRules", discountRuleManager.getRulesAsMaps());
        listing.put("createdAt", FieldValue.serverTimestamp());

        CloudinaryUploader.upload(selectedImageUri, new UploadCallback() {
            @Override
            public void onStart(String requestId) {}

            @Override
            public void onProgress(String requestId, long bytes, long totalBytes) {}

            @Override
            public void onSuccess(String requestId, Map resultData) {
                Object secureUrl = resultData.get("secure_url");

                if (secureUrl == null) {
                    resetCreateButton();
                    Toast.makeText(CreateListingActivity.this, "Image upload failed.", Toast.LENGTH_LONG).show();
                    return;
                }

                listing.put("imageUrl", secureUrl.toString());

                firestore.collection("listings").document(listingId).set(listing)
                        .addOnSuccessListener(unused -> {
                            Toast.makeText(CreateListingActivity.this, R.string.listing_created, Toast.LENGTH_LONG).show();
                            finish();
                        })
                        .addOnFailureListener(exception -> {
                            resetCreateButton();
                            Toast.makeText(CreateListingActivity.this, "Failed to create listing: " + exception.getMessage(), Toast.LENGTH_LONG).show();
                        });
            }

            @Override
            public void onError(String requestId, ErrorInfo error) {
                resetCreateButton();
                Toast.makeText(CreateListingActivity.this, "Image upload failed: " + error.getDescription(), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onReschedule(String requestId, ErrorInfo error) {}
        });
    }

    private void resetCreateButton() {
        buttonCreateListing.setEnabled(true);
        buttonCreateListing.setText(R.string.create_listing_button);
        buttonCancel.setEnabled(true);
    }
}