package com.stmary.warehouse.dao;

import com.stmary.warehouse.database.DatabaseManager;
import com.stmary.warehouse.model.Inventory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Inventory operations
 * Implements CRUD operations with proper exception handling and validation
 */
public class InventoryDAO {
    private final DatabaseManager dbManager;
    
    // SQL Statements
    private static final String INSERT_INVENTORY = 
        "INSERT INTO inventory (item_name, item_quantity, item_location) VALUES (?, ?, ?)";
    
    private static final String UPDATE_INVENTORY = 
        "UPDATE inventory SET item_name = ?, item_quantity = ?, item_location = ?, updated_at = CURRENT_TIMESTAMP WHERE item_id = ?";
    
    private static final String DELETE_INVENTORY = 
        "DELETE FROM inventory WHERE item_id = ?";
    
    private static final String SELECT_ALL_INVENTORY = 
        "SELECT item_id, item_name, item_quantity, item_location, created_at, updated_at FROM inventory ORDER BY item_name";
    
    private static final String SELECT_INVENTORY_BY_ID = 
        "SELECT item_id, item_name, item_quantity, item_location, created_at, updated_at FROM inventory WHERE item_id = ?";
    
    private static final String SELECT_INVENTORY_BY_NAME = 
        "SELECT item_id, item_name, item_quantity, item_location, created_at, updated_at FROM inventory WHERE item_name LIKE ? ORDER BY item_name";
    
    private static final String SELECT_INVENTORY_BY_LOCATION = 
        "SELECT item_id, item_name, item_quantity, item_location, created_at, updated_at FROM inventory WHERE item_location LIKE ? ORDER BY item_name";
    
    private static final String SELECT_LOW_STOCK = 
        "SELECT item_id, item_name, item_quantity, item_location, created_at, updated_at FROM inventory WHERE item_quantity <= ? ORDER BY item_quantity";
    
    private static final String UPDATE_QUANTITY = 
        "UPDATE inventory SET item_quantity = ?, updated_at = CURRENT_TIMESTAMP WHERE item_id = ?";
    
    private static final String CHECK_NAME_EXISTS = 
        "SELECT COUNT(*) FROM inventory WHERE item_name = ? AND item_id != ?";
    
