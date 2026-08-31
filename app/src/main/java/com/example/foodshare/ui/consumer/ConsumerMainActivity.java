package com.example.foodshare.ui.consumer;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.foodshare.R;
import com.example.foodshare.database.CartDatabase;
import com.example.foodshare.database.CartItem;
import com.example.foodshare.ui.orders.ConsumerOrdersFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConsumerMainActivity extends AppCompatActivity {

    private static final String KEY_SELECTED_TAB = "selected_consumer_tab";
    public static final String EXTRA_SELECTED_TAB = "selected_consumer_tab";
    private BottomNavigationView bottomNav;
    private TextView cartBadge;
    private final ExecutorService cartBadgeExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consumer_main);

        bottomNav = findViewById(R.id.consumerBottomNav);
        cartBadge = findViewById(R.id.consumerCartBadge);
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
        bottomNav.post(this::positionCartBadge);
        refreshCartBadge();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshCartBadge();
    }

    public void refreshCartBadge() {
        if (cartBadge == null) return;
        cartBadgeExecutor.execute(() -> {
            List<CartItem> items = CartDatabase.getInstance(getApplicationContext())
                    .cartDao().getAllCartItems();
            int count = 0;
            if (items != null) {
                for (CartItem item : items) count += Math.max(0, item.quantity);
            }
            int finalCount = count;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed() || cartBadge == null) return;
                cartBadge.setText(finalCount > 99 ? "99+" : String.valueOf(finalCount));
                cartBadge.setVisibility(finalCount > 0 ? TextView.VISIBLE : TextView.GONE);
                positionCartBadge();
            });
        });
    }

    private void positionCartBadge() {
        if (bottomNav == null || cartBadge == null || bottomNav.getWidth() <= 0) return;
        float itemCenter = bottomNav.getWidth() * 0.625f;
        float offset = 10 * getResources().getDisplayMetrics().density;
        cartBadge.setX(itemCenter - cartBadge.getWidth() / 2f + offset);
        cartBadge.setY(3 * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        cartBadgeExecutor.shutdownNow();
        super.onDestroy();
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
