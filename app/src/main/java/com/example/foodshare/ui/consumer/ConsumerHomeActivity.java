package com.example.foodshare.ui.consumer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.foodshare.R;
import com.example.foodshare.ui.auth.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.example.foodshare.ui.profile.ProfileActivity;

public class ConsumerHomeActivity extends AppCompatActivity {

    private Button buttonLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consumer_home);

        Button buttonProfile = findViewById(
                R.id.buttonConsumerProfile
        );

        buttonLogout = findViewById(
                R.id.buttonConsumerLogout
        );

        buttonProfile.setOnClickListener(view -> {
            Intent intent = new Intent(
                    ConsumerHomeActivity.this,
                    ProfileActivity.class
            );

            startActivity(intent);
        });

        buttonLogout.setOnClickListener(
                view -> logout()
        );
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();

        Intent intent = new Intent(
                ConsumerHomeActivity.this,
                LoginActivity.class
        );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
    }
}