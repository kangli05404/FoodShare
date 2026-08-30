package com.example.foodshare.ui.consumer;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.foodshare.R;
import com.example.foodshare.ui.orders.ConsumerOrdersFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ConsumerMainActivity extends AppCompatActivity {

    private static final String KEY_SELECTED_TAB = "selected_consumer_tab";
    public static final String EXTRA_SELECTED_TAB = "selected_consumer_tab";
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consumer_main);

        bottomNav = findViewById(R.id.consumerBottomNav);
        bottomNav.setItemActiveIndicatorColor(ColorStateList.valueOf(
                getColor(R.color.consumer_lime_light)));
        bottomNav.setOnItemSelectedListener(item -> {
            showTab(item.getItemId());
            return true;
        });

        int selectedTab;
        if (savedInstanceState != null) {
            selectedTab = savedInstanceState.getInt(KEY_SELECTED_TAB, R.id.nav_home);
        } else {
            selectedTab = getIntent().getIntExtra(EXTRA_SELECTED_TAB, R.id.nav_home);
        }
        bottomNav.setSelectedItemId(selectedTab);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (bottomNav != null) {
            int selectedTab = intent.getIntExtra(EXTRA_SELECTED_TAB, R.id.nav_home);
            bottomNav.setSelectedItemId(selectedTab);
        }
    }

    private void showTab(int id) {
        Fragment fragment;
        if (id == R.id.nav_orders) {
            fragment = new ConsumerOrdersFragment();
        } else if (id == R.id.nav_cart) {
            fragment = new ConsumerCartFragment();
        } else if (id == R.id.nav_profile) {
            fragment = new ConsumerProfileFragment();
        } else {
            fragment = new ConsumerHomeFragment();
        }

        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.consumerContent, fragment)
                .commit();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(KEY_SELECTED_TAB, bottomNav.getSelectedItemId());
        super.onSaveInstanceState(outState);
    }
}
