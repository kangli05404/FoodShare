package com.example.foodshare.model;

public class DiscountRule {
    private String startTime;
    private String endTime;
    private double discountPercent;

    public DiscountRule() {}

    public DiscountRule(String startTime, String endTime, double discountPercent) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.discountPercent = discountPercent;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public double getDiscountPercent() {
        return discountPercent;
    }

    public void setDiscountPercent(double discountPercent) {
        this.discountPercent = discountPercent;
    }
}