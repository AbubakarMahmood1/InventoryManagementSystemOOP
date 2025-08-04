package com.stmary.warehouse.dao;

import com.stmary.warehouse.database.DatabaseManager;
import com.stmary.warehouse.model.Order;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Order operations
 * Implements CRUD operations with proper exception handling and validation
 */
public class OrderDAO {
    private final DatabaseManager dbManager;
    
    // SQL Statements
    private static final String INSERT_ORDER = 
        "INSERT INTO orders (order_date, customer_name, order_status) VALUES (?, ?, ?)";
    
    private static final String UPDATE_ORDER = 
        "UPDATE orders SET order_date = ?, customer_name = ?, order_status = ?, updated_at = CURRENT_TIMESTAMP WHERE order_id = ?";
    
    private static final String DELETE_ORDER = 
        "DELETE FROM orders WHERE order_id = ?";
    
    private static final String SELECT_ALL_ORDERS = 
        "SELECT order_id, order_date, customer_name, order_status, created_at, updated_at FROM orders ORDER BY order_date DESC";
    
    private static final String SELECT_ORDER_BY_ID = 
        "SELECT order_id, order_date, customer_name, order_status, created_at, updated_at FROM orders WHERE order_id = ?";
    
    private static final String SELECT_ORDERS_BY_CUSTOMER = 
        "SELECT order_id, order_date, customer_name, order_status, created_at, updated_at FROM orders WHERE customer_name LIKE ? ORDER BY order_date DESC";
    
    private static final String SELECT_ORDERS_BY_STATUS = 
        "SELECT order_id, order_date, customer_name, order_status, created_at, updated_at FROM orders WHERE order_status = ? ORDER BY order_date DESC";
    
    private static final String SELECT_ORDERS_BY_DATE_RANGE = 
        "SELECT order_id, order_date, customer_name, order_status, created_at, updated_at FROM orders WHERE order_date BETWEEN ? AND ? ORDER BY order_date DESC";
    
    private static final String UPDATE_ORDER_STATUS = 
        "UPDATE orders SET order_status = ?, updated_at = CURRENT_TIMESTAMP WHERE order_id = ?";
    
    private static final String SELECT_ORDERS_COUNT_BY_STATUS = 
        "SELECT order_status, COUNT(*) as count FROM orders GROUP BY order_status";
    
    private static final String SELECT_RECENT_ORDERS = 
        "SELECT order_id, order_date, customer_name, order_status, created_at, updated_at FROM orders WHERE order_date >= ? ORDER BY order_date DESC LIMIT ?";
    
