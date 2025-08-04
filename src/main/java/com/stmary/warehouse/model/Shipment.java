package com.stmary.warehouse.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Shipment entity representing warehouse shipments
 * Implements proper encapsulation with validation and business logic
 */
public class Shipment {
    private Integer shipmentId;
    private String destination;
    private LocalDate shipmentDate;
    private ShipmentStatus shipmentStatus;
    
    // Shipment status enum for type safety
    public enum ShipmentStatus {
        PREPARING("Preparing"),
        IN_TRANSIT("In Transit"),
        OUT_FOR_DELIVERY("Out for Delivery"),
        DELIVERED("Delivered"),
        RETURNED("Returned"),
        CANCELLED("Cancelled");
        
        private final String displayName;
        
        ShipmentStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public static ShipmentStatus fromString(String status) {
            if (status == null) return null;
            for (ShipmentStatus s : ShipmentStatus.values()) {
                if (s.displayName.equalsIgnoreCase(status) || s.name().equalsIgnoreCase(status)) {
                    return s;
                }
            }
            throw new IllegalArgumentException("Invalid shipment status: " + status);
        }
    }
    
    // Default constructor
    public Shipment() {
        this.shipmentDate = LocalDate.now();
        this.shipmentStatus = ShipmentStatus.PREPARING;
    }
    
    // Constructor with parameters
    public Shipment(String destination) {
        this();
        this.destination = destination;
    }
    
    // Constructor with date
    public Shipment(String destination, LocalDate shipmentDate, ShipmentStatus shipmentStatus) {
        this.destination = destination;
        this.shipmentDate = shipmentDate;
        this.shipmentStatus = shipmentStatus;
    }
    
    // Full constructor
    public Shipment(Integer shipmentId, String destination, LocalDate shipmentDate, ShipmentStatus shipmentStatus) {
        this.shipmentId = shipmentId;
        this.destination = destination;
        this.shipmentDate = shipmentDate;
        this.shipmentStatus = shipmentStatus;
    }
    
    // Getters and setters with validation
    public Integer getShipmentId() {
        return shipmentId;
    }
    
    public void setShipmentId(Integer shipmentId) {
        this.shipmentId = shipmentId;
    }
    
    public String getDestination() {
        return destination;
    }
    
    public void setDestination(String destination) {
        if (destination == null || destination.trim().isEmpty()) {
            throw new IllegalArgumentException("Destination cannot be null or empty");
        }
        this.destination = destination.trim();
    }
    
    public LocalDate getShipmentDate() {
        return shipmentDate;
    }
    
    public void setShipmentDate(LocalDate shipmentDate) {
        if (shipmentDate == null) {
            throw new IllegalArgumentException("Shipment date cannot be null");
        }
        if (shipmentDate.isAfter(LocalDate.now().plusDays(7))) {
            throw new IllegalArgumentException("Shipment date cannot be more than 7 days in the future");
        }
        this.shipmentDate = shipmentDate;
    }
    
    public ShipmentStatus getShipmentStatus() {
        return shipmentStatus;
    }
    
    public void setShipmentStatus(ShipmentStatus shipmentStatus) {
        if (shipmentStatus == null) {
            throw new IllegalArgumentException("Shipment status cannot be null");
        }
        this.shipmentStatus = shipmentStatus;
    }
    
    // Convenience method for string status
    public void setShipmentStatusFromString(String status) {
        this.shipmentStatus = ShipmentStatus.fromString(status);
    }
    
    public String getShipmentStatusString() {
        return shipmentStatus != null ? shipmentStatus.getDisplayName() : null;
    }
    
    // Validation method
    public boolean isValid() {
        return destination != null && !destination.trim().isEmpty() &&
               shipmentDate != null && !shipmentDate.isAfter(LocalDate.now().plusDays(7)) &&
               shipmentStatus != null;
    }
    
    // Business logic methods
    public boolean canBeCancelled() {
        return shipmentStatus == ShipmentStatus.PREPARING || shipmentStatus == ShipmentStatus.IN_TRANSIT;
    }
    
    public boolean canBeTracked() {
        return shipmentStatus == ShipmentStatus.IN_TRANSIT || shipmentStatus == ShipmentStatus.OUT_FOR_DELIVERY;
    }
    
    public boolean isCompleted() {
        return shipmentStatus == ShipmentStatus.DELIVERED || 
               shipmentStatus == ShipmentStatus.RETURNED || 
               shipmentStatus == ShipmentStatus.CANCELLED;
    }
    
    public void ship() {
        if (shipmentStatus != ShipmentStatus.PREPARING) {
            throw new IllegalStateException("Only preparing shipments can be shipped");
        }
        this.shipmentStatus = ShipmentStatus.IN_TRANSIT;
    }
    
    public void outForDelivery() {
        if (shipmentStatus != ShipmentStatus.IN_TRANSIT) {
            throw new IllegalStateException("Only in-transit shipments can be out for delivery");
        }
        this.shipmentStatus = ShipmentStatus.OUT_FOR_DELIVERY;
    }
    
    public void deliver() {
        if (shipmentStatus != ShipmentStatus.OUT_FOR_DELIVERY) {
            throw new IllegalStateException("Only out-for-delivery shipments can be delivered");
        }
        this.shipmentStatus = ShipmentStatus.DELIVERED;
    }
    
    public void returnShipment() {
        if (shipmentStatus == ShipmentStatus.DELIVERED || shipmentStatus == ShipmentStatus.CANCELLED) {
            throw new IllegalStateException("Cannot return delivered or cancelled shipments");
        }
        this.shipmentStatus = ShipmentStatus.RETURNED;
    }
    
    public void cancelShipment() {
        if (!canBeCancelled()) {
            throw new IllegalStateException("Shipment cannot be cancelled in current status: " + shipmentStatus);
        }
        this.shipmentStatus = ShipmentStatus.CANCELLED;
    }
    
    public long getDaysInTransit() {
        return shipmentDate.until(LocalDate.now()).getDays();
    }
    
    public boolean isDelayed(int expectedDays) {
        return getDaysInTransit() > expectedDays && !isCompleted();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Shipment shipment = (Shipment) o;
        return Objects.equals(shipmentId, shipment.shipmentId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(shipmentId);
    }
    
    @Override
    public String toString() {
        return String.format("Shipment{shipmentId=%d, destination='%s', shipmentDate=%s, shipmentStatus=%s}",
                shipmentId, destination, shipmentDate, shipmentStatus);
    }
}