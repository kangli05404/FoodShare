package com.example.foodshare.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.foodshare.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import com.example.foodshare.ui.consumer.ConsumerMainActivity;
import com.example.foodshare.ui.vendor.VendorMainActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText editTextEmail;
    private EditText editTextPassword;
    private Button buttonLogin;
    private TextView textViewCreateAccount;
    private TextView textViewForgotPassword;
    private ProgressBar progressBar;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        editTextEmail = findViewById(R.id.editTextLoginEmail);
        editTextPassword =
                findViewById(R.id.editTextLoginPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        textViewCreateAccount =
                findViewById(R.id.textViewCreateAccount);
        textViewForgotPassword =
                findViewById(R.id.textViewForgotPassword);
        progressBar = findViewById(R.id.progressBarLogin);

        buttonLogin.setOnClickListener(
                view -> loginUser()
        );

        textViewForgotPassword.setOnClickListener(
                view -> sendPasswordResetEmail()
        );

        textViewCreateAccount.setOnClickListener(view -> {
            Intent intent = new Intent(
                    LoginActivity.this,
                    RegisterActivity.class
            );

            startActivity(intent);
        });
    }

    private void sendPasswordResetEmail() {
        String email =
                editTextEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError(
                    getString(R.string.reset_email_required)
            );
            editTextEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError(
                    getString(R.string.reset_email_invalid)
            );
            editTextEmail.requestFocus();
            return;
        }

        showLoading(true);

        firebaseAuth
                .sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    showLoading(false);

                    if (task.isSuccessful()) {
                        Toast.makeText(
                                LoginActivity.this,
                                getString(R.string.reset_email_sent),
                                Toast.LENGTH_LONG
                        ).show();
                    } else {
                        Toast.makeText(
                                LoginActivity.this,
                                getString(R.string.reset_email_failed),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private void loginUser() {
        String email =
                editTextEmail.getText().toString().trim();

        String password =
                editTextPassword.getText().toString();

        if (!validateInputs(email, password)) {
            return;
        }

        showLoading(true);

        firebaseAuth
                .signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        showLoading(false);

                        String message = "Login failed.";

                        if (task.getException() != null
                                && task.getException().getMessage() != null) {
                            message = task.getException().getMessage();
                        }

                        Toast.makeText(
                                LoginActivity.this,
                                message,
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    FirebaseUser firebaseUser =
                            firebaseAuth.getCurrentUser();

                    if (firebaseUser == null) {
                        showLoading(false);

                        Toast.makeText(
                                LoginActivity.this,
                                "Unable to retrieve the account.",
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    retrieveUserRole(firebaseUser);
                });
    }

    private void retrieveUserRole(FirebaseUser firebaseUser) {
        firestore
                .collection("users")
                .document(firebaseUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    showLoading(false);

                    if (!documentSnapshot.exists()) {
                        firebaseAuth.signOut();

                        Toast.makeText(
                                LoginActivity.this,
                                "User profile was not found.",
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    String role =
                            documentSnapshot.getString("role");

                    if (role == null || role.isEmpty()) {
                        firebaseAuth.signOut();

                        Toast.makeText(
                                LoginActivity.this,
                                "User role was not found.",
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    openDashboard(role);

                    // Consumer/vendor navigation will be added next.
                })
                .addOnFailureListener(exception -> {
                    showLoading(false);
                    firebaseAuth.signOut();

                    Toast.makeText(
                            LoginActivity.this,
                            "Could not retrieve profile: "
                                    + exception.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void openDashboard(String role) {
        Intent intent;

        if ("CONSUMER".equalsIgnoreCase(role)) {
            intent = new Intent(
                    LoginActivity.this,
                    ConsumerMainActivity.class
            );
        } else if ("VENDOR".equalsIgnoreCase(role)) {
            intent = new Intent(
                    LoginActivity.this,
                    VendorMainActivity.class
            );
        } else {
            firebaseAuth.signOut();

            Toast.makeText(
                    LoginActivity.this,
                    "Invalid user role.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
    }

    private boolean validateInputs(
            String email,
            String password
    ) {
        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError("Email is required.");
            editTextEmail.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError("Enter a valid email.");
            editTextEmail.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            editTextPassword.setError("Password is required.");
            editTextPassword.requestFocus();
            return false;
        }

        return true;
    }

    private void showLoading(boolean loading) {
        progressBar.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );

        buttonLogin.setEnabled(!loading);
        textViewForgotPassword.setEnabled(!loading);
    }
}
