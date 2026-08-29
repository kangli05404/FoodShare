package com.example.foodshare.ui.vendor;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

public class VendorOrderAdapter extends RecyclerView.Adapter<VendorOrderAdapter.OrderViewHolder> {
    public interface OnOrderActionListener {
        void onActionClick(DocumentSnapshot order);
    }

    private final List<DocumentSnapshot> orders;
    private final OnOrderActionListener listener;

    public VendorOrderAdapter(List<DocumentSnapshot> orders, OnOrderActionListener listener) {
        this.orders = orders;
        this.listener = listener;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_vendor_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        DocumentSnapshot order = orders.get(position);

        String orderId = order.getId();
        String itemName = order.getString("itemName");
        String status = order.getString("status");
        String imageUrl = order.getString("imageUrl");
        String pickupTime = order.getString("pickUpTime");
        String payment = order.getString("payment");
        String paymentMethod = order.getString("paymentMethod");

        Long quantityValue = order.getLong("quantity");
        int quantity = quantityValue == null ? 1 : quantityValue.intValue();

        Double total = order.getDouble("totalNetPrice");

        if (total == null) {
            total = order.getDouble("totalAmount");
        }

        if (total == null) {
            total = 0.0;
        }

        String shortOrderId = orderId.length() > 6 ? orderId.substring(0, 6).toUpperCase(Locale.getDefault()) : orderId.toUpperCase(Locale.getDefault());

        holder.textOrderId.setText("Order #" + shortOrderId);
        holder.textItemName.setText(itemName == null || itemName.isEmpty() ? "Surprise Box" : itemName);
        holder.textQuantity.setText("Quantity: " + quantity);
        holder.textTotal.setText(String.format(Locale.getDefault(), "Total: RM %.2f", total));
        holder.textPickupTime.setText("Pickup: " + (pickupTime == null || pickupTime.isEmpty() ? "Not available" : pickupTime));
        holder.textStatus.setText(formatStatus(status));

        String paymentText = payment == null || payment.isEmpty() ? "Unknown" : payment;

        if (paymentMethod != null && !paymentMethod.isEmpty()) {
            paymentText += " • " + paymentMethod;
        }

        holder.textPayment.setText("Payment: " + paymentText);

        Timestamp timestamp = order.getTimestamp("timestamp");

        if (timestamp == null) {
            timestamp = order.getTimestamp("createdAt");
        }

        if (timestamp != null) {
            SimpleDateFormat format = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
            holder.textOrderTime.setText(format.format(timestamp.toDate()));
        } else {
            holder.textOrderTime.setText("Order time not available");
        }

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.magic_box_01)
                    .into(holder.imageOrder);
        } else {
            holder.imageOrder.setImageResource(R.drawable.magic_box_01);
        }

        setupActionButton(holder.buttonOrderAction, order, status);
    }

    private void setupActionButton(Button button, DocumentSnapshot order, String status) {
        if (isUpcomingStatus(status)) {
            button.setVisibility(View.VISIBLE);
            button.setText("Mark Ready");
            button.setOnClickListener(v -> listener.onActionClick(order));
            return;
        }

        if (isReadyStatus(status)) {
            button.setVisibility(View.VISIBLE);
            button.setText("Complete Order");
            button.setOnClickListener(v -> listener.onActionClick(order));
            return;
        }

        button.setVisibility(View.GONE);
        button.setOnClickListener(null);
    }

    private boolean isUpcomingStatus(String status) {
        if (status == null) return false;

        return status.equalsIgnoreCase("Upcoming")
                || status.equalsIgnoreCase("PENDING")
                || status.equalsIgnoreCase("CONFIRMED");
    }

    private boolean isReadyStatus(String status) {
        if (status == null) return false;

        return status.equalsIgnoreCase("READY")
                || status.equalsIgnoreCase("Ready to Pick Up")
                || status.equalsIgnoreCase("READY_FOR_PICKUP");
    }

    private String formatStatus(String status) {
        if (status == null || status.isEmpty()) {
            return "UNKNOWN";
        }

        if (isUpcomingStatus(status)) {
            return "UPCOMING";
        }

        if (isReadyStatus(status)) {
            return "READY FOR PICKUP";
        }

        if (status.equalsIgnoreCase("COMPLETED")) {
            return "COMPLETED";
        }

        if (status.equalsIgnoreCase("CANCELED")) {
            return "CANCELED";
        }

        return status.toUpperCase(Locale.getDefault());
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        ImageView imageOrder;
        TextView textOrderId;
        TextView textItemName;
        TextView textStatus;
        TextView textQuantity;
        TextView textTotal;
        TextView textPickupTime;
        TextView textPayment;
        TextView textOrderTime;
        Button buttonOrderAction;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);

            imageOrder = itemView.findViewById(R.id.imageOrder);
            textOrderId = itemView.findViewById(R.id.textOrderId);
            textItemName = itemView.findViewById(R.id.textItemName);
            textStatus = itemView.findViewById(R.id.textOrderStatus);
            textQuantity = itemView.findViewById(R.id.textOrderQuantity);
            textTotal = itemView.findViewById(R.id.textOrderTotal);
            textPickupTime = itemView.findViewById(R.id.textPickupTime);
            textPayment = itemView.findViewById(R.id.textPayment);
            textOrderTime = itemView.findViewById(R.id.textOrderTime);
            buttonOrderAction = itemView.findViewById(R.id.buttonOrderAction);
        }
    }
}