package com.example.foodshare.ui.vendor;

import android.app.TimePickerDialog;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.foodshare.R;
import com.example.foodshare.model.DiscountRule;
import com.example.foodshare.util.TimeUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DiscountRuleManager {
    private final AppCompatActivity activity;
    private final LinearLayout layoutDiscountRules;
    private final ArrayList<DiscountRule> discountRules = new ArrayList<>();

    public DiscountRuleManager(AppCompatActivity activity, LinearLayout layoutDiscountRules) {
        this.activity = activity;
        this.layoutDiscountRules = layoutDiscountRules;
        updateDiscountRules();
    }

    public void showAddRuleDialog() {
        showDiscountRuleDialog(-1);
    }

    private void showDiscountRuleDialog(int editIndex) {
        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = dpToPx(20);
        layout.setPadding(padding, padding, padding, padding);

        Button startButton = new Button(activity);
        startButton.setText("Select Start Time");
        layout.addView(startButton);

        Button endButton = new Button(activity);
        endButton.setText("Select End Time");
        LinearLayout.LayoutParams endParams = new LinearLayout.LayoutParams(-1, -2);
        endParams.topMargin = dpToPx(8);
        layout.addView(endButton, endParams);

        EditText discountInput = new EditText(activity);
        discountInput.setHint("Discount Percentage");
        discountInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        LinearLayout.LayoutParams discountParams = new LinearLayout.LayoutParams(-1, -2);
        discountParams.topMargin = dpToPx(12);
        layout.addView(discountInput, discountParams);

        String[] ruleStart = {""};
        String[] ruleEnd = {""};

        if (editIndex >= 0 && editIndex < discountRules.size()) {
            DiscountRule rule = discountRules.get(editIndex);
            ruleStart[0] = rule.getStartTime();
            ruleEnd[0] = rule.getEndTime();
            startButton.setText("Start: " + rule.getStartTime());
            endButton.setText("End: " + rule.getEndTime());
            discountInput.setText(TimeUtils.formatDiscount(rule.getDiscountPercent()));
        }

        startButton.setOnClickListener(view -> showTimePicker(true, ruleStart, ruleEnd, startButton, endButton));
        endButton.setOnClickListener(view -> showTimePicker(false, ruleStart, ruleEnd, startButton, endButton));

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(editIndex >= 0 ? "Edit Discount Rule" : "Add Discount Rule")
                .setView(layout)
                .setNegativeButton("Cancel", null)
                .setPositiveButton(editIndex >= 0 ? "Save" : "Add", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            if (ruleStart[0].isEmpty() || ruleEnd[0].isEmpty()) {
                Toast.makeText(activity, "Please select both start and end time.", Toast.LENGTH_SHORT).show();
                return;
            }

            int start = TimeUtils.convertTimeToMinutes(ruleStart[0]);
            int end = TimeUtils.convertTimeToMinutes(ruleEnd[0]);

            if (start < 0 || end < 0 || start >= end) {
                Toast.makeText(activity, "Start time must be before end time.", Toast.LENGTH_SHORT).show();
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
            } catch (NumberFormatException exception) {
                discountInput.setError("Invalid discount");
                return;
            }

            if (discount <= 0 || discount > 100) {
                discountInput.setError("Discount must be between 1 and 100");
                return;
            }

            if (hasOverlappingRule(ruleStart[0], ruleEnd[0], editIndex)) {
                Toast.makeText(activity, "Discount rules cannot overlap.", Toast.LENGTH_LONG).show();
                return;
            }

            DiscountRule newRule = new DiscountRule(ruleStart[0], ruleEnd[0], discount);

            if (editIndex >= 0) discountRules.set(editIndex, newRule);
            else discountRules.add(newRule);

            sortDiscountRules();
            updateDiscountRules();
            dialog.dismiss();
        }));

        dialog.show();
    }

    private void showTimePicker(boolean isStart, String[] ruleStart, String[] ruleEnd, Button startButton, Button endButton) {
        Calendar calendar = Calendar.getInstance();

        new TimePickerDialog(activity, (view, hour, minute) -> {
            String time = TimeUtils.formatTime(hour, minute);

            if (isStart) {
                ruleStart[0] = time;
                startButton.setText("Start: " + time);
            } else {
                ruleEnd[0] = time;
                endButton.setText("End: " + time);
            }
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show();
    }

    private boolean hasOverlappingRule(String newStartTime, String newEndTime, int editIndex) {
        int newStart = TimeUtils.convertTimeToMinutes(newStartTime);
        int newEnd = TimeUtils.convertTimeToMinutes(newEndTime);

        for (int i = 0; i < discountRules.size(); i++) {
            if (i == editIndex) continue;

            DiscountRule rule = discountRules.get(i);
            int existingStart = TimeUtils.convertTimeToMinutes(rule.getStartTime());
            int existingEnd = TimeUtils.convertTimeToMinutes(rule.getEndTime());

            if (newStart < existingEnd && newEnd > existingStart) return true;
        }

        return false;
    }

    public boolean hasContinuousCoverage() {
        if (discountRules.isEmpty()) return false;

        ArrayList<DiscountRule> sortedRules = new ArrayList<>(discountRules);
        sortedRules.sort((rule1, rule2) -> Integer.compare(
                TimeUtils.convertTimeToMinutes(rule1.getStartTime()),
                TimeUtils.convertTimeToMinutes(rule2.getStartTime())
        ));

        for (int i = 1; i < sortedRules.size(); i++) {
            int previousEnd = TimeUtils.convertTimeToMinutes(sortedRules.get(i - 1).getEndTime());
            int currentStart = TimeUtils.convertTimeToMinutes(sortedRules.get(i).getStartTime());

            if (previousEnd != currentStart) return false;
        }

        return true;
    }

    private void sortDiscountRules() {
        discountRules.sort((rule1, rule2) -> Integer.compare(
                TimeUtils.convertTimeToMinutes(rule1.getStartTime()),
                TimeUtils.convertTimeToMinutes(rule2.getStartTime())
        ));
    }

    private void updateDiscountRules() {
        layoutDiscountRules.removeAllViews();

        if (discountRules.isEmpty()) {
            TextView emptyText = new TextView(activity);
            emptyText.setText("No discount rules added.");
            emptyText.setTextSize(14);
            emptyText.setTextColor(activity.getColor(R.color.foodshare_text_secondary));
            layoutDiscountRules.addView(emptyText);
            return;
        }

        for (int i = 0; i < discountRules.size(); i++) addRuleView(i, discountRules.get(i));
    }

    private void addRuleView(int index, DiscountRule rule) {
        LinearLayout ruleLayout = new LinearLayout(activity);
        ruleLayout.setOrientation(LinearLayout.HORIZONTAL);
        ruleLayout.setGravity(Gravity.CENTER_VERTICAL);
        ruleLayout.setPadding(dpToPx(14), dpToPx(10), dpToPx(6), dpToPx(10));
        ruleLayout.setBackgroundResource(R.drawable.bg_discount_rule);

        LinearLayout infoLayout = new LinearLayout(activity);
        infoLayout.setOrientation(LinearLayout.VERTICAL);
        ruleLayout.addView(infoLayout, new LinearLayout.LayoutParams(0, -2, 1));

        TextView timeText = new TextView(activity);
        timeText.setText(rule.getStartTime() + " - " + rule.getEndTime());
        timeText.setTextSize(16);
        timeText.setTextColor(activity.getColor(R.color.foodshare_text));
        timeText.setTypeface(null, android.graphics.Typeface.BOLD);
        infoLayout.addView(timeText);

        TextView discountText = new TextView(activity);
        discountText.setText(TimeUtils.formatDiscount(rule.getDiscountPercent()) + "% off");
        discountText.setTextSize(14);
        discountText.setTextColor(activity.getColor(R.color.foodshare_green));
        LinearLayout.LayoutParams discountParams = new LinearLayout.LayoutParams(-2, -2);
        discountParams.topMargin = dpToPx(3);
        infoLayout.addView(discountText, discountParams);

        ImageButton editButton = new ImageButton(activity);
        editButton.setImageResource(R.drawable.ic_edit);
        editButton.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        editButton.setContentDescription("Edit discount rule");
        editButton.setPadding(dpToPx(10), dpToPx(10), dpToPx(10), dpToPx(10));
        editButton.setOnClickListener(view -> showDiscountRuleDialog(index));
        ruleLayout.addView(editButton, new LinearLayout.LayoutParams(dpToPx(48), dpToPx(48)));

        ImageButton deleteButton = new ImageButton(activity);
        deleteButton.setImageResource(R.drawable.ic_delete);
        deleteButton.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        deleteButton.setContentDescription("Delete discount rule");
        deleteButton.setPadding(dpToPx(10), dpToPx(10), dpToPx(10), dpToPx(10));
        deleteButton.setOnClickListener(view -> confirmDeleteRule(index));
        ruleLayout.addView(deleteButton, new LinearLayout.LayoutParams(dpToPx(48), dpToPx(48)));

        LinearLayout.LayoutParams ruleParams = new LinearLayout.LayoutParams(-1, -2);
        ruleParams.topMargin = dpToPx(8);
        layoutDiscountRules.addView(ruleLayout, ruleParams);
    }

    private void confirmDeleteRule(int index) {
        new AlertDialog.Builder(activity)
                .setTitle("Delete Discount Rule?")
                .setMessage("Are you sure you want to delete this discount rule?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (index >= 0 && index < discountRules.size()) {
                        discountRules.remove(index);
                        updateDiscountRules();
                    }
                })
                .show();
    }

    public void setDiscountRules(List<DiscountRule> rules) {
        discountRules.clear();
        if (rules != null) discountRules.addAll(rules);
        sortDiscountRules();
        updateDiscountRules();
    }

    public boolean hasRules() {
        return !discountRules.isEmpty();
    }

    public boolean hasAnyData() {
        return !discountRules.isEmpty();
    }

    public ArrayList<Map<String, Object>> getRulesAsMaps() {
        ArrayList<Map<String, Object>> result = new ArrayList<>();

        for (DiscountRule rule : discountRules) {
            Map<String, Object> map = new HashMap<>();
            map.put("startTime", rule.getStartTime());
            map.put("endTime", rule.getEndTime());
            map.put("discountPercent", rule.getDiscountPercent());
            result.add(map);
        }

        return result;
    }

    private int dpToPx(int dp) {
        return (int) (dp * activity.getResources().getDisplayMetrics().density + 0.5f);
    }
}