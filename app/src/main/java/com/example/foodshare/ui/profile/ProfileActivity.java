package com.example.foodshare.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.foodshare.R;
import com.example.foodshare.model.User;
import com.example.foodshare.ui.auth.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import android.widget.ImageView;

public class ProfileActivity extends AppCompatActivity {

    private TextView textViewName;
    private TextView textViewEmail;
    private TextView textViewPhone;
    private TextView textViewRole;
    private ProgressBar progressBar;
    private Button buttonLogout;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        ImageView imageViewLogo = findViewById(R.id.imageViewLogo);
        imageViewLogo.setClipToOutline(true);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        textViewName = findViewById(
                R.id.textViewProfileName
        );
        textViewEmail = findViewById(
                R.id.textViewProfileEmail
        );
        textViewPhone = findViewById(
                R.id.textViewProfilePhone
        );
        textViewRole = findViewById(
                R.id.textViewProfileRole
        );
        progressBar = findViewById(
                R.id.progressBarProfile
        );
        buttonLogout = findViewById(
                R.id.buttonProfileLogout
        );

        buttonLogout.setOnClickListener(
                view -> logout()
        );

        loadUserProfile();
    }

    private void loadUserProfile() {
        FirebaseUser currentUser =
                firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            openLogin();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        firestore
                .collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    progressBar.setVisibility(View.GONE);

                    User user = documentSnapshot.toObject(
                            User.class
                    );

                    if (user == null) {
                        Toast.makeText(
                                ProfileActivity.this,
                                "Profile was not found.",
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    textViewName.setText(user.getName());
                    textViewEmail.setText(user.getEmail());
                    textViewPhone.setText(user.getPhone());
                    textViewRole.setText(user.getRole());
                })
                .addOnFailureListener(exception -> {
                    progressBar.setVisibility(View.GONE);

                    Toast.makeText(
                            ProfileActivity.this,
                            "Unable to load profile: "
                                    + exception.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void logout() {
        firebaseAuth.signOut();
        openLogin();
    }

    private void openLogin() {
        Intent intent = new Intent(
                ProfileActivity.this,
                LoginActivity.class
        );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
    }
}