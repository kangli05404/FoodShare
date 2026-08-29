package com.example.foodshare.ui.consumer;

import android.content.Intent;
import android.os.Bundle;
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
import com.example.foodshare.ui.profile.ProfileActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.example.foodshare.ui.orders.OrderHistoryActivity;

import java.util.ArrayList;
import java.util.List;

public class ConsumerHomeActivity extends AppCompatActivity {

    private RecyclerView recyclerListings;
    private ListingAdapter adapter;
    private final List<Listing> listingList = new ArrayList<>();
    private FirebaseFirestore db;
    private TextInputEditText editSearch;
    private BottomNavigationView bottomNav;

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

        adapter = new ListingAdapter(this, listingList, listing -> {
            Intent intent = new Intent(ConsumerHomeActivity.this, ListingDetailsActivity.class);
            intent.putExtra("listingId", listing.getListingId());
            startActivity(intent);
        });
        recyclerListings.setAdapter(adapter);

        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                return true; // already here
            } else if (id == R.id.nav_orders) {
                startActivity(new Intent(ConsumerHomeActivity.this, OrderHistoryActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_cart) {
                startActivity(new Intent(ConsumerHomeActivity.this, CartActivity.class));
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(ConsumerHomeActivity.this, ProfileActivity.class));
                return true;
            }
            return false;
        });

        loadActiveListings();
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

    private void loadActiveListings() {
        db.collection("listings")
                .whereEqualTo("status", "ACTIVE")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    listingList.clear();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        Listing listing = document.toObject(Listing.class);
                        listing.setListingId(document.getId());
                        listingList.add(listing);
                    }
                    adapter.updateFullList(listingList);
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load listings: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}