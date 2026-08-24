package com.example.foodshare.ui.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.foodshare.R;
import com.example.foodshare.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class RegisterActivity extends AppCompatActivity {

    private EditText editTextName;
    private EditText editTextEmail;
    private EditText editTextPhone;
    private EditText editTextPassword;
    private EditText editTextConfirmPassword;
    private RadioButton radioConsumer;
    private Button buttonRegister;
    private ProgressBar progressBar;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        editTextName = findViewById(R.id.editTextName);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPhone = findViewById(R.id.editTextPhone);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextConfirmPassword =
                findViewById(R.id.editTextConfirmPassword);
        radioConsumer = findViewById(R.id.radioConsumer);
        buttonRegister = findViewById(R.id.buttonRegister);
        progressBar = findViewById(R.id.progressBar);

        buttonRegister.setOnClickListener(
                view -> registerUser()
        );
    }

    private void registerUser() {
        String name = editTextName.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String phone = editTextPhone.getText().toString().trim();
        String password =
                editTextPassword.getText().toString().trim();
        String confirmPassword =
                editTextConfirmPassword.getText().toString().trim();

        String role = radioConsumer.isChecked()
                ? "CONSUMER"
                : "VENDOR";

        if (!validateInputs(
                name,
                email,
                phone,
                password,
                confirmPassword
        )) {
            return;
        }

        showLoading(true);

        firebaseAuth
                .createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        showLoading(false);

                        String message = "Registration failed.";

                        if (task.getException() != null
                                && task.getException().getMessage() != null) {
                            message = task.getException().getMessage();
                        }

                        Toast.makeText(
                                RegisterActivity.this,
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
                                RegisterActivity.this,
                                "Unable to retrieve the new account.",
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    saveUserProfile(
                            firebaseUser,
                            name,
                            email,
                            phone,
                            role
                    );
                });
    }

    private void saveUserProfile(
            @NonNull FirebaseUser firebaseUser,
            String name,
            String email,
            String phone,
            String role
    ) {
        String userId = firebaseUser.getUid();

        User user = new User(
                userId,
                name,
                email,
                phone,
                role
        );

        firestore
                .collection("users")
                .document(userId)
                .set(user)
                .addOnSuccessListener(unused -> {
                    showLoading(false);

                    Toast.makeText(
                            RegisterActivity.this,
                            "Account created successfully.",
                            Toast.LENGTH_SHORT
                    ).show();

                    finish();
                })
                .addOnFailureListener(exception -> {
                    showLoading(false);

                    firebaseUser.delete();

                    Toast.makeText(
                            RegisterActivity.this,
                            "Could not save profile: "
                                    + exception.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private boolean validateInputs(
            String name,
            String email,
            String phone,
            String password,
            String confirmPassword
    ) {
        if (TextUtils.isEmpty(name)) {
            editTextName.setError("Name is required.");
            editTextName.requestFocus();
            return false;
        }

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

        if (TextUtils.isEmpty(phone)) {
            editTextPhone.setError("Phone number is required.");
            editTextPhone.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            editTextPassword.setError("Password is required.");
            editTextPassword.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            editTextPassword.setError(
                    "Password must contain at least 6 characters."
            );
            editTextPassword.requestFocus();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            editTextConfirmPassword.setError(
                    "Passwords do not match."
            );
            editTextConfirmPassword.requestFocus();
            return false;
        }

        return true;
    }

    private void showLoading(boolean loading) {
        progressBar.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );

        buttonRegister.setEnabled(!loading);
    }
}