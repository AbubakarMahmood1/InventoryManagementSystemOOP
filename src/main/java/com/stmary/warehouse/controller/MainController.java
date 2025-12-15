package com.stmary.warehouse.controller;

import com.stmary.warehouse.model.Inventory;
import com.stmary.warehouse.model.Order;
import com.stmary.warehouse.model.Shipment;
import com.stmary.warehouse.service.InventoryService;
import com.stmary.warehouse.service.OrderService;
import com.stmary.warehouse.service.ShipmentService;
import javafx.application.Platform;
import javafx.concurrent.Task;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Main controller implementing MVC pattern
 * Coordinates between services and view layer with proper error handling
 */
public class MainController {
    private final InventoryService inventoryService;
    private final OrderService orderService;
    private final ShipmentService shipmentService;
    
    // Observer pattern for view updates
    private Consumer<String> errorHandler;
    private Consumer<String> successHandler;
    private Consumer<String> progressHandler;
    
    public MainController() {
        this.inventoryService = new InventoryService();
        this.orderService = new OrderService();
        this.shipmentService = new ShipmentService();
    }
    
    // Constructor for dependency injection (testing)
    public MainController(InventoryService inventoryService, OrderService orderService, ShipmentService shipmentService) {
        this.inventoryService = inventoryService;
        this.orderService = orderService;
        this.shipmentService = shipmentService;
    }
    
    /**
     * Set event handlers for UI feedback
     */
    public void setEventHandlers(Consumer<String> errorHandler, Consumer<String> successHandler, Consumer<String> progressHandler) {
        this.errorHandler = errorHandler;
        this.successHandler = successHandler;
        this.progressHandler = progressHandler;
    }
    
    // ===============================
    // INVENTORY OPERATIONS
    // ===============================
    
    /**
     * Create new inventory item
     */
    public void createInventory(Inventory inventory, Consumer<Inventory> onSuccess) {
        executeAsyncTask(
            "Creating inventory item...",
            () -> inventoryService.createInventory(inventory),
            onSuccess,
            "Inventory item created successfully"
        );
    }
    
    /**
     * Update inventory item
     */
    public void updateInventory(Inventory inventory, Consumer<Boolean> onSuccess) {
        executeAsyncTask(
            "Updating inventory item...",
            () -> inventoryService.updateInventory(inventory),
            onSuccess,
            "Inventory item updated successfully"
        );
    }
    
    /**
     * Delete inventory item
     */
    public void deleteInventory(Integer itemId, Consumer<Boolean> onSuccess) {
        executeAsyncTask(
            "Deleting inventory item...",
            () -> inventoryService.deleteInventory(itemId),
            onSuccess,
            "Inventory item deleted successfully"
        );
    }
    
    /**
     * Load all inventory items
     */
    public void loadAllInventory(Consumer<List<Inventory>> onSuccess) {
        executeAsyncTask(
            "Loading inventory...",
            () -> inventoryService.getAllInventory(),
            onSuccess,
            null // No success message for loading operations
        );
    }
    
    /**
     * Search inventory by name
     */
    public void searchInventoryByName(String name, Consumer<List<Inventory>> onSuccess) {
        inventoryService.searchByNameAsync(name)
            .thenAccept(result -> Platform.runLater(() -> onSuccess.accept(result)))
            .exceptionally(this::handleException);
    }
    
    /**
     * Search inventory by location
     */
    public void searchInventoryByLocation(String location, Consumer<List<Inventory>> onSuccess) {
        inventoryService.searchByLocationAsync(location)
            .thenAccept(result -> Platform.runLater(() -> onSuccess.accept(result)))
            .exceptionally(this::handleException);
    }
    
    /**
     * Get low stock items
     */
    public void getLowStockItems(Consumer<List<Inventory>> onSuccess) {
        executeAsyncTask(
            "Loading low stock items...",
            () -> inventoryService.getAllInventory().stream()
                .filter(item -> item.isLowStock(10))
                .toList(),
            onSuccess,
            null
        );
    }
    
    /**
     * Update stock quantity
     */
    public void updateStock(Integer itemId, Integer newQuantity, Consumer<Boolean> onSuccess) {
        inventoryService.updateStockAsync(itemId, newQuantity)
            .thenAccept(result -> Platform.runLater(() -> {
                onSuccess.accept(result);
                if (result && successHandler != null) {
                    successHandler.accept("Stock updated successfully");
                }
            }))
            .exceptionally(this::handleException);
    }
    
