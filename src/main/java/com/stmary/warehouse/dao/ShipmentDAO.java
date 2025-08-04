package com.stmary.warehouse.dao;

import com.stmary.warehouse.database.DatabaseManager;
import com.stmary.warehouse.model.Shipment;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Shipment operations
 * Implements CRUD operations with proper exception handling and validation
 */
public class ShipmentDAO {
    private final DatabaseManager dbManager;
    
    // SQL Statements
    private static final String INSERT_SHIPMENT = 
        "INSERT INTO shipments (destination, shipment_date, shipment_status) VALUES (?, ?, ?)";
    
    private static final String UPDATE_SHIPMENT = 
        "UPDATE shipments SET destination = ?, shipment_date = ?, shipment_status = ?, updated_at = CURRENT_TIMESTAMP WHERE shipment_id = ?";
    
    private static final String DELETE_SHIPMENT = 
        "DELETE FROM shipments WHERE shipment_id = ?";
    
    private static final String SELECT_ALL_SHIPMENTS = 
        "SELECT shipment_id, destination, shipment_date, shipment_status, created_at, updated_at FROM shipments ORDER BY shipment_date DESC";
    
    private static final String SELECT_SHIPMENT_BY_ID = 
        "SELECT shipment_id, destination, shipment_date, shipment_status, created_at, updated_at FROM shipments WHERE shipment_id = ?";
    
    private static final String SELECT_SHIPMENTS_BY_DESTINATION = 
        "SELECT shipment_id, destination, shipment_date, shipment_status, created_at, updated_at FROM shipments WHERE destination LIKE ? ORDER BY shipment_date DESC";
    
    private static final String SELECT_SHIPMENTS_BY_STATUS = 
        "SELECT shipment_id, destination, shipment_date, shipment_status, created_at, updated_at FROM shipments WHERE shipment_status = ? ORDER BY shipment_date DESC";
    
    private static final String SELECT_SHIPMENTS_BY_DATE_RANGE = 
        "SELECT shipment_id, destination, shipment_date, shipment_status, created_at, updated_at FROM shipments WHERE shipment_date BETWEEN ? AND ? ORDER BY shipment_date DESC";
    
    private static final String UPDATE_SHIPMENT_STATUS = 
        "UPDATE shipments SET shipment_status = ?, updated_at = CURRENT_TIMESTAMP WHERE shipment_id = ?";
    
    private static final String SELECT_SHIPMENTS_COUNT_BY_STATUS = 
        "SELECT shipment_status, COUNT(*) as count FROM shipments GROUP BY shipment_status";
    
    private static final String SELECT_RECENT_SHIPMENTS = 
        "SELECT shipment_id, destination, shipment_date, shipment_status, created_at, updated_at FROM shipments WHERE shipment_date >= ? ORDER BY shipment_date DESC LIMIT ?";
    
    private static final String SELECT_DELAYED_SHIPMENTS = 
        "SELECT shipment_id, destination, shipment_date, shipment_status, created_at, updated_at FROM shipments WHERE shipment_date < ? AND shipment_status NOT IN ('Delivered', 'Cancelled', 'Returned') ORDER BY shipment_date";
    
