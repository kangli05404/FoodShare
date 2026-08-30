package com.example.foodshare.ui.consumer.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.foodshare.R;
import com.example.foodshare.database.CartItem;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Locale;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    public interface OnCartActionListener {
        void onIncrease(CartItem item);
        void onDecrease(CartItem item);
        void onItemClick(CartItem item);
    }

    private final List<CartItem> cartItems;
    private final OnCartActionListener listener;

    public CartAdapter(List<CartItem> cartItems, OnCartActionListener listener) {
        this.cartItems = cartItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem item = cartItems.get(position);

        holder.textFoodName.setText(item.foodName);
        holder.textPrice.setText(String.format(Locale.getDefault(), "RM %.2f", item.price));
        holder.textQuantity.setText(String.valueOf(item.quantity));
        holder.textVendorName.setText("FoodShare Partner");
        holder.itemView.setTag(item.id);

        if (item.vendorId != null && !item.vendorId.isEmpty()) {
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(item.vendorId)
                    .get()
                    .addOnSuccessListener(document -> {
                        Object boundItemId = holder.itemView.getTag();
                        if (!(boundItemId instanceof Integer) || (Integer) boundItemId != item.id) {
                            return;
                        }
                        String vendorName = document.getString("name");
                        holder.textVendorName.setText(
                                vendorName == null || vendorName.isEmpty()
                                        ? "FoodShare Partner" : vendorName);
                    });
        }

        if (item.imageUrl != null && !item.imageUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(item.imageUrl)
                    .placeholder(R.drawable.magic_box_01)
                    .into(holder.imageCartItem);
        } else {
            holder.imageCartItem.setImageResource(R.drawable.magic_box_01);
        }

        holder.buttonIncrease.setOnClickListener(v -> listener.onIncrease(item));
        holder.buttonDecrease.setOnClickListener(v -> listener.onDecrease(item));
        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    static class CartViewHolder extends RecyclerView.ViewHolder {
        ImageView imageCartItem;
        TextView textFoodName, textVendorName, textPrice, textQuantity, buttonDecrease;
        ImageButton buttonIncrease;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            imageCartItem = itemView.findViewById(R.id.imageCartItem);
            textFoodName = itemView.findViewById(R.id.textCartFoodName);
            textVendorName = itemView.findViewById(R.id.textCartVendorName);
            textPrice = itemView.findViewById(R.id.textCartPrice);
            textQuantity = itemView.findViewById(R.id.textCartQuantity);
            buttonIncrease = itemView.findViewById(R.id.buttonIncrease);
            buttonDecrease = itemView.findViewById(R.id.buttonDecrease);
        }
    }
}
