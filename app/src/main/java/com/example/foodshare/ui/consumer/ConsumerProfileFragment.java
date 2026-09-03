package com.example.foodshare.ui.consumer;

import android.os.Bundle;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.foodshare.R;
import com.example.foodshare.model.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ConsumerProfileFragment extends Fragment {

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private TextView profileName, profileEmail, profilePhone, profileRole;
    private ProgressBar progressBar;
    private MaterialButton editPersonalInfoButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.activity_profile, container, false);
        root.setBackgroundColor(ContextCompat.getColor(requireContext(),
                R.color.consumer_milky));
        View header = root.findViewById(R.id.profileHeaderBackground);
        if (header != null) header.setBackgroundResource(R.drawable.bg_consumer_header);
        int lime = ContextCompat.getColor(requireContext(), R.color.consumer_lime);
        TextView role = root.findViewById(R.id.textViewProfileRole);
        if (role != null) role.setTextColor(lime);
        MaterialButton logoutButton = root.findViewById(R.id.buttonProfileLogout);
        if (logoutButton != null) logoutButton.setBackgroundTintList(ColorStateList.valueOf(lime));
        MaterialButton changePasswordButton = root.findViewById(R.id.buttonChangePassword);
        if (changePasswordButton != null) {
            changePasswordButton.setTextColor(lime);
            changePasswordButton.setStrokeColor(ColorStateList.valueOf(lime));
        }
        ImageButton backButton = root.findViewById(R.id.buttonProfileBack);
        if (backButton != null) backButton.setVisibility(View.GONE);
        View vendorActions = root.findViewById(R.id.layoutVendorAccountActions);
        if (vendorActions != null) vendorActions.setVisibility(View.GONE);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        profileName = view.findViewById(R.id.textViewProfileName);
        profileEmail = view.findViewById(R.id.textViewProfileEmail);
        profilePhone = view.findViewById(R.id.textViewProfilePhone);
        profileRole = view.findViewById(R.id.textViewProfileRole);
        progressBar = view.findViewById(R.id.progressBarProfile);
        editPersonalInfoButton = view.findViewById(R.id.buttonEditPersonalInfo);

        Button changePassword = view.findViewById(R.id.buttonChangePassword);
        Button logout = view.findViewById(R.id.buttonProfileLogout);
        if (changePassword != null) {
            changePassword.setVisibility(View.VISIBLE);
            changePassword.setOnClickListener(v -> showChangePasswordDialog());
        }
        if (editPersonalInfoButton != null) {
            editPersonalInfoButton.setOnClickListener(v -> showEditPersonalInfoDialog());
        }
        logout.setOnClickListener(v -> logout());
        loadUserProfile();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (firebaseAuth != null && profileName != null) loadUserProfile();
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) return;
        progressBar.setVisibility(View.VISIBLE);
        firestore.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(snapshot -> {
                    progressBar.setVisibility(View.GONE);
                    User user = snapshot.toObject(User.class);
                    if (user == null) {
                        Toast.makeText(requireContext(), "Profile was not found.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    profileName.setText(user.getName());
                    profileEmail.setText(user.getEmail());
                    profilePhone.setText(user.getPhone());
                    profileRole.setText(user.getRole());
                    if (editPersonalInfoButton != null) {
                        editPersonalInfoButton.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    if (isAdded()) Toast.makeText(requireContext(),
                            "Unable to load profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void showEditPersonalInfoDialog() {
        if (!isAdded()) return;

        View form = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_edit_personal_info, null, false);
        TextInputEditText nameInput = form.findViewById(R.id.editNameInput);
        TextInputEditText phoneInput = form.findViewById(R.id.editPhoneInput);
        nameInput.setText(profileName.getText());
        phoneInput.setText(profilePhone.getText());
        nameInput.setSelectAllOnFocus(true);
        phoneInput.setSelectAllOnFocus(true);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(form)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null)
                .create();

        dialog.setOnShowListener(ignored -> {
            Button saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            saveButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.consumer_lime));
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.foodshare_text_secondary));
            saveButton.setOnClickListener(v -> {
                String name = nameInput.getText().toString().trim();
                String phone = phoneInput.getText().toString().trim();

                if (name.isEmpty()) {
                    nameInput.setError("Full name is required");
                    return;
                }
                if (phone.isEmpty()) {
                    phoneInput.setError("Phone number is required");
                    return;
                }

                FirebaseUser currentUser = firebaseAuth.getCurrentUser();
                if (currentUser == null) {
                    Toast.makeText(requireContext(), "Please sign in again.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                saveButton.setEnabled(false);
                Map<String, Object> updates = new HashMap<>();
                updates.put("name", name);
                updates.put("phone", phone);

                firestore.collection("users").document(currentUser.getUid())
                        .update(updates)
                        .addOnSuccessListener(unused -> {
                            if (!isAdded()) return;
                            profileName.setText(name);
                            profilePhone.setText(phone);
                            dialog.dismiss();
                            Toast.makeText(requireContext(), "Personal information updated.",
                                    Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            saveButton.setEnabled(true);
                            if (isAdded()) {
                                Toast.makeText(requireContext(),
                                        "Unable to update profile: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            }
                        });
            });
        });
        dialog.show();
    }

    private void showChangePasswordDialog() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Toast.makeText(requireContext(), "Please sign in again.", Toast.LENGTH_SHORT).show();
            return;
        }

        View form = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_change_password, null, false);
        TextInputLayout currentPasswordLayout = form.findViewById(R.id.currentPasswordLayout);
        TextInputLayout newPasswordLayout = form.findViewById(R.id.newPasswordLayout);
        TextInputLayout confirmPasswordLayout = form.findViewById(R.id.confirmPasswordLayout);
        TextInputEditText currentPassword = form.findViewById(R.id.currentPasswordInput);
        TextInputEditText newPassword = form.findViewById(R.id.newPasswordInput);
        TextInputEditText confirmPassword = form.findViewById(R.id.confirmPasswordInput);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.change_password)
                .setView(form)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.change_password, null)
                .create();

        dialog.setOnShowListener(ignored -> {
            Button changeButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            changeButton.setTextColor(ContextCompat.getColor(
                    requireContext(), R.color.consumer_lime));
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.foodshare_text_secondary));

            changeButton.setOnClickListener(v -> {
                currentPasswordLayout.setError(null);
                newPasswordLayout.setError(null);
                confirmPasswordLayout.setError(null);

                String currentValue = currentPassword.getText().toString();
                String newValue = newPassword.getText().toString();
                String confirmValue = confirmPassword.getText().toString();

                if (currentValue.isEmpty()) {
                    currentPasswordLayout.setError(getString(R.string.current_password));
                    return;
                }
                if (newValue.length() < 6) {
                    newPasswordLayout.setError(getString(R.string.password_minimum));
                    return;
                }
                if (!newValue.equals(confirmValue)) {
                    confirmPasswordLayout.setError(getString(R.string.passwords_not_match));
                    return;
                }

                changeButton.setEnabled(false);
                AuthCredential credential = EmailAuthProvider.getCredential(
                        user.getEmail(), currentValue);
                user.reauthenticate(credential)
                        .addOnSuccessListener(unused -> user.updatePassword(newValue)
                                .addOnSuccessListener(passwordUnused -> {
                                    if (!isAdded()) return;
                                    dialog.dismiss();
                                    Toast.makeText(requireContext(),
                                            R.string.password_changed,
                                            Toast.LENGTH_LONG).show();
                                })
                                .addOnFailureListener(e -> {
                                    changeButton.setEnabled(true);
                                    if (isAdded()) {
                                        Toast.makeText(requireContext(),
                                                getString(R.string.password_change_failed,
                                                        safeMessage(e)),
                                                Toast.LENGTH_LONG).show();
                                    }
                                }))
                        .addOnFailureListener(e -> {
                            changeButton.setEnabled(true);
                            currentPasswordLayout.setError(safeMessage(e));
                        });
            });
        });
        dialog.show();
    }

    private String safeMessage(Exception exception) {
        return exception.getMessage() == null ? "Unknown error" : exception.getMessage();
    }

    private void logout() {
        firebaseAuth.signOut();
        android.content.Intent intent = new android.content.Intent(requireContext(),
                com.example.foodshare.ui.auth.LoginActivity.class);
        intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
