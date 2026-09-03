package com.example.foodshare.model;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class Listing {
    private String listingId;
    private String vendorId;
    private String foodName;
    private String category;
    private String description;
    private double originalPrice;
    private int quantity;
    private int availableQuantity;
    private String imageName;
    private String imageUrl;
    private List<Map<String, Object>> discountRules;
    private Date createdAt;

    public Listing() {}

    public String getListingId() { return listingId; }
    public void setListingId(String listingId) { this.listingId = listingId; }

    public String getVendorId() { return vendorId; }
    public void setVendorId(String vendorId) { this.vendorId = vendorId; }

    public String getFoodName() { return foodName; }
    public void setFoodName(String foodName) { this.foodName = foodName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(double originalPrice) { this.originalPrice = originalPrice; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public int getAvailableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(int availableQuantity) { this.availableQuantity = availableQuantity; }

    public String getImageName() { return imageName; }
    public void setImageName(String imageName) { this.imageName = imageName; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public List<Map<String, Object>> getDiscountRules() { return discountRules; }
    public void setDiscountRules(List<Map<String, Object>> discountRules) { this.discountRules = discountRules; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}