    public InventoryDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }
    
    /**
     * Create a new inventory item
     */
    public Inventory create(Inventory inventory) throws SQLException {
        if (inventory == null || !inventory.isValid()) {
            throw new IllegalArgumentException("Invalid inventory data");
        }
        
        // Check for duplicate name
        if (isNameExists(inventory.getItemName(), null)) {
            throw new SQLException("Item name already exists: " + inventory.getItemName());
        }
        
        return dbManager.executeTransaction(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(INSERT_INVENTORY, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, inventory.getItemName());
                stmt.setInt(2, inventory.getItemQuantity());
                stmt.setString(3, inventory.getItemLocation());
                
                int affectedRows = stmt.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Creating inventory failed, no rows affected");
                }
                
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        inventory.setItemId(generatedKeys.getInt(1));
                        return inventory;
                    } else {
                        throw new SQLException("Creating inventory failed, no ID obtained");
                    }
                }
            }
        });
    }
    
    /**
     * Update an existing inventory item
     */
    public boolean update(Inventory inventory) throws SQLException {
        if (inventory == null || inventory.getItemId() == null || !inventory.isValid()) {
            throw new IllegalArgumentException("Invalid inventory data for update");
        }
        
        // Check for duplicate name (excluding current item)
        if (isNameExists(inventory.getItemName(), inventory.getItemId())) {
            throw new SQLException("Item name already exists: " + inventory.getItemName());
        }
        
        return dbManager.executeTransaction(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(UPDATE_INVENTORY)) {
                stmt.setString(1, inventory.getItemName());
                stmt.setInt(2, inventory.getItemQuantity());
                stmt.setString(3, inventory.getItemLocation());
                stmt.setInt(4, inventory.getItemId());
                
                int affectedRows = stmt.executeUpdate();
                return affectedRows > 0;
            }
        });
    }
    
    /**
     * Delete an inventory item
     */
    public boolean delete(Integer itemId) throws SQLException {
        if (itemId == null) {
            throw new IllegalArgumentException("Item ID cannot be null");
        }
        
        return dbManager.executeTransaction(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(DELETE_INVENTORY)) {
                stmt.setInt(1, itemId);
                int affectedRows = stmt.executeUpdate();
                return affectedRows > 0;
            }
        });
    }
    
    /**
     * Find inventory item by ID
     */
    public Optional<Inventory> findById(Integer itemId) throws SQLException {
        if (itemId == null) {
            return Optional.empty();
        }
        
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SELECT_INVENTORY_BY_ID)) {
                stmt.setInt(1, itemId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapResultSetToInventory(rs));
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
     * Find all inventory items
     */
    public List<Inventory> findAll() throws SQLException {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SELECT_ALL_INVENTORY);
                 ResultSet rs = stmt.executeQuery()) {
                
                List<Inventory> inventories = new ArrayList<>();
                while (rs.next()) {
                    inventories.add(mapResultSetToInventory(rs));
                }
                return inventories;
            }
        } finally {
            if (conn != null) {
                dbManager.returnConnection(conn);
            }
        }
    }
    
    /**
     * Search inventory items by name
     */
    public List<Inventory> findByName(String name) throws SQLException {
        if (name == null || name.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SELECT_INVENTORY_BY_NAME)) {
                stmt.setString(1, "%" + name.trim() + "%");
                try (ResultSet rs = stmt.executeQuery()) {
                    List<Inventory> inventories = new ArrayList<>();
                    while (rs.next()) {
                        inventories.add(mapResultSetToInventory(rs));
                    }
                    return inventories;
                }
            }
        } finally {
            if (conn != null) {
                dbManager.returnConnection(conn);
            }
        }
    }
    
    /**
     * Find inventory items by location
     */
    public List<Inventory> findByLocation(String location) throws SQLException {
        if (location == null || location.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SELECT_INVENTORY_BY_LOCATION)) {
                stmt.setString(1, "%" + location.trim() + "%");
                try (ResultSet rs = stmt.executeQuery()) {
                    List<Inventory> inventories = new ArrayList<>();
                    while (rs.next()) {
                        inventories.add(mapResultSetToInventory(rs));
                    }
                    return inventories;
                }
            }
        } finally {
            if (conn != null) {
                dbManager.returnConnection(conn);
            }
        }
    }
    
    /**
     * Find inventory items with low stock
     */
    public List<Inventory> findLowStock(int threshold) throws SQLException {
        if (threshold < 0) {
            throw new IllegalArgumentException("Threshold cannot be negative");
        }
        
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SELECT_LOW_STOCK)) {
                stmt.setInt(1, threshold);
                try (ResultSet rs = stmt.executeQuery()) {
                    List<Inventory> inventories = new ArrayList<>();
                    while (rs.next()) {
                        inventories.add(mapResultSetToInventory(rs));
                    }
                    return inventories;
                }
            }
        } finally {
            if (conn != null) {
                dbManager.returnConnection(conn);
            }
        }
    }
    
    /**
     * Update only the quantity of an inventory item
     */
    public boolean updateQuantity(Integer itemId, Integer newQuantity) throws SQLException {
        if (itemId == null || newQuantity == null || newQuantity < 0) {
            throw new IllegalArgumentException("Invalid parameters for quantity update");
        }
        
        return dbManager.executeTransaction(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(UPDATE_QUANTITY)) {
                stmt.setInt(1, newQuantity);
                stmt.setInt(2, itemId);
                int affectedRows = stmt.executeUpdate();
                return affectedRows > 0;
            }
        });
    }
    
    /**
     * Check if item name exists (excluding specific item ID)
     */
    private boolean isNameExists(String name, Integer excludeItemId) throws SQLException {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(CHECK_NAME_EXISTS)) {
                stmt.setString(1, name);
                stmt.setInt(2, excludeItemId != null ? excludeItemId : -1);
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next() && rs.getInt(1) > 0;
                }
            }
        } finally {
            if (conn != null) {
                dbManager.returnConnection(conn);
            }
        }
    }
    
    /**
     * Map ResultSet to Inventory object
     */
    private Inventory mapResultSetToInventory(ResultSet rs) throws SQLException {
        return new Inventory(
            rs.getInt("item_id"),
            rs.getString("item_name"),
            rs.getInt("item_quantity"),
            rs.getString("item_location")
        );
    }
    
    /**
     * Get total count of inventory items
     */
    public int getTotalCount() throws SQLException {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM inventory");
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
     * Get total value of inventory (quantity sum)
     */
    public int getTotalQuantity() throws SQLException {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COALESCE(SUM(item_quantity), 0) FROM inventory");
                 ResultSet rs = stmt.executeQuery()) {
                
                return rs.next() ? rs.getInt(1) : 0;
            }
        } finally {
            if (conn != null) {
                dbManager.returnConnection(conn);
            }
        }
    }
}