package com.example.foodshare.ui.consumer;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.animation.ValueAnimator;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import android.view.animation.DecelerateInterpolator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodshare.R;
import com.example.foodshare.database.CartDatabase;
import com.example.foodshare.database.CartItem;
import com.example.foodshare.model.Listing;
import com.example.foodshare.ui.consumer.adapter.ListingAdapter;
import com.example.foodshare.util.TimeUtils;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConsumerHomeFragment extends Fragment {

    private RecyclerView recyclerListings;
    private ListingAdapter adapter;
    private FirebaseFirestore db;
    private ListenerRegistration listingsListener;
    private final List<Listing> allListings = new ArrayList<>();
    private final List<Listing> displayedListings = new ArrayList<>();
    private final Handler timeHandler = new Handler(Looper.getMainLooper());
    private ChipGroup chipGroupCategories;
    private String selectedCategory = "All";
    private View welcomeRow;
    private ValueAnimator welcomeRowAnimator;
    private int welcomeRowHeight;
    private boolean welcomeCollapsed;
    private final ExecutorService notificationExecutor = Executors.newSingleThreadExecutor();

    private static final String[] CATEGORIES = {
            "All", "Vegetarian", "Bakery & Pastry", "Rice & Noodles", "Meat",
            "Seafood", "Dessert & Snacks", "Drinks", "Mixed Food", "Halal", "Others"
    };

    private final Runnable timeRefresh = new Runnable() {
        @Override
        public void run() {
            showAvailableListings();
            timeHandler.postDelayed(this, 60000);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.activity_consumer_home, container, false);
        View oldBottomNav = root.findViewById(R.id.bottomNav);
        if (oldBottomNav != null) oldBottomNav.setVisibility(View.GONE);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        welcomeRow = view.findViewById(R.id.consumerWelcomeRow);
        welcomeRow.post(() -> welcomeRowHeight = welcomeRow.getHeight());
        ImageButton notifications = view.findViewById(R.id.buttonHomeNotifications);
        notifications.setOnClickListener(v -> showNotificationsDialog());

        recyclerListings = view.findViewById(R.id.recyclerListings);
        TextInputEditText editSearch = view.findViewById(R.id.editSearch);
        chipGroupCategories = view.findViewById(R.id.chipGroupCategories);
        db = FirebaseFirestore.getInstance();

        recyclerListings.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerListings.addOnScrollListener(new RecyclerView.OnScrollListener() {
            private boolean collapsed;

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 6 && !collapsed) {
                    collapsed = true;
                    animateWelcomeRow(true);
                } else if (dy < -6 && collapsed) {
                    collapsed = false;
                    animateWelcomeRow(false);
                }
            }
        });
        adapter = new ListingAdapter(requireContext(), displayedListings, listing -> {
            Intent intent = new Intent(requireContext(), ListingDetailsActivity.class);
            intent.putExtra("listingId", listing.getListingId());
            startActivity(intent);
        });
        recyclerListings.setAdapter(adapter);

        setupCategoryChips();
        editSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.setSearchQuery(s.toString());
            }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    private void setupCategoryChips() {
        for (String category : CATEGORIES) {
            Chip chip = new Chip(requireContext());
            chip.setId(View.generateViewId());
            chip.setText(categoryIcon(category) + "  " + category);
            chip.setCheckable(true);
            chip.setChecked(category.equals(selectedCategory));
            int[][] checkedState = new int[][] {
                    new int[] { android.R.attr.state_checked }, new int[] {}
            };
            chip.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(
                    requireContext(), R.color.foodshare_dark_green)));
            chip.setChipBackgroundColor(new ColorStateList(checkedState, new int[] {
                    ContextCompat.getColor(requireContext(), R.color.consumer_lime),
                    ContextCompat.getColor(requireContext(), R.color.consumer_lime_light)
            }));
            chip.setChipStrokeColor(new ColorStateList(checkedState, new int[] {
                    ContextCompat.getColor(requireContext(), R.color.foodshare_dark_green),
                    ContextCompat.getColor(requireContext(), R.color.consumer_lime)
            }));
            chip.setChipStrokeWidth(dpToPx(1));
            chip.setTextSize(13f);
            chip.setMinHeight(dpToPx(40));
            chip.setEnsureMinTouchTargetSize(true);
            chip.setOnClickListener(v -> {
                selectedCategory = category;
                adapter.setCategoryFilter(category);
            });
            chipGroupCategories.addView(chip);
        }
    }

    private String categoryIcon(String category) {
        switch (category) {
            case "Vegetarian": return "🥬";
            case "Bakery & Pastry": return "🥐";
            case "Rice & Noodles": return "🍜";
            case "Meat": return "🍗";
            case "Seafood": return "🐟";
            case "Dessert & Snacks": return "🍰";
            case "Drinks": return "🥤";
            case "Mixed Food": return "🍱";
            case "Halal": return "✓";
            case "Others": return "📦";
            default: return "✨";
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void animateWelcomeRow(boolean collapse) {
        if (welcomeRow == null) return;

        if (welcomeRowHeight == 0) {
            welcomeRowHeight = welcomeRow.getHeight();
        }
        if (welcomeRowHeight == 0) return;

        if (welcomeRowAnimator != null) {
            welcomeRowAnimator.cancel();
        }

        int startHeight = welcomeRow.getLayoutParams().height;
        if (startHeight < 0) startHeight = welcomeRowHeight;
        int endHeight = collapse ? 0 : welcomeRowHeight;
        float startAlpha = collapse ? 1f : 0f;
        float endAlpha = collapse ? 0f : 1f;

        welcomeRow.setAlpha(startAlpha);
        welcomeRowAnimator = ValueAnimator.ofInt(startHeight, endHeight);
        welcomeRowAnimator.setDuration(180L);
        welcomeRowAnimator.setInterpolator(new DecelerateInterpolator());
        welcomeRowAnimator.addUpdateListener(animation -> {
            ViewGroup.LayoutParams params = welcomeRow.getLayoutParams();
            params.height = (Integer) animation.getAnimatedValue();
            welcomeRow.setLayoutParams(params);
            float progress = animation.getAnimatedFraction();
            welcomeRow.setAlpha(startAlpha + (endAlpha - startAlpha) * progress);
        });
        welcomeRowAnimator.start();
    }

    @Override
    public void onDestroyView() {
        if (welcomeRowAnimator != null) {
            welcomeRowAnimator.cancel();
            welcomeRowAnimator = null;
        }
        welcomeRow = null;
        super.onDestroyView();
    }

    private void showNotificationsDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_consumer_notifications, null);
        TextView headline = dialogView.findViewById(R.id.consumerNotificationHeadline);
        TextView message = dialogView.findViewById(R.id.consumerNotificationMessage);
        TextView orderCountView = dialogView.findViewById(R.id.consumerNotificationOrdersCount);
        TextView cartCountView = dialogView.findViewById(R.id.consumerNotificationCartCount);

        final int[] counts = {0, 0};
        updateNotificationSummary(headline, message, counts[0], counts[1]);
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Notifications")
                .setView(dialogView)
                .setPositiveButton("Done", null)
                .create();
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(requireContext().getColor(R.color.foodshare_green));
        });
        dialog.show();

        android.content.Context appContext = requireContext().getApplicationContext();
        notificationExecutor.execute(() -> {
            List<CartItem> items = CartDatabase.getInstance(appContext)
                    .cartDao().getAllCartItems();
            int cartCount = 0;
            if (items != null) {
                for (CartItem item : items) cartCount += Math.max(0, item.quantity);
            }
            int finalCartCount = cartCount;
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    counts[1] = finalCartCount;
                    updateNotificationSummary(headline, message, counts[0], counts[1]);
                    cartCountView.setText(String.valueOf(finalCartCount));
                });
            }
        });

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            message.setText("Log in to see pickup reminders and order updates.");
            return;
        }

        db.collection("orders")
                .whereEqualTo("userId", FirebaseAuth.getInstance().getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    int pickupOrders = 0;
                    for (DocumentSnapshot order : snapshot.getDocuments()) {
                        String status = order.getString("status");
                        if (isPickupStatus(status)) pickupOrders++;
                    }
                    counts[0] = pickupOrders;
                    orderCountView.setText(String.valueOf(pickupOrders));
                    updateNotificationSummary(headline, message, counts[0], counts[1]);
                });
    }

    private boolean isPickupStatus(String status) {
        if (status == null) return false;
        return status.equalsIgnoreCase("Upcoming")
                || status.equalsIgnoreCase("PENDING")
                || status.equalsIgnoreCase("CONFIRMED")
                || status.equalsIgnoreCase("READY")
                || status.equalsIgnoreCase("Ready to Pick Up")
                || status.equalsIgnoreCase("READY_FOR_PICKUP");
    }

    private void updateNotificationSummary(TextView headline, TextView message,
                                           int pickupOrders, int cartItems) {
        if (!isAdded()) return;
        if (pickupOrders == 0 && cartItems == 0) {
            headline.setText("You’re all caught up");
            message.setText("No pickup reminders or cart updates right now.");
            return;
        }

        headline.setText("A few things to check");
        StringBuilder summary = new StringBuilder();
        if (pickupOrders > 0) {
            summary.append(String.format(Locale.getDefault(), "%d pickup order%s waiting", pickupOrders,
                    pickupOrders == 1 ? " is" : "s are"));
        }
        if (cartItems > 0) {
            if (summary.length() > 0) summary.append(" and ");
            summary.append(String.format(Locale.getDefault(), "%d cart item%s saved", cartItems,
                    cartItems == 1 ? " is" : "s are"));
        }
        message.setText(summary.toString());
    }

    @Override
    public void onDestroy() {
        notificationExecutor.shutdownNow();
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
        listenForListings();
        timeHandler.post(timeRefresh);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (listingsListener != null) {
            listingsListener.remove();
            listingsListener = null;
        }
        timeHandler.removeCallbacks(timeRefresh);
    }

    private void listenForListings() {
        if (listingsListener != null) listingsListener.remove();
        listingsListener = db.collection("listings").addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Failed to load listings: "
                            + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
                return;
            }
            allListings.clear();
            if (snapshot != null) {
                for (QueryDocumentSnapshot document : snapshot) {
                    Listing listing = document.toObject(Listing.class);
                    listing.setListingId(document.getId());
                    allListings.add(listing);
                }
            }
            showAvailableListings();
        });
    }

    private void showAvailableListings() {
        if (adapter == null) return;
        List<Listing> availableListings = new ArrayList<>();
        for (Listing listing : allListings) {
            if (listing.getAvailableQuantity() > 0
                    && TimeUtils.isWithinDiscountRules(listing.getDiscountRules())) {
                availableListings.add(listing);
            }
        }
        adapter.updateFullList(availableListings);
    }
}