    public OrderDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }
    
    /**
     * Create a new order
     */
    public Order create(Order order) throws SQLException {
        if (order == null || !order.isValid()) {
            throw new IllegalArgumentException("Invalid order data");
        }
        
        return dbManager.executeTransaction(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(INSERT_ORDER, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setDate(1, Date.valueOf(order.getOrderDate()));
                stmt.setString(2, order.getCustomerName());
                stmt.setString(3, order.getOrderStatusString());
                
                int affectedRows = stmt.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Creating order failed, no rows affected");
                }
                
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        order.setOrderId(generatedKeys.getInt(1));
                        return order;
                    } else {
                        throw new SQLException("Creating order failed, no ID obtained");
                    }
                }
            }
        });
    }
    
    /**
     * Update an existing order
     */
    public boolean update(Order order) throws SQLException {
        if (order == null || order.getOrderId() == null || !order.isValid()) {
            throw new IllegalArgumentException("Invalid order data for update");
        }
        
        return dbManager.executeTransaction(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(UPDATE_ORDER)) {
                stmt.setDate(1, Date.valueOf(order.getOrderDate()));
                stmt.setString(2, order.getCustomerName());
                stmt.setString(3, order.getOrderStatusString());
                stmt.setInt(4, order.getOrderId());
                
                int affectedRows = stmt.executeUpdate();
                return affectedRows > 0;
            }
        });
    }
    
    /**
     * Delete an order
     */
    public boolean delete(Integer orderId) throws SQLException {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID cannot be null");
        }
        
        return dbManager.executeTransaction(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(DELETE_ORDER)) {
                stmt.setInt(1, orderId);
                int affectedRows = stmt.executeUpdate();
                return affectedRows > 0;
            }
        });
    }
    
    /**
     * Find order by ID
     */
    public Optional<Order> findById(Integer orderId) throws SQLException {
        if (orderId == null) {
            return Optional.empty();
        }
        
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SELECT_ORDER_BY_ID)) {
                stmt.setInt(1, orderId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapResultSetToOrder(rs));
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
     * Find all orders
     */
    public List<Order> findAll() throws SQLException {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SELECT_ALL_ORDERS);
                 ResultSet rs = stmt.executeQuery()) {
                
                List<Order> orders = new ArrayList<>();
                while (rs.next()) {
                    orders.add(mapResultSetToOrder(rs));
                }
                return orders;
            }
        } finally {
            if (conn != null) {
                dbManager.returnConnection(conn);
            }
        }
    }
    
    /**
     * Search orders by customer name
     */
    public List<Order> findByCustomer(String customerName) throws SQLException {
        if (customerName == null || customerName.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SELECT_ORDERS_BY_CUSTOMER)) {
                stmt.setString(1, "%" + customerName.trim() + "%");
                try (ResultSet rs = stmt.executeQuery()) {
                    List<Order> orders = new ArrayList<>();
                    while (rs.next()) {
                        orders.add(mapResultSetToOrder(rs));
                    }
                    return orders;
                }
            }
        } finally {
            if (conn != null) {
                dbManager.returnConnection(conn);
            }
        }
    }
    
    /**
     * Find orders by status
     */
    public List<Order> findByStatus(Order.OrderStatus status) throws SQLException {
        if (status == null) {
            return new ArrayList<>();
        }
        
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SELECT_ORDERS_BY_STATUS)) {
                stmt.setString(1, status.getDisplayName());
                try (ResultSet rs = stmt.executeQuery()) {
                    List<Order> orders = new ArrayList<>();
                    while (rs.next()) {
                        orders.add(mapResultSetToOrder(rs));
                    }
                    return orders;
                }
            }
        } finally {
            if (conn != null) {
                dbManager.returnConnection(conn);
            }
        }
    }
    
    /**
     * Find orders within date range
     */
    public List<Order> findByDateRange(LocalDate startDate, LocalDate endDate) throws SQLException {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date cannot be null");
        }
        
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }
        
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SELECT_ORDERS_BY_DATE_RANGE)) {
                stmt.setDate(1, Date.valueOf(startDate));
                stmt.setDate(2, Date.valueOf(endDate));
                try (ResultSet rs = stmt.executeQuery()) {
                    List<Order> orders = new ArrayList<>();
                    while (rs.next()) {
                        orders.add(mapResultSetToOrder(rs));
                    }
                    return orders;
                }
            }
        } finally {
            if (conn != null) {
                dbManager.returnConnection(conn);
            }
        }
    }
    
    /**
     * Get recent orders
     */
    public List<Order> findRecentOrders(int days, int limit) throws SQLException {
        if (days < 0 || limit < 0) {
            throw new IllegalArgumentException("Days and limit must be non-negative");
        }
        
        LocalDate cutoffDate = LocalDate.now().minusDays(days);
        
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SELECT_RECENT_ORDERS)) {
                stmt.setDate(1, Date.valueOf(cutoffDate));
                stmt.setInt(2, limit);
                try (ResultSet rs = stmt.executeQuery()) {
                    List<Order> orders = new ArrayList<>();
                    while (rs.next()) {
                        orders.add(mapResultSetToOrder(rs));
                    }
                    return orders;
                }
            }
        } finally {
            if (conn != null) {
                dbManager.returnConnection(conn);
            }
        }
    }
    
    /**
     * Update only the status of an order
     */
    public boolean updateStatus(Integer orderId, Order.OrderStatus newStatus) throws SQLException {
        if (orderId == null || newStatus == null) {
            throw new IllegalArgumentException("Order ID and status cannot be null");
        }
        
        return dbManager.executeTransaction(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(UPDATE_ORDER_STATUS)) {
                stmt.setString(1, newStatus.getDisplayName());
                stmt.setInt(2, orderId);
                int affectedRows = stmt.executeUpdate();
                return affectedRows > 0;
            }
        });
    }
    
    /**
     * Get count of orders by status
     */
    public List<StatusCount> getOrderCountsByStatus() throws SQLException {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SELECT_ORDERS_COUNT_BY_STATUS);
                 ResultSet rs = stmt.executeQuery()) {
                
                List<StatusCount> counts = new ArrayList<>();
                while (rs.next()) {
                    counts.add(new StatusCount(
                        rs.getString("order_status"),
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
     * Map ResultSet to Order object
     */
    private Order mapResultSetToOrder(ResultSet rs) throws SQLException {
        return new Order(
            rs.getInt("order_id"),
            rs.getDate("order_date").toLocalDate(),
            rs.getString("customer_name"),
            Order.OrderStatus.fromString(rs.getString("order_status"))
        );
    }
    
    /**
     * Get total count of orders
     */
    public int getTotalCount() throws SQLException {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM orders");
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