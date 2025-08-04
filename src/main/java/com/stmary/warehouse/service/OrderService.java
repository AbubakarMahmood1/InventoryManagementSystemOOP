package com.stmary.warehouse.service;

import com.stmary.warehouse.dao.OrderDAO;
import com.stmary.warehouse.model.Order;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service layer for Order business logic
 * Implements validation, business rules, and async operations
 */
public class OrderService {
    private final OrderDAO orderDAO;
    private final ExecutorService executorService;
    
    public OrderService() {
        this.orderDAO = new OrderDAO();
        this.executorService = Executors.newFixedThreadPool(5);
    }
    
    public OrderService(OrderDAO orderDAO) {
        this.orderDAO = orderDAO;
        this.executorService = Executors.newFixedThreadPool(5);
    }
    
    /**
     * Create a new order with business validation
     */
    public CompletableFuture<Order> createOrderAsync(Order order) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                validateOrderForCreation(order);
                return orderDAO.create(order);
            } catch (SQLException e) {
                throw new OrderServiceException("Failed to create order", e);
            }
        }, executorService);
    }
    
    /**
     * Create order synchronously
     */
    public Order createOrder(Order order) throws OrderServiceException {
        try {
            validateOrderForCreation(order);
            return orderDAO.create(order);
        } catch (SQLException e) {
            throw new OrderServiceException("Failed to create order", e);
        }
    }
    
    /**
     * Update order with business validation
     */
    public CompletableFuture<Boolean> updateOrderAsync(Order order) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                validateOrderForUpdate(order);
                validateStatusTransition(order);
                return orderDAO.update(order);
            } catch (SQLException e) {
                throw new OrderServiceException("Failed to update order", e);
            }
        }, executorService);
    }
    
    /**
     * Update order synchronously
     */
    public boolean updateOrder(Order order) throws OrderServiceException {
        try {
            validateOrderForUpdate(order);
            validateStatusTransition(order);
            return orderDAO.update(order);
        } catch (SQLException e) {
            throw new OrderServiceException("Failed to update order", e);
        }
    }
    
    /**
     * Delete order with business rules
     */
    public CompletableFuture<Boolean> deleteOrderAsync(Integer orderId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                validateOrderForDeletion(orderId);
                
                // Check if order can be deleted based on status
                Optional<Order> orderOpt = orderDAO.findById(orderId);
                if (orderOpt.isPresent()) {
                    Order order = orderOpt.get();
                    if (order.getOrderStatus() == Order.OrderStatus.DELIVERED) {
                        throw new OrderServiceException("Cannot delete delivered orders");
                    }
                }
                
                return orderDAO.delete(orderId);
            } catch (SQLException e) {
                throw new OrderServiceException("Failed to delete order", e);
            }
        }, executorService);
    }
    
    /**
     * Delete order synchronously
     */
    public boolean deleteOrder(Integer orderId) throws OrderServiceException {
        try {
            validateOrderForDeletion(orderId);
            
            // Check if order can be deleted based on status
            Optional<Order> orderOpt = orderDAO.findById(orderId);
            if (orderOpt.isPresent()) {
                Order order = orderOpt.get();
                if (order.getOrderStatus() == Order.OrderStatus.DELIVERED) {
                    throw new OrderServiceException("Cannot delete delivered orders");
                }
            }
            
            return orderDAO.delete(orderId);
        } catch (SQLException e) {
            throw new OrderServiceException("Failed to delete order", e);
        }
    }
    
    /**
     * Find order by ID
     */
    public CompletableFuture<Optional<Order>> findOrderByIdAsync(Integer orderId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return orderDAO.findById(orderId);
            } catch (SQLException e) {
                throw new OrderServiceException("Failed to find order", e);
            }
        }, executorService);
    }
    
    /**
     * Find order by ID synchronously
     */
    public Optional<Order> findOrderById(Integer orderId) throws OrderServiceException {
        try {
            return orderDAO.findById(orderId);
        } catch (SQLException e) {
            throw new OrderServiceException("Failed to find order", e);
        }
    }
    
    /**
     * Get all orders
     */
    public CompletableFuture<List<Order>> getAllOrdersAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return orderDAO.findAll();
            } catch (SQLException e) {
                throw new OrderServiceException("Failed to retrieve orders", e);
            }
        }, executorService);
    }
    
    /**
     * Get all orders synchronously
     */
    public List<Order> getAllOrders() throws OrderServiceException {
        try {
            return orderDAO.findAll();
        } catch (SQLException e) {
            throw new OrderServiceException("Failed to retrieve orders", e);
        }
    }
    
    /**
     * Search orders by customer name
     */
    public CompletableFuture<List<Order>> searchByCustomerAsync(String customerName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (customerName == null || customerName.trim().isEmpty()) {
                    return orderDAO.findAll();
                }
                return orderDAO.findByCustomer(customerName);
            } catch (SQLException e) {
                throw new OrderServiceException("Failed to search orders by customer", e);
            }
        }, executorService);
    }
    
    /**
     * Get orders by status
     */
    public CompletableFuture<List<Order>> getOrdersByStatusAsync(Order.OrderStatus status) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return orderDAO.findByStatus(status);
            } catch (SQLException e) {
                throw new OrderServiceException("Failed to get orders by status", e);
            }
        }, executorService);
    }
    
    /**
     * Get orders within date range
     */
    public CompletableFuture<List<Order>> getOrdersByDateRangeAsync(LocalDate startDate, LocalDate endDate) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                validateDateRange(startDate, endDate);
                return orderDAO.findByDateRange(startDate, endDate);
            } catch (SQLException e) {
                throw new OrderServiceException("Failed to get orders by date range", e);
            }
        }, executorService);
    }
    
    /**
     * Get recent orders
     */
    public CompletableFuture<List<Order>> getRecentOrdersAsync(int days, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                validateRecentOrdersParams(days, limit);
                return orderDAO.findRecentOrders(days, limit);
            } catch (SQLException e) {
                throw new OrderServiceException("Failed to get recent orders", e);
            }
        }, executorService);
    }
    
    /**
     * Update order status with business logic validation
     */
    public CompletableFuture<Boolean> updateOrderStatusAsync(Integer orderId, Order.OrderStatus newStatus) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                validateStatusUpdate(orderId, newStatus);
                
                // Get current order to validate status transition
                Optional<Order> orderOpt = orderDAO.findById(orderId);
                if (orderOpt.isEmpty()) {
                    throw new OrderServiceException("Order not found with ID: " + orderId);
                }
                
                Order order = orderOpt.get();
                validateStatusTransition(order.getOrderStatus(), newStatus);
                
                return orderDAO.updateStatus(orderId, newStatus);
            } catch (SQLException e) {
                throw new OrderServiceException("Failed to update order status", e);
            }
        }, executorService);
    }
    
    /**
     * Order workflow methods
     */
    public CompletableFuture<Boolean> confirmOrderAsync(Integer orderId) {
        return updateOrderStatusAsync(orderId, Order.OrderStatus.CONFIRMED);
    }
    
    public CompletableFuture<Boolean> processOrderAsync(Integer orderId) {
        return updateOrderStatusAsync(orderId, Order.OrderStatus.PROCESSING);
    }
    
    public CompletableFuture<Boolean> shipOrderAsync(Integer orderId) {
        return updateOrderStatusAsync(orderId, Order.OrderStatus.SHIPPED);
    }
    
    public CompletableFuture<Boolean> deliverOrderAsync(Integer orderId) {
        return updateOrderStatusAsync(orderId, Order.OrderStatus.DELIVERED);
    }
    
    public CompletableFuture<Boolean> cancelOrderAsync(Integer orderId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Optional<Order> orderOpt = orderDAO.findById(orderId);
                if (orderOpt.isEmpty()) {
                    throw new OrderServiceException("Order not found with ID: " + orderId);
                }
                
                Order order = orderOpt.get();
                if (!order.canBeCancelled()) {
                    throw new OrderServiceException("Order cannot be cancelled in current status: " + 
                        order.getOrderStatus().getDisplayName());
                }
                
                return orderDAO.updateStatus(orderId, Order.OrderStatus.CANCELLED);
            } catch (SQLException e) {
                throw new OrderServiceException("Failed to cancel order", e);
            }
        }, executorService);
    }
    
    /**
     * Get order statistics
     */
    public CompletableFuture<OrderStats> getOrderStatsAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                int totalOrders = orderDAO.getTotalCount();
                List<OrderDAO.StatusCount> statusCounts = orderDAO.getOrderCountsByStatus();
                List<Order> recentOrders = orderDAO.findRecentOrders(7, 10);
                
                return new OrderStats(totalOrders, statusCounts, recentOrders.size());
            } catch (SQLException e) {
                throw new OrderServiceException("Failed to retrieve order statistics", e);
            }
        }, executorService);
    }
    
    /**
     * Batch update multiple orders
     */
    public CompletableFuture<Boolean> batchUpdateAsync(List<Order> orders) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                for (Order order : orders) {
                    validateOrderForUpdate(order);
                }
                
                boolean allSuccessful = true;
                for (Order order : orders) {
                    boolean result = orderDAO.update(order);
                    if (!result) {
                        allSuccessful = false;
                    }
                }
                return allSuccessful;
            } catch (SQLException e) {
                throw new OrderServiceException("Failed to batch update orders", e);
            }
        }, executorService);
    }
    
    // Validation methods
    private void validateOrderForCreation(Order order) throws OrderServiceException {
        if (order == null) {
            throw new OrderServiceException("Order cannot be null");
        }
        
        if (!order.isValid()) {
            throw new OrderServiceException("Invalid order data");
        }
        
        if (order.getCustomerName().length() > 255) {
            throw new OrderServiceException("Customer name cannot exceed 255 characters");
        }
        
        if (order.getOrderDate().isAfter(LocalDate.now())) {
            throw new OrderServiceException("Order date cannot be in the future");
        }
    }
    
    private void validateOrderForUpdate(Order order) throws OrderServiceException {
        validateOrderForCreation(order);
        
        if (order.getOrderId() == null) {
            throw new OrderServiceException("Order ID is required for update");
        }
    }
    
    private void validateOrderForDeletion(Integer orderId) throws OrderServiceException {
        if (orderId == null) {
            throw new OrderServiceException("Order ID cannot be null for deletion");
        }
    }
    
    private void validateStatusUpdate(Integer orderId, Order.OrderStatus newStatus) throws OrderServiceException {
        if (orderId == null) {
            throw new OrderServiceException("Order ID cannot be null");
        }
        
        if (newStatus == null) {
            throw new OrderServiceException("New status cannot be null");
        }
    }
    
    private void validateStatusTransition(Order order) throws OrderServiceException {
        if (order.getOrderId() != null) {
            try {
                Optional<Order> existingOrder = orderDAO.findById(order.getOrderId());
                if (existingOrder.isPresent()) {
                    validateStatusTransition(existingOrder.get().getOrderStatus(), order.getOrderStatus());
                }
            } catch (SQLException e) {
                // Log but don't fail - allow update to proceed
            }
        }
    }
    
    private void validateStatusTransition(Order.OrderStatus currentStatus, Order.OrderStatus newStatus) 
            throws OrderServiceException {
        
        // Define valid status transitions
        boolean isValidTransition = switch (currentStatus) {
            case PENDING -> newStatus == Order.OrderStatus.CONFIRMED || 
                           newStatus == Order.OrderStatus.CANCELLED;
            case CONFIRMED -> newStatus == Order.OrderStatus.PROCESSING || 
                             newStatus == Order.OrderStatus.CANCELLED;
            case PROCESSING -> newStatus == Order.OrderStatus.SHIPPED;
            case SHIPPED -> newStatus == Order.OrderStatus.DELIVERED;
            case DELIVERED, CANCELLED -> false; // Final states
        };
        
        if (!isValidTransition) {
            throw new OrderServiceException(
                String.format("Invalid status transition from %s to %s", 
                    currentStatus.getDisplayName(), newStatus.getDisplayName()));
        }
    }
    
    private void validateDateRange(LocalDate startDate, LocalDate endDate) throws OrderServiceException {
        if (startDate == null || endDate == null) {
            throw new OrderServiceException("Start date and end date cannot be null");
        }
        
        if (startDate.isAfter(endDate)) {
            throw new OrderServiceException("Start date cannot be after end date");
        }
    }
    
    private void validateRecentOrdersParams(int days, int limit) throws OrderServiceException {
        if (days < 0) {
            throw new OrderServiceException("Days must be non-negative");
        }
        
        if (limit < 0) {
            throw new OrderServiceException("Limit must be non-negative");
        }
    }
    
    /**
     * Shutdown executor service
     */
    public void shutdown() {
        executorService.shutdown();
    }
    
    /**
     * Order statistics class
     */
    public static class OrderStats {
        private final int totalOrders;
        private final List<OrderDAO.StatusCount> statusCounts;
        private final int recentOrdersCount;
        
        public OrderStats(int totalOrders, List<OrderDAO.StatusCount> statusCounts, int recentOrdersCount) {
            this.totalOrders = totalOrders;
            this.statusCounts = statusCounts;
            this.recentOrdersCount = recentOrdersCount;
        }
        
        public int getTotalOrders() { return totalOrders; }
        public List<OrderDAO.StatusCount> getStatusCounts() { return statusCounts; }
        public int getRecentOrdersCount() { return recentOrdersCount; }
        
        @Override
        public String toString() {
            return String.format("OrderStats{totalOrders=%d, statusCounts=%s, recentOrdersCount=%d}",
                    totalOrders, statusCounts, recentOrdersCount);
        }
    }
    
    /**
     * Custom exception for order service operations
     */
    public static class OrderServiceException extends RuntimeException {
        public OrderServiceException(String message) {
            super(message);
        }
        
        public OrderServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}