package com.example.foodshare.ui.consumer;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodshare.R;
import com.example.foodshare.model.Listing;
import com.example.foodshare.ui.consumer.adapter.ListingAdapter;
import com.example.foodshare.ui.orders.OrderHistoryActivity;
import com.example.foodshare.util.TimeUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

public class ConsumerHomeActivity extends AppCompatActivity {
    private RecyclerView recyclerListings;
    private ListingAdapter adapter;
    private TextInputEditText editSearch;
    private BottomNavigationView bottomNav;
    private FirebaseFirestore db;
    private ListenerRegistration listingsListener;
    private final List<Listing> allListings = new ArrayList<>();
    private final List<Listing> displayedListings = new ArrayList<>();
    private final Handler timeHandler = new Handler(Looper.getMainLooper());
    private ChipGroup chipGroupCategories;
    private String selectedCategory = "All";

    private static final String[] CATEGORIES = {
            "All",
            "Vegetarian",
            "Bakery & Pastry",
            "Rice & Noodles",
            "Meat",
            "Seafood",
            "Dessert & Snacks",
            "Drinks",
            "Mixed Food",
            "Halal"
    };

    private void setupCategoryChips() {
        for (String category : CATEGORIES) {
            Chip chip = new Chip(this);
            chip.setText(category);
            chip.setCheckable(true);
            chip.setChecked(category.equals(selectedCategory));
            chip.setOnClickListener(v -> {
                selectedCategory = category;
                adapter.setCategoryFilter(category);
            });
            chipGroupCategories.addView(chip);
        }
    }
    private final Runnable timeRefresh = new Runnable() {
        @Override
        public void run() {
            showAvailableListings();
            timeHandler.postDelayed(this, 60000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consumer_home);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        recyclerListings = findViewById(R.id.recyclerListings);
        editSearch = findViewById(R.id.editSearch);
        bottomNav = findViewById(R.id.bottomNav);
        db = FirebaseFirestore.getInstance();

        recyclerListings.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ListingAdapter(this, displayedListings, listing -> {
            Intent intent = new Intent(this, ListingDetailsActivity.class);
            intent.putExtra("listingId", listing.getListingId());
            startActivity(intent);
        });

        recyclerListings.setAdapter(adapter);

        chipGroupCategories = findViewById(R.id.chipGroupCategories);
        setupCategoryChips();

        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.setSearchQuery(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) return true;

            if (id == R.id.nav_orders) {
                startActivity(new Intent(this, OrderHistoryActivity.class));
                bottomNav.post(() -> bottomNav.setSelectedItemId(R.id.nav_home));
                return true;
            }

            if (id == R.id.nav_cart) {
                startActivity(new Intent(this, CartActivity.class));
                bottomNav.post(() -> bottomNav.setSelectedItemId(R.id.nav_home));
                return true;
            }

            if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ConsumerProfileActivity.class)); // see Problem 2 below
                bottomNav.post(() -> bottomNav.setSelectedItemId(R.id.nav_home));
                return true;
            }

            return false;
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        listenForListings();
        timeHandler.post(timeRefresh);
    }

    @Override
    protected void onStop() {
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
                Toast.makeText(this, "Failed to load listings: " + error.getMessage(), Toast.LENGTH_SHORT).show();
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
        List<Listing> availableListings = new ArrayList<>();

        for (Listing listing : allListings) {
            if (listing.getAvailableQuantity() > 0 && TimeUtils.isWithinDiscountRules(listing.getDiscountRules())) {
                availableListings.add(listing);
            }
        }
        adapter.updateFullList(availableListings);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top_toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_cart) {
            startActivity(new Intent(this, CartActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}