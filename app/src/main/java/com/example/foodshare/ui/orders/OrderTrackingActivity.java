package com.example.foodshare.ui.orders;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.foodshare.R;
import com.example.foodshare.ui.consumer.ConsumerMainActivity;
import com.example.foodshare.ui.vendor.StoreLocationActivity;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class OrderTrackingActivity extends AppCompatActivity {

    private TextView tvItemName, tvVendorName, tvRestaurantLocation, tvQuantity, tvSubtotal, tvDiscount, tvFinalTotal, tvOrderTime, tvPaymentMethod, tvPaymentStatus;
    private ImageView ivOrderImage, btnBack;
    private Button buttonBackToOrders, btnTrackMap, btnCancelOrder;

    private FirebaseFirestore db;
    private String orderId;
    private boolean fromCheckout = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_tracking);

        // Bind views
        tvItemName = findViewById(R.id.tvItemName);
        tvVendorName = findViewById(R.id.tvVendorName);
        tvRestaurantLocation = findViewById(R.id.tvRestaurantLocation);
        tvQuantity = findViewById(R.id.tvQuantity);
        tvSubtotal = findViewById(R.id.tvSubtotal);
        tvDiscount = findViewById(R.id.tvDiscount);
        tvFinalTotal = findViewById(R.id.tvFinalTotal);
        tvPaymentMethod = findViewById(R.id.tvPaymentMethod);
        tvPaymentStatus = findViewById(R.id.tvPaymentStatus);
        ivOrderImage = findViewById(R.id.ivOrderImage);
        tvOrderTime = findViewById(R.id.tvOrderTime);
        btnBack = findViewById(R.id.btnBack);
        btnTrackMap = findViewById(R.id.btnTrackMap);
        buttonBackToOrders = findViewById(R.id.buttonBackToOrders);
        btnCancelOrder = findViewById(R.id.btnCancelOrder);

        db = FirebaseFirestore.getInstance();

        // Get extras passed from intent
        orderId = getIntent().getStringExtra("ORDER_ID");
        fromCheckout = getIntent().getBooleanExtra("FROM_CHECKOUT", false);

        // Set initial state for the map button so user knows it's loading
        if (btnTrackMap != null) {
            btnTrackMap.setText("Loading Location...");
            btnTrackMap.setEnabled(false);
        }

        if (orderId != null && !orderId.isEmpty()) {
            loadOrderDetails();
        } else {
            Toast.makeText(this, "Error: Order ID not found", Toast.LENGTH_SHORT).show();
        }

        // Top back button: Cart page if from checkout, otherwise normal finish
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (fromCheckout) {
                    Intent intent = new Intent(OrderTrackingActivity.this, ConsumerMainActivity.class);
                    intent.putExtra(ConsumerMainActivity.EXTRA_SELECTED_TAB, R.id.nav_cart);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    finish();
                }
            });
        }

        // Bottom action button: Home if from checkout, Orders list if from history
        if (buttonBackToOrders != null) {
            if (fromCheckout) {
                buttonBackToOrders.setText("Back to Home");
                buttonBackToOrders.setOnClickListener(v -> {
                    Intent intent = new Intent(OrderTrackingActivity.this, ConsumerMainActivity.class);
                    intent.putExtra(ConsumerMainActivity.EXTRA_SELECTED_TAB, R.id.nav_home);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                });
            } else {
                buttonBackToOrders.setText("Back to Orders");
                buttonBackToOrders.setOnClickListener(v -> finish());
            }
        }
    }

    private void loadOrderDetails() {
        db.collection("orders").document(orderId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String itemName = documentSnapshot.getString("itemName");
                        Long quantityLong = documentSnapshot.getLong("quantity");
                        int quantity = (quantityLong != null) ? quantityLong.intValue() : 1;

                        Double subtotal = documentSnapshot.getDouble("subtotal");
                        Double discountPercent = documentSnapshot.getDouble("discountPercent");
                        Double totalNetPrice = documentSnapshot.getDouble("totalNetPrice");
                        String paymentMethod = documentSnapshot.getString("paymentMethod");
                        String paymentStatus = documentSnapshot.getString("payment");
                        String imageUrl = documentSnapshot.getString("imageUrl");
                        String currentStatus = documentSnapshot.getString("status");

                        // Get vendorId directly from the order document
                        String vendorId = documentSnapshot.getString("vendorId");

                        Timestamp timestamp = documentSnapshot.getTimestamp("timestamp");
                        if (timestamp == null) {
                            timestamp = documentSnapshot.getTimestamp("createdAt");
                        }

                        final String formattedTime;
                        if (timestamp != null) {
                            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
                            formattedTime = sdf.format(timestamp.toDate());
                        } else {
                            String dateString = documentSnapshot.getString("timestamp");
                            if (dateString == null) dateString = documentSnapshot.getString("createdAt");

                            if (dateString != null && !dateString.isEmpty()) {
                                formattedTime = dateString;
                            } else {
                                formattedTime = "Time not available";
                            }
                        }

                        runOnUiThread(() -> {
                            if (tvItemName != null && itemName != null) tvItemName.setText(itemName);
                            if (tvQuantity != null) tvQuantity.setText("x" + quantity);
                            if (tvOrderTime != null) tvOrderTime.setText(formattedTime);
                            if (tvSubtotal != null && subtotal != null) tvSubtotal.setText(String.format(Locale.getDefault(), "RM %.2f", subtotal));
                            if (tvDiscount != null && discountPercent != null) tvDiscount.setText(String.format(Locale.getDefault(), "%.0f%%", discountPercent));
                            if (tvFinalTotal != null && totalNetPrice != null) tvFinalTotal.setText(String.format(Locale.getDefault(), "RM %.2f", totalNetPrice));
                            if (tvPaymentMethod != null && paymentMethod != null) tvPaymentMethod.setText(paymentMethod);
                            if (tvPaymentStatus != null && paymentStatus != null) tvPaymentStatus.setText(paymentStatus);
                            if (ivOrderImage != null && imageUrl != null && !imageUrl.isEmpty()) {
                                Glide.with(this).load(imageUrl).placeholder(R.drawable.magic_box_01).into(ivOrderImage);
                            }

                            // Fetch vendor location directly using vendorId
                            if (btnTrackMap != null) {
                                if (vendorId != null && !vendorId.isEmpty()) {
                                    fetchVendorLocation(vendorId);
                                } else {
                                    android.util.Log.d("MapDebug", "vendorId is null or empty in order document.");
                                    btnTrackMap.setText("Location Not Available");
                                    btnTrackMap.setEnabled(false);
                                }
                            }

                            // Hide cancel button completely if coming straight from checkout, otherwise show based on status
                            if (btnCancelOrder != null) {
                                if (fromCheckout) {
                                    btnCancelOrder.setVisibility(View.GONE);
                                } else if ("PENDING".equalsIgnoreCase(currentStatus) || "CONFIRMED".equalsIgnoreCase(currentStatus) || "Upcoming".equalsIgnoreCase(currentStatus)) {
                                    btnCancelOrder.setVisibility(View.VISIBLE);
                                    btnCancelOrder.setOnClickListener(v -> cancelOrder());
                                } else {
                                    btnCancelOrder.setVisibility(View.GONE);
                                }
                            }
                        });
                    } else {
                        Toast.makeText(this, "Order details not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load order: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchVendorLocation(String vendorId) {
        db.collection("users").document(vendorId).get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        String vendorName = userDoc.getString("name");
                        if (tvVendorName != null) {
                            tvVendorName.setText(vendorName == null || vendorName.isEmpty()
                                    ? "FoodShare Partner" : vendorName);
                        }
                        Double lat = userDoc.getDouble("storeLatitude");
                        Double lng = userDoc.getDouble("storeLongitude");
                        String address = userDoc.getString("storeAddress");
                        if (tvRestaurantLocation != null && address != null && !address.isEmpty()) {
                            tvRestaurantLocation.setText("📍 " + address);
                        }

                        if (lat != null && lng != null && address != null && btnTrackMap != null) {
                            runOnUiThread(() -> {
                                btnTrackMap.setText("Open Store Map");
                                btnTrackMap.setEnabled(true);
                                btnTrackMap.setOnClickListener(v -> openMapLocation(lat, lng, address));
                            });
                        } else {
                            android.util.Log.d("MapDebug", "Store coordinates or address missing. lat=" + lat + ", lng=" + lng + ", address=" + address);
                            runOnUiThread(() -> {
                                btnTrackMap.setText("Location Not Available");
                                btnTrackMap.setEnabled(false);
                            });
                        }
                    } else {
                        android.util.Log.d("MapDebug", "Vendor user document not found for ID: " + vendorId);
                        runOnUiThread(() -> {
                            btnTrackMap.setText("Location Not Available");
                            btnTrackMap.setEnabled(false);
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("MapDebug", "Failed to fetch vendor user document", e);
                    runOnUiThread(() -> {
                        btnTrackMap.setText("Location Not Available");
                        btnTrackMap.setEnabled(false);
                    });
                });
    }

    private void openMapLocation(double latitude, double longitude, String address) {
        String uri = String.format(Locale.ENGLISH, "geo:%f,%f?q=%f,%f(%s)", latitude, longitude, latitude, longitude, android.net.Uri.encode(address));
        Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(uri));
        intent.setPackage("com.google.android.apps.maps");

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Intent genericMapIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(uri));
            startActivity(genericMapIntent);
        }
    }

    private void cancelOrder() {
        if (orderId == null || orderId.isEmpty()) {
            Toast.makeText(this, "Error: Order ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show a confirmation dialog before proceeding with cancellation
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Cancel Order")
                .setMessage("Are you sure you want to cancel this order? The item quantity will be returned to the vendor.")
                .setPositiveButton("Yes, Cancel", (dialog, which) -> executeOrderCancellation())
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void executeOrderCancellation() {
        if (btnCancelOrder != null) {
            btnCancelOrder.setEnabled(false);
        }

        db.runTransaction(transaction -> {
            com.google.firebase.firestore.DocumentReference orderRef = db.collection("orders").document(orderId);
            com.google.firebase.firestore.DocumentSnapshot orderSnapshot = transaction.get(orderRef);

            if (!orderSnapshot.exists()) {
                throw new FirebaseFirestoreException("Order not found", FirebaseFirestoreException.Code.NOT_FOUND);
            }

            String listingId = orderSnapshot.getString("listingId");
            Long quantityLong = orderSnapshot.getLong("quantity");
            int purchasedQuantity = (quantityLong != null) ? quantityLong.intValue() : 1;

            if (listingId != null && !listingId.isEmpty()) {
                com.google.firebase.firestore.DocumentReference listingRef = db.collection("listings").document(listingId);
                com.google.firebase.firestore.DocumentSnapshot listingSnapshot = transaction.get(listingRef);

                if (listingSnapshot.exists()) {
                    Long currentAvailable = listingSnapshot.getLong("availableQuantity");
                    if (currentAvailable == null) {
                        currentAvailable = listingSnapshot.getLong("quantity");
                    }

                    if (currentAvailable != null) {
                        long restoredQuantity = currentAvailable + purchasedQuantity;
                        transaction.update(listingRef, "availableQuantity", restoredQuantity);
                    }
                }
            }

            // Update order status to CANCELED and payment status to Refunded
            transaction.update(orderRef, "status", "CANCELED");
            transaction.update(orderRef, "payment", "Refunded");

            return null;
        }).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Order canceled and payment refunded", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> {
            if (btnCancelOrder != null) {
                btnCancelOrder.setEnabled(true);
            }
            Toast.makeText(this, "Failed to cancel order: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }
}
