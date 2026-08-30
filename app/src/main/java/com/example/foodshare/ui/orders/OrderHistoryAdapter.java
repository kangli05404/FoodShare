package com.example.foodshare.ui.orders;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.foodshare.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class OrderHistoryAdapter extends RecyclerView.Adapter<OrderHistoryAdapter.OrderViewHolder> {

    private List<DocumentSnapshot> orderList;

    public OrderHistoryAdapter(List<DocumentSnapshot> orderList) {
        this.orderList = orderList;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_card, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        DocumentSnapshot doc = orderList.get(position);
        String orderId = doc.getId();

        String itemName = doc.getString("itemName");
        String status = doc.getString("status");
        String imageUrl = doc.getString("imageUrl");

        Double price = doc.getDouble("totalNetPrice");
        if (price == null) {
            price = doc.getDouble("totalAmount");
        }
        if (price == null) {
            price = 0.0;
        }

        holder.tvCardItemName.setText(itemName != null ? itemName : "Surprise Meal Box");
        // Keep the Firebase status as CANCELED for order filtering, but show
        // the correctly-spelled label to the consumer.
        String displayStatus = "CANCELED".equalsIgnoreCase(status)
                ? "Cancelled"
                : (status != null ? status : "PENDING");
        holder.tvCardStatus.setText(displayStatus);
        holder.tvCardPrice.setText(String.format(Locale.getDefault(), "RM %.2f", price));

        // Format and display the timestamp
        Timestamp timestamp = doc.getTimestamp("timestamp");
        if (timestamp == null) {
            timestamp = doc.getTimestamp("createdAt");
        }

        if (timestamp != null && holder.tvCardTime != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
            holder.tvCardTime.setText(sdf.format(timestamp.toDate()));
        } else if (holder.tvCardTime != null) {
            String dateString = doc.getString("timestamp");
            if (dateString == null) dateString = doc.getString("createdAt");

            if (dateString != null && !dateString.isEmpty()) {
                holder.tvCardTime.setText(dateString);
            } else {
                holder.tvCardTime.setText("Time not available");
            }
        }

        if (holder.ivCardItem != null && imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.magic_box_01)
                    .into(holder.ivCardItem);
        }

        // Open OrderTrackingActivity and pass that it came from History (FROM_CHECKOUT = false)
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), OrderTrackingActivity.class);
            intent.putExtra("ORDER_ID", orderId);
            intent.putExtra("FROM_CHECKOUT", false);
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    public static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvCardItemName, tvCardStatus, tvCardPrice, tvCardTime;
        ImageView ivCardItem;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCardItemName = itemView.findViewById(R.id.tvCardItemName);
            tvCardStatus = itemView.findViewById(R.id.tvCardStatus);
            tvCardPrice = itemView.findViewById(R.id.tvCardPrice);
            tvCardTime = itemView.findViewById(R.id.tvCardTime);
            ivCardItem = itemView.findViewById(R.id.ivCardItem);
        }
    }
}
