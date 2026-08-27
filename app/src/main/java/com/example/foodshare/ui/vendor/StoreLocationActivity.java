package com.example.foodshare.ui.vendor;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.foodshare.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
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

public class StoreLocationActivity extends AppCompatActivity
        implements OnMapReadyCallback {

    private static final LatLng KUALA_LUMPUR = new LatLng(3.1390, 101.6869);

    private GoogleMap googleMap;
    private Marker selectedMarker;
    private LatLng selectedLocation;
    private LocationManager locationManager;
    private ActivityResultLauncher<String[]> locationPermissionLauncher;
    private Button useCurrentButton;
    private Button saveButton;
    private final Handler locationHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService geocoderExecutor = Executors.newSingleThreadExecutor();

    private final Runnable locationTimeout = () -> {
        stopLocationUpdates();
        useCurrentButton.setEnabled(true);
        useCurrentButton.setText(R.string.use_current_location);
        Toast.makeText(this, R.string.location_unavailable, Toast.LENGTH_LONG).show();
    };

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            stopLocationUpdates();
            useCurrentButton.setEnabled(true);
            useCurrentButton.setText(R.string.use_current_location);
            moveToLocation(new LatLng(location.getLatitude(), location.getLongitude()));
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store_location);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        useCurrentButton = findViewById(R.id.buttonUseCurrentLocation);
        saveButton = findViewById(R.id.buttonSaveStoreLocation);

        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    boolean granted = Boolean.TRUE.equals(
                            result.get(Manifest.permission.ACCESS_FINE_LOCATION))
                            || Boolean.TRUE.equals(
                            result.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                    if (granted) {
                        enableMyLocation();
                        useCurrentLocation();
                    } else {
                        Toast.makeText(this, R.string.location_permission_required,
                                Toast.LENGTH_LONG).show();
                    }
                });

        useCurrentButton.setOnClickListener(v -> requestLocationPermissionOrUse());
        saveButton.setOnClickListener(v -> saveSelectedLocation());

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.storeMap);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        loadSavedLocation();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.setOnMapClickListener(this::moveToLocation);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                selectedLocation == null ? KUALA_LUMPUR : selectedLocation, 12f));
        if (selectedLocation != null) {
            addMarker(selectedLocation);
        }
        enableMyLocation();
    }

    private void loadSavedLocation() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore.getInstance().collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    Double lat = snapshot.getDouble("storeLatitude");
                    Double lng = snapshot.getDouble("storeLongitude");
                    if (lat != null && lng != null) {
                        selectedLocation = new LatLng(lat, lng);
                        if (googleMap != null) {
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                    selectedLocation, 16f));
                            addMarker(selectedLocation);
                        }
                    }
                });
    }

    private void requestLocationPermissionOrUse() {
        boolean fine = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (fine || coarse) {
            useCurrentLocation();
        } else {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void enableMyLocation() {
        if (googleMap == null) return;
        boolean fine = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (!fine && !coarse) return;
        try {
            googleMap.setMyLocationEnabled(true);
        } catch (SecurityException ignored) {
            // Permission can be revoked while the map is being created.
        }
    }

    private void useCurrentLocation() {
        boolean fine = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (!fine && !coarse) return;

        useCurrentButton.setEnabled(false);
        useCurrentButton.setText(R.string.getting_location);

        try {
            Location best = null;
            if (fine && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                best = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
            if (best == null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                best = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            if (best != null) {
                useCurrentButton.setEnabled(true);
                useCurrentButton.setText(R.string.use_current_location);
                moveToLocation(new LatLng(best.getLatitude(), best.getLongitude()));
                return;
            }

            boolean requested = false;
            if (fine && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        1000L, 1f, locationListener, getMainLooper());
                requested = true;
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                        1000L, 1f, locationListener, getMainLooper());
                requested = true;
            }
            if (!requested) {
                locationTimeout.run();
            } else {
                locationHandler.postDelayed(locationTimeout, 15000L);
            }
        } catch (SecurityException e) {
            locationTimeout.run();
        }
    }

    private void moveToLocation(@NonNull LatLng location) {
        selectedLocation = location;
        addMarker(location);
        if (googleMap != null) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 16f));
        }
    }

    private void addMarker(@NonNull LatLng location) {
        if (googleMap == null) return;
        if (selectedMarker != null) selectedMarker.remove();
        selectedMarker = googleMap.addMarker(new MarkerOptions()
                .position(location)
                .title(getString(R.string.store_map_title)));
    }

    private void saveSelectedLocation() {
        if (selectedLocation == null) {
            Toast.makeText(this, R.string.store_map_hint, Toast.LENGTH_LONG).show();
            return;
        }

        saveButton.setEnabled(false);
        double lat = selectedLocation.latitude;
        double lng = selectedLocation.longitude;
        String fallback = String.format(Locale.getDefault(), "%.6f, %.6f", lat, lng);

        geocoderExecutor.execute(() -> {
            String address = fallback;
            if (Geocoder.isPresent()) {
                try {
                    Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                    @SuppressWarnings("deprecation")
                    List<Address> results = geocoder.getFromLocation(lat, lng, 1);
                    if (results != null && !results.isEmpty()
                            && results.get(0).getAddressLine(0) != null) {
                        address = results.get(0).getAddressLine(0);
                    }
                } catch (IOException ignored) {
                    // Coordinates remain a valid store location.
                }
            }
            String finalAddress = address;
            runOnUiThread(() -> saveToFirestore(lat, lng, finalAddress));
        });
    }

    private void saveToFirestore(double lat, double lng, String address) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            saveButton.setEnabled(true);
            return;
        }
        Map<String, Object> updates = new HashMap<>();
        updates.put("storeLatitude", lat);
        updates.put("storeLongitude", lng);
        updates.put("storeAddress", address);
        updates.put("storeLocationUpdatedAt", FieldValue.serverTimestamp());
        FirebaseFirestore.getInstance().collection("users")
                .document(user.getUid())
                .update(updates)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, R.string.location_saved, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    saveButton.setEnabled(true);
                    Toast.makeText(this,
                            getString(R.string.location_save_failed,
                                    e.getMessage() == null ? "Unknown error" : e.getMessage()),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void stopLocationUpdates() {
        locationHandler.removeCallbacks(locationTimeout);
        if (locationManager == null) return;
        try {
            locationManager.removeUpdates(locationListener);
        } catch (SecurityException ignored) {
        }
    }

    @Override
    protected void onDestroy() {
        stopLocationUpdates();
        geocoderExecutor.shutdownNow();
        super.onDestroy();
    }
}