    public ShipmentDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }
    
    /**
     * Create a new shipment
     */
    public Shipment create(Shipment shipment) throws SQLException {
        if (shipment == null || !shipment.isValid()) {
            throw new IllegalArgumentException("Invalid shipment data");
        }
        
        return dbManager.executeTransaction(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(INSERT_SHIPMENT, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, shipment.getDestination());
                stmt.setDate(2, Date.valueOf(shipment.getShipmentDate()));
                stmt.setString(3, shipment.getShipmentStatusString());
                
                int affectedRows = stmt.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Creating shipment failed, no rows affected");
                }
                
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        shipment.setShipmentId(generatedKeys.getInt(1));
                        return shipment;
                    } else {
                        throw new SQLException("Creating shipment failed, no ID obtained");
                    }
                }
            }
        });
    }
    
    /**
     * Update an existing shipment
     */
    public boolean update(Shipment shipment) throws SQLException {
        if (shipment == null || shipment.getShipmentId() == null || !shipment.isValid()) {
            throw new IllegalArgumentException("Invalid shipment data for update");
        }
        
        return dbManager.executeTransaction(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(UPDATE_SHIPMENT)) {
                stmt.setString(1, shipment.getDestination());
                stmt.setDate(2, Date.valueOf(shipment.getShipmentDate()));
                stmt.setString(3, shipment.getShipmentStatusString());
                stmt.setInt(4, shipment.getShipmentId());
                
                int affectedRows = stmt.executeUpdate();
                return affectedRows > 0;
            }
        });
    }
    
    /**
     * Delete a shipment
     */
    public boolean delete(Integer shipmentId) throws SQLException {
        if (shipmentId == null) {
            throw new IllegalArgumentException("Shipment ID cannot be null");
        }
        
        return dbManager.executeTransaction(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(DELETE_SHIPMENT)) {
                stmt.setInt(1, shipmentId);
                int affectedRows = stmt.executeUpdate();
                return affectedRows > 0;
            }
        });
    }
    
    /**
     * Find shipment by ID
     */
    public Optional<Shipment> findById(Integer shipmentId) throws SQLException {
        if (shipmentId == null) {
            return Optional.empty();
        }
        
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SELECT_SHIPMENT_BY_ID)) {
                stmt.setInt(1, shipmentId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapResultSetToShipment(rs));
                    }
                }
            }
        } finally {
            if (conn != null) {
                dbManager.returnConnection(conn);
            }
        }
        return Optional.empty();
    }
    
    /**
     * Find all shipments
     */
    public List<Shipment> findAll() throws SQLException {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SELECT_ALL_SHIPMENTS);
                 ResultSet rs = stmt.executeQuery()) {
                
                List<Shipment> shipments = new ArrayList<>();
                while (rs.next()) {
                    shipments.add(mapResultSetToShipment(rs));
                }
                return shipments;
            }
        } finally {
            if (conn != null) {
                dbManager.returnConnection(conn);
            }
        }
    }
    
    /**
     * Search shipments by destination
     */
    public List<Shipment> findByDestination(String destination) throws SQLException {
        if (destination == null || destination.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SELECT_SHIPMENTS_BY_DESTINATION)) {
                stmt.setString(1, "%" + destination.trim() + "%");
                try (ResultSet rs = stmt.executeQuery()) {
                    List<Shipment> shipments = new ArrayList<>();
                    while (rs.next()) {
                        shipments.add(mapResultSetToShipment(rs));
                    }
                    return shipments;
                }
            }
        } finally {
            if (conn != null) {
                dbManager.returnConnection(conn);
            }
        }
    }
    
    /**
     * Find shipments by status
     */
    public List<Shipment> findByStatus(Shipment.ShipmentStatus status) throws SQLException {
        if (status == null) {
            return new ArrayList<>();
        }
        
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SELECT_SHIPMENTS_BY_STATUS)) {
                stmt.setString(1, status.getDisplayName());
                try (ResultSet rs = stmt.executeQuery()) {
                    List<Shipment> shipments = new ArrayList<>();
                    while (rs.next()) {
                        shipments.add(mapResultSetToShipment(rs));
                    }
                    return shipments;
                }
            }
        } finally {
            if (conn != null) {
                dbManager.returnConnection(conn);
            }
        }
    }
    
    /**
     * Find shipments within date range
     */
    public List<Shipment> findByDateRange(LocalDate startDate, LocalDate endDate) throws SQLException {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date cannot be null");
        }
        
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }
        
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SELECT_SHIPMENTS_BY_DATE_RANGE)) {
                stmt.setDate(1, Date.valueOf(startDate));
                stmt.setDate(2, Date.valueOf(endDate));
                try (ResultSet rs = stmt.executeQuery()) {
                    List<Shipment> shipments = new ArrayList<>();
                    while (rs.next()) {
                        shipments.add(mapResultSetToShipment(rs));
                    }
                    return shipments;
                }
            }
        } finally {
            if (conn != null) {
                dbManager.returnConnection(conn);
            }
        }
    }
    
    /**
     * Get recent shipments
     */
    public List<Shipment> findRecentShipments(int days, int limit) throws SQLException {
        if (days < 0 || limit < 0) {
            throw new IllegalArgumentException("Days and limit must be non-negative");
        }
        
        LocalDate cutoffDate = LocalDate.now().minusDays(days);
        
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SELECT_RECENT_SHIPMENTS)) {
                stmt.setDate(1, Date.valueOf(cutoffDate));
                stmt.setInt(2, limit);
                try (ResultSet rs = stmt.executeQuery()) {
                    List<Shipment> shipments = new ArrayList<>();
                    while (rs.next()) {
                        shipments.add(mapResultSetToShipment(rs));
                    }
                    return shipments;
                }
            }
        } finally {
            if (conn != null) {
                dbManager.returnConnection(conn);
            }
        }
    }
    
    /**
     * Find delayed shipments
     */
    public List<Shipment> findDelayedShipments(int expectedDays) throws SQLException {
        if (expectedDays < 0) {
            throw new IllegalArgumentException("Expected days must be non-negative");
        }
        
        LocalDate cutoffDate = LocalDate.now().minusDays(expectedDays);
        
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SELECT_DELAYED_SHIPMENTS)) {
                stmt.setDate(1, Date.valueOf(cutoffDate));
                try (ResultSet rs = stmt.executeQuery()) {
                    List<Shipment> shipments = new ArrayList<>();
                    while (rs.next()) {
                        shipments.add(mapResultSetToShipment(rs));
                    }
                    return shipments;
                }
            }
        } finally {
            if (conn != null) {
                dbManager.returnConnection(conn);
            }
        }
    }
    
    /**
     * Update only the status of a shipment
     */
    public boolean updateStatus(Integer shipmentId, Shipment.ShipmentStatus newStatus) throws SQLException {
        if (shipmentId == null || newStatus == null) {
            throw new IllegalArgumentException("Shipment ID and status cannot be null");
        }
        
        return dbManager.executeTransaction(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(UPDATE_SHIPMENT_STATUS)) {
                stmt.setString(1, newStatus.getDisplayName());
                stmt.setInt(2, shipmentId);
                int affectedRows = stmt.executeUpdate();
                return affectedRows > 0;
            }
        });
    }
    
    /**
     * Get count of shipments by status
     */
    public List<StatusCount> getShipmentCountsByStatus() throws SQLException {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SELECT_SHIPMENTS_COUNT_BY_STATUS);
                 ResultSet rs = stmt.executeQuery()) {
                
                List<StatusCount> counts = new ArrayList<>();
                while (rs.next()) {
                    counts.add(new StatusCount(
                        rs.getString("shipment_status"),
                        rs.getInt("count")
                    ));
                }
                return counts;
            }
        } finally {
            if (conn != null) {
                dbManager.returnConnection(conn);
            }
        }
    }
    
    /**
     * Map ResultSet to Shipment object
     */
    private Shipment mapResultSetToShipment(ResultSet rs) throws SQLException {
        return new Shipment(
            rs.getInt("shipment_id"),
            rs.getString("destination"),
            rs.getDate("shipment_date").toLocalDate(),
            Shipment.ShipmentStatus.fromString(rs.getString("shipment_status"))
        );
    }
    
    /**
     * Get total count of shipments
     */
    public int getTotalCount() throws SQLException {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM shipments");
                 ResultSet rs = stmt.executeQuery()) {
                
                return rs.next() ? rs.getInt(1) : 0;
            }
        } finally {
            if (conn != null) {
                dbManager.returnConnection(conn);
            }
        }
    }
    
    /**
     * Status count helper class
     */
    public static class StatusCount {
        private final String status;
        private final int count;
        
        public StatusCount(String status, int count) {
            this.status = status;
            this.count = count;
        }
        
        public String getStatus() { return status; }
        public int getCount() { return count; }
        
        @Override
        public String toString() {
            return String.format("StatusCount{status='%s', count=%d}", status, count);
        }
    }
}