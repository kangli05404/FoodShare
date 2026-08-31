package com.example.foodshare.ui.consumer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.example.foodshare.R;
import com.example.foodshare.database.CartDao;
import com.example.foodshare.database.CartDatabase;
import com.example.foodshare.database.CartItem;
import com.example.foodshare.ui.consumer.adapter.CartAdapter;
import com.example.foodshare.ui.orders.CheckoutActivity;
import com.example.foodshare.ui.profile.ProfileActivity;
import com.example.foodshare.util.TimeUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CartActivity extends AppCompatActivity {

    private RecyclerView recyclerCart;
    private TextView textCartTotal;
    private MaterialButton buttonCheckout;
    private List<CartItem> currentCartItems;
    private CartAdapter adapter;
    private CartDao cartDao;
    private FirebaseFirestore firestore;
    private BottomNavigationView bottomNav;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        recyclerCart = findViewById(R.id.recyclerCart);
        textCartTotal = findViewById(R.id.textCartTotal);
        buttonCheckout = findViewById(R.id.buttonCheckout);
        bottomNav = findViewById(R.id.bottomNav);

        cartDao = CartDatabase.getInstance(getApplicationContext()).cartDao();
        firestore = FirebaseFirestore.getInstance();
        recyclerCart.setLayoutManager(new LinearLayoutManager(this));

        // Checkout Button Click Listener
        buttonCheckout.setOnClickListener(v -> {
            if (currentCartItems == null || currentCartItems.isEmpty()) {
                Toast.makeText(this, "Your cart is empty!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Launches CheckoutActivity directly without needing extra intent puts
            // since CheckoutActivity queries the Room database directly!
            Intent intent = new Intent(CartActivity.this, CheckoutActivity.class);
            startActivity(intent);
        });

        bottomNav.setSelectedItemId(R.id.nav_cart);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                Intent intent = new Intent(CartActivity.this, ConsumerMainActivity.class);
                intent.putExtra(ConsumerMainActivity.EXTRA_SELECTED_TAB, R.id.nav_home);
                startActivity(intent);
                finish();
                return true;
            } else if (id == R.id.nav_orders) {
                Intent intent = new Intent(CartActivity.this, ConsumerMainActivity.class);
                intent.putExtra(ConsumerMainActivity.EXTRA_SELECTED_TAB, R.id.nav_orders);
                startActivity(intent);
                finish();
                return true;
            } else if (id == R.id.nav_cart) {
                return true;
            } else if (id == R.id.nav_profile) {
                Intent intent = new Intent(CartActivity.this, ConsumerMainActivity.class);
                intent.putExtra(ConsumerMainActivity.EXTRA_SELECTED_TAB, R.id.nav_profile);
                startActivity(intent);
                finish();
                return true;
            }
            return false;
        });

        loadCartItems();
    }

    private void loadCartItems() {
        executor.execute(() -> {
            List<CartItem> items = removeExpiredItems(cartDao.getAllCartItems());
            currentCartItems = items; // This populates the field and clears the warning

            runOnUiThread(() -> {
                adapter = new CartAdapter(items, new CartAdapter.OnCartActionListener() {
                    @Override
                    public void onIncrease(CartItem item) {
                        item.quantity += 1;
                        updateItem(item);
                    }

                    @Override
                    public void onDecrease(CartItem item) {
                        if (item.quantity > 1) {
                            item.quantity -= 1;
                            updateItem(item);
                        } else {
                            deleteItem(item);
                        }
                    }
                    @Override
                    public void onItemClick(CartItem item) {
                        Intent intent = new Intent(CartActivity.this, ListingDetailsActivity.class);
                        intent.putExtra("listingId", item.listingId);
                        startActivity(intent);
                    }
                });
                recyclerCart.setAdapter(adapter);
                calculateTotal(items);
            });
        });
    }

    private List<CartItem> removeExpiredItems(List<CartItem> items) {
        List<CartItem> validItems = new ArrayList<>();
        if (items == null) return validItems;

        for (CartItem item : items) {
            try {
                DocumentSnapshot listing = Tasks.await(
                        firestore.collection("listings").document(item.listingId).get());

                if (!listing.exists()) {
                    cartDao.delete(item);
                    continue;
                }

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> rules =
                        (List<Map<String, Object>>) listing.get("discountRules");

                if (!TimeUtils.isWithinDiscountRules(rules)) {
                    cartDao.delete(item);
                    continue;
                }

                validItems.add(item);
            } catch (Exception ignored) {
                // Keep the item if the listing cannot be checked temporarily.
                validItems.add(item);
            }
        }

        return validItems;
    }

    private void updateItem(CartItem item) {
        executor.execute(() -> {
            cartDao.update(item);
            runOnUiThread(this::refreshCart);
        });
    }

    private void deleteItem(CartItem item) {
        executor.execute(() -> {
            cartDao.delete(item);
            runOnUiThread(this::refreshCart);
        });
    }

    private void refreshCart() {
        loadCartItems();
    }

    private void calculateTotal(List<CartItem> items) {
        double total = 0;
        for (CartItem item : items) {
            total += item.price * item.quantity;
        }
        textCartTotal.setText(String.format(Locale.getDefault(), "RM %.2f", total));
    }
}
