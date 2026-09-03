package com.example.foodshare.ui.consumer.adapter;

import android.content.Context;
import android.app.Activity;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.foodshare.R;
import com.example.foodshare.database.CartDatabase;
import com.example.foodshare.database.CartItem;
import com.example.foodshare.database.CartDao;
import com.example.foodshare.model.Listing;
import com.example.foodshare.ui.consumer.ConsumerMainActivity;
import com.example.foodshare.util.TimeUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ListingAdapter extends RecyclerView.Adapter<ListingAdapter.ListingViewHolder> {
    private final List<Listing> listings;
    private final List<Listing> listingsFull;
    private final Context context;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final OnListingClickListener clickListener;
    private String currentQuery = "";
    private String currentCategory = "All";

    public interface OnListingClickListener {
        void onListingClicked(Listing listing);
    }

    public ListingAdapter(Context context, List<Listing> listings, OnListingClickListener clickListener) {
        this.context = context;
        this.listings = listings;
        this.listingsFull = new ArrayList<>(listings);
        this.clickListener = clickListener;
    }

    public void setSearchQuery(String query) {
        this.currentQuery = query == null ? "" : query;
        applyFilters();
    }

    public void setCategoryFilter(String category) {
        this.currentCategory = category == null ? "All" : category;
        applyFilters();
    }

    private void applyFilters() {
        listings.clear();

        String lowerQuery = currentQuery.toLowerCase(Locale.getDefault()).trim();
        boolean categoryIsAll = currentCategory.equalsIgnoreCase("All");

        for (Listing listing : listingsFull) {
            String foodName = listing.getFoodName();
            String category = listing.getCategory();

            boolean matchesQuery = lowerQuery.isEmpty()
                    || (foodName != null && foodName.toLowerCase(Locale.getDefault()).contains(lowerQuery));

            boolean matchesCategory = categoryIsAll
                    || (category != null && category.equalsIgnoreCase(currentCategory));

            if (matchesQuery && matchesCategory) {
                listings.add(listing);
            }
        }

        notifyDataSetChanged();
    }

    // Call this whenever fresh listings arrive from Firestore, so filters re-apply to new data
    public void updateFullList(List<Listing> newList) {
        listingsFull.clear();
        listingsFull.addAll(newList);
        applyFilters();
    }

    @NonNull
    @Override
    public ListingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_listing_consumer, parent, false);
        return new ListingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ListingViewHolder holder, int position) {
        Listing listing = listings.get(position);

        holder.textFoodName.setText(listing.getFoodName() == null ? "Surprise Box" : listing.getFoodName());

        if (listing.getCategory() == null || listing.getCategory().isEmpty()) {
            holder.textCategory.setVisibility(View.GONE);
        } else {
            holder.textCategory.setVisibility(View.VISIBLE);
            holder.textCategory.setText(listing.getCategory());
        }

        double discount = TimeUtils.getCurrentDiscountPercent(listing.getDiscountRules());
        double currentPrice = TimeUtils.calculateDiscountedPrice(listing.getOriginalPrice(), discount);

        holder.textPrice.setText(String.format(Locale.getDefault(), "RM %.2f", currentPrice));
        if (discount > 0 && listing.getOriginalPrice() > currentPrice) {
            holder.textOriginalPrice.setVisibility(View.VISIBLE);
            holder.textOriginalPrice.setText(String.format(Locale.getDefault(),
                    "RM %.2f", listing.getOriginalPrice()));
            holder.textDiscountPercent.setVisibility(View.VISIBLE);
            holder.textDiscountPercent.setText(String.format(Locale.getDefault(),
                    "%s%% off", TimeUtils.formatDiscount(discount)));
        } else {
            holder.textOriginalPrice.setVisibility(View.GONE);
            holder.textOriginalPrice.setText("");
            holder.textDiscountPercent.setVisibility(View.GONE);
            holder.textDiscountPercent.setText("");
        }

        holder.textQuantity.setText(String.format(Locale.getDefault(), "Available: %d", listing.getAvailableQuantity()));

        String remainingLabel = TimeUtils.getRemainingAvailabilityLabel(listing.getDiscountRules());
        if (remainingLabel.isEmpty()) {
            holder.textTimeRemaining.clearAnimation();
            holder.textTimeRemaining.setVisibility(View.GONE);
        } else {
            holder.textTimeRemaining.setText(remainingLabel);
            holder.textTimeRemaining.setVisibility(View.VISIBLE);
            holder.textTimeRemaining.clearAnimation();
            ScaleAnimation pulse = new ScaleAnimation(
                    1f, 1.04f, 1f, 1.04f,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f);
            pulse.setDuration(1100L);
            pulse.setRepeatMode(Animation.REVERSE);
            pulse.setRepeatCount(Animation.INFINITE);
            pulse.setInterpolator(new AccelerateDecelerateInterpolator());
            holder.textTimeRemaining.startAnimation(pulse);
        }

        String start = TimeUtils.getScheduleStart(listing.getDiscountRules());
        String end = TimeUtils.getScheduleEnd(listing.getDiscountRules());
        holder.textDiscountPeriod.setText("Available: "
                + TimeUtils.formatDisplayTime(start)
                + " - " + TimeUtils.formatDisplayTime(end));

        String imageUrl = listing.getImageUrl();

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(context).load(imageUrl).placeholder(R.drawable.ic_food_placeholder).into(holder.imageListing);
        } else {
            holder.imageListing.setImageResource(R.drawable.ic_food_placeholder);
        }

        holder.buttonAddToCart.setOnClickListener(view -> {
            if (listing.getAvailableQuantity() <= 0) {
                Toast.makeText(context, "This box is sold out.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!TimeUtils.isWithinDiscountRules(listing.getDiscountRules())) {
                Toast.makeText(context, "This listing is not available at this time.", Toast.LENGTH_SHORT).show();
                return;
            }

            double discountAtAddTime = TimeUtils.getCurrentDiscountPercent(listing.getDiscountRules());
            double priceToStore = TimeUtils.calculateDiscountedPrice(listing.getOriginalPrice(), discountAtAddTime);

            executor.execute(() -> {
                CartDao cartDao = CartDatabase.getInstance(context.getApplicationContext()).cartDao();
                CartItem existing = cartDao.getByListingId(listing.getListingId());

                if (existing != null) {
                    existing.quantity += 1;
                    cartDao.update(existing);
                } else {
                    CartItem newItem = new CartItem(
                            listing.getListingId(),
                            listing.getVendorId(),
                            listing.getFoodName(),
                            priceToStore,
                            1,
                            listing.getImageUrl()
                    );
                    cartDao.insert(newItem);
                }

                if (context instanceof ConsumerMainActivity) {
                    ((ConsumerMainActivity) context).runOnUiThread(() ->
                            ((ConsumerMainActivity) context).refreshCartBadge());
                }
            });

            showAddedToCartMessage(view);
        });

        holder.itemView.setOnClickListener(view -> clickListener.onListingClicked(listing));
    }

    @Override
    public int getItemCount() {
        return listings.size();
    }

    @Override
    public void onViewRecycled(@NonNull ListingViewHolder holder) {
        holder.textTimeRemaining.clearAnimation();
        super.onViewRecycled(holder);
    }

    private void showAddedToCartMessage(View sourceView) {
        Snackbar snackbar = Snackbar.make(sourceView, "Added to cart", Snackbar.LENGTH_SHORT);

        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            View anchor = activity.findViewById(R.id.consumerBottomNav);
            if (anchor == null) {
                anchor = activity.findViewById(R.id.bottomNav);
            }
            if (anchor != null) {
                snackbar.setAnchorView(anchor);
            }
        }

        View snackbarView = snackbar.getView();
        snackbarView.setBackground(ContextCompat.getDrawable(context,
                R.drawable.bg_cart_snackbar));
        snackbarView.setElevation(8f);
        TextView message = snackbarView.findViewById(
                com.google.android.material.R.id.snackbar_text);
        if (message != null) {
            message.setTextColor(ContextCompat.getColor(context, R.color.foodshare_surface));
            message.setTextSize(15f);
            message.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        snackbar.setAction("VIEW CART", v -> {
            Intent intent = new Intent(context, ConsumerMainActivity.class);
            intent.putExtra(ConsumerMainActivity.EXTRA_SELECTED_TAB, R.id.nav_cart);
            if (!(context instanceof Activity)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(intent);
        });
        snackbar.setActionTextColor(ContextCompat.getColor(context,
                R.color.foodshare_surface));
        snackbar.show();
    }

    static class ListingViewHolder extends RecyclerView.ViewHolder {
        ImageView imageListing;
        TextView textFoodName, textCategory, textPrice, textOriginalPrice, textDiscountPercent,
                textQuantity, textDiscountPeriod, textTimeRemaining;
        MaterialButton buttonAddToCart;

        public ListingViewHolder(@NonNull View itemView) {
            super(itemView);

            imageListing = itemView.findViewById(R.id.imageListing);
            textFoodName = itemView.findViewById(R.id.textFoodName);
            textCategory = itemView.findViewById(R.id.textCategory);
            textPrice = itemView.findViewById(R.id.textPrice);
            textOriginalPrice = itemView.findViewById(R.id.textOriginalPrice);
            textDiscountPercent = itemView.findViewById(R.id.textDiscountPercent);
            textOriginalPrice.setPaintFlags(textOriginalPrice.getPaintFlags()
                    | Paint.STRIKE_THRU_TEXT_FLAG);
            textQuantity = itemView.findViewById(R.id.textQuantity);
            textDiscountPeriod = itemView.findViewById(R.id.textDiscountPeriod);
            textTimeRemaining = itemView.findViewById(R.id.textTimeRemaining);
            buttonAddToCart = itemView.findViewById(R.id.buttonAddToCart);
        }
    }
}
