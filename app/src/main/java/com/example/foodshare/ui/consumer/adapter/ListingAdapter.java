package com.example.foodshare.ui.consumer.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodshare.R;
import com.example.foodshare.database.CartDatabase;
import com.example.foodshare.database.CartItem;
import com.example.foodshare.model.Listing;
import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ListingAdapter extends RecyclerView.Adapter<ListingAdapter.ListingViewHolder> {

    private final List<Listing> listings;
    private final List<Listing> listingsFull;
    private final Context context;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public interface OnListingClickListener {
        void onListingClicked(Listing listing);
    }

    private final OnListingClickListener clickListener;

    public ListingAdapter(Context context, List<Listing> listings, OnListingClickListener clickListener) {
        this.context = context;
        this.listings = listings;
        this.listingsFull = new java.util.ArrayList<>(listings);
        this.clickListener = clickListener;
    }

    public void updateFullList(List<Listing> newList) {
        listingsFull.clear();
        listingsFull.addAll(newList);
    }

    public void filter(String query) {
        listings.clear();
        if (query == null || query.trim().isEmpty()) {
            listings.addAll(listingsFull);
        } else {
            String lowerQuery = query.toLowerCase().trim();
            for (Listing listing : listingsFull) {
                if (listing.getFoodName() != null &&
                        listing.getFoodName().toLowerCase().contains(lowerQuery)) {
                    listings.add(listing);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ListingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_listing_consumer, parent, false);
        return new ListingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ListingViewHolder holder, int position) {
        Listing listing = listings.get(position);

        holder.textFoodName.setText(listing.getFoodName());
        holder.textPrice.setText(String.format(Locale.getDefault(), "RM %.2f", listing.getOriginalPrice()));
        holder.textQuantity.setText(String.format(Locale.getDefault(), "Available: %d", listing.getAvailableQuantity()));
        holder.textDiscountPeriod.setText(String.format("Collect by: %s", listing.getDiscountEnd()));

        // TODO: category field doesn't exist on Listing yet — hide or remove this view until Member 1 adds it
        holder.textCategory.setVisibility(View.GONE);

        // TODO: load actual image using Glide + Firebase Storage reference from listing.getImageName()
        // Example (once Glide dependency is added):
        // FirebaseStorage.getInstance().getReference(listing.getImageName()).getDownloadUrl()
        //     .addOnSuccessListener(uri -> Glide.with(context).load(uri).into(holder.imageListing));

        holder.buttonAddToCart.setOnClickListener(v -> {
            executor.execute(() -> {
                // Pass vendorId along with the rest of the cart fields
                CartItem cartItem = new CartItem(
                        listing.getListingId(),
                        listing.getVendorId(),
                        listing.getFoodName(),
                        listing.getOriginalPrice(),
                        1,
                        listing.getImageUrl()
                );
                CartDatabase.getInstance(context.getApplicationContext())
                        .cartDao().insert(cartItem);
            });
            Toast.makeText(context, "Added to cart", Toast.LENGTH_SHORT).show();
        });

        holder.itemView.setOnClickListener(v -> clickListener.onListingClicked(listing));
    }

    @Override
    public int getItemCount() {
        return listings.size();
    }

    static class ListingViewHolder extends RecyclerView.ViewHolder {
        TextView textFoodName, textCategory, textPrice, textQuantity, textDiscountPeriod;
        MaterialButton buttonAddToCart;

        public ListingViewHolder(@NonNull View itemView) {
            super(itemView);
            textFoodName = itemView.findViewById(R.id.textFoodName);
            textCategory = itemView.findViewById(R.id.textCategory);
            textPrice = itemView.findViewById(R.id.textPrice);
            textQuantity = itemView.findViewById(R.id.textQuantity);
            textDiscountPeriod = itemView.findViewById(R.id.textDiscountPeriod);
            buttonAddToCart = itemView.findViewById(R.id.buttonAddToCart);
        }
    }
}