package com.example.foodshare.ui.vendor;

import android.app.TimePickerDialog;
import android.net.Uri;
import android.os.Bundle;
import java.io.File;
import java.io.IOException;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;

import com.example.foodshare.R;
import com.example.foodshare.util.CloudinaryUploader;
import com.bumptech.glide.Glide;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EditListingActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    private EditText editDescription;
    private EditText editPrice;
    private EditText editQuantity;

    private ImageView imageListing;

    private Button buttonChooseImage;
    private Button buttonDiscountStart;
    private Button buttonDiscountEnd;
    private Button buttonAddDiscountRule;
    private Button buttonSaveListing;
    private Button buttonCancel;

    private LinearLayout layoutDiscountRules;

    private String listingId = "";

    private String selectedImageName = "";
    private int selectedImageResource = 0;
    private Uri selectedImageUri;
    private String existingImageUrl = "";
    private ActivityResultLauncher<String> imagePickerLauncher;
    private ActivityResultLauncher<Uri> cameraLauncher;
    private Uri cameraImageUri;

    private String discountStart = "";
    private String discountEnd = "";

    private String previousDiscountStart = "";
    private String previousDiscountEnd = "";

    private final ArrayList<DiscountRule> discountRules = new ArrayList<>();

    private static class DiscountRule {
        String startTime;
        String endTime;
        double discountPercent;

        DiscountRule(String startTime, String endTime, double discountPercent) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.discountPercent = discountPercent;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_listing);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        selectedImageName = "uploaded";
                        imageListing.setImageURI(uri);
                        Toast.makeText(this, R.string.image_selected, Toast.LENGTH_SHORT).show();
                    }
                }
        );

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                captured -> {
                    if (captured && cameraImageUri != null) {
                        selectedImageUri = cameraImageUri;
                        selectedImageName = "camera";
                        imageListing.setImageURI(cameraImageUri);
                        Toast.makeText(this, R.string.image_selected, Toast.LENGTH_SHORT).show();
                    }
                }
        );

        listingId = getIntent().getStringExtra("listingId");

        if (listingId == null || listingId.isEmpty()) {
            Toast.makeText(this, "Invalid listing.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        initViews();

        ImageButton buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(view -> finish());

        buttonCancel.setOnClickListener(view -> {
            if (hasUnsavedChanges()) {
                showDiscardChangesDialog();
            } else {
                finish();
            }
        });

        setupListeners();
        loadListing();
    }

    private void initViews() {
        editDescription = findViewById(R.id.editDescription);
        editPrice = findViewById(R.id.editPrice);
        editQuantity = findViewById(R.id.editQuantity);
        imageListing = findViewById(R.id.imageListing);
        buttonChooseImage = findViewById(R.id.buttonChooseImage);
        buttonDiscountStart = findViewById(R.id.buttonDiscountStart);
        buttonDiscountEnd = findViewById(R.id.buttonDiscountEnd);
        buttonAddDiscountRule = findViewById(R.id.buttonAddDiscountRule);
        buttonSaveListing = findViewById(R.id.buttonSaveListing);
        buttonCancel = findViewById(R.id.buttonCancel);
        layoutDiscountRules = findViewById(R.id.layoutDiscountRules);
    }

    private boolean hasUnsavedChanges() {
        String description = editDescription.getText().toString().trim();
        String price = editPrice.getText().toString().trim();
        String quantity = editQuantity.getText().toString().trim();

        return !description.isEmpty() || !price.isEmpty()
                || !quantity.isEmpty() || selectedImageUri != null
                || !discountStart.isEmpty() || !discountEnd.isEmpty()
                || !discountRules.isEmpty();
    }

    private void showDiscardChangesDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Discard Changes?")
                .setMessage("You have unsaved changes. Are you sure you want to leave?")
                .setPositiveButton("Discard", (dialog, which) -> finish())
                .setNegativeButton("Stay", null)
                .show();
    }

    private void setupListeners() {
        buttonChooseImage.setOnClickListener(view -> showImageSourceDialog());
        buttonDiscountStart.setOnClickListener(view -> showDiscountStartPicker());
        buttonDiscountEnd.setOnClickListener(view -> showDiscountEndPicker());
        buttonAddDiscountRule.setOnClickListener(view -> {
            if (discountStart.isEmpty() || discountEnd.isEmpty()) {
                Toast.makeText(this, "Please select Discount Start and End first.", Toast.LENGTH_LONG).show();
                return;
            }
            showDiscountRuleDialog(-1);
        });
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
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Toast.makeText(this, "Listing not found.", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                    String vendorId = documentSnapshot.getString("vendorId");
                    String currentVendorId = firebaseAuth.getCurrentUser().getUid();

                    if (vendorId == null || !vendorId.equals(currentVendorId)) {
                        Toast.makeText(this, "You cannot edit this listing.", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                    String description = documentSnapshot.getString("description");
                    if (description != null) {
                        editDescription.setText(description);
                    }

                    Double price = documentSnapshot.getDouble("originalPrice");
                    if (price != null) {
                        editPrice.setText(formatNumber(price));
                    }

                    Long quantity = documentSnapshot.getLong("quantity");
                    if (quantity != null) {
                        editQuantity.setText(String.valueOf(quantity));
                    }

                    selectedImageName = documentSnapshot.getString("imageName");
                    if (selectedImageName == null) {
                        selectedImageName = "";
                    }
                    existingImageUrl = documentSnapshot.getString("imageUrl");
                    if (existingImageUrl == null) {
                        existingImageUrl = "";
                    }

                    if (!existingImageUrl.isEmpty()) {
                        Glide.with(this)
                                .load(existingImageUrl)
                                .placeholder(R.drawable.magic_box_01)
                                .into(imageListing);
                    } else {
                        setImageFromName(selectedImageName);
                    }

                    discountStart = documentSnapshot.getString("discountStart");
                    if (discountStart == null) {
                        discountStart = "";
                    }
                    if (!discountStart.isEmpty()) {
                        buttonDiscountStart.setText("Discount Start: " + discountStart);
                    }

                    discountEnd = documentSnapshot.getString("discountEnd");
                    if (discountEnd == null) {
                        discountEnd = "";
                    }
                    if (!discountEnd.isEmpty()) {
                        buttonDiscountEnd.setText("Discount End: " + discountEnd);
                    }

                    discountRules.clear();

                    ArrayList<Map<String, Object>> rules = (ArrayList<Map<String, Object>>) documentSnapshot.get("discountRules");

                    if (rules != null) {
                        for (Map<String, Object> ruleData : rules) {
                            String startTime = (String) ruleData.get("startTime");
                            String endTime = (String) ruleData.get("endTime");
                            Object discountObject = ruleData.get("discountPercent");

                            if (startTime == null || endTime == null || discountObject == null) {
                                continue;
                            }

                            double discount;
                            if (discountObject instanceof Number) {
                                discount = ((Number) discountObject).doubleValue();
                            } else {
                                continue;
                            }

                            discountRules.add(new DiscountRule(startTime, endTime, discount));
                        }
                    }

                    updateDiscountRules();
                    buttonSaveListing.setEnabled(true);
                })
                .addOnFailureListener(exception -> {
                    buttonSaveListing.setEnabled(true);
                    Toast.makeText(this, "Failed to load listing: " + exception.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void showMagicBoxImageDialog() {
        String[] imageNames = {"magic_box_01", "magic_box_02", "magic_box_03", "magic_box_04", "magic_box_05"};
        int[] imageResources = {R.drawable.magic_box_01, R.drawable.magic_box_02, R.drawable.magic_box_03, R.drawable.magic_box_04, R.drawable.magic_box_05};

        LinearLayout gridLayout = new LinearLayout(this);
        gridLayout.setOrientation(LinearLayout.VERTICAL);
        gridLayout.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

        int columns = 2;
        LinearLayout currentRow = null;

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Choose Magic Box Image")
                .setNegativeButton("Cancel", null);

        final AlertDialog[] dialogRef = new AlertDialog[1];

        for (int i = 0; i < imageNames.length; i++) {
            if (i % columns == 0) {
                currentRow = new LinearLayout(this);
                currentRow.setOrientation(LinearLayout.HORIZONTAL);
                currentRow.setGravity(Gravity.CENTER);
                gridLayout.addView(currentRow);
            }

            LinearLayout itemLayout = new LinearLayout(this);
            itemLayout.setOrientation(LinearLayout.VERTICAL);
            itemLayout.setGravity(Gravity.CENTER);
            itemLayout.setLayoutParams(new LinearLayout.LayoutParams(0, dpToPx(130), 1));
            itemLayout.setBackgroundResource(R.drawable.bg_discount_rule);
            itemLayout.setPadding(dpToPx(8), dpToPx(12), dpToPx(8), dpToPx(12));
            int margin = dpToPx(6);
            ((LinearLayout.LayoutParams) itemLayout.getLayoutParams()).setMargins(margin, margin, margin, margin);

            ImageView imageView = new ImageView(this);
            imageView.setImageResource(imageResources[i]);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(80), dpToPx(80)));

            TextView nameText = new TextView(this);
            nameText.setText("Box " + (i + 1));
            nameText.setTextSize(11);
            nameText.setTextColor(getColor(R.color.foodshare_text));
            nameText.setGravity(Gravity.CENTER);
            nameText.setPadding(0, dpToPx(4), 0, 0);

            itemLayout.addView(imageView);
            itemLayout.addView(nameText);

            int finalIndex = i;
            itemLayout.setOnClickListener(view -> {
                selectedImageName = imageNames[finalIndex];
                selectedImageResource = imageResources[finalIndex];
                imageListing.setImageResource(selectedImageResource);
                Toast.makeText(EditListingActivity.this, "Image selected", Toast.LENGTH_SHORT).show();
                if (dialogRef[0] != null) {
                    dialogRef[0].dismiss();
                }
            });

            currentRow.addView(itemLayout);
        }

        builder.setView(gridLayout);
        AlertDialog dialog = builder.create();
        dialogRef[0] = dialog;
        dialog.show();
    }

    private void setImageFromName(String imageName) {
        if (imageName == null || imageName.isEmpty()) {
            return;
        }

        switch (imageName) {
            case "magic_box_01":
                selectedImageResource = R.drawable.magic_box_01;
                break;
            case "magic_box_02":
                selectedImageResource = R.drawable.magic_box_02;
                break;
            case "magic_box_03":
                selectedImageResource = R.drawable.magic_box_03;
                break;
            case "magic_box_04":
                selectedImageResource = R.drawable.magic_box_04;
                break;
            case "magic_box_05":
                selectedImageResource = R.drawable.magic_box_05;
                break;
            default:
                selectedImageResource = 0;
                return;
        }

        imageListing.setImageResource(selectedImageResource);
    }

    private void storeDiscountPeriod() {
        previousDiscountStart = discountStart;
        previousDiscountEnd = discountEnd;
    }

    private void restoreDiscountPeriod() {
        discountStart = previousDiscountStart;
        discountEnd = previousDiscountEnd;
        buttonDiscountStart.setText("Discount Start: " + discountStart);
        buttonDiscountEnd.setText("Discount End: " + discountEnd);
    }

    private boolean validateExistingRules() {
        if (discountRules.isEmpty()) {
            return true;
        }

        if (discountStart.isEmpty() || discountEnd.isEmpty()) {
            return true;
        }

        int periodStart = convertTimeToMinutes(discountStart);
        int periodEnd = convertTimeToMinutes(discountEnd);

        List<DiscountRule> invalidRules = new ArrayList<>();

        for (DiscountRule rule : discountRules) {
            int ruleStart = convertTimeToMinutes(rule.startTime);
            int ruleEnd = convertTimeToMinutes(rule.endTime);
            if (ruleStart < periodStart || ruleEnd > periodEnd) {
                invalidRules.add(rule);
            }
        }

        if (!invalidRules.isEmpty()) {
            StringBuilder message = new StringBuilder("The following rules are outside the new discount period:\n\n");
            for (DiscountRule rule : invalidRules) {
                message.append(rule.startTime)
                        .append(" - ")
                        .append(rule.endTime)
                        .append(" (")
                        .append(formatDiscount(rule.discountPercent))
                        .append("%)\n");
            }
            message.append("\nWould you like to remove them?");

            new AlertDialog.Builder(this)
                    .setTitle("Rules Out of Period")
                    .setMessage(message.toString())
                    .setPositiveButton("Remove", (dialog, which) -> {
                        discountRules.removeAll(invalidRules);
                        updateDiscountRules();
                        Toast.makeText(this, "Invalid rules removed.", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> {
                        restoreDiscountPeriod();
                        updateDiscountRules();
                    })
                    .show();
            return false;
        }

        return true;
    }

    private void showDiscountStartPicker() {
        Calendar calendar = Calendar.getInstance();

        TimePickerDialog dialog = new TimePickerDialog(
                this,
                (view, hour, minute) -> {
                    String time = formatTime(hour, minute);

                    if (!discountEnd.isEmpty()) {
                        int start = convertTimeToMinutes(time);
                        int end = convertTimeToMinutes(discountEnd);
                        if (start >= end) {
                            Toast.makeText(this, "Discount Start must be before Discount End.", Toast.LENGTH_LONG).show();
                            return;
                        }
                    }

                    storeDiscountPeriod();
                    discountStart = time;
                    buttonDiscountStart.setText("Discount Start: " + time);

                    if (!validateExistingRules()) {
                        updateDiscountRules();
                    }

                    updateDiscountRules();
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false
        );
        dialog.show();
    }

    private void showDiscountEndPicker() {
        Calendar calendar = Calendar.getInstance();

        TimePickerDialog dialog = new TimePickerDialog(
                this,
                (view, hour, minute) -> {
                    String time = formatTime(hour, minute);

                    if (!discountStart.isEmpty()) {
                        int start = convertTimeToMinutes(discountStart);
                        int end = convertTimeToMinutes(time);
                        if (end <= start) {
                            Toast.makeText(this, "Discount End must be after Discount Start.", Toast.LENGTH_LONG).show();
                            return;
                        }
                    }

                    storeDiscountPeriod();
                    discountEnd = time;
                    buttonDiscountEnd.setText("Discount End: " + time);

                    if (!validateExistingRules()) {
                        updateDiscountRules();
                    }

                    updateDiscountRules();
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false
        );
        dialog.show();
    }

    private void showDiscountRuleDialog(int editIndex) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = dpToPx(20);
        layout.setPadding(padding, padding, padding, padding);

        Button startButton = new Button(this);
        startButton.setText("Select Rule Start Time");
        layout.addView(startButton);

        Button endButton = new Button(this);
        endButton.setText("Select Rule End Time");
        LinearLayout.LayoutParams endParams = new LinearLayout.LayoutParams(-1, -2);
        endParams.topMargin = dpToPx(8);
        layout.addView(endButton, endParams);

        EditText discountInput = new EditText(this);
        discountInput.setHint("Discount Percentage (%)");
        discountInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        LinearLayout.LayoutParams discountParams = new LinearLayout.LayoutParams(-1, -2);
        discountParams.topMargin = dpToPx(12);
        layout.addView(discountInput, discountParams);

        final String[] ruleStart = {""};
        final String[] ruleEnd = {""};

        if (editIndex >= 0 && editIndex < discountRules.size()) {
            DiscountRule rule = discountRules.get(editIndex);
            ruleStart[0] = rule.startTime;
            ruleEnd[0] = rule.endTime;
            startButton.setText("Start: " + rule.startTime);
            endButton.setText("End: " + rule.endTime);
            discountInput.setText(formatDiscount(rule.discountPercent));
        }

        startButton.setOnClickListener(view -> showRuleTimePicker(true, ruleStart, ruleEnd, startButton, endButton));
        endButton.setOnClickListener(view -> showRuleTimePicker(false, ruleStart, ruleEnd, startButton, endButton));

        String title = editIndex >= 0 ? "Edit Discount Rule" : "Add Discount Rule";

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(layout)
                .setNegativeButton("Cancel", null)
                .setPositiveButton(editIndex >= 0 ? "Save" : "Add", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(view -> {
                if (ruleStart[0].isEmpty() || ruleEnd[0].isEmpty()) {
                    Toast.makeText(this, "Please select both start and end time.", Toast.LENGTH_SHORT).show();
                    return;
                }

                int start = convertTimeToMinutes(ruleStart[0]);
                int end = convertTimeToMinutes(ruleEnd[0]);

                if (start >= end) {
                    Toast.makeText(this, "Rule start time must be before rule end time.", Toast.LENGTH_SHORT).show();
                    return;
                }

                int periodStart = convertTimeToMinutes(discountStart);
                int periodEnd = convertTimeToMinutes(discountEnd);

                if (start < periodStart || end > periodEnd) {
                    Toast.makeText(this, "Rule time must be within Discount Start and End.", Toast.LENGTH_LONG).show();
                    return;
                }

                String discountText = discountInput.getText().toString().trim();
                if (discountText.isEmpty()) {
                    discountInput.setError("Enter discount percentage");
                    return;
                }

                double discount;
                try {
                    discount = Double.parseDouble(discountText);
                } catch (NumberFormatException e) {
                    discountInput.setError("Invalid discount");
                    return;
                }

                if (discount <= 0 || discount > 100) {
                    discountInput.setError("Enter a value between 1 and 100");
                    return;
                }

                if (hasOverlappingRule(ruleStart[0], ruleEnd[0], editIndex)) {
                    Toast.makeText(this, "This time overlaps with another discount rule.", Toast.LENGTH_LONG).show();
                    return;
                }

                DiscountRule newRule = new DiscountRule(ruleStart[0], ruleEnd[0], discount);
                if (editIndex >= 0) {
                    discountRules.set(editIndex, newRule);
                } else {
                    discountRules.add(newRule);
                }

                updateDiscountRules();
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private void showRuleTimePicker(boolean isStart, String[] ruleStart, String[] ruleEnd,
                                    Button startButton, Button endButton) {
        Calendar calendar = Calendar.getInstance();

        TimePickerDialog dialog = new TimePickerDialog(
                this,
                (view, hour, minute) -> {
                    String time = formatTime(hour, minute);

                    int selected = convertTimeToMinutes(time);
                    int periodStart = convertTimeToMinutes(discountStart);
                    int periodEnd = convertTimeToMinutes(discountEnd);

                    if (selected < periodStart || selected > periodEnd) {
                        Toast.makeText(this, "Please select a time between " + discountStart + " and " + discountEnd, Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (isStart) {
                        ruleStart[0] = time;
                        startButton.setText("Start: " + time);
                    } else {
                        ruleEnd[0] = time;
                        endButton.setText("End: " + time);
                    }
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false
        );
        dialog.show();
    }

    private boolean hasOverlappingRule(String newStartTime, String newEndTime, int editIndex) {
        int newStart = convertTimeToMinutes(newStartTime);
        int newEnd = convertTimeToMinutes(newEndTime);

        for (int i = 0; i < discountRules.size(); i++) {
            if (i == editIndex) {
                continue;
            }

            DiscountRule rule = discountRules.get(i);
            int existingStart = convertTimeToMinutes(rule.startTime);
            int existingEnd = convertTimeToMinutes(rule.endTime);

            if (newStart < existingEnd && newEnd > existingStart) {
                return true;
            }
        }

        return false;
    }

    private void updateDiscountRules() {
        layoutDiscountRules.removeAllViews();

        if (discountRules.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("No discount rules added yet.");
            emptyText.setTextSize(14);
            emptyText.setTextColor(getColor(R.color.foodshare_text_secondary));
            layoutDiscountRules.addView(emptyText);
            return;
        }

        for (int i = 0; i < discountRules.size(); i++) {
            addRuleView(i, discountRules.get(i));
        }
    }

    private void addRuleView(int index, DiscountRule rule) {
        LinearLayout ruleLayout = new LinearLayout(this);
        ruleLayout.setOrientation(LinearLayout.HORIZONTAL);
        ruleLayout.setGravity(Gravity.CENTER_VERTICAL);
        ruleLayout.setPadding(dpToPx(14), dpToPx(10), dpToPx(6), dpToPx(10));
        ruleLayout.setBackgroundResource(R.drawable.bg_discount_rule);

        LinearLayout infoLayout = new LinearLayout(this);
        infoLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(0, -2, 1);
        ruleLayout.addView(infoLayout, infoParams);

        TextView timeText = new TextView(this);
        timeText.setText(rule.startTime + " – " + rule.endTime);
        timeText.setTextSize(16);
        timeText.setTextColor(getColor(R.color.foodshare_text));
        timeText.setTypeface(null, android.graphics.Typeface.BOLD);
        infoLayout.addView(timeText);

        TextView discountText = new TextView(this);
        discountText.setText(formatDiscount(rule.discountPercent) + "% discount");
        discountText.setTextSize(14);
        discountText.setTextColor(getColor(R.color.foodshare_green));
        LinearLayout.LayoutParams discountParams = new LinearLayout.LayoutParams(-2, -2);
        discountParams.topMargin = dpToPx(3);
        infoLayout.addView(discountText, discountParams);

        ImageButton editButton = new ImageButton(this);
        editButton.setImageResource(R.drawable.ic_edit);
        editButton.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        editButton.setContentDescription("Edit discount rule");
        editButton.setPadding(dpToPx(10), dpToPx(10), dpToPx(10), dpToPx(10));
        ruleLayout.addView(editButton, new LinearLayout.LayoutParams(dpToPx(48), dpToPx(48)));
        editButton.setOnClickListener(view -> showDiscountRuleDialog(index));

        ImageButton deleteButton = new ImageButton(this);
        deleteButton.setImageResource(R.drawable.ic_delete);
        deleteButton.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        deleteButton.setContentDescription("Delete discount rule");
        deleteButton.setPadding(dpToPx(10), dpToPx(10), dpToPx(10), dpToPx(10));
        ruleLayout.addView(deleteButton, new LinearLayout.LayoutParams(dpToPx(48), dpToPx(48)));
        deleteButton.setOnClickListener(view -> confirmDeleteRule(index));

        LinearLayout.LayoutParams ruleParams = new LinearLayout.LayoutParams(-1, -2);
        ruleParams.topMargin = dpToPx(8);
        layoutDiscountRules.addView(ruleLayout, ruleParams);
    }

    private void confirmDeleteRule(int index) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Discount Rule?")
                .setMessage("Are you sure you want to delete this rule?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (index >= 0 && index < discountRules.size()) {
                        discountRules.remove(index);
                        updateDiscountRules();
                    }
                })
                .show();
    }

    private void updateListing() {
        String description = editDescription.getText().toString().trim();
        String priceText = editPrice.getText().toString().trim();
        String quantityText = editQuantity.getText().toString().trim();

        if (priceText.isEmpty()) {
            editPrice.setError("Please enter price");
            editPrice.requestFocus();
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceText);
        } catch (NumberFormatException e) {
            editPrice.setError("Invalid price");
            editPrice.requestFocus();
            return;
        }

        if (price <= 0) {
            editPrice.setError("Price must be greater than 0");
            editPrice.requestFocus();
            return;
        }

        if (quantityText.isEmpty()) {
            editQuantity.setError("Please enter quantity");
            editQuantity.requestFocus();
            return;
        }

        int quantity;
        try {
            quantity = Integer.parseInt(quantityText);
        } catch (NumberFormatException e) {
            editQuantity.setError("Invalid quantity");
            editQuantity.requestFocus();
            return;
        }

        if (quantity <= 0) {
            editQuantity.setError("Quantity must be greater than 0");
            editQuantity.requestFocus();
            return;
        }

        if (selectedImageUri == null && selectedImageName.isEmpty() && existingImageUrl.isEmpty()) {
            Toast.makeText(this, R.string.choose_image_error, Toast.LENGTH_LONG).show();
            return;
        }

        if (discountStart.isEmpty() || discountEnd.isEmpty()) {
            Toast.makeText(this, "Please select Discount Start and End.", Toast.LENGTH_LONG).show();
            return;
        }

        if (discountRules.isEmpty()) {
            Toast.makeText(this, "Please add at least one discount rule.", Toast.LENGTH_LONG).show();
            return;
        }

        buttonSaveListing.setEnabled(false);
        buttonSaveListing.setText("Saving...");
        buttonCancel.setEnabled(false);

        Map<String, Object> updates = new HashMap<>();
        // Keep the listing name consistent with the Magic Box concept.
        updates.put("foodName", "Magic Box");
        updates.put("description", description);
        updates.put("originalPrice", price);
        updates.put("quantity", quantity);
        updates.put("imageName", selectedImageUri != null ? "" : selectedImageName);
        if (selectedImageUri == null && !existingImageUrl.isEmpty()) {
            updates.put("imageUrl", existingImageUrl);
        }
        updates.put("discountStart", discountStart);
        updates.put("discountEnd", discountEnd);

        ArrayList<Map<String, Object>> rules = new ArrayList<>();
        for (DiscountRule rule : discountRules) {
            Map<String, Object> ruleData = new HashMap<>();
            ruleData.put("startTime", rule.startTime);
            ruleData.put("endTime", rule.endTime);
            ruleData.put("discountPercent", rule.discountPercent);
            rules.add(ruleData);
        }
        updates.put("discountRules", rules);

        if (selectedImageUri != null) {
            CloudinaryUploader.upload(selectedImageUri, new UploadCallback() {
                @Override
                public void onStart(String requestId) {
                }

                @Override
                public void onProgress(String requestId, long bytes, long totalBytes) {
                }

                @Override
                public void onSuccess(String requestId, Map resultData) {
                    Object secureUrl = resultData.get("secure_url");
                    if (secureUrl == null) {
                        resetSaveButton();
                        Toast.makeText(EditListingActivity.this,
                                String.format(getString(R.string.image_upload_failed),
                                        "Cloudinary did not return an image URL."),
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    updates.put("imageUrl", secureUrl.toString());
                    saveListingUpdates(updates);
                }

                @Override
                public void onError(String requestId, ErrorInfo error) {
                    resetSaveButton();
                    Toast.makeText(EditListingActivity.this,
                            String.format(getString(R.string.image_upload_failed), error.getDescription()),
                            Toast.LENGTH_LONG).show();
                }

                @Override
                public void onReschedule(String requestId, ErrorInfo error) {
                }
            });
        } else {
            saveListingUpdates(updates);
        }
    }

    private void showImageSourceDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.choose_image_source)
                .setItems(new String[]{
                        getString(R.string.choose_from_device),
                        getString(R.string.take_photo)
                }, (dialog, which) -> {
                    if (which == 0) {
                        imagePickerLauncher.launch("image/*");
                    } else {
                        openCamera();
                    }
                })
                .show();
    }

    private void openCamera() {
        try {
            File imageFile = File.createTempFile("foodshare_camera_", ".jpg", getCacheDir());
            cameraImageUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    imageFile
            );
            cameraLauncher.launch(cameraImageUri);
        } catch (IOException exception) {
            Toast.makeText(this, R.string.camera_open_failed, Toast.LENGTH_LONG).show();
        }
    }

    private void saveListingUpdates(Map<String, Object> updates) {
        firestore.collection("listings").document(listingId)
                .update(updates)
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

    private String formatDiscount(double discount) {
        if (discount == (int) discount) {
            return String.valueOf((int) discount);
        }
        return String.valueOf(discount);
    }

    private String formatNumber(double number) {
        if (number == (int) number) {
            return String.valueOf((int) number);
        }
        return String.valueOf(number);
    }

    private String formatTime(int hour, int minute) {
        return String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
    }

    private int convertTimeToMinutes(String time) {
        String[] parts = time.split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);
        return hour * 60 + minute;
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }
}
