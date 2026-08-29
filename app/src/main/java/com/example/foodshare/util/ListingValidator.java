package com.example.foodshare.util;

public class ListingValidator {
    private ListingValidator() {}

    public static String validatePrice(String priceText) {
        if (priceText == null || priceText.trim().isEmpty()) {
            return "Please enter price";
        }

        try {
            double price = Double.parseDouble(priceText);
            if (price <= 0) {
                return "Price must be greater than 0";
            }
        } catch (NumberFormatException exception) {
            return "Invalid price";
        }

        return null;
    }

    public static String validateQuantity(String quantityText) {
        if (quantityText == null || quantityText.trim().isEmpty()) {
            return "Please enter quantity";
        }

        try {
            int quantity = Integer.parseInt(quantityText);
            if (quantity <= 0) {
                return "Quantity must be greater than 0";
            }
        } catch (NumberFormatException exception) {
            return "Invalid quantity";
        }

        return null;
    }
}