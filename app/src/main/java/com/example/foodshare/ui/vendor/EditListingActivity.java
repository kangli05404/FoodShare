package com.example.foodshare.ui.vendor;

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

import com.bumptech.glide.Glide;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.foodshare.R;
import com.example.foodshare.model.DiscountRule;
import com.example.foodshare.util.CloudinaryUploader;
import com.example.foodshare.util.ListingValidator;
import com.example.foodshare.util.TimeUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditListingActivity extends AppCompatActivity {
    private static final String[] CATEGORIES = {
            "Vegetarian", "Bakery & Pastry", "Rice & Noodles", "Meat", "Seafood",
            "Dessert & Snacks", "Drinks", "Mixed Food", "Halal"
    };

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private EditText editFoodName, editDescription, editPrice, editQuantity;
    private AutoCompleteTextView editCategory;
    private ImageView imageListing;
    private Button buttonChooseImage, buttonAddDiscountRule, buttonSaveListing, buttonCancel;
    private LinearLayout layoutDiscountRules;
    private String listingId = "", selectedImageName = "", existingImageUrl = "";
    private ListingImageHelper imageHelper;
    private DiscountRuleManager discountRuleManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_listing);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        listingId = getIntent().getStringExtra("listingId");

        if (listingId == null || listingId.isEmpty()) {
            Toast.makeText(this, "Invalid listing.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        initViews();
        setupCategoryDropdown();

        imageHelper = new ListingImageHelper(this, imageListing);
        discountRuleManager = new DiscountRuleManager(this, layoutDiscountRules);

        setupListeners();
        loadListing();
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
        buttonSaveListing = findViewById(R.id.buttonSaveListing);
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
        buttonBack.setOnClickListener(view -> finish());

        buttonCancel.setOnClickListener(view -> finish());
        buttonChooseImage.setOnClickListener(view -> imageHelper.showImageSourceDialog());
        buttonAddDiscountRule.setOnClickListener(view -> discountRuleManager.showAddRuleDialog());
        buttonSaveListing.setOnClickListener(view -> updateListing());
    }

    private void loadListing() {
        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        buttonSaveListing.setEnabled(false);

        firestore.collection("listings").document(listingId).get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        Toast.makeText(this, "Listing not found.", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                    String vendorId = document.getString("vendorId");
                    String currentVendorId = firebaseAuth.getCurrentUser().getUid();

                    if (vendorId == null || !vendorId.equals(currentVendorId)) {
                        Toast.makeText(this, "You cannot edit this listing.", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                    String foodName = document.getString("foodName");
                    String category = document.getString("category");
                    String description = document.getString("description");
                    Double price = document.getDouble("originalPrice");
                    Long quantity = document.getLong("quantity");

                    if (foodName != null) editFoodName.setText(foodName);
                    if (category != null) editCategory.setText(category, false);
                    if (description != null) editDescription.setText(description);
                    if (price != null) editPrice.setText(TimeUtils.formatNumber(price));
                    if (quantity != null) editQuantity.setText(String.valueOf(quantity));

                    selectedImageName = document.getString("imageName");
                    existingImageUrl = document.getString("imageUrl");
                    if (selectedImageName == null) selectedImageName = "";
                    if (existingImageUrl == null) existingImageUrl = "";

                    if (!existingImageUrl.isEmpty()) Glide.with(this).load(existingImageUrl).placeholder(R.drawable.magic_box_01).into(imageListing);
                    else setImageFromName(selectedImageName);

                    ArrayList<DiscountRule> loadedRules = new ArrayList<>();
                    Object rulesObject = document.get("discountRules");

                    if (rulesObject instanceof List<?>) {
                        for (Object item : (List<?>) rulesObject) {
                            if (!(item instanceof Map<?, ?>)) continue;

                            Map<?, ?> map = (Map<?, ?>) item;
                            Object startObject = map.get("startTime");
                            Object endObject = map.get("endTime");
                            Object discountObject = map.get("discountPercent");

                            if (startObject instanceof String && endObject instanceof String && discountObject instanceof Number) {
                                loadedRules.add(new DiscountRule(
                                        (String) startObject,
                                        (String) endObject,
                                        ((Number) discountObject).doubleValue()
                                ));
                            }
                        }
                    }

                    discountRuleManager.setDiscountRules(loadedRules);
                    buttonSaveListing.setEnabled(true);
                })
                .addOnFailureListener(exception -> {
                    buttonSaveListing.setEnabled(true);
                    Toast.makeText(this, "Failed to load listing: " + exception.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void setImageFromName(String imageName) {
        int imageResource;

        switch (imageName) {
            case "magic_box_01": imageResource = R.drawable.magic_box_01; break;
            case "magic_box_02": imageResource = R.drawable.magic_box_02; break;
            case "magic_box_03": imageResource = R.drawable.magic_box_03; break;
            case "magic_box_04": imageResource = R.drawable.magic_box_04; break;
            case "magic_box_05": imageResource = R.drawable.magic_box_05; break;
            default: return;
        }

        imageListing.setImageResource(imageResource);
    }

    private void updateListing() {
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

        if (selectedImageUri == null && selectedImageName.isEmpty() && existingImageUrl.isEmpty()) {
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

        buttonSaveListing.setEnabled(false);
        buttonSaveListing.setText("Saving...");
        buttonCancel.setEnabled(false);

        Map<String, Object> updates = new HashMap<>();
        updates.put("foodName", foodName);
        updates.put("category", category);
        updates.put("description", description);
        updates.put("originalPrice", Double.parseDouble(priceText));
        updates.put("quantity", Integer.parseInt(quantityText));
        updates.put("imageName", selectedImageUri != null ? "" : selectedImageName);
        updates.put("discountRules", discountRuleManager.getRulesAsMaps());

        // Clean old fields from existing Firestore documents.
        updates.put("discountStart", FieldValue.delete());
        updates.put("discountEnd", FieldValue.delete());
        updates.put("status", FieldValue.delete());

        if (selectedImageUri == null && !existingImageUrl.isEmpty()) updates.put("imageUrl", existingImageUrl);

        if (selectedImageUri == null) {
            saveListingUpdates(updates);
            return;
        }

        CloudinaryUploader.upload(selectedImageUri, new UploadCallback() {
            @Override
            public void onStart(String requestId) {}

            @Override
            public void onProgress(String requestId, long bytes, long totalBytes) {}

            @Override
            public void onSuccess(String requestId, Map resultData) {
                Object secureUrl = resultData.get("secure_url");

                if (secureUrl == null) {
                    resetSaveButton();
                    Toast.makeText(EditListingActivity.this, "Image upload failed.", Toast.LENGTH_LONG).show();
                    return;
                }

                updates.put("imageUrl", secureUrl.toString());
                saveListingUpdates(updates);
            }

            @Override
            public void onError(String requestId, ErrorInfo error) {
                resetSaveButton();
                Toast.makeText(EditListingActivity.this, "Image upload failed: " + error.getDescription(), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onReschedule(String requestId, ErrorInfo error) {}
        });
    }

    private void saveListingUpdates(Map<String, Object> updates) {
        firestore.collection("listings").document(listingId).update(updates)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Listing updated successfully!", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(exception -> {
                    resetSaveButton();
                    Toast.makeText(this, "Failed to update listing: " + exception.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void resetSaveButton() {
        buttonSaveListing.setEnabled(true);
        buttonSaveListing.setText("Save Changes");
        buttonCancel.setEnabled(true);
    }
}