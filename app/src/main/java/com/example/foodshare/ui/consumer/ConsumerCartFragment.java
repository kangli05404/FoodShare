package com.example.foodshare.ui.consumer;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodshare.R;
import com.example.foodshare.database.CartDao;
import com.example.foodshare.database.CartDatabase;
import com.example.foodshare.database.CartItem;
import com.example.foodshare.ui.consumer.adapter.CartAdapter;
import com.example.foodshare.ui.orders.CheckoutActivity;
import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConsumerCartFragment extends Fragment {

    private RecyclerView recyclerCart;
    private TextView textCartTotal;
    private MaterialButton buttonCheckout;
    private List<CartItem> currentCartItems;
    private CartDao cartDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.activity_cart, container, false);
        View oldBottomNav = root.findViewById(R.id.bottomNav);
        if (oldBottomNav != null) oldBottomNav.setVisibility(View.GONE);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerCart = view.findViewById(R.id.recyclerCart);
        textCartTotal = view.findViewById(R.id.textCartTotal);
        buttonCheckout = view.findViewById(R.id.buttonCheckout);
        cartDao = CartDatabase.getInstance(requireContext().getApplicationContext()).cartDao();
        recyclerCart.setLayoutManager(new LinearLayoutManager(requireContext()));

        buttonCheckout.setOnClickListener(v -> {
            if (currentCartItems == null || currentCartItems.isEmpty()) {
                Toast.makeText(requireContext(), "Your cart is empty!", Toast.LENGTH_SHORT).show();
                return;
            }
            startActivity(new Intent(requireContext(), CheckoutActivity.class));
        });

        loadCartItems();
    }

    private void loadCartItems() {
        executor.execute(() -> {
            List<CartItem> items = cartDao.getAllCartItems();
            currentCartItems = items;
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) return;
                CartAdapter adapter = new CartAdapter(items, new CartAdapter.OnCartActionListener() {
                    @Override public void onIncrease(CartItem item) {
                        item.quantity += 1;
                        updateItem(item);
                    }
                    @Override public void onDecrease(CartItem item) {
                        if (item.quantity > 1) {
                            item.quantity -= 1;
                            updateItem(item);
                        } else {
                            deleteItem(item);
                        }
                    }
                    @Override public void onItemClick(CartItem item) {
                        Intent intent = new Intent(requireContext(), ListingDetailsActivity.class);
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
            if (isAdded()) requireActivity().runOnUiThread(this::loadCartItems);
        });
    }

    private void deleteItem(CartItem item) {
        executor.execute(() -> {
            cartDao.delete(item);
            if (isAdded()) requireActivity().runOnUiThread(this::loadCartItems);
        });
    }

    private void calculateTotal(List<CartItem> items) {
        double total = 0;
        for (CartItem item : items) total += item.price * item.quantity;
        textCartTotal.setText(String.format(Locale.getDefault(), "RM %.2f", total));
    }

    @Override
    public void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
