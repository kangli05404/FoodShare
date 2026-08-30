package com.example.foodshare.ui.consumer;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.foodshare.R;
import com.example.foodshare.model.User;
import com.example.foodshare.ui.auth.LoginActivity;
import com.example.foodshare.ui.orders.OrderHistoryActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ConsumerProfileActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consumer_profile);

        // Hide the shared layout's back button — bottom nav replaces it here.
        View backButton = findViewById(R.id.buttonProfileBack);
        if (backButton != null) backButton.setVisibility(View.GONE);

        bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_profile);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, ConsumerHomeActivity.class));
                finish();
                return true;
            }
            if (id == R.id.nav_orders) {
                startActivity(new Intent(this, OrderHistoryActivity.class));
                bottomNav.post(() -> bottomNav.setSelectedItemId(R.id.nav_profile));
                return true;
            }
            if (id == R.id.nav_cart) {
                startActivity(new Intent(this, CartActivity.class));
                bottomNav.post(() -> bottomNav.setSelectedItemId(R.id.nav_profile));
                return true;
            }
            if (id == R.id.nav_profile) return true;
            return false;
        });

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        TextView textViewName = findViewById(R.id.textViewProfileName);
        TextView textViewEmail = findViewById(R.id.textViewProfileEmail);
        TextView textViewPhone = findViewById(R.id.textViewProfilePhone);
        TextView textViewRole = findViewById(R.id.textViewProfileRole);
        ProgressBar progressBar = findViewById(R.id.progressBarProfile);
        View buttonLogout = findViewById(R.id.buttonProfileLogout);
        View buttonChangePassword = findViewById(R.id.buttonChangePassword);

        buttonLogout.setOnClickListener(v -> logout());
        if (buttonChangePassword != null) {
            buttonChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        }

        loadUserProfile(textViewName, textViewEmail, textViewPhone, textViewRole, progressBar);
    }

    private void loadUserProfile(TextView name, TextView email, TextView phone, TextView role, ProgressBar progressBar) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            openLogin();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        firestore.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    progressBar.setVisibility(View.GONE);
                    User user = documentSnapshot.toObject(User.class);
                    if (user == null) {
                        Toast.makeText(this, "Profile was not found.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    name.setText(user.getName());
                    email.setText(user.getEmail());
                    phone.setText(user.getPhone());
                    role.setText(user.getRole());
                })
                .addOnFailureListener(exception -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Unable to load profile: " + exception.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void showChangePasswordDialog() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) return;

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int padding = dpToPx(24);
        form.setPadding(padding, dpToPx(8), padding, 0);
        EditText current = passwordField(getString(R.string.current_password));
        EditText newPassword = passwordField(getString(R.string.new_password));
        EditText confirm = passwordField(getString(R.string.confirm_new_password));
        form.addView(current);
        form.addView(newPassword);
        form.addView(confirm);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.change_password)
                .setView(form)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.change_password, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String oldValue = current.getText().toString();
                    String newValue = newPassword.getText().toString();
                    String confirmValue = confirm.getText().toString();
                    if (oldValue.isEmpty()) {
                        current.setError(getString(R.string.current_password));
                        return;
                    }
                    if (newValue.length() < 6) {
                        newPassword.setError(getString(R.string.password_minimum));
                        return;
                    }
                    if (!newValue.equals(confirmValue)) {
                        confirm.setError(getString(R.string.passwords_not_match));
                        return;
                    }

                    Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                    positive.setEnabled(false);
                    AuthCredential credential = EmailAuthProvider.getCredential(
                            user.getEmail(), oldValue);
                    user.reauthenticate(credential)
                            .addOnSuccessListener(unused -> user.updatePassword(newValue)
                                    .addOnSuccessListener(passwordUnused -> {
                                        dialog.dismiss();
                                        Toast.makeText(this, R.string.password_changed,
                                                Toast.LENGTH_LONG).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        positive.setEnabled(true);
                                        Toast.makeText(this,
                                                getString(R.string.password_change_failed,
                                                        safeMessage(e)),
                                                Toast.LENGTH_LONG).show();
                                    }))
                            .addOnFailureListener(e -> {
                                positive.setEnabled(true);
                                current.setError(safeMessage(e));
                            });
                }));
        dialog.show();
    }

    private EditText passwordField(String hint) {
        EditText field = new EditText(this);
        field.setHint(hint);
        field.setSingleLine(true);
        field.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dpToPx(8);
        field.setLayoutParams(params);
        return field;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private String safeMessage(Exception e) {
        return e.getMessage() == null ? "Unknown error" : e.getMessage();
    }

    private void logout() {
        firebaseAuth.signOut();
        openLogin();
    }

    private void openLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
