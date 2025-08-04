package com.stmary.warehouse.model;

import java.util.Objects;

/**
 * Inventory entity representing warehouse inventory items
 * Implements proper encapsulation with validation
 */
public class Inventory {
    private Integer itemId;
    private String itemName;
    private Integer itemQuantity;
    private String itemLocation;
    
    // Default constructor
    public Inventory() {}
    
    // Constructor with parameters
    public Inventory(String itemName, Integer itemQuantity, String itemLocation) {
        this.itemName = itemName;
        this.itemQuantity = itemQuantity;
        this.itemLocation = itemLocation;
    }
    
    // Full constructor
    public Inventory(Integer itemId, String itemName, Integer itemQuantity, String itemLocation) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.itemQuantity = itemQuantity;
        this.itemLocation = itemLocation;
    }
    
    // Getters and setters with validation
    public Integer getItemId() {
        return itemId;
    }
    
    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }
    
    public String getItemName() {
        return itemName;
    }
    
    public void setItemName(String itemName) {
        if (itemName == null || itemName.trim().isEmpty()) {
            throw new IllegalArgumentException("Item name cannot be null or empty");
        }
        this.itemName = itemName.trim();
    }
    
    public Integer getItemQuantity() {
        return itemQuantity;
    }
    
    public void setItemQuantity(Integer itemQuantity) {
        if (itemQuantity == null || itemQuantity < 0) {
            throw new IllegalArgumentException("Item quantity cannot be null or negative");
        }
        this.itemQuantity = itemQuantity;
    }
    
    public String getItemLocation() {
        return itemLocation;
    }
    
    public void setItemLocation(String itemLocation) {
        if (itemLocation == null || itemLocation.trim().isEmpty()) {
            throw new IllegalArgumentException("Item location cannot be null or empty");
        }
        this.itemLocation = itemLocation.trim();
    }
    
    // Validation method
    public boolean isValid() {
        return itemName != null && !itemName.trim().isEmpty() &&
               itemQuantity != null && itemQuantity >= 0 &&
               itemLocation != null && !itemLocation.trim().isEmpty();
    }
    
    // Business logic methods
    public void updateQuantity(int newQuantity) {
        if (newQuantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
        this.itemQuantity = newQuantity;
    }
    
    public void addStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Stock addition must be positive");
        }
        this.itemQuantity += quantity;
    }
    
    public void removeStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Stock removal must be positive");
        }
        if (this.itemQuantity < quantity) {
            throw new IllegalStateException("Insufficient stock available");
        }
        this.itemQuantity -= quantity;
    }
    
    public boolean isLowStock(int threshold) {
        return this.itemQuantity <= threshold;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Inventory inventory = (Inventory) o;
        return Objects.equals(itemId, inventory.itemId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(itemId);
    }
    
    @Override
    public String toString() {
        return String.format("Inventory{itemId=%d, itemName='%s', itemQuantity=%d, itemLocation='%s'}",
                itemId, itemName, itemQuantity, itemLocation);
    }
}