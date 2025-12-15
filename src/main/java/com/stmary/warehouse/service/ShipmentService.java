package com.stmary.warehouse.service;

import com.stmary.warehouse.dao.ShipmentDAO;
import com.stmary.warehouse.model.Shipment;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service layer for Shipment business logic
 * Implements validation, business rules, and async operations
 */
public class ShipmentService {
    private final ShipmentDAO shipmentDAO;
    private final ExecutorService executorService;
    private final int DELAYED_SHIPMENT_DAYS = 7; // Default delay threshold
    
    public ShipmentService() {
        this.shipmentDAO = new ShipmentDAO();
        this.executorService = Executors.newFixedThreadPool(5);
    }
    
    public ShipmentService(ShipmentDAO shipmentDAO) {
        this.shipmentDAO = shipmentDAO;
        this.executorService = Executors.newFixedThreadPool(5);
    }
    
    /**
     * Create a new shipment with business validation
     */
    public CompletableFuture<Shipment> createShipmentAsync(Shipment shipment) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                validateShipmentForCreation(shipment);
                return shipmentDAO.create(shipment);
            } catch (SQLException e) {
                throw new ShipmentServiceException("Failed to create shipment", e);
            }
        }, executorService);
    }
    
    /**
     * Create shipment synchronously
     */
    public Shipment createShipment(Shipment shipment) throws ShipmentServiceException {
        try {
            validateShipmentForCreation(shipment);
            return shipmentDAO.create(shipment);
        } catch (SQLException e) {
            throw new ShipmentServiceException("Failed to create shipment", e);
        }
    }
    
    /**
     * Update shipment with business validation
     */
    public CompletableFuture<Boolean> updateShipmentAsync(Shipment shipment) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                validateShipmentForUpdate(shipment);
                return shipmentDAO.update(shipment);
            } catch (SQLException e) {
                throw new ShipmentServiceException("Failed to update shipment", e);
            }
        }, executorService);
    }
    
    /**
     * Update shipment synchronously
     */
    public boolean updateShipment(Shipment shipment) throws ShipmentServiceException {
        try {
            validateShipmentForUpdate(shipment);
            return shipmentDAO.update(shipment);
        } catch (SQLException e) {
            throw new ShipmentServiceException("Failed to update shipment", e);
        }
    }
    
    /**
     * Delete shipment with business rules
     */
    public CompletableFuture<Boolean> deleteShipmentAsync(Integer shipmentId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                validateShipmentForDeletion(shipmentId);
                
                // Check if shipment can be deleted based on status
                Optional<Shipment> shipmentOpt = shipmentDAO.findById(shipmentId);
                if (shipmentOpt.isPresent()) {
                    Shipment shipment = shipmentOpt.get();
                    if (shipment.getShipmentStatus() == Shipment.ShipmentStatus.DELIVERED) {
                        throw new ShipmentServiceException("Cannot delete delivered shipments");
                    }
                }
                
                return shipmentDAO.delete(shipmentId);
            } catch (SQLException e) {
                throw new ShipmentServiceException("Failed to delete shipment", e);
            }
        }, executorService);
    }
    
    /**
     * Delete shipment synchronously
     */
    public boolean deleteShipment(Integer shipmentId) throws ShipmentServiceException {
        try {
            validateShipmentForDeletion(shipmentId);
            
            // Check if shipment can be deleted based on status
            Optional<Shipment> shipmentOpt = shipmentDAO.findById(shipmentId);
            if (shipmentOpt.isPresent()) {
                Shipment shipment = shipmentOpt.get();
                if (shipment.getShipmentStatus() == Shipment.ShipmentStatus.DELIVERED) {
                    throw new ShipmentServiceException("Cannot delete delivered shipments");
                }
            }
            
            return shipmentDAO.delete(shipmentId);
        } catch (SQLException e) {
            throw new ShipmentServiceException("Failed to delete shipment", e);
        }
    }
    
    /**
     * Find shipment by ID
     */
    public CompletableFuture<Optional<Shipment>> findShipmentByIdAsync(Integer shipmentId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return shipmentDAO.findById(shipmentId);
            } catch (SQLException e) {
                throw new ShipmentServiceException("Failed to find shipment", e);
            }
        }, executorService);
    }
    
    /**
     * Find shipment by ID synchronously
     */
    public Optional<Shipment> findShipmentById(Integer shipmentId) throws ShipmentServiceException {
        try {
            return shipmentDAO.findById(shipmentId);
        } catch (SQLException e) {
            throw new ShipmentServiceException("Failed to find shipment", e);
        }
    }
    
    /**
     * Get all shipments
     */
    public CompletableFuture<List<Shipment>> getAllShipmentsAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return shipmentDAO.findAll();
            } catch (SQLException e) {
                throw new ShipmentServiceException("Failed to retrieve shipments", e);
            }
        }, executorService);
    }
    
    /**
     * Get all shipments synchronously
     */
    public List<Shipment> getAllShipments() throws ShipmentServiceException {
        try {
            return shipmentDAO.findAll();
        } catch (SQLException e) {
            throw new ShipmentServiceException("Failed to retrieve shipments", e);
        }
    }
    
    /**
     * Search shipments by destination
     */
    public CompletableFuture<List<Shipment>> searchByDestinationAsync(String destination) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (destination == null || destination.trim().isEmpty()) {
                    return shipmentDAO.findAll();
                }
                return shipmentDAO.findByDestination(destination);
            } catch (SQLException e) {
                throw new ShipmentServiceException("Failed to search shipments by destination", e);
            }
        }, executorService);
    }
    
    /**
     * Get shipments by status
     */
    public CompletableFuture<List<Shipment>> getShipmentsByStatusAsync(Shipment.ShipmentStatus status) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return shipmentDAO.findByStatus(status);
            } catch (SQLException e) {
                throw new ShipmentServiceException("Failed to get shipments by status", e);
            }
        }, executorService);
    }
    
    /**
     * Get shipments within date range
     */
    public CompletableFuture<List<Shipment>> getShipmentsByDateRangeAsync(LocalDate startDate, LocalDate endDate) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                validateDateRange(startDate, endDate);
                return shipmentDAO.findByDateRange(startDate, endDate);
            } catch (SQLException e) {
                throw new ShipmentServiceException("Failed to get shipments by date range", e);
            }
        }, executorService);
    }
    
    /**
     * Get recent shipments
     */
    public CompletableFuture<List<Shipment>> getRecentShipmentsAsync(int days, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                validateRecentShipmentsParams(days, limit);
                return shipmentDAO.findRecentShipments(days, limit);
            } catch (SQLException e) {
                throw new ShipmentServiceException("Failed to get recent shipments", e);
            }
        }, executorService);
    }
    
    /**
     * Get delayed shipments
     */
    public CompletableFuture<List<Shipment>> getDelayedShipmentsAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return shipmentDAO.findDelayedShipments(DELAYED_SHIPMENT_DAYS);
            } catch (SQLException e) {
                throw new ShipmentServiceException("Failed to get delayed shipments", e);
            }
        }, executorService);
    }
    
    /**
     * Get delayed shipments with custom threshold
     */
    public CompletableFuture<List<Shipment>> getDelayedShipmentsAsync(int delayThreshold) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return shipmentDAO.findDelayedShipments(delayThreshold);
            } catch (SQLException e) {
                throw new ShipmentServiceException("Failed to get delayed shipments", e);
            }
        }, executorService);
    }
    
    /**
     * Update shipment status with business logic validation
     */
    public CompletableFuture<Boolean> updateShipmentStatusAsync(Integer shipmentId, Shipment.ShipmentStatus newStatus) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                validateStatusUpdate(shipmentId, newStatus);
                
                // Get current shipment to validate status transition
                Optional<Shipment> shipmentOpt = shipmentDAO.findById(shipmentId);
                if (shipmentOpt.isEmpty()) {
                    throw new ShipmentServiceException("Shipment not found with ID: " + shipmentId);
                }
                
                Shipment shipment = shipmentOpt.get();
                validateStatusTransition(shipment.getShipmentStatus(), newStatus);
                
                return shipmentDAO.updateStatus(shipmentId, newStatus);
            } catch (SQLException e) {
                throw new ShipmentServiceException("Failed to update shipment status", e);
            }
        }, executorService);
    }
    
    /**
     * Shipment workflow methods
     */
    public CompletableFuture<Boolean> shipAsync(Integer shipmentId) {
        return updateShipmentStatusAsync(shipmentId, Shipment.ShipmentStatus.IN_TRANSIT);
    }
    
    public CompletableFuture<Boolean> outForDeliveryAsync(Integer shipmentId) {
        return updateShipmentStatusAsync(shipmentId, Shipment.ShipmentStatus.OUT_FOR_DELIVERY);
    }
    
    public CompletableFuture<Boolean> deliverAsync(Integer shipmentId) {
        return updateShipmentStatusAsync(shipmentId, Shipment.ShipmentStatus.DELIVERED);
    }
    
    public CompletableFuture<Boolean> returnShipmentAsync(Integer shipmentId) {
        return updateShipmentStatusAsync(shipmentId, Shipment.ShipmentStatus.RETURNED);
    }
    
    public CompletableFuture<Boolean> cancelShipmentAsync(Integer shipmentId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Optional<Shipment> shipmentOpt = shipmentDAO.findById(shipmentId);
                if (shipmentOpt.isEmpty()) {
                    throw new ShipmentServiceException("Shipment not found with ID: " + shipmentId);
                }
                
                Shipment shipment = shipmentOpt.get();
                if (!shipment.canBeCancelled()) {
                    throw new ShipmentServiceException("Shipment cannot be cancelled in current status: " + 
                        shipment.getShipmentStatus().getDisplayName());
                }
                
                return shipmentDAO.updateStatus(shipmentId, Shipment.ShipmentStatus.CANCELLED);
            } catch (SQLException e) {
                throw new ShipmentServiceException("Failed to cancel shipment", e);
            }
        }, executorService);
    }
    
    /**
     * Get in-transit shipments for tracking
     */
    public CompletableFuture<List<Shipment>> getTrackableShipmentsAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Shipment> inTransit = shipmentDAO.findByStatus(Shipment.ShipmentStatus.IN_TRANSIT);
                List<Shipment> outForDelivery = shipmentDAO.findByStatus(Shipment.ShipmentStatus.OUT_FOR_DELIVERY);
                
                inTransit.addAll(outForDelivery);
                return inTransit;
            } catch (SQLException e) {
                throw new ShipmentServiceException("Failed to get trackable shipments", e);
            }
        }, executorService);
    }
    
    /**
     * Get shipment statistics
     */
    public CompletableFuture<ShipmentStats> getShipmentStatsAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                int totalShipments = shipmentDAO.getTotalCount();
                List<ShipmentDAO.StatusCount> statusCounts = shipmentDAO.getShipmentCountsByStatus();
                List<Shipment> delayed = shipmentDAO.findDelayedShipments(DELAYED_SHIPMENT_DAYS);
                
                return new ShipmentStats(totalShipments, statusCounts, delayed.size());
            } catch (SQLException e) {
                throw new ShipmentServiceException("Failed to retrieve shipment statistics", e);
            }
        }, executorService);
    }
    
    /**
     * Batch update multiple shipments
     */
    public CompletableFuture<Boolean> batchUpdateAsync(List<Shipment> shipments) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                for (Shipment shipment : shipments) {
                    validateShipmentForUpdate(shipment);
                }
                
                boolean allSuccessful = true;
                for (Shipment shipment : shipments) {
                    boolean result = shipmentDAO.update(shipment);
                    if (!result) {
                        allSuccessful = false;
                    }
                }
                return allSuccessful;
            } catch (SQLException e) {
                throw new ShipmentServiceException("Failed to batch update shipments", e);
            }
        }, executorService);
    }
    
    // Validation methods
    private void validateShipmentForCreation(Shipment shipment) throws ShipmentServiceException {
        if (shipment == null) {
            throw new ShipmentServiceException("Shipment cannot be null");
        }
        
        if (!shipment.isValid()) {
            throw new ShipmentServiceException("Invalid shipment data");
        }
        
        if (shipment.getDestination().length() > 255) {
            throw new ShipmentServiceException("Destination cannot exceed 255 characters");
        }
    }
    
    private void validateShipmentForUpdate(Shipment shipment) throws ShipmentServiceException {
        validateShipmentForCreation(shipment);
        
        if (shipment.getShipmentId() == null) {
            throw new ShipmentServiceException("Shipment ID is required for update");
        }
    }
    
    private void validateShipmentForDeletion(Integer shipmentId) throws ShipmentServiceException {
        if (shipmentId == null) {
            throw new ShipmentServiceException("Shipment ID cannot be null for deletion");
        }
    }
    
    private void validateStatusUpdate(Integer shipmentId, Shipment.ShipmentStatus newStatus) throws ShipmentServiceException {
        if (shipmentId == null) {
            throw new ShipmentServiceException("Shipment ID cannot be null");
        }
        
        if (newStatus == null) {
            throw new ShipmentServiceException("New status cannot be null");
        }
    }
    
    private void validateStatusTransition(Shipment.ShipmentStatus currentStatus, Shipment.ShipmentStatus newStatus) 
            throws ShipmentServiceException {
        
        // Define valid status transitions
        boolean isValidTransition = switch (currentStatus) {
            case PREPARING -> newStatus == Shipment.ShipmentStatus.IN_TRANSIT || 
                            newStatus == Shipment.ShipmentStatus.CANCELLED;
            case IN_TRANSIT -> newStatus == Shipment.ShipmentStatus.OUT_FOR_DELIVERY || 
                              newStatus == Shipment.ShipmentStatus.RETURNED ||
                              newStatus == Shipment.ShipmentStatus.CANCELLED;
            case OUT_FOR_DELIVERY -> newStatus == Shipment.ShipmentStatus.DELIVERED || 
                                   newStatus == Shipment.ShipmentStatus.RETURNED;
            case DELIVERED, RETURNED, CANCELLED -> false; // Final states
        };
        
        if (!isValidTransition) {
            throw new ShipmentServiceException(
                String.format("Invalid status transition from %s to %s", 
                    currentStatus.getDisplayName(), newStatus.getDisplayName()));
        }
    }
    
    private void validateDateRange(LocalDate startDate, LocalDate endDate) throws ShipmentServiceException {
        if (startDate == null || endDate == null) {
            throw new ShipmentServiceException("Start date and end date cannot be null");
        }
        
        if (startDate.isAfter(endDate)) {
            throw new ShipmentServiceException("Start date cannot be after end date");
        }
    }
    
    private void validateRecentShipmentsParams(int days, int limit) throws ShipmentServiceException {
        if (days < 0) {
            throw new ShipmentServiceException("Days must be non-negative");
        }
        
        if (limit < 0) {
            throw new ShipmentServiceException("Limit must be non-negative");
        }
    }
    
    /**
     * Shutdown executor service
     */
    public void shutdown() {
        executorService.shutdown();
    }
    
    /**
     * Shipment statistics class
     */
    public static class ShipmentStats {
        private final int totalShipments;
        private final List<ShipmentDAO.StatusCount> statusCounts;
        private final int delayedShipments;
        
        public ShipmentStats(int totalShipments, List<ShipmentDAO.StatusCount> statusCounts, int delayedShipments) {
            this.totalShipments = totalShipments;
            this.statusCounts = statusCounts;
            this.delayedShipments = delayedShipments;
        }
        
        public int getTotalShipments() { return totalShipments; }
        public List<ShipmentDAO.StatusCount> getStatusCounts() { return statusCounts; }
        public int getDelayedShipments() { return delayedShipments; }
        
        @Override
        public String toString() {
            return String.format("ShipmentStats{totalShipments=%d, statusCounts=%s, delayedShipments=%d}",
                    totalShipments, statusCounts, delayedShipments);
        }
    }
    
    /**
     * Custom exception for shipment service operations
     */
    public static class ShipmentServiceException extends RuntimeException {
        public ShipmentServiceException(String message) {
            super(message);
        }
        
        public ShipmentServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}