    /**
     * Get inventory statistics
     */
    public void getInventoryStats(Consumer<InventoryService.InventoryStats> onSuccess) {
        inventoryService.getInventoryStatsAsync()
            .thenAccept(result -> Platform.runLater(() -> onSuccess.accept(result)))
            .exceptionally(this::handleException);
    }
    
    // ===============================
    // ORDER OPERATIONS
    // ===============================
    
    /**
     * Create new order
     */
    public void createOrder(Order order, Consumer<Order> onSuccess) {
        executeAsyncTask(
            "Creating order...",
            () -> orderService.createOrder(order),
            onSuccess,
            "Order created successfully"
        );
    }
    
    /**
     * Update order
     */
    public void updateOrder(Order order, Consumer<Boolean> onSuccess) {
        executeAsyncTask(
            "Updating order...",
            () -> orderService.updateOrder(order),
            onSuccess,
            "Order updated successfully"
        );
    }
    
    /**
     * Delete order
     */
    public void deleteOrder(Integer orderId, Consumer<Boolean> onSuccess) {
        executeAsyncTask(
            "Deleting order...",
            () -> orderService.deleteOrder(orderId),
            onSuccess,
            "Order deleted successfully"
        );
    }
    
    /**
     * Load all orders
     */
    public void loadAllOrders(Consumer<List<Order>> onSuccess) {
        executeAsyncTask(
            "Loading orders...",
            () -> orderService.getAllOrders(),
            onSuccess,
            null
        );
    }
    
    /**
     * Search orders by customer
     */
    public void searchOrdersByCustomer(String customerName, Consumer<List<Order>> onSuccess) {
        orderService.searchByCustomerAsync(customerName)
            .thenAccept(result -> Platform.runLater(() -> onSuccess.accept(result)))
            .exceptionally(this::handleException);
    }
    
    /**
     * Get orders by status
     */
    public void getOrdersByStatus(Order.OrderStatus status, Consumer<List<Order>> onSuccess) {
        orderService.getOrdersByStatusAsync(status)
            .thenAccept(result -> Platform.runLater(() -> onSuccess.accept(result)))
            .exceptionally(this::handleException);
    }
    
    /**
     * Get orders in date range
     */
    public void getOrdersByDateRange(LocalDate startDate, LocalDate endDate, Consumer<List<Order>> onSuccess) {
        orderService.getOrdersByDateRangeAsync(startDate, endDate)
            .thenAccept(result -> Platform.runLater(() -> onSuccess.accept(result)))
            .exceptionally(this::handleException);
    }
    
    /**
     * Update order status
     */
    public void updateOrderStatus(Integer orderId, Order.OrderStatus newStatus, Consumer<Boolean> onSuccess) {
        orderService.updateOrderStatusAsync(orderId, newStatus)
            .thenAccept(result -> Platform.runLater(() -> {
                onSuccess.accept(result);
                if (result && successHandler != null) {
                    successHandler.accept("Order status updated to " + newStatus.getDisplayName());
                }
            }))
            .exceptionally(this::handleException);
    }
    
    /**
     * Process order workflow methods
     */
    public void confirmOrder(Integer orderId, Consumer<Boolean> onSuccess) {
        updateOrderStatus(orderId, Order.OrderStatus.CONFIRMED, onSuccess);
    }
    
    public void processOrder(Integer orderId, Consumer<Boolean> onSuccess) {
        updateOrderStatus(orderId, Order.OrderStatus.PROCESSING, onSuccess);
    }
    
    public void shipOrder(Integer orderId, Consumer<Boolean> onSuccess) {
        updateOrderStatus(orderId, Order.OrderStatus.SHIPPED, onSuccess);
    }
    
    public void deliverOrder(Integer orderId, Consumer<Boolean> onSuccess) {
        updateOrderStatus(orderId, Order.OrderStatus.DELIVERED, onSuccess);
    }
    
    public void cancelOrder(Integer orderId, Consumer<Boolean> onSuccess) {
        orderService.cancelOrderAsync(orderId)
            .thenAccept(result -> Platform.runLater(() -> {
                onSuccess.accept(result);
                if (result && successHandler != null) {
                    successHandler.accept("Order cancelled successfully");
                }
            }))
            .exceptionally(this::handleException);
    }
    
