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
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class VendorOrderAdapter extends RecyclerView.Adapter<VendorOrderAdapter.OrderViewHolder> {
    public interface OnOrderActionListener {
        void onActionClick(DocumentSnapshot order);
    }

    private final List<DocumentSnapshot> orders;
    private final OnOrderActionListener listener;
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private final Map<String, CustomerContact> contactCache = new HashMap<>();

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
        bindCustomerContact(holder, order);

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
                    .placeholder(R.drawable.ic_food_placeholder)
                    .into(holder.imageOrder);
        } else {
            holder.imageOrder.setImageResource(R.drawable.ic_food_placeholder);
        }

        setupActionButton(holder.buttonOrderAction, order, status);
    }

    private void bindCustomerContact(OrderViewHolder holder, DocumentSnapshot order) {
        String userId = order.getString("userId");
        String customerName = order.getString("consumerName");
        String customerPhone = order.getString("consumerPhone");
        String customerEmail = order.getString("consumerEmail");

        holder.layoutCustomerContact.setTag(order.getId());

        if (hasContactDetails(customerName, customerPhone, customerEmail)) {
            showCustomerContact(holder,
                    new CustomerContact(customerName, customerPhone, customerEmail));
            return;
        }

        CustomerContact cachedContact = contactCache.get(userId);
        if (cachedContact != null) {
            showCustomerContact(holder, cachedContact);
            return;
        }

        if (userId == null || userId.trim().isEmpty()) {
            showUnavailableContact(holder);
            return;
        }

        holder.textCustomerName.setText("Customer: Loading...");
        holder.textCustomerPhone.setText("Phone: Loading...");
        holder.textCustomerEmail.setText("Email: Loading...");
        holder.textCustomerPhone.setVisibility(View.VISIBLE);
        holder.textCustomerEmail.setVisibility(View.VISIBLE);

        String boundOrderId = order.getId();
        firestore.collection("users").document(userId).get()
                .addOnSuccessListener(snapshot -> {
                    CustomerContact contact = new CustomerContact(
                            snapshot.getString("name"),
                            snapshot.getString("phone"),
                            snapshot.getString("email"));
                    contactCache.put(userId, contact);

                    if (boundOrderId.equals(holder.layoutCustomerContact.getTag())) {
                        showCustomerContact(holder, contact);
                    }
                })
                .addOnFailureListener(error -> {
                    if (boundOrderId.equals(holder.layoutCustomerContact.getTag())) {
                        showUnavailableContact(holder);
                    }
                });
    }

    private boolean hasContactDetails(String name, String phone, String email) {
        return isNotBlank(name) || isNotBlank(phone) || isNotBlank(email);
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private void showCustomerContact(OrderViewHolder holder, CustomerContact contact) {
        holder.textCustomerName.setText("Customer: "
                + (isNotBlank(contact.name) ? contact.name : "Not provided"));
        holder.textCustomerPhone.setText("Phone: "
                + (isNotBlank(contact.phone) ? contact.phone : "Not provided"));
        holder.textCustomerEmail.setText("Email: "
                + (isNotBlank(contact.email) ? contact.email : "Not provided"));
        holder.textCustomerPhone.setVisibility(View.VISIBLE);
        holder.textCustomerEmail.setVisibility(View.VISIBLE);
    }

    private void showUnavailableContact(OrderViewHolder holder) {
        holder.textCustomerName.setText("Customer details unavailable");
        holder.textCustomerPhone.setVisibility(View.GONE);
        holder.textCustomerEmail.setVisibility(View.GONE);
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
            return "CANCELLED";
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
        View layoutCustomerContact;
        TextView textCustomerName;
        TextView textCustomerPhone;
        TextView textCustomerEmail;
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
            layoutCustomerContact = itemView.findViewById(R.id.layoutCustomerContact);
            textCustomerName = itemView.findViewById(R.id.textCustomerName);
            textCustomerPhone = itemView.findViewById(R.id.textCustomerPhone);
            textCustomerEmail = itemView.findViewById(R.id.textCustomerEmail);
            buttonOrderAction = itemView.findViewById(R.id.buttonOrderAction);
        }
    }

    private static class CustomerContact {
        final String name;
        final String phone;
        final String email;

        CustomerContact(String name, String phone, String email) {
            this.name = name;
            this.phone = phone;
            this.email = email;
        }
    }
}
