package com.example.foodshare.ui.vendor;

import android.app.TimePickerDialog;
import android.net.Uri;
import android.os.Bundle;
import java.io.File;
import java.io.IOException;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
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
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CreateListingActivity extends AppCompatActivity {

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
    private Button buttonCreateListing;
    private Button buttonCancel;
    private LinearLayout layoutDiscountRules;

    private String selectedImageName = "";
    private int selectedImageResource = 0;
    private Uri selectedImageUri;
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
        setContentView(R.layout.activity_create_listing);

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

        editDescription = findViewById(R.id.editDescription);
        editPrice = findViewById(R.id.editPrice);
        editQuantity = findViewById(R.id.editQuantity);
        imageListing = findViewById(R.id.imageListing);
        buttonChooseImage = findViewById(R.id.buttonChooseImage);
        buttonDiscountStart = findViewById(R.id.buttonDiscountStart);
        buttonDiscountEnd = findViewById(R.id.buttonDiscountEnd);
        buttonAddDiscountRule = findViewById(R.id.buttonAddDiscountRule);
        buttonCreateListing = findViewById(R.id.buttonCreateListing);
        buttonCancel = findViewById(R.id.buttonCancel);
        layoutDiscountRules = findViewById(R.id.layoutDiscountRules);

        ImageButton buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(view -> {
            if (hasUnsavedChanges()) {
                showDiscardChangesDialog();
            } else {
                finish();
            }
        });

        buttonCancel.setOnClickListener(view -> {
            if (hasUnsavedChanges()) {
                showDiscardChangesDialog();
            } else {
                finish();
            }
        });

        updateDiscountRules();

        buttonChooseImage.setOnClickListener(view -> showImageSourceDialog());
        buttonDiscountStart.setOnClickListener(view -> showDiscountStartPicker());
        buttonDiscountEnd.setOnClickListener(view -> showDiscountEndPicker());
        buttonAddDiscountRule.setOnClickListener(view -> {
            if (discountStart.isEmpty() || discountEnd.isEmpty()) {
                Toast.makeText(CreateListingActivity.this, R.string.please_select_period, Toast.LENGTH_LONG).show();
                return;
            }
            showDiscountRuleDialog(-1);
        });
        buttonCreateListing.setOnClickListener(view -> createListing());
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
                .setTitle(R.string.discard_changes_title)
                .setMessage(R.string.discard_changes_message)
                .setPositiveButton(R.string.discard, (dialog, which) -> finish())
                .setNegativeButton(R.string.stay, null)
                .show();
    }

    private void showMagicBoxImageDialog() {
        String[] imageNames = {"magic_box_01", "magic_box_02", "magic_box_03", "magic_box_04", "magic_box_05"};
        int[] imageResources = {R.drawable.magic_box_01, R.drawable.magic_box_02, R.drawable.magic_box_03, R.drawable.magic_box_04, R.drawable.magic_box_05};

        LinearLayout gridLayout = new LinearLayout(this);
        gridLayout.setOrientation(LinearLayout.VERTICAL);
        gridLayout.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

        int columns = 2;
        LinearLayout currentRow = null;

        AlertDialog.Builder builder = new AlertDialog.Builder(CreateListingActivity.this)
                .setTitle(R.string.choose_magic_box_image)
                .setNegativeButton(R.string.cancel, null);

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
                Toast.makeText(CreateListingActivity.this, R.string.image_selected, Toast.LENGTH_SHORT).show();
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
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog dialog = new TimePickerDialog(CreateListingActivity.this,
                (view, selectedHour, selectedMinute) -> {
                    String time = formatTime(selectedHour, selectedMinute);

                    if (!discountEnd.isEmpty()) {
                        int start = convertTimeToMinutes(time);
                        int end = convertTimeToMinutes(discountEnd);
                        if (start >= end) {
                            Toast.makeText(CreateListingActivity.this, R.string.discount_start_before_end, Toast.LENGTH_LONG).show();
                            return;
                        }
                    }

                    storeDiscountPeriod();
                    discountStart = time;
                    buttonDiscountStart.setText(String.format(getString(R.string.discount_start_label), time));

                    if (!validateExistingRules()) {
                        updateDiscountRules();
                    }

                    updateDiscountRules();
                }, hour, minute, false);
        dialog.show();
    }

    private void showDiscountEndPicker() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog dialog = new TimePickerDialog(CreateListingActivity.this,
                (view, selectedHour, selectedMinute) -> {
                    String time = formatTime(selectedHour, selectedMinute);

                    if (!discountStart.isEmpty()) {
                        int start = convertTimeToMinutes(discountStart);
                        int end = convertTimeToMinutes(time);
                        if (end <= start) {
                            Toast.makeText(CreateListingActivity.this, R.string.discount_end_after_start, Toast.LENGTH_LONG).show();
                            return;
                        }
                    }

                    storeDiscountPeriod();
                    discountEnd = time;
                    buttonDiscountEnd.setText(String.format(getString(R.string.discount_end_label), time));

                    if (!validateExistingRules()) {
                        updateDiscountRules();
                    }

                    updateDiscountRules();
                }, hour, minute, false);
        dialog.show();
    }

    private void showDiscountRuleDialog(int editIndex) {
        LinearLayout layout = new LinearLayout(CreateListingActivity.this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = dpToPx(20);
        layout.setPadding(padding, padding, padding, padding);

        Button startButton = new Button(CreateListingActivity.this);
        startButton.setText(R.string.select_rule_start);
        layout.addView(startButton);

        Button endButton = new Button(CreateListingActivity.this);
        endButton.setText(R.string.select_rule_end);
        LinearLayout.LayoutParams endParams = new LinearLayout.LayoutParams(-1, -2);
        endParams.topMargin = dpToPx(8);
        layout.addView(endButton, endParams);

        EditText discountInput = new EditText(CreateListingActivity.this);
        discountInput.setHint(R.string.discount_percentage_hint);
        discountInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        LinearLayout.LayoutParams discountParams = new LinearLayout.LayoutParams(-1, -2);
        discountParams.topMargin = dpToPx(12);
        layout.addView(discountInput, discountParams);

        final String[] ruleStart = {""};
        final String[] ruleEnd = {""};

        if (editIndex >= 0 && editIndex < discountRules.size()) {
            DiscountRule existingRule = discountRules.get(editIndex);
            ruleStart[0] = existingRule.startTime;
            ruleEnd[0] = existingRule.endTime;
            startButton.setText(String.format(getString(R.string.rule_start_label), existingRule.startTime));
            endButton.setText(String.format(getString(R.string.rule_end_label), existingRule.endTime));
            discountInput.setText(formatDiscount(existingRule.discountPercent));
        }

        startButton.setOnClickListener(view -> showRuleTimePicker(true, ruleStart, ruleEnd, startButton, endButton));
        endButton.setOnClickListener(view -> showRuleTimePicker(false, ruleStart, ruleEnd, startButton, endButton));

        String dialogTitle = editIndex >= 0 ? getString(R.string.edit_discount_rule) : getString(R.string.add_discount_rule_dialog);
        String positiveButtonText = editIndex >= 0 ? getString(R.string.save) : getString(R.string.add);

        AlertDialog dialog = new AlertDialog.Builder(CreateListingActivity.this)
                .setTitle(dialogTitle)
                .setView(layout)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(positiveButtonText, null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(view -> {
                if (ruleStart[0].isEmpty() || ruleEnd[0].isEmpty()) {
                    Toast.makeText(CreateListingActivity.this, R.string.select_both_times, Toast.LENGTH_SHORT).show();
                    return;
                }

                int start = convertTimeToMinutes(ruleStart[0]);
                int end = convertTimeToMinutes(ruleEnd[0]);
                int discountStartMinutes = convertTimeToMinutes(discountStart);
                int discountEndMinutes = convertTimeToMinutes(discountEnd);

                if (start >= end) {
                    Toast.makeText(CreateListingActivity.this, R.string.rule_start_before_end, Toast.LENGTH_SHORT).show();
                    return;
                }

                if (start < discountStartMinutes || end > discountEndMinutes) {
                    Toast.makeText(CreateListingActivity.this, R.string.rule_within_period, Toast.LENGTH_LONG).show();
                    return;
                }

                String discountText = discountInput.getText().toString().trim();
                if (discountText.isEmpty()) {
                    discountInput.setError(getString(R.string.enter_discount_percentage));
                    return;
                }

                double discount;
                try {
                    discount = Double.parseDouble(discountText);
                } catch (NumberFormatException e) {
                    discountInput.setError(getString(R.string.invalid_discount));
                    return;
                }

                if (discount <= 0 || discount > 100) {
                    discountInput.setError(getString(R.string.discount_range_error));
                    return;
                }

                if (hasOverlappingRule(ruleStart[0], ruleEnd[0], editIndex)) {
                    Toast.makeText(CreateListingActivity.this, R.string.overlap_error, Toast.LENGTH_LONG).show();
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
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog dialog = new TimePickerDialog(CreateListingActivity.this,
                (view, selectedHour, selectedMinute) -> {
                    String time = formatTime(selectedHour, selectedMinute);
                    int selectedMinutes = convertTimeToMinutes(time);
                    int discountStartMinutes = convertTimeToMinutes(discountStart);
                    int discountEndMinutes = convertTimeToMinutes(discountEnd);

                    if (selectedMinutes < discountStartMinutes || selectedMinutes > discountEndMinutes) {
                        Toast.makeText(CreateListingActivity.this,
                                String.format(getString(R.string.time_between), discountStart, discountEnd),
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (isStart) {
                        ruleStart[0] = time;
                        startButton.setText(String.format(getString(R.string.rule_start_label), time));
                    } else {
                        ruleEnd[0] = time;
                        endButton.setText(String.format(getString(R.string.rule_end_label), time));
                    }
                }, hour, minute, false);
        dialog.show();
    }

    private boolean hasOverlappingRule(String newStartTime, String newEndTime, int editIndex) {
        int newStart = convertTimeToMinutes(newStartTime);
        int newEnd = convertTimeToMinutes(newEndTime);

        for (int i = 0; i < discountRules.size(); i++) {
            if (i == editIndex) continue;
            DiscountRule existingRule = discountRules.get(i);
            int existingStart = convertTimeToMinutes(existingRule.startTime);
            int existingEnd = convertTimeToMinutes(existingRule.endTime);
            if (newStart < existingEnd && newEnd > existingStart) {
                return true;
            }
        }
        return false;
    }

    private void updateDiscountRules() {
        layoutDiscountRules.removeAllViews();

        if (discountRules.isEmpty()) {
            TextView emptyText = new TextView(CreateListingActivity.this);
            emptyText.setText(R.string.no_discount_rules);
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
        LinearLayout ruleLayout = new LinearLayout(CreateListingActivity.this);
        ruleLayout.setOrientation(LinearLayout.HORIZONTAL);
        ruleLayout.setGravity(Gravity.CENTER_VERTICAL);
        ruleLayout.setPadding(dpToPx(14), dpToPx(10), dpToPx(6), dpToPx(10));
        ruleLayout.setBackgroundResource(R.drawable.bg_discount_rule);

        LinearLayout infoLayout = new LinearLayout(CreateListingActivity.this);
        infoLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(0, -2, 1);
        ruleLayout.addView(infoLayout, infoParams);

        TextView timeText = new TextView(CreateListingActivity.this);
        timeText.setText(String.format(getString(R.string.time_range_format), rule.startTime, rule.endTime));
        timeText.setTextSize(16);
        timeText.setTextColor(getColor(R.color.foodshare_text));
        timeText.setTypeface(null, android.graphics.Typeface.BOLD);
        infoLayout.addView(timeText);

        TextView discountText = new TextView(CreateListingActivity.this);
        discountText.setText(String.format(getString(R.string.discount_percent_format), formatDiscount(rule.discountPercent)));
        discountText.setTextSize(14);
        discountText.setTextColor(getColor(R.color.foodshare_green));
        LinearLayout.LayoutParams discountParams = new LinearLayout.LayoutParams(-2, -2);
        discountParams.topMargin = dpToPx(3);
        infoLayout.addView(discountText, discountParams);

        ImageButton editButton = new ImageButton(CreateListingActivity.this);
        editButton.setImageResource(R.drawable.ic_edit);
        editButton.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        editButton.setContentDescription(getString(R.string.edit_discount_rule));
        editButton.setPadding(dpToPx(10), dpToPx(10), dpToPx(10), dpToPx(10));
        LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(dpToPx(48), dpToPx(48));
        ruleLayout.addView(editButton, editParams);
        editButton.setOnClickListener(view -> showDiscountRuleDialog(index));

        ImageButton deleteButton = new ImageButton(CreateListingActivity.this);
        deleteButton.setImageResource(R.drawable.ic_delete);
        deleteButton.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        deleteButton.setContentDescription(getString(R.string.delete_discount_rule));
        deleteButton.setPadding(dpToPx(10), dpToPx(10), dpToPx(10), dpToPx(10));
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(dpToPx(48), dpToPx(48));
        ruleLayout.addView(deleteButton, deleteParams);
        deleteButton.setOnClickListener(view -> confirmDeleteRule(index));

        LinearLayout.LayoutParams ruleParams = new LinearLayout.LayoutParams(-1, -2);
        ruleParams.topMargin = dpToPx(8);
        layoutDiscountRules.addView(ruleLayout, ruleParams);
    }

    private void confirmDeleteRule(int index) {
        new AlertDialog.Builder(CreateListingActivity.this)
                .setTitle(R.string.delete_discount_rule)
                .setMessage(R.string.delete_confirm_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    if (index >= 0 && index < discountRules.size()) {
                        discountRules.remove(index);
                        updateDiscountRules();
                    }
                })
                .show();
    }

    private void createListing() {
        String description = editDescription.getText().toString().trim();
        String priceText = editPrice.getText().toString().trim();
        String quantityText = editQuantity.getText().toString().trim();

        if (priceText.isEmpty()) {
            editPrice.setError(getString(R.string.enter_price));
            editPrice.requestFocus();
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceText);
        } catch (NumberFormatException e) {
            editPrice.setError(getString(R.string.invalid_price));
            editPrice.requestFocus();
            return;
        }

        if (price <= 0) {
            editPrice.setError(getString(R.string.price_positive));
            editPrice.requestFocus();
            return;
        }

        if (quantityText.isEmpty()) {
            editQuantity.setError(getString(R.string.enter_quantity));
            editQuantity.requestFocus();
            return;
        }

        int quantity;
        try {
            quantity = Integer.parseInt(quantityText);
        } catch (NumberFormatException e) {
            editQuantity.setError(getString(R.string.invalid_quantity));
            editQuantity.requestFocus();
            return;
        }

        if (quantity <= 0) {
            editQuantity.setError(getString(R.string.quantity_positive));
            editQuantity.requestFocus();
            return;
        }

        if (selectedImageUri == null) {
            Toast.makeText(CreateListingActivity.this, R.string.choose_image_error, Toast.LENGTH_LONG).show();
            return;
        }

        if (discountStart.isEmpty() || discountEnd.isEmpty()) {
            Toast.makeText(CreateListingActivity.this, R.string.select_period_error, Toast.LENGTH_LONG).show();
            return;
        }

        if (discountRules.isEmpty()) {
            Toast.makeText(CreateListingActivity.this, R.string.add_rule_error, Toast.LENGTH_LONG).show();
            return;
        }

        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(CreateListingActivity.this, R.string.login_again, Toast.LENGTH_LONG).show();
            return;
        }

        buttonCreateListing.setEnabled(false);
        buttonCreateListing.setText(R.string.creating_listing);
        buttonCancel.setEnabled(false);

        String vendorId = firebaseAuth.getCurrentUser().getUid();
        String listingId = firestore.collection("listings").document().getId();

        Map<String, Object> listing = new HashMap<>();
        listing.put("listingId", listingId);
        listing.put("vendorId", vendorId);
        // Every vendor listing represents one standard Magic Box.
        listing.put("foodName", "Magic Box");
        listing.put("description", description);
        listing.put("originalPrice", price);
        listing.put("quantity", quantity);
        listing.put("availableQuantity", quantity);
        listing.put("imageName", "");
        listing.put("discountStart", discountStart);
        listing.put("discountEnd", discountEnd);

        ArrayList<Map<String, Object>> rules = new ArrayList<>();
        for (DiscountRule rule : discountRules) {
            Map<String, Object> ruleData = new HashMap<>();
            ruleData.put("startTime", rule.startTime);
            ruleData.put("endTime", rule.endTime);
            ruleData.put("discountPercent", rule.discountPercent);
            rules.add(ruleData);
        }
        listing.put("discountRules", rules);

        listing.put("status", "ACTIVE");
        listing.put("createdAt", FieldValue.serverTimestamp());

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
                    resetCreateButton();
                    Toast.makeText(CreateListingActivity.this,
                            String.format(getString(R.string.image_upload_failed),
                                    "Cloudinary did not return an image URL."),
                            Toast.LENGTH_LONG).show();
                    return;
                }
                listing.put("imageUrl", secureUrl.toString());
                    firestore.collection("listings").document(listingId)
                            .set(listing)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(CreateListingActivity.this, R.string.listing_created, Toast.LENGTH_LONG).show();
                                finish();
                            })
                            .addOnFailureListener(exception -> {
                                resetCreateButton();
                                Toast.makeText(CreateListingActivity.this,
                                        String.format(getString(R.string.listing_failed), exception.getMessage()),
                                        Toast.LENGTH_LONG).show();
                            });
            }

            @Override
            public void onError(String requestId, ErrorInfo error) {
                resetCreateButton();
                Toast.makeText(CreateListingActivity.this,
                        String.format(getString(R.string.image_upload_failed), error.getDescription()),
                        Toast.LENGTH_LONG).show();
            }

            @Override
            public void onReschedule(String requestId, ErrorInfo error) {
            }
        });
    }

    private void resetCreateButton() {
        buttonCreateListing.setEnabled(true);
        buttonCreateListing.setText(R.string.create_listing_button);
        buttonCancel.setEnabled(true);
    }

    private String formatDiscount(double discount) {
        if (discount == (int) discount) {
            return String.valueOf((int) discount);
        }
        return String.valueOf(discount);
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
