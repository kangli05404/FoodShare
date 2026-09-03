package com.example.foodshare.database;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "cart_items")
public class CartItem {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @NonNull
    @ColumnInfo(name = "listing_id")
    public String listingId;

    @ColumnInfo(name = "vendor_id")
    public String vendorId;

    @ColumnInfo(name = "food_name")
    public String foodName;

    @ColumnInfo(name = "price")
    public double price;

    @ColumnInfo(name = "quantity")
    public int quantity;

    @ColumnInfo(name = "image_url")
    public String imageUrl;

    // Update your constructor to accept vendorId
    public CartItem(@NonNull String listingId, String vendorId, String foodName, double price, int quantity, String imageUrl) {
        this.listingId = listingId;
        this.vendorId = vendorId;
        this.foodName = foodName;
        this.price = price;
        this.quantity = quantity;
        this.imageUrl = imageUrl;
    }
}