    /**
     * Get pending orders
     */
    public void getPendingOrders(Consumer<List<Order>> onSuccess) {
        getOrdersByStatus(Order.OrderStatus.PENDING, onSuccess);
    }
    
    /**
     * Get order statistics
     */
    public void getOrderStats(Consumer<OrderService.OrderStats> onSuccess) {
        orderService.getOrderStatsAsync()
            .thenAccept(result -> Platform.runLater(() -> onSuccess.accept(result)))
            .exceptionally(this::handleException);
    }
    
    // ===============================
    // SHIPMENT OPERATIONS
    // ===============================
    
    /**
     * Create new shipment
     */
    public void createShipment(Shipment shipment, Consumer<Shipment> onSuccess) {
        executeAsyncTask(
            "Creating shipment...",
            () -> shipmentService.createShipment(shipment),
            onSuccess,
            "Shipment created successfully"
        );
    }
    
    /**
     * Update shipment
     */
    public void updateShipment(Shipment shipment, Consumer<Boolean> onSuccess) {
        executeAsyncTask(
            "Updating shipment...",
            () -> shipmentService.updateShipment(shipment),
            onSuccess,
            "Shipment updated successfully"
        );
    }
    
    /**
     * Delete shipment
     */
    public void deleteShipment(Integer shipmentId, Consumer<Boolean> onSuccess) {
        executeAsyncTask(
            "Deleting shipment...",
            () -> shipmentService.deleteShipment(shipmentId),
            onSuccess,
            "Shipment deleted successfully"
        );
    }
    
    /**
     * Load all shipments
     */
    public void loadAllShipments(Consumer<List<Shipment>> onSuccess) {
        executeAsyncTask(
            "Loading shipments...",
            () -> shipmentService.getAllShipments(),
            onSuccess,
            null
        );
    }
    
    /**
     * Search shipments by destination
     */
    public void searchShipmentsByDestination(String destination, Consumer<List<Shipment>> onSuccess) {
        shipmentService.searchByDestinationAsync(destination)
            .thenAccept(result -> Platform.runLater(() -> onSuccess.accept(result)))
            .exceptionally(this::handleException);
    }
    
    /**
     * Get shipments by status
     */
    public void getShipmentsByStatus(Shipment.ShipmentStatus status, Consumer<List<Shipment>> onSuccess) {
        shipmentService.getShipmentsByStatusAsync(status)
            .thenAccept(result -> Platform.runLater(() -> onSuccess.accept(result)))
            .exceptionally(this::handleException);
    }
    
    /**
     * Get shipments in date range
     */
    public void getShipmentsByDateRange(LocalDate startDate, LocalDate endDate, Consumer<List<Shipment>> onSuccess) {
        shipmentService.getShipmentsByDateRangeAsync(startDate, endDate)
            .thenAccept(result -> Platform.runLater(() -> onSuccess.accept(result)))
            .exceptionally(this::handleException);
    }
    
    /**
     * Update shipment status
     */
    public void updateShipmentStatus(Integer shipmentId, Shipment.ShipmentStatus newStatus, Consumer<Boolean> onSuccess) {
        shipmentService.updateShipmentStatusAsync(shipmentId, newStatus)
            .thenAccept(result -> Platform.runLater(() -> {
                onSuccess.accept(result);
                if (result && successHandler != null) {
                    successHandler.accept("Shipment status updated to " + newStatus.getDisplayName());
                }
            }))
            .exceptionally(this::handleException);
    }
    
    /**
     * Shipment workflow methods
     */
    public void shipShipment(Integer shipmentId, Consumer<Boolean> onSuccess) {
        updateShipmentStatus(shipmentId, Shipment.ShipmentStatus.IN_TRANSIT, onSuccess);
    }
    
    public void outForDelivery(Integer shipmentId, Consumer<Boolean> onSuccess) {
        updateShipmentStatus(shipmentId, Shipment.ShipmentStatus.OUT_FOR_DELIVERY, onSuccess);
    }
    
    public void deliverShipment(Integer shipmentId, Consumer<Boolean> onSuccess) {
        updateShipmentStatus(shipmentId, Shipment.ShipmentStatus.DELIVERED, onSuccess);
    }
    
