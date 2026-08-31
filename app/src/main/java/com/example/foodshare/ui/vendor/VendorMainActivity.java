package com.example.foodshare.ui.vendor;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.widget.Toast;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.foodshare.R;
import com.example.foodshare.ui.auth.LoginActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

/** Single vendor shell. The navigation bar stays alive while fragments change above it. */
public class VendorMainActivity extends AppCompatActivity {

    private BottomNavigationView navigation;
    private TextView listingsBadge;
    private TextView ordersBadge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vendor_main);

        navigation = findViewById(R.id.vendorBottomNavigation);
        listingsBadge = findViewById(R.id.vendorListingsBadge);
        ordersBadge = findViewById(R.id.vendorOrdersBadge);
        navigation.setItemHorizontalTranslationEnabled(false);
        navigation.setItemActiveIndicatorColor(ColorStateList.valueOf(getColor(R.color.consumer_lime_light)));
        navigation.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_vendor_dashboard) {
                showFragment(new VendorDashboardFragment(), R.id.nav_vendor_dashboard);
                return true;
            }
            if (item.getItemId() == R.id.nav_vendor_listings) {
                showFragment(new VendorListingsFragment(), R.id.nav_vendor_listings);
                return true;
            }
            if (item.getItemId() == R.id.nav_vendor_orders) {
                showFragment(new VendorOrdersFragment(), R.id.nav_vendor_orders);
                return true;
            }
            if (item.getItemId() == R.id.nav_vendor_profile) {
                showFragment(new VendorProfileFragment(), R.id.nav_vendor_profile);
                return true;
            }
            return false;
        });

        navigation.post(this::positionBadges);

        if (savedInstanceState == null) {
            navigation.setSelectedItemId(R.id.nav_vendor_dashboard);
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (navigation.getSelectedItemId() != R.id.nav_vendor_dashboard) {
                    navigation.setSelectedItemId(R.id.nav_vendor_dashboard);
                } else {
                    finish();
                }
            }
        });
    }

    private void showFragment(Fragment fragment, int selectedId) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.vendorFragmentContainer, fragment)
                .commit();
    }

    public void logout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    public void updateListingsBadge(int count) {
        if (listingsBadge == null) return;
        listingsBadge.setText(String.valueOf(count));
        listingsBadge.setVisibility(count > 0 ? TextView.VISIBLE : TextView.GONE);
        listingsBadge.post(this::positionBadges);
    }

    public void updateOrdersBadge(int count) {
        if (ordersBadge == null) return;
        ordersBadge.setText(String.valueOf(count));
        ordersBadge.setVisibility(count > 0 ? TextView.VISIBLE : TextView.GONE);
        ordersBadge.post(this::positionBadges);
    }

    private void positionBadges() {
        if (navigation == null || listingsBadge == null || navigation.getWidth() <= 0) return;
        float badgeOffset = 10 * getResources().getDisplayMetrics().density;
        positionBadge(listingsBadge, 0.375f, badgeOffset);
        if (ordersBadge != null) positionBadge(ordersBadge, 0.625f, badgeOffset);
    }

    private void positionBadge(TextView badge, float itemCenterFraction, float offset) {
        float itemCenter = navigation.getWidth() * itemCenterFraction;
        badge.setX(itemCenter - badge.getWidth() / 2f + offset);
        badge.setY(3 * getResources().getDisplayMetrics().density);
    }
}
