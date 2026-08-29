package com.example.foodshare.ui.orders;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.foodshare.R;
import com.example.foodshare.database.CartDao;
import com.example.foodshare.database.CartDatabase;
import com.example.foodshare.database.CartItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

public class CheckoutActivity extends AppCompatActivity {

    private TextView tvItemName, tvVendorName, tvItemPrice, tvItemQuantity, tvSubtotal, tvDiscount, tvFinalTotal, tvPickUpTime;
    private ImageView ivCheckoutItem, btnBack;
    private RadioGroup rgPaymentMethod;
    private Button btnConfirmOrder;

    private CartDao cartDao;
    private FirebaseFirestore firestore;

    private double subtotal = 0.0;
    private double discountPercent = 0.0;
    private double finalNetTotal = 0.0;
    private String listingId = "";
    private String vendorId = "";
    private String selectedPaymentMethod = "Touch 'n Go";
    private String calculatedPickupTime = "";

    // Fields to save details into Firestore order document
    private String savedItemName = "";
    private int savedQuantity = 0;
    private String savedImageUrl = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        // Bind views
        tvItemName = findViewById(R.id.tvItemName);
        tvVendorName = findViewById(R.id.tvVendorName);
        tvItemPrice = findViewById(R.id.tvItemPrice);
        tvItemQuantity = findViewById(R.id.tvItemQuantity);
        tvSubtotal = findViewById(R.id.tvSubtotal);
        tvDiscount = findViewById(R.id.tvDiscount);
        tvFinalTotal = findViewById(R.id.tvFinalTotal);
        tvPickUpTime = findViewById(R.id.tvPickUpTime);
        ivCheckoutItem = findViewById(R.id.imageCheckoutItem);
        btnBack = findViewById(R.id.btnBack);
        rgPaymentMethod = findViewById(R.id.rgPaymentMethod);
        btnConfirmOrder = findViewById(R.id.btnConfirmOrder);

        cartDao = CartDatabase.getInstance(getApplicationContext()).cartDao();
        firestore = FirebaseFirestore.getInstance();

        btnBack.setOnClickListener(v -> finish());

