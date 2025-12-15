package com.stmary.warehouse.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Database connection manager implementing singleton pattern.
 *
 * Notes on robustness:
 * - SQLite JDBC Connections are not safe to share concurrently across threads.
 * - The previous implementation attempted to "pool" connections but could hand the same Connection
 *   to multiple callers at once.
 * - This implementation returns a fresh Connection per request and closes it on return, which is
 *   simpler and safer for typical desktop workloads.
 * Handles SQLite database initialization and connection management
 */
public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:stmary_warehouse.db";
    
    private static DatabaseManager instance;
    private static final Object lock = new Object();
    
    private volatile boolean isInitialized = false;
    
    // Private constructor for singleton
    private DatabaseManager() {
        initializeDatabase();
    }
    
    /**
     * Get singleton instance of DatabaseManager
     */
    public static DatabaseManager getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new DatabaseManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Initialize database and create tables if they don't exist
     */
    private void initializeDatabase() {
        try (Connection conn = createNewConnection()) {
            createTables(conn);
            isInitialized = true;
            
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }
    
    /**
     * Create database tables
     */
    private void createTables(Connection conn) throws SQLException {
        String[] createTableStatements = {
            // Inventory table
            """
            CREATE TABLE IF NOT EXISTS inventory (
                item_id INTEGER PRIMARY KEY AUTOINCREMENT,
                item_name TEXT NOT NULL UNIQUE,
                item_quantity INTEGER NOT NULL CHECK(item_quantity >= 0),
                item_location TEXT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """,
            
            // Orders table
            """
            CREATE TABLE IF NOT EXISTS orders (
                order_id INTEGER PRIMARY KEY AUTOINCREMENT,
                order_date DATE NOT NULL,
                customer_name TEXT NOT NULL,
                order_status TEXT NOT NULL CHECK(order_status IN ('Pending', 'Confirmed', 'Processing', 'Shipped', 'Delivered', 'Cancelled')),
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """,
            
            // Shipments table
            """
            CREATE TABLE IF NOT EXISTS shipments (
                shipment_id INTEGER PRIMARY KEY AUTOINCREMENT,
                destination TEXT NOT NULL,
                shipment_date DATE NOT NULL,
                shipment_status TEXT NOT NULL CHECK(shipment_status IN ('Preparing', 'In Transit', 'Out for Delivery', 'Delivered', 'Returned', 'Cancelled')),
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """,
            
            // Indexes for better performance
            "CREATE INDEX IF NOT EXISTS idx_inventory_name ON inventory(item_name)",
            "CREATE INDEX IF NOT EXISTS idx_inventory_location ON inventory(item_location)",
            "CREATE INDEX IF NOT EXISTS idx_orders_customer ON orders(customer_name)",
            "CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(order_status)",
            "CREATE INDEX IF NOT EXISTS idx_orders_date ON orders(order_date)",
            "CREATE INDEX IF NOT EXISTS idx_shipments_destination ON shipments(destination)",
            "CREATE INDEX IF NOT EXISTS idx_shipments_status ON shipments(shipment_status)",
            "CREATE INDEX IF NOT EXISTS idx_shipments_date ON shipments(shipment_date)"
        };
        
        try (Statement stmt = conn.createStatement()) {
            for (String sql : createTableStatements) {
                stmt.execute(sql);
            }
        }
    }
    
    /**
     * Create a new database connection
     */
    private Connection createNewConnection() throws SQLException {
        Properties props = new Properties();
        props.setProperty("enable_load_extension", "false");
        
        Connection conn = DriverManager.getConnection(DB_URL, props);
        conn.setAutoCommit(true);
        configureConnection(conn);
        return conn;
    }

    /**
     * Configure per-connection SQLite settings for consistency and robustness.
     * Many PRAGMA values are per-connection (e.g., foreign_keys, busy_timeout).
     */
    private void configureConnection(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
            stmt.execute("PRAGMA journal_mode = WAL");
            stmt.execute("PRAGMA synchronous = NORMAL");
            stmt.execute("PRAGMA cache_size = 10000");
            stmt.execute("PRAGMA temp_store = MEMORY");
            stmt.execute("PRAGMA busy_timeout = 5000");
        }
    }
    
    /**
     * Get a database connection.
     */
    public Connection getConnection() throws SQLException {
        if (!isInitialized) {
            throw new SQLException("Database not initialized");
        }
        return createNewConnection();
    }
    
    /**
     * Return a connection (closed for this implementation).
     */
    public void returnConnection(Connection conn) {
        closeConnection(conn);
    }
    
    /**
     * Close a connection.
     */
    public void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                // Log error but don't throw
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
    }
    
    /**
     * Execute transaction with automatic rollback on failure
     */
    public <T> T executeTransaction(TransactionCallback<T> callback) throws SQLException {
        try (Connection conn = getConnection()) {
            boolean previousAutoCommit = true;
            try {
                previousAutoCommit = conn.getAutoCommit();
                conn.setAutoCommit(false);

                T result = callback.execute(conn);
                conn.commit();
                return result;
            } catch (Exception e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    e.addSuppressed(rollbackEx);
                }
                throw new SQLException("Transaction failed", e);
            } finally {
                try {
                    conn.setAutoCommit(previousAutoCommit);
                } catch (SQLException e) {
                    System.err.println("Error resetting auto-commit: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Functional interface for transaction callbacks
     */
    @FunctionalInterface
    public interface TransactionCallback<T> {
        T execute(Connection conn) throws SQLException;
    }
    
    /**
     * Test database connectivity
     */
    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
    
    /**
     * Get database statistics
     */
    public DatabaseStats getStats() {
        return new DatabaseStats(
            0,
            0,
            0,
            isInitialized
        );
    }
    
    /**
     * Close all connections and shutdown
     */
    public void shutdown() {
        // No-op for per-connection strategy.
    }
    
    /**
     * Database statistics class
     */
    public static class DatabaseStats {
        private final int activeConnections;
        private final int pooledConnections;
        private final int maxConnections;
        private final boolean isInitialized;
        
        public DatabaseStats(int activeConnections, int pooledConnections, int maxConnections, boolean isInitialized) {
            this.activeConnections = activeConnections;
            this.pooledConnections = pooledConnections;
            this.maxConnections = maxConnections;
            this.isInitialized = isInitialized;
        }
        
        public int getActiveConnections() { return activeConnections; }
        public int getPooledConnections() { return pooledConnections; }
        public int getMaxConnections() { return maxConnections; }
        public boolean isInitialized() { return isInitialized; }
        
        @Override
        public String toString() {
            return String.format("DatabaseStats{active=%d, pooled=%d, max=%d, initialized=%b}",
                    activeConnections, pooledConnections, maxConnections, isInitialized);
        }
    }
}
