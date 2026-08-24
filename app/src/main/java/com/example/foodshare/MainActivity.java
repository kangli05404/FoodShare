package com.example.foodshare;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.foodshare.ui.auth.LoginActivity;
import com.example.foodshare.ui.consumer.ConsumerHomeActivity;
import com.example.foodshare.ui.vendor.VendorDashboardActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        checkCurrentUser();
    }

    private void checkCurrentUser() {
        FirebaseUser currentUser =
                firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            openLogin();
            return;
        }

        firestore
                .collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        firebaseAuth.signOut();
                        openLogin();
                        return;
                    }

                    String role =
                            documentSnapshot.getString("role");

                    openDashboard(role);
                })
                .addOnFailureListener(exception -> {
                    firebaseAuth.signOut();

                    Toast.makeText(
                            MainActivity.this,
                            "Unable to retrieve your profile.",
                            Toast.LENGTH_LONG
                    ).show();

                    openLogin();
                });
    }

    private void openDashboard(String role) {
        Intent intent;

        if ("CONSUMER".equalsIgnoreCase(role)) {
            intent = new Intent(
                    MainActivity.this,
                    ConsumerHomeActivity.class
            );
        } else if ("VENDOR".equalsIgnoreCase(role)) {
            intent = new Intent(
                    MainActivity.this,
                    VendorDashboardActivity.class
            );
        } else {
            firebaseAuth.signOut();

            Toast.makeText(
                    MainActivity.this,
                    "Invalid user role.",
                    Toast.LENGTH_LONG
            ).show();

            openLogin();
            return;
        }

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
    }

    private void openLogin() {
        Intent intent = new Intent(
                MainActivity.this,
                LoginActivity.class
        );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
    }
}