    public void returnShipment(Integer shipmentId, Consumer<Boolean> onSuccess) {
        updateShipmentStatus(shipmentId, Shipment.ShipmentStatus.RETURNED, onSuccess);
    }
    
    public void cancelShipment(Integer shipmentId, Consumer<Boolean> onSuccess) {
        shipmentService.cancelShipmentAsync(shipmentId)
            .thenAccept(result -> Platform.runLater(() -> {
                onSuccess.accept(result);
                if (result && successHandler != null) {
                    successHandler.accept("Shipment cancelled successfully");
                }
            }))
            .exceptionally(this::handleException);
    }
    
    /**
     * Get trackable shipments
     */
    public void getTrackableShipments(Consumer<List<Shipment>> onSuccess) {
        shipmentService.getTrackableShipmentsAsync()
            .thenAccept(result -> Platform.runLater(() -> onSuccess.accept(result)))
            .exceptionally(this::handleException);
    }
    
    /**
     * Get delayed shipments
     */
    public void getDelayedShipments(Consumer<List<Shipment>> onSuccess) {
        shipmentService.getDelayedShipmentsAsync()
            .thenAccept(result -> Platform.runLater(() -> onSuccess.accept(result)))
            .exceptionally(this::handleException);
    }
    
    /**
     * Get shipment statistics
     */
    public void getShipmentStats(Consumer<ShipmentService.ShipmentStats> onSuccess) {
        shipmentService.getShipmentStatsAsync()
            .thenAccept(result -> Platform.runLater(() -> onSuccess.accept(result)))
            .exceptionally(this::handleException);
    }
    
    // ===============================
    // UTILITY METHODS
    // ===============================
    
    /**
     * Generic method to execute async tasks with progress indication
     */
    private <T> void executeAsyncTask(String progressMessage, TaskSupplier<T> taskSupplier, 
                                     Consumer<T> onSuccess, String successMessage) {
        
        Task<T> task = new Task<T>() {
            @Override
            protected T call() throws Exception {
                return taskSupplier.get();
            }
            
            @Override
            protected void succeeded() {
                T result = getValue();
                onSuccess.accept(result);
                if (successMessage != null && successHandler != null) {
                    successHandler.accept(successMessage);
                }
            }
            
            @Override
            protected void failed() {
                Throwable exception = getException();
                handleError("Operation failed: " + exception.getMessage());
            }
        };
        
        if (progressHandler != null && progressMessage != null) {
            progressHandler.accept(progressMessage);
        }
        
        new Thread(task).start();
    }
    
    /**
     * Handle CompletableFuture exceptions
     */
    private <T> T handleException(Throwable throwable) {
        Platform.runLater(() -> handleError("Operation failed: " + throwable.getMessage()));
        return null;
    }
    
    /**
     * Handle errors with UI feedback
     */
    private void handleError(String message) {
        if (errorHandler != null) {
            errorHandler.accept(message);
        } else {
            System.err.println("Error: " + message);
        }
    }
    
    /**
     * Find entity by ID with proper error handling
     */
    public void findInventoryById(Integer itemId, Consumer<Optional<Inventory>> onSuccess) {
        inventoryService.findInventoryByIdAsync(itemId)
            .thenAccept(result -> Platform.runLater(() -> onSuccess.accept(result)))
            .exceptionally(this::handleException);
    }
    
    public void findOrderById(Integer orderId, Consumer<Optional<Order>> onSuccess) {
        orderService.findOrderByIdAsync(orderId)
            .thenAccept(result -> Platform.runLater(() -> onSuccess.accept(result)))
            .exceptionally(this::handleException);
    }
    
    public void findShipmentById(Integer shipmentId, Consumer<Optional<Shipment>> onSuccess) {
        shipmentService.findShipmentByIdAsync(shipmentId)
            .thenAccept(result -> Platform.runLater(() -> onSuccess.accept(result)))
            .exceptionally(this::handleException);
    }
    
    /**
     * Shutdown all services
     */
    public void shutdown() {
        inventoryService.shutdown();
        orderService.shutdown();
        shipmentService.shutdown();
    }
    
    /**
     * Functional interface for task suppliers
     */
    @FunctionalInterface
    private interface TaskSupplier<T> {
        T get() throws Exception;
    }
}