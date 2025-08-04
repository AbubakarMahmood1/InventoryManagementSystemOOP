/**
 * Module configuration for St Mary's Warehouse Management System
 */
module com.stmary.warehouse {
    // JavaFX modules
    requires javafx.controls;
    requires javafx.fxml;
    
    // Java SQL module
    requires java.sql;
    
    // SQLite JDBC (if modular)
    requires org.xerial.sqlitejdbc;
    
    // Export packages for JavaFX
    exports com.stmary.warehouse;
    exports com.stmary.warehouse.controller;
    exports com.stmary.warehouse.view;
    exports com.stmary.warehouse.model;
    exports com.stmary.warehouse.service;
    exports com.stmary.warehouse.dao;
    exports com.stmary.warehouse.database;
    exports com.stmary.warehouse.util;
    
    // Open packages for JavaFX reflection
    opens com.stmary.warehouse to javafx.fxml;
    opens com.stmary.warehouse.controller to javafx.fxml;
    opens com.stmary.warehouse.view to javafx.fxml;
    opens com.stmary.warehouse.model to javafx.base;
}