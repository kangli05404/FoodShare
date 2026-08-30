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
import com.example.foodshare.util.TimeUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

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

    private double subtotal = 0, discountPercent = 0, finalNetTotal = 0;
    private String listingId = "", vendorId = "", selectedPaymentMethod = "Touch 'n Go", calculatedPickupTime = "";
    private String savedItemName = "", savedImageUrl = "";
    private int savedQuantity = 0;
    private boolean listingAvailableNow = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

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

        btnBack.setOnClickListener(view -> finish());

        rgPaymentMethod.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton radioButton = findViewById(checkedId);
            if (radioButton != null) selectedPaymentMethod = radioButton.getText().toString();
        });

        generateEstimatedPickupTime();
        loadCartAndFetchDiscount();
        btnConfirmOrder.setOnClickListener(view -> createOrderInFirebase());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (cartDao != null && firestore != null) loadCartAndFetchDiscount();
    }

    private void generateEstimatedPickupTime() {
        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();
        start.add(Calendar.MINUTE, 20);
        end.add(Calendar.MINUTE, 40);

        SimpleDateFormat format = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        calculatedPickupTime = format.format(start.getTime()) + " - " + format.format(end.getTime());
        tvPickUpTime.setText(calculatedPickupTime);
    }

    private void loadCartAndFetchDiscount() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<CartItem> cartItems = cartDao.getAllCartItems();

            if (cartItems == null || cartItems.isEmpty()) {
                runOnUiThread(() -> btnConfirmOrder.setEnabled(false));
                return;
            }

            CartItem firstItem = cartItems.get(0);
            listingId = firstItem.listingId;
            vendorId = firstItem.vendorId;
            subtotal = 0;
            savedQuantity = 0;

            for (CartItem item : cartItems) {
                subtotal += item.price * item.quantity;
                savedQuantity += item.quantity;
            }

            savedItemName = firstItem.foodName;
            savedImageUrl = firstItem.imageUrl;
            double itemPrice = firstItem.price;

            firestore.collection("listings").document(listingId).get()
                    .addOnSuccessListener(document -> {
                        if (!document.exists()) {
                            listingAvailableNow = false;
                            updateCheckoutDisplay(itemPrice);
                            return;
                        }

                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> rules = (List<Map<String, Object>>) document.get("discountRules");

                        Long availableQuantity = document.getLong("availableQuantity");
                        if (availableQuantity == null) availableQuantity = document.getLong("quantity");

                        discountPercent = TimeUtils.getCurrentDiscountPercent(rules);
                        listingAvailableNow = TimeUtils.isWithinDiscountRules(rules)
                                && availableQuantity != null
                                && availableQuantity >= savedQuantity;

                        updateCheckoutDisplay(itemPrice);
                    })
                    .addOnFailureListener(exception -> {
                        listingAvailableNow = false;
                        updateCheckoutDisplay(itemPrice);
                    });
        });
    }

    private void updateCheckoutDisplay(double itemPrice) {
        // CartItem.price is already the discounted price captured when the item
        // was added, so do not deduct the offer a second time at checkout.
        finalNetTotal = subtotal;

        runOnUiThread(() -> {
            tvItemName.setText(savedItemName);
            tvItemPrice.setText(String.format(Locale.getDefault(), "RM %.2f", itemPrice));
            tvItemQuantity.setText("x" + savedQuantity);
            tvSubtotal.setText(String.format(Locale.getDefault(), "RM %.2f", subtotal));
            tvDiscount.setText("Included in price");
            tvFinalTotal.setText(String.format(Locale.getDefault(), "RM %.2f", finalNetTotal));

            if (savedImageUrl != null && !savedImageUrl.isEmpty()) {
                Glide.with(this).load(savedImageUrl).placeholder(R.drawable.magic_box_01).into(ivCheckoutItem);
            }

            btnConfirmOrder.setEnabled(listingAvailableNow);
            btnConfirmOrder.setText(listingAvailableNow ? "Confirm Order" : "Not Available");
        });
    }

    private void createOrderInFirebase() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Please sign in to place an order.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (listingId.isEmpty()) {
            Toast.makeText(this, "Listing ID is missing.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnConfirmOrder.setEnabled(false);
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        firestore.runTransaction(transaction -> {
            com.google.firebase.firestore.DocumentReference listingRef = firestore.collection("listings").document(listingId);
            com.google.firebase.firestore.DocumentSnapshot listing = transaction.get(listingRef);

            if (!listing.exists()) {
                throw new FirebaseFirestoreException("Listing no longer exists.", FirebaseFirestoreException.Code.NOT_FOUND);
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rules = (List<Map<String, Object>>) listing.get("discountRules");

            if (!TimeUtils.isWithinDiscountRules(rules)) {
                throw new FirebaseFirestoreException("This listing is not available at this time.", FirebaseFirestoreException.Code.ABORTED);
            }

            double currentDiscount = TimeUtils.getCurrentDiscountPercent(rules);

            if (currentDiscount <= 0) {
                throw new FirebaseFirestoreException("No active discount rule was found.", FirebaseFirestoreException.Code.ABORTED);
            }

            Long availableQuantity = listing.getLong("availableQuantity");
            if (availableQuantity == null) availableQuantity = listing.getLong("quantity");

            if (availableQuantity == null || availableQuantity < savedQuantity) {
                throw new FirebaseFirestoreException("Not enough quantity available.", FirebaseFirestoreException.Code.ABORTED);
            }

            // The cart subtotal already contains the active offer price.
            double currentFinalTotal = subtotal;
            transaction.update(listingRef, "availableQuantity", availableQuantity - savedQuantity);

            com.google.firebase.firestore.DocumentReference orderRef = firestore.collection("orders").document();

            Map<String, Object> order = new HashMap<>();
            order.put("userId", userId);
            order.put("vendorId", vendorId);
            order.put("listingId", listingId);
            order.put("itemName", savedItemName);
            order.put("quantity", savedQuantity);
            order.put("imageUrl", savedImageUrl);
            order.put("subtotal", subtotal);
            // Keep the percentage for order history display; the amount is not
            // deducted again because the cart price is already discounted.
            order.put("discountPercent", currentDiscount);
            order.put("totalNetPrice", currentFinalTotal);
            order.put("payment", "Completed");
            order.put("paymentMethod", selectedPaymentMethod);
            order.put("status", "Upcoming");
            order.put("pickUpTime", calculatedPickupTime);
            order.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());

            transaction.set(orderRef, order);
            return orderRef.getId();
        }).addOnSuccessListener(orderId -> {
            Toast.makeText(this, "Order placed successfully!", Toast.LENGTH_SHORT).show();
            Executors.newSingleThreadExecutor().execute(() -> cartDao.clearCart());

            Intent intent = new Intent(this, OrderTrackingActivity.class);
            intent.putExtra("ORDER_ID", orderId);
            intent.putExtra("FROM_CHECKOUT", true);
            startActivity(intent);
            finish();
        }).addOnFailureListener(exception -> {
            btnConfirmOrder.setEnabled(true);
            Toast.makeText(this, "Unable to place order: " + exception.getMessage(), Toast.LENGTH_LONG).show();
            loadCartAndFetchDiscount();
        });
    }
}
