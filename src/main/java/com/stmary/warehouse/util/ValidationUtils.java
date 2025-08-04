package com.stmary.warehouse.util;

import java.time.LocalDate;
import java.util.regex.Pattern;

/**
 * Utility class for input validation across the application
 * Provides common validation methods for forms and data input
 */
public class ValidationUtils {
    
    // Regex patterns for validation
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"
    );
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^\\+?[1-9]\\d{1,14}$" // International phone number format
    );
    
    private static final Pattern NAME_PATTERN = Pattern.compile(
        "^[a-zA-Z\\s\\-\\.]{2,50}$" // Names with spaces, hyphens, dots
    );
    
    /**
     * Validate if string is not null or empty
     */
    public static boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }
    
    /**
     * Validate string length
     */
    public static boolean isValidLength(String value, int minLength, int maxLength) {
        if (value == null) return false;
        int length = value.trim().length();
        return length >= minLength && length <= maxLength;
    }
    
    /**
     * Validate email format
     */
    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }
    
    /**
     * Validate phone number format
     */
    public static boolean isValidPhone(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone.replaceAll("[\\s\\-\\(\\)]", "")).matches();
    }
    
    /**
     * Validate name format (letters, spaces, hyphens, dots only)
     */
    public static boolean isValidName(String name) {
        return name != null && NAME_PATTERN.matcher(name.trim()).matches();
    }
    
    /**
     * Validate positive integer
     */
    public static boolean isPositiveInteger(String value) {
        try {
            int intValue = Integer.parseInt(value);
            return intValue > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Validate non-negative integer
     */
    public static boolean isNonNegativeInteger(String value) {
        try {
            int intValue = Integer.parseInt(value);
            return intValue >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Validate date is not in the future
     */
    public static boolean isNotFutureDate(LocalDate date) {
        return date != null && !date.isAfter(LocalDate.now());
    }
    
    /**
     * Validate date range
     */
    public static boolean isValidDateRange(LocalDate startDate, LocalDate endDate) {
        return startDate != null && endDate != null && !startDate.isAfter(endDate);
    }
    
    /**
     * Sanitize input string (remove dangerous characters)
     */
    public static String sanitizeInput(String input) {
        if (input == null) return null;
        
        // Remove potential SQL injection characters and trim
        return input.replaceAll("[';\"\\\\]", "").trim();
    }
    
    /**
     * Validate and sanitize customer name
     */
    public static String validateCustomerName(String name) throws IllegalArgumentException {
        if (!isNotEmpty(name)) {
            throw new IllegalArgumentException("Customer name cannot be empty");
        }
        
        String sanitized = sanitizeInput(name);
        
        if (!isValidLength(sanitized, 2, 100)) {
            throw new IllegalArgumentException("Customer name must be between 2 and 100 characters");
        }
        
        return sanitized;
    }
    
    /**
     * Validate and sanitize item name
     */
    public static String validateItemName(String name) throws IllegalArgumentException {
        if (!isNotEmpty(name)) {
            throw new IllegalArgumentException("Item name cannot be empty");
        }
        
        String sanitized = sanitizeInput(name);
        
        if (!isValidLength(sanitized, 1, 255)) {
            throw new IllegalArgumentException("Item name must be between 1 and 255 characters");
        }
        
        return sanitized;
    }
    
    /**
     * Validate and sanitize location
     */
    public static String validateLocation(String location) throws IllegalArgumentException {
        if (!isNotEmpty(location)) {
            throw new IllegalArgumentException("Location cannot be empty");
        }
        
        String sanitized = sanitizeInput(location);
        
        if (!isValidLength(sanitized, 1, 100)) {
            throw new IllegalArgumentException("Location must be between 1 and 100 characters");
        }
        
        return sanitized;
    }
    
    /**
     * Validate quantity
     */
    public static int validateQuantity(String quantityStr) throws IllegalArgumentException {
        if (!isNotEmpty(quantityStr)) {
            throw new IllegalArgumentException("Quantity cannot be empty");
        }
        
        if (!isNonNegativeInteger(quantityStr)) {
            throw new IllegalArgumentException("Quantity must be a non-negative number");
        }
        
        int quantity = Integer.parseInt(quantityStr);
        
        if (quantity > 999999) {
            throw new IllegalArgumentException("Quantity cannot exceed 999,999");
        }
        
        return quantity;
    }
    
    /**
     * Format error message for UI display
     */
    public static String formatErrorMessage(String fieldName, String error) {
        return String.format("%s: %s", fieldName, error);
    }
    
    /**
     * Check if string contains only alphanumeric characters and spaces
     */
    public static boolean isAlphanumericWithSpaces(String value) {
        return value != null && value.matches("^[a-zA-Z0-9\\s]+$");
    }
    
    /**
     * Validate warehouse location format (e.g., "A1-B2", "ZONE-01")
     */
    public static boolean isValidWarehouseLocation(String location) {
        if (location == null) return false;
        
        // Allow formats like: A1, A1-B2, ZONE-01, etc.
        return location.matches("^[A-Z0-9]+(-[A-Z0-9]+)*$");
    }
}