        rgPaymentMethod.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton rb = findViewById(checkedId);
            if (rb != null) {
                selectedPaymentMethod = rb.getText().toString();
            }
        });

        // Generate dynamic pickup range (20 to 40 minutes from now)
        generateEstimatedPickupTime();

        loadCartAndFetchDiscount();

        btnConfirmOrder.setOnClickListener(v -> createOrderInFirebase());
    }

    private void generateEstimatedPickupTime() {
        Calendar startCalendar = Calendar.getInstance();
        startCalendar.add(Calendar.MINUTE, 20); // 20 minutes from now

        Calendar endCalendar = Calendar.getInstance();
        endCalendar.add(Calendar.MINUTE, 40);  // 40 minutes from now

        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        String startTime = sdf.format(startCalendar.getTime());
        String endTime = sdf.format(endCalendar.getTime());

        calculatedPickupTime = startTime + " - " + endTime;

        if (tvPickUpTime != null) {
            tvPickUpTime.setText(calculatedPickupTime);
        }
    }

    private void loadCartAndFetchDiscount() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<CartItem> cartItems = cartDao.getAllCartItems();

            if (cartItems != null && !cartItems.isEmpty()) {
                CartItem firstItem = cartItems.get(0);
                listingId = firstItem.listingId;
                vendorId = firstItem.vendorId;

                subtotal = 0.0;
                int totalQuantity = 0;
                for (CartItem item : cartItems) {
                    subtotal += (item.price * item.quantity);
                    totalQuantity += item.quantity;
                }

                savedItemName = firstItem.foodName;
                savedQuantity = totalQuantity;
                savedImageUrl = firstItem.imageUrl;

                final double finalItemPrice = firstItem.price;

                // Query Firebase for matching listing to get discountRules
                if (listingId != null && !listingId.isEmpty()) {
                    firestore.collection("listings").document(listingId)
                            .get()
                            .addOnSuccessListener(documentSnapshot -> {
                                if (documentSnapshot.exists()) {
                                    List<Map<String, Object>> rules = (List<Map<String, Object>>) documentSnapshot.get("discountRules");
                                    if (rules != null && !rules.isEmpty()) {
                                        Object percentObj = rules.get(0).get("discountPercent");
                                        if (percentObj instanceof Number) {
                                            discountPercent = ((Number) percentObj).doubleValue();
                                        }
                                    }
                                }
                                calculateAndDisplayTotals(savedItemName, finalItemPrice, savedQuantity, savedImageUrl);
                            })
                            .addOnFailureListener(e -> {
                                calculateAndDisplayTotals(savedItemName, finalItemPrice, savedQuantity, savedImageUrl);
                            });
                } else {
                    calculateAndDisplayTotals(savedItemName, finalItemPrice, savedQuantity, savedImageUrl);
                }
            }
        });
    }

    private void calculateAndDisplayTotals(String itemName, double itemPrice, int quantity, String imageUrl) {
        double discountAmount = subtotal * (discountPercent / 100.0);
        finalNetTotal = subtotal - discountAmount;

        runOnUiThread(() -> {
            tvItemName.setText(itemName);
            tvItemPrice.setText(String.format(Locale.getDefault(), "RM %.2f", itemPrice));
            tvItemQuantity.setText("x" + quantity);

            tvSubtotal.setText(String.format(Locale.getDefault(), "RM %.2f", subtotal));
            tvDiscount.setText(String.format(Locale.getDefault(), "%.0f%%", discountPercent));
            tvFinalTotal.setText(String.format(Locale.getDefault(), "RM %.2f", finalNetTotal));

            if (ivCheckoutItem != null && imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(this)
                        .load(imageUrl)
                        .placeholder(R.drawable.magic_box_01)
                        .into(ivCheckoutItem);
            }
        });
    }

    private void createOrderInFirebase() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Please sign in to place an order.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (listingId == null || listingId.isEmpty()) {
            Toast.makeText(this, "Error: Listing ID is missing.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnConfirmOrder.setEnabled(false);

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Map<String, Object> orderMap = new HashMap<>();
        orderMap.put("userId", currentUserId);
        orderMap.put("itemName", savedItemName);
        orderMap.put("quantity", savedQuantity);
        orderMap.put("imageUrl", savedImageUrl);
        orderMap.put("payment", "Completed");
        orderMap.put("paymentMethod", selectedPaymentMethod);
        orderMap.put("subtotal", subtotal);
        orderMap.put("discountPercent", discountPercent);
        orderMap.put("totalNetPrice", finalNetTotal);
        orderMap.put("listingId", listingId);
        orderMap.put("vendorId", vendorId);
        orderMap.put("status", "Upcoming");
        orderMap.put("pickUpTime", calculatedPickupTime);
        orderMap.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());

        // Use a Firestore transaction to safely create the order and decrease the quantity at the same time
        firestore.runTransaction(transaction -> {
            com.google.firebase.firestore.DocumentReference listingRef = firestore.collection("listings").document(listingId);
            com.google.firebase.firestore.DocumentSnapshot listingSnapshot = transaction.get(listingRef);

            if (listingSnapshot.exists()) {
                Long currentAvailable = listingSnapshot.getLong("availableQuantity");
                if (currentAvailable == null) {
                    currentAvailable = listingSnapshot.getLong("quantity"); // Fallback if availableQuantity isn't set
                }

                if (currentAvailable != null) {
                    long newAvailable = currentAvailable - savedQuantity;
                    if (newAvailable < 0) newAvailable = 0; // Prevent negative inventory

                    // Update listing available quantity
                    transaction.update(listingRef, "availableQuantity", newAvailable);

                    // Optionally update status to SOLD_OUT if inventory hits 0
                    if (newAvailable == 0) {
                        transaction.update(listingRef, "status", "SOLD_OUT");
                    }
                }
            }

            // Create the order document inside the same transaction
            com.google.firebase.firestore.DocumentReference newOrderRef = firestore.collection("orders").document();
            transaction.set(newOrderRef, orderMap);

            return newOrderRef.getId();
        }).addOnSuccessListener(orderId -> {
            Toast.makeText(CheckoutActivity.this, "Order placed successfully!", Toast.LENGTH_SHORT).show();

            Executors.newSingleThreadExecutor().execute(() -> {
                cartDao.clearCart();
            });

            Intent intent = new Intent(CheckoutActivity.this, OrderTrackingActivity.class);
            intent.putExtra("ORDER_ID", orderId);
            intent.putExtra("FROM_CHECKOUT", true);
            startActivity(intent);
            finish();
        }).addOnFailureListener(e -> {
            btnConfirmOrder.setEnabled(true);
            Toast.makeText(CheckoutActivity.this, "Failed to place order: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }
}