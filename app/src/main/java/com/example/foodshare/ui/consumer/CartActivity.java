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
import com.example.foodshare.ui.orders.OrderHistoryActivity;
import com.example.foodshare.ui.profile.ProfileActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CartActivity extends AppCompatActivity {

    private RecyclerView recyclerCart;
    private TextView textCartTotal;
    private MaterialButton buttonCheckout;
    private List<CartItem> currentCartItems;
    private CartAdapter adapter;
    private CartDao cartDao;
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

        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());

        cartDao = CartDatabase.getInstance(getApplicationContext()).cartDao();
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
                startActivity(new Intent(CartActivity.this, ConsumerHomeActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_orders) {
                // If your teammate added OrderHistoryActivity, you can point this to it:
                startActivity(new Intent(CartActivity.this, OrderHistoryActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_cart) {
                return true; // already here
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(CartActivity.this, ProfileActivity.class));
                finish();
                return true;
            }
            return false;
        });

        loadCartItems();
    }

    private void loadCartItems() {
        executor.execute(() -> {
            List<CartItem> items = cartDao.getAllCartItems();
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