package com.stmary.warehouse.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Order entity representing customer orders
 * Implements proper encapsulation with validation and business logic
 */
public class Order {
    private Integer orderId;
    private LocalDate orderDate;
    private String customerName;
    private OrderStatus orderStatus;
    
    // Order status enum for type safety
    public enum OrderStatus {
        PENDING("Pending"),
        CONFIRMED("Confirmed"),
        PROCESSING("Processing"),
        SHIPPED("Shipped"),
        DELIVERED("Delivered"),
        CANCELLED("Cancelled");
        
        private final String displayName;
        
        OrderStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public static OrderStatus fromString(String status) {
            if (status == null) return null;
            for (OrderStatus s : OrderStatus.values()) {
                if (s.displayName.equalsIgnoreCase(status) || s.name().equalsIgnoreCase(status)) {
                    return s;
                }
            }
            throw new IllegalArgumentException("Invalid order status: " + status);
        }
    }
    
    // Default constructor
    public Order() {
        this.orderDate = LocalDate.now();
        this.orderStatus = OrderStatus.PENDING;
    }
    
    // Constructor with parameters
    public Order(String customerName) {
        this();
        this.customerName = customerName;
    }
    
    // Constructor with date
    public Order(LocalDate orderDate, String customerName, OrderStatus orderStatus) {
        this.orderDate = orderDate;
        this.customerName = customerName;
        this.orderStatus = orderStatus;
    }
    
    // Full constructor
    public Order(Integer orderId, LocalDate orderDate, String customerName, OrderStatus orderStatus) {
        this.orderId = orderId;
        this.orderDate = orderDate;
        this.customerName = customerName;
        this.orderStatus = orderStatus;
    }
    
    // Getters and setters with validation
    public Integer getOrderId() {
        return orderId;
    }
    
    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }
    
    public LocalDate getOrderDate() {
        return orderDate;
    }
    
    public void setOrderDate(LocalDate orderDate) {
        if (orderDate == null) {
            throw new IllegalArgumentException("Order date cannot be null");
        }
        if (orderDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Order date cannot be in the future");
        }
        this.orderDate = orderDate;
    }
    
    public String getCustomerName() {
        return customerName;
    }
    
    public void setCustomerName(String customerName) {
        if (customerName == null || customerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name cannot be null or empty");
        }
        this.customerName = customerName.trim();
    }
    
    public OrderStatus getOrderStatus() {
        return orderStatus;
    }
    
    public void setOrderStatus(OrderStatus orderStatus) {
        if (orderStatus == null) {
            throw new IllegalArgumentException("Order status cannot be null");
        }
        this.orderStatus = orderStatus;
    }
    
    // Convenience method for string status
    public void setOrderStatusFromString(String status) {
        this.orderStatus = OrderStatus.fromString(status);
    }
    
    public String getOrderStatusString() {
        return orderStatus != null ? orderStatus.getDisplayName() : null;
    }
    
    // Validation method
    public boolean isValid() {
        return customerName != null && !customerName.trim().isEmpty() &&
               orderDate != null && !orderDate.isAfter(LocalDate.now()) &&
               orderStatus != null;
    }
    
    // Business logic methods
    public boolean canBeCancelled() {
        return orderStatus == OrderStatus.PENDING || orderStatus == OrderStatus.CONFIRMED;
    }
    
    public boolean canBeShipped() {
        return orderStatus == OrderStatus.PROCESSING;
    }
    
    public boolean isCompleted() {
        return orderStatus == OrderStatus.DELIVERED || orderStatus == OrderStatus.CANCELLED;
    }
    
    public void confirmOrder() {
        if (orderStatus != OrderStatus.PENDING) {
            throw new IllegalStateException("Only pending orders can be confirmed");
        }
        this.orderStatus = OrderStatus.CONFIRMED;
    }
    
    public void processOrder() {
        if (orderStatus != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Only confirmed orders can be processed");
        }
        this.orderStatus = OrderStatus.PROCESSING;
    }
    
    public void shipOrder() {
        if (orderStatus != OrderStatus.PROCESSING) {
            throw new IllegalStateException("Only processing orders can be shipped");
        }
        this.orderStatus = OrderStatus.SHIPPED;
    }
    
    public void deliverOrder() {
        if (orderStatus != OrderStatus.SHIPPED) {
            throw new IllegalStateException("Only shipped orders can be delivered");
        }
        this.orderStatus = OrderStatus.DELIVERED;
    }
    
    public void cancelOrder() {
        if (!canBeCancelled()) {
            throw new IllegalStateException("Order cannot be cancelled in current status: " + orderStatus);
        }
        this.orderStatus = OrderStatus.CANCELLED;
    }
    
    public long getDaysFromOrder() {
        return orderDate.until(LocalDate.now()).getDays();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Objects.equals(orderId, order.orderId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(orderId);
    }
    
    @Override
    public String toString() {
        return String.format("Order{orderId=%d, orderDate=%s, customerName='%s', orderStatus=%s}",
                orderId, orderDate, customerName, orderStatus);
    }
}