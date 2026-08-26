package com.example.foodshare.model;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class Listing {
    private String listingId;
    private String vendorId;
    private String foodName;
    private String description;
    private double originalPrice;
    private int quantity;
    private int availableQuantity;
    private String imageName;
    private String discountStart;
    private String discountEnd;
    private List<Map<String, Object>> discountRules;
    private String status;
    private Date createdAt;

    public Listing() {}

    public Listing(String vendorId, String foodName, String description,
                   double originalPrice, int quantity, String imageName,
                   String discountStart, String discountEnd,
                   List<Map<String, Object>> discountRules) {
        this.vendorId = vendorId;
        this.foodName = foodName;
        this.description = description;
        this.originalPrice = originalPrice;
        this.quantity = quantity;
        this.availableQuantity = quantity;
        this.imageName = imageName;
        this.discountStart = discountStart;
        this.discountEnd = discountEnd;
        this.discountRules = discountRules;
        this.status = "ACTIVE";
    }

    public String getListingId() { return listingId; }
    public void setListingId(String listingId) { this.listingId = listingId; }

    public String getVendorId() { return vendorId; }
    public void setVendorId(String vendorId) { this.vendorId = vendorId; }

    public String getFoodName() { return foodName; }
    public void setFoodName(String foodName) { this.foodName = foodName; }

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

    public String getDiscountStart() { return discountStart; }
    public void setDiscountStart(String discountStart) { this.discountStart = discountStart; }

    public String getDiscountEnd() { return discountEnd; }
    public void setDiscountEnd(String discountEnd) { this.discountEnd = discountEnd; }

    public List<Map<String, Object>> getDiscountRules() { return discountRules; }
    public void setDiscountRules(List<Map<String, Object>> discountRules) { this.discountRules = discountRules; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}