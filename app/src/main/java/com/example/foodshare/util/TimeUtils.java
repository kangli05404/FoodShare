package com.example.foodshare.util;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TimeUtils {
    private TimeUtils() {}

    public static String formatTime(int hour, int minute) {
        return String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
    }

    public static int convertTimeToMinutes(String time) {
        if (time == null || time.trim().isEmpty()) return -1;

        try {
            String[] parts = time.trim().split(":");
            if (parts.length != 2) return -1;

            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);

            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) return -1;
            return hour * 60 + minute;
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    public static int getCurrentTimeInMinutes() {
        Calendar calendar = Calendar.getInstance();
        return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);
    }

    public static boolean isWithinDiscountRules(List<Map<String, Object>> rules) {
        if (rules == null || rules.isEmpty()) return false;

        int current = getCurrentTimeInMinutes();

        for (Map<String, Object> rule : rules) {
            String startTime = stringValue(rule.get("startTime"));
            String endTime = stringValue(rule.get("endTime"));
            int start = convertTimeToMinutes(startTime);
            int end = convertTimeToMinutes(endTime);

            if (start >= 0 && end >= 0 && current >= start && current < end) return true;
        }

        return false;
    }

    public static double getCurrentDiscountPercent(List<Map<String, Object>> rules) {
        if (rules == null || rules.isEmpty()) return 0;

        int current = getCurrentTimeInMinutes();

        for (Map<String, Object> rule : rules) {
            String startTime = stringValue(rule.get("startTime"));
            String endTime = stringValue(rule.get("endTime"));
            Object discountObject = rule.get("discountPercent");

            if (!(discountObject instanceof Number)) continue;

            int start = convertTimeToMinutes(startTime);
            int end = convertTimeToMinutes(endTime);

            if (start >= 0 && end >= 0 && current >= start && current < end) {
                return ((Number) discountObject).doubleValue();
            }
        }

        return 0;
    }

    public static String getScheduleStart(List<Map<String, Object>> rules) {
        if (rules == null || rules.isEmpty()) return "";

        int earliest = Integer.MAX_VALUE;
        String result = "";

        for (Map<String, Object> rule : rules) {
            String startTime = stringValue(rule.get("startTime"));
            int minutes = convertTimeToMinutes(startTime);

            if (minutes >= 0 && minutes < earliest) {
                earliest = minutes;
                result = startTime;
            }
        }

        return result;
    }

    public static String getScheduleEnd(List<Map<String, Object>> rules) {
        if (rules == null || rules.isEmpty()) return "";

        int latest = -1;
        String result = "";

        for (Map<String, Object> rule : rules) {
            String endTime = stringValue(rule.get("endTime"));
            int minutes = convertTimeToMinutes(endTime);

            if (minutes > latest) {
                latest = minutes;
                result = endTime;
            }
        }

        return result;
    }

    public static double calculateDiscountedPrice(double originalPrice, double discountPercent) {
        return originalPrice * (1 - discountPercent / 100.0);
    }

    public static String formatDiscount(double discount) {
        if (discount == (int) discount) return String.valueOf((int) discount);
        return String.valueOf(discount);
    }

    public static String formatNumber(double number) {
        if (number == (int) number) return String.valueOf((int) number);
        return String.valueOf(number);
    }

    private static String stringValue(Object value) {
        return value instanceof String ? (String) value : "";
    }
}