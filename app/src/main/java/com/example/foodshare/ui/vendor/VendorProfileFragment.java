package com.example.foodshare.ui.vendor;

import android.Manifest;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VendorProfileFragment extends Fragment {

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private LocationManager locationManager;
    private ActivityResultLauncher<String[]> locationPermissionLauncher;
    private final ExecutorService geocoderExecutor = Executors.newSingleThreadExecutor();
    private final Handler locationHandler = new Handler(Looper.getMainLooper());
    private final Runnable locationTimeout = () -> {
        stopLocationUpdates();
        resetLocationButton();
        if (isAdded()) {
            Toast.makeText(requireContext(), R.string.location_unavailable,
                    Toast.LENGTH_LONG).show();
        }
    };
    private TextView storeLocation;
    private Button setLocationButton;
    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            stopLocationUpdates();
            resolveAddressAndSave(location);
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    boolean fine = Boolean.TRUE.equals(
                            result.get(Manifest.permission.ACCESS_FINE_LOCATION));
                    boolean coarse = Boolean.TRUE.equals(
                            result.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                    if (fine || coarse) {
                        fetchCurrentLocation();
                    } else {
                        Toast.makeText(requireContext(),
                                R.string.location_permission_required,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable android.os.Bundle savedInstanceState) {
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

        MaterialButton setLocationButton = root.findViewById(R.id.buttonSetStoreLocation);
        if (setLocationButton != null) {
            setLocationButton.setTextColor(lime);
            setLocationButton.setStrokeColor(ColorStateList.valueOf(lime));
        }

        View oldNavigation = root.findViewById(R.id.vendorBottomNavigation);
        if (oldNavigation != null) oldNavigation.setVisibility(View.GONE);
        ImageButton back = root.findViewById(R.id.buttonProfileBack);
        if (back != null) back.setVisibility(View.GONE);
        ImageView logo = root.findViewById(R.id.imageViewLogo);
        if (logo != null) logo.setClipToOutline(true);
        View vendorActions = root.findViewById(R.id.layoutVendorAccountActions);
        if (vendorActions != null) vendorActions.setVisibility(View.VISIBLE);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable android.os.Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        locationManager = (LocationManager) requireContext()
                .getSystemService(Context.LOCATION_SERVICE);

        TextView name = view.findViewById(R.id.textViewProfileName);
        TextView email = view.findViewById(R.id.textViewProfileEmail);
        TextView phone = view.findViewById(R.id.textViewProfilePhone);
        TextView role = view.findViewById(R.id.textViewProfileRole);
        ProgressBar progress = view.findViewById(R.id.progressBarProfile);
        storeLocation = view.findViewById(R.id.textViewStoreLocation);
        setLocationButton = view.findViewById(R.id.buttonSetStoreLocation);
        Button changePassword = view.findViewById(R.id.buttonChangePassword);
        Button logout = view.findViewById(R.id.buttonProfileLogout);
        logout.setOnClickListener(v -> ((VendorMainActivity) requireActivity()).logout());
        setLocationButton.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), StoreLocationActivity.class)));
        changePassword.setOnClickListener(v -> showChangePasswordDialog());

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) return;
        progress.setVisibility(View.VISIBLE);
        firestore.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    progress.setVisibility(View.GONE);
                    User user = snapshot.toObject(User.class);
                    if (user != null) {
                        name.setText(user.getName());
                        email.setText(user.getEmail());
                        phone.setText(user.getPhone());
                        role.setText(user.getRole());
                    }
                    String address = snapshot.getString("storeAddress");
                    Double lat = snapshot.getDouble("storeLatitude");
                    Double lng = snapshot.getDouble("storeLongitude");
                    if (address != null && !address.trim().isEmpty()) {
                        storeLocation.setText(address);
                    } else if (lat != null && lng != null) {
                        storeLocation.setText(formatCoordinates(lat, lng));
                    }
                })
                .addOnFailureListener(e -> progress.setVisibility(View.GONE));
    }

    private void requestStoreLocation() {
        boolean fine = ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (fine || coarse) {
            fetchCurrentLocation();
        } else {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void fetchCurrentLocation() {
        boolean fine = ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (!fine && !coarse) return;

        setLocationButton.setEnabled(false);
        setLocationButton.setText(R.string.getting_location);
        try {
            String provider = fine
                    && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    ? LocationManager.GPS_PROVIDER
                    : LocationManager.NETWORK_PROVIDER;

            Location lastKnown = locationManager.getLastKnownLocation(provider);
            if (lastKnown != null) {
                resolveAddressAndSave(lastKnown);
                return;
            }

            if (!locationManager.isProviderEnabled(provider)) {
                resetLocationButton();
                Toast.makeText(requireContext(), R.string.location_unavailable,
                        Toast.LENGTH_LONG).show();
                return;
            }
            locationManager.requestLocationUpdates(provider, 1000L, 1f,
                    locationListener, requireActivity().getMainLooper());
            // A first GPS fix can take a while indoors, but never leave the
            // button spinning forever if the device cannot obtain one.
            locationHandler.postDelayed(locationTimeout, 15000L);
        } catch (SecurityException e) {
            resetLocationButton();
            Toast.makeText(requireContext(), R.string.location_permission_required,
                    Toast.LENGTH_LONG).show();
        } catch (IllegalArgumentException e) {
            resetLocationButton();
            Toast.makeText(requireContext(), R.string.location_unavailable,
                    Toast.LENGTH_LONG).show();
        }
    }

    private void stopLocationUpdates() {
        locationHandler.removeCallbacks(locationTimeout);
        if (locationManager == null) return;
        try {
            locationManager.removeUpdates(locationListener);
        } catch (SecurityException ignored) {
            // Permission may have been revoked while the request was running.
        }
    }

    private void resolveAddressAndSave(Location location) {
        double lat = location.getLatitude();
        double lng = location.getLongitude();
        String fallback = formatCoordinates(lat, lng);
        if (!Geocoder.isPresent()) {
            saveStoreLocation(lat, lng, fallback);
            return;
        }

        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocation(lat, lng, 1, new Geocoder.GeocodeListener() {
                @Override
                public void onGeocode(@NonNull List<Address> addresses) {
                    String label = addressLabel(addresses, fallback);
                    if (isAdded()) {
                        requireActivity().runOnUiThread(
                                () -> saveStoreLocation(lat, lng, label));
                    }
                }

                @Override
                public void onError(@Nullable String errorMessage) {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(
                                () -> saveStoreLocation(lat, lng, fallback));
                    }
                }
            });
        } else {
            geocoderExecutor.execute(() -> {
                String label = fallback;
                try {
                    @SuppressWarnings("deprecation")
                    List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
                    label = addressLabel(addresses, fallback);
                } catch (IOException ignored) {
                    // Coordinates remain available if reverse geocoding fails.
                }
                String finalLabel = label;
                if (isAdded()) {
                    requireActivity().runOnUiThread(
                            () -> saveStoreLocation(lat, lng, finalLabel));
                }
            });
        }
    }

    private String addressLabel(@Nullable List<Address> addresses, String fallback) {
        if (addresses == null || addresses.isEmpty()) return fallback;
        String line = addresses.get(0).getAddressLine(0);
        return line == null || line.trim().isEmpty() ? fallback : line;
    }

    private void saveStoreLocation(double lat, double lng, String address) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            resetLocationButton();
            return;
        }
        Map<String, Object> updates = new HashMap<>();
        updates.put("storeLatitude", lat);
        updates.put("storeLongitude", lng);
        updates.put("storeAddress", address);
        updates.put("storeLocationUpdatedAt", FieldValue.serverTimestamp());
        firestore.collection("users").document(currentUser.getUid()).update(updates)
                .addOnSuccessListener(unused -> {
                    storeLocation.setText(address);
                    resetLocationButton();
                    Toast.makeText(requireContext(), R.string.location_saved,
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    resetLocationButton();
                    Toast.makeText(requireContext(),
                            getString(R.string.location_save_failed, safeMessage(e)),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void resetLocationButton() {
        if (setLocationButton != null) {
            setLocationButton.setEnabled(true);
            setLocationButton.setText(R.string.set_current_store_location);
        }
    }

    private void showChangePasswordDialog() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) return;

        View form = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_change_password, null, false);
        TextInputLayout currentLayout = form.findViewById(R.id.currentPasswordLayout);
        TextInputLayout newPasswordLayout = form.findViewById(R.id.newPasswordLayout);
        TextInputLayout confirmLayout = form.findViewById(R.id.confirmPasswordLayout);
        TextInputEditText current = form.findViewById(R.id.currentPasswordInput);
        TextInputEditText newPassword = form.findViewById(R.id.newPasswordInput);
        TextInputEditText confirm = form.findViewById(R.id.confirmPasswordInput);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.change_password)
                .setView(form)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.change_password, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    currentLayout.setError(null);
                    newPasswordLayout.setError(null);
                    confirmLayout.setError(null);

                    String oldValue = current.getText().toString();
                    String newValue = newPassword.getText().toString();
                    String confirmValue = confirm.getText().toString();
                    if (oldValue.isEmpty()) {
                        currentLayout.setError(getString(R.string.current_password));
                        return;
                    }
                    if (newValue.length() < 6) {
                        newPasswordLayout.setError(getString(R.string.password_minimum));
                        return;
                    }
                    if (!newValue.equals(confirmValue)) {
                        confirmLayout.setError(getString(R.string.passwords_not_match));
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
                                        Toast.makeText(requireContext(),
                                                R.string.password_changed,
                                                Toast.LENGTH_LONG).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        positive.setEnabled(true);
                                        Toast.makeText(requireContext(),
                                                getString(R.string.password_change_failed,
                                                        safeMessage(e)),
                                                Toast.LENGTH_LONG).show();
                                    }))
                            .addOnFailureListener(e -> {
                                positive.setEnabled(true);
                                currentLayout.setError(safeMessage(e));
                            });
                }));
        dialog.show();
    }

    private String formatCoordinates(double lat, double lng) {
        return String.format(Locale.getDefault(), "%.6f, %.6f", lat, lng);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshStoreLocation();
    }

    private void refreshStoreLocation() {
        if (!isAdded() || firestore == null || storeLocation == null) {
            return;
        }

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        firestore.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    String address = snapshot.getString("storeAddress");
                    Double lat = snapshot.getDouble("storeLatitude");
                    Double lng = snapshot.getDouble("storeLongitude");

                    if (address != null && !address.trim().isEmpty()) {
                        storeLocation.setText(address);
                    } else if (lat != null && lng != null) {
                        storeLocation.setText(formatCoordinates(lat, lng));
                    }
                });
    }

    private String safeMessage(Exception e) {
        return e.getMessage() == null ? "Unknown error" : e.getMessage();
    }

    @Override
    public void onDestroy() {
        stopLocationUpdates();
        geocoderExecutor.shutdownNow();
        super.onDestroy();
    }
}
