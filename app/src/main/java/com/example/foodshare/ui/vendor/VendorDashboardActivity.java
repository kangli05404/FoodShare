package com.example.foodshare.ui.vendor;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.foodshare.R;
import com.example.foodshare.ui.auth.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.example.foodshare.ui.profile.ProfileActivity;

public class VendorDashboardActivity extends AppCompatActivity {

    private Button buttonLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vendor_dashboard);

        Button buttonProfile = findViewById(
                R.id.buttonVendorProfile
        );

        buttonProfile.setOnClickListener(view -> {
            Intent intent = new Intent(
                    VendorDashboardActivity.this,
                    ProfileActivity.class
            );

            startActivity(intent);
        });

        buttonLogout = findViewById(
                R.id.buttonVendorLogout
        );

        buttonLogout.setOnClickListener(view -> logout());
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();

        Intent intent = new Intent(
                VendorDashboardActivity.this,
                LoginActivity.class
        );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
    }
}