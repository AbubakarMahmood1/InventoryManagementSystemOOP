package com.stmary.warehouse.service;

import com.stmary.warehouse.dao.InventoryDAO;
import com.stmary.warehouse.model.Inventory;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service layer for Inventory business logic
 * Implements validation, business rules, and async operations
 */
public class InventoryService {
    private final InventoryDAO inventoryDAO;
    private final ExecutorService executorService;
    private final int LOW_STOCK_THRESHOLD = 10; // Default low stock threshold
    
    public InventoryService() {
        this.inventoryDAO = new InventoryDAO();
        this.executorService = Executors.newFixedThreadPool(5);
    }
    
    public InventoryService(InventoryDAO inventoryDAO) {
        this.inventoryDAO = inventoryDAO;
        this.executorService = Executors.newFixedThreadPool(5);
    }
    
    /**
     * Create a new inventory item with business validation
     */
    public CompletableFuture<Inventory> createInventoryAsync(Inventory inventory) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                validateInventoryForCreation(inventory);
                return inventoryDAO.create(inventory);
            } catch (SQLException e) {
                throw new InventoryServiceException("Failed to create inventory item", e);
            }
        }, executorService);
    }
    
    /**
     * Create inventory item synchronously
     */
    public Inventory createInventory(Inventory inventory) throws InventoryServiceException {
        try {
            validateInventoryForCreation(inventory);
            return inventoryDAO.create(inventory);
        } catch (SQLException e) {
            throw new InventoryServiceException("Failed to create inventory item", e);
        }
    }
    
    /**
     * Update inventory item with business validation
     */
    public CompletableFuture<Boolean> updateInventoryAsync(Inventory inventory) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                validateInventoryForUpdate(inventory);
                return inventoryDAO.update(inventory);
            } catch (SQLException e) {
                throw new InventoryServiceException("Failed to update inventory item", e);
            }
        }, executorService);
    }
    
    /**
     * Update inventory item synchronously
     */
    public boolean updateInventory(Inventory inventory) throws InventoryServiceException {
        try {
            validateInventoryForUpdate(inventory);
            return inventoryDAO.update(inventory);
        } catch (SQLException e) {
            throw new InventoryServiceException("Failed to update inventory item", e);
        }
    }
    
    /**
     * Delete inventory item with business rules
     */
    public CompletableFuture<Boolean> deleteInventoryAsync(Integer itemId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                validateInventoryForDeletion(itemId);
                return inventoryDAO.delete(itemId);
            } catch (SQLException e) {
                throw new InventoryServiceException("Failed to delete inventory item", e);
            }
        }, executorService);
    }
    
    /**
     * Delete inventory item synchronously
     */
    public boolean deleteInventory(Integer itemId) throws InventoryServiceException {
        try {
            validateInventoryForDeletion(itemId);
            return inventoryDAO.delete(itemId);
        } catch (SQLException e) {
            throw new InventoryServiceException("Failed to delete inventory item", e);
        }
    }
    
    /**
     * Find inventory by ID
     */
    public CompletableFuture<Optional<Inventory>> findInventoryByIdAsync(Integer itemId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return inventoryDAO.findById(itemId);
            } catch (SQLException e) {
                throw new InventoryServiceException("Failed to find inventory item", e);
            }
        }, executorService);
    }
    
    /**
     * Find inventory by ID synchronously
     */
    public Optional<Inventory> findInventoryById(Integer itemId) throws InventoryServiceException {
        try {
            return inventoryDAO.findById(itemId);
        } catch (SQLException e) {
            throw new InventoryServiceException("Failed to find inventory item", e);
        }
    }
    
    /**
     * Get all inventory items
     */
    public CompletableFuture<List<Inventory>> getAllInventoryAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return inventoryDAO.findAll();
            } catch (SQLException e) {
                throw new InventoryServiceException("Failed to retrieve inventory items", e);
            }
        }, executorService);
    }
    
    /**
     * Get all inventory items synchronously
     */
    public List<Inventory> getAllInventory() throws InventoryServiceException {
        try {
            return inventoryDAO.findAll();
        } catch (SQLException e) {
            throw new InventoryServiceException("Failed to retrieve inventory items", e);
        }
    }
    
    /**
     * Search inventory by name
     */
    public CompletableFuture<List<Inventory>> searchByNameAsync(String name) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (name == null || name.trim().isEmpty()) {
                    return inventoryDAO.findAll();
                }
                return inventoryDAO.findByName(name);
            } catch (SQLException e) {
                throw new InventoryServiceException("Failed to search inventory by name", e);
            }
        }, executorService);
    }
    
    /**
     * Search inventory by location
     */
    public CompletableFuture<List<Inventory>> searchByLocationAsync(String location) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (location == null || location.trim().isEmpty()) {
                    return inventoryDAO.findAll();
                }
                return inventoryDAO.findByLocation(location);
            } catch (SQLException e) {
                throw new InventoryServiceException("Failed to search inventory by location", e);
            }
        }, executorService);
    }
    
    /**
     * Get low stock items
     */
    public CompletableFuture<List<Inventory>> getLowStockItemsAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return inventoryDAO.findLowStock(LOW_STOCK_THRESHOLD);
            } catch (SQLException e) {
                throw new InventoryServiceException("Failed to retrieve low stock items", e);
            }
        }, executorService);
    }
    
    /**
     * Get low stock items with custom threshold
     */
    public CompletableFuture<List<Inventory>> getLowStockItemsAsync(int threshold) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return inventoryDAO.findLowStock(threshold);
            } catch (SQLException e) {
                throw new InventoryServiceException("Failed to retrieve low stock items", e);
            }
        }, executorService);
    }
    
    /**
     * Update stock quantity with business logic
     */
    public CompletableFuture<Boolean> updateStockAsync(Integer itemId, Integer newQuantity) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                validateStockUpdate(itemId, newQuantity);
                return inventoryDAO.updateQuantity(itemId, newQuantity);
            } catch (SQLException e) {
                throw new InventoryServiceException("Failed to update stock quantity", e);
            }
        }, executorService);
    }
    
    /**
     * Add stock to existing inventory
     */
    public CompletableFuture<Boolean> addStockAsync(Integer itemId, Integer quantity) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                validateStockAddition(itemId, quantity);
                
                Optional<Inventory> inventoryOpt = inventoryDAO.findById(itemId);
                if (inventoryOpt.isEmpty()) {
                    throw new InventoryServiceException("Inventory item not found with ID: " + itemId);
                }
                
                Inventory inventory = inventoryOpt.get();
                inventory.addStock(quantity);
                
                return inventoryDAO.updateQuantity(itemId, inventory.getItemQuantity());
            } catch (SQLException e) {
                throw new InventoryServiceException("Failed to add stock", e);
            }
        }, executorService);
    }
    
    /**
     * Remove stock from inventory
     */
    public CompletableFuture<Boolean> removeStockAsync(Integer itemId, Integer quantity) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                validateStockRemoval(itemId, quantity);
                
                Optional<Inventory> inventoryOpt = inventoryDAO.findById(itemId);
                if (inventoryOpt.isEmpty()) {
                    throw new InventoryServiceException("Inventory item not found with ID: " + itemId);
                }
                
                Inventory inventory = inventoryOpt.get();
                
                if (inventory.getItemQuantity() < quantity) {
                    throw new InventoryServiceException("Insufficient stock. Available: " + 
                        inventory.getItemQuantity() + ", Requested: " + quantity);
                }
                
                inventory.removeStock(quantity);
                
                return inventoryDAO.updateQuantity(itemId, inventory.getItemQuantity());
            } catch (SQLException e) {
                throw new InventoryServiceException("Failed to remove stock", e);
            }
        }, executorService);
    }
    
    /**
     * Get inventory statistics
     */
    public CompletableFuture<InventoryStats> getInventoryStatsAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                int totalItems = inventoryDAO.getTotalCount();
                int totalQuantity = inventoryDAO.getTotalQuantity();
                List<Inventory> lowStockItems = inventoryDAO.findLowStock(LOW_STOCK_THRESHOLD);
                
                return new InventoryStats(totalItems, totalQuantity, lowStockItems.size());
            } catch (SQLException e) {
                throw new InventoryServiceException("Failed to retrieve inventory statistics", e);
            }
        }, executorService);
    }
    
    /**
     * Batch update multiple inventory items
     */
    public CompletableFuture<Boolean> batchUpdateAsync(List<Inventory> inventoryList) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                for (Inventory inventory : inventoryList) {
                    validateInventoryForUpdate(inventory);
                }
                
                boolean allSuccessful = true;
                for (Inventory inventory : inventoryList) {
                    boolean result = inventoryDAO.update(inventory);
                    if (!result) {
                        allSuccessful = false;
                    }
                }
                return allSuccessful;
            } catch (SQLException e) {
                throw new InventoryServiceException("Failed to batch update inventory items", e);
            }
        }, executorService);
    }
    
    // Validation methods
    private void validateInventoryForCreation(Inventory inventory) throws InventoryServiceException {
        if (inventory == null) {
            throw new InventoryServiceException("Inventory item cannot be null");
        }
        
        if (!inventory.isValid()) {
            throw new InventoryServiceException("Invalid inventory data");
        }
        
        if (inventory.getItemName().length() > 255) {
            throw new InventoryServiceException("Item name cannot exceed 255 characters");
        }
        
        if (inventory.getItemLocation().length() > 255) {
            throw new InventoryServiceException("Item location cannot exceed 255 characters");
        }
    }
    
    private void validateInventoryForUpdate(Inventory inventory) throws InventoryServiceException {
        validateInventoryForCreation(inventory);
        
        if (inventory.getItemId() == null) {
            throw new InventoryServiceException("Item ID is required for update");
        }
    }
    
    private void validateInventoryForDeletion(Integer itemId) throws InventoryServiceException {
        if (itemId == null) {
            throw new InventoryServiceException("Item ID cannot be null for deletion");
        }
    }
    
    private void validateStockUpdate(Integer itemId, Integer newQuantity) throws InventoryServiceException {
        if (itemId == null) {
            throw new InventoryServiceException("Item ID cannot be null");
        }
        
        if (newQuantity == null || newQuantity < 0) {
            throw new InventoryServiceException("Quantity must be non-negative");
        }
    }
    
    private void validateStockAddition(Integer itemId, Integer quantity) throws InventoryServiceException {
        if (itemId == null) {
            throw new InventoryServiceException("Item ID cannot be null");
        }
        
        if (quantity == null || quantity <= 0) {
            throw new InventoryServiceException("Addition quantity must be positive");
        }
    }
    
    private void validateStockRemoval(Integer itemId, Integer quantity) throws InventoryServiceException {
        if (itemId == null) {
            throw new InventoryServiceException("Item ID cannot be null");
        }
        
        if (quantity == null || quantity <= 0) {
            throw new InventoryServiceException("Removal quantity must be positive");
        }
    }
    
    /**
     * Shutdown executor service
     */
    public void shutdown() {
        executorService.shutdown();
    }
    
    /**
     * Inventory statistics class
     */
    public static class InventoryStats {
        private final int totalItems;
        private final int totalQuantity;
        private final int lowStockItems;
        
        public InventoryStats(int totalItems, int totalQuantity, int lowStockItems) {
            this.totalItems = totalItems;
            this.totalQuantity = totalQuantity;
            this.lowStockItems = lowStockItems;
        }
        
        public int getTotalItems() { return totalItems; }
        public int getTotalQuantity() { return totalQuantity; }
        public int getLowStockItems() { return lowStockItems; }
        
        @Override
        public String toString() {
            return String.format("InventoryStats{totalItems=%d, totalQuantity=%d, lowStockItems=%d}",
                    totalItems, totalQuantity, lowStockItems);
        }
    }
    
    /**
     * Custom exception for inventory service operations
     */
    public static class InventoryServiceException extends RuntimeException {
        public InventoryServiceException(String message) {
            super(message);
        }
        
        public InventoryServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}