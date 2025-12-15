package com.stmary.warehouse.view;

import com.stmary.warehouse.controller.MainController;
import com.stmary.warehouse.database.DatabaseManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Main view containing all application views and navigation
 * Implements tabbed interface with status bar
 */
public class MainView {
    private final MainController controller;
    private BorderPane root;
    private TabPane tabPane;
    
    // View instances
    private DashboardView dashboardView;
    private InventoryView inventoryView;
    private OrderView orderView;
    private ShipmentView shipmentView;
    
    // Status bar components
    private Label statusLabel;
    private ProgressBar progressBar;
    private Label connectionLabel;
    private Label timeLabel;
    
    // Timeline for clock update
    private Timeline clockTimeline;
    
    public MainView(MainController controller) {
        this.controller = controller;
        initializeView();
        setupEventHandlers();
        startClock();
    }
    
    /**
     * Initialize main view layout
     */
    private void initializeView() {
        root = new BorderPane();
        root.getStyleClass().add("main-container");
        
        // Create header
        VBox header = createHeader();
        root.setTop(header);
        
        // Create tab pane
        tabPane = createTabPane();
        root.setCenter(tabPane);
        
        // Create status bar
        HBox statusBar = createStatusBar();
        root.setBottom(statusBar);
    }
    
    /**
     * Create application header
     */
    private VBox createHeader() {
        VBox header = new VBox();
        header.getStyleClass().add("header");
        header.setPadding(new Insets(20));
        header.setAlignment(Pos.CENTER);
        
        // Title
        Label title = new Label("St Mary's Warehouse Management System");
        title.getStyleClass().add("title");
        
        // Subtitle
        Label subtitle = new Label("Inventory • Orders • Shipments");
        subtitle.getStyleClass().add("subtitle");
        
        header.getChildren().addAll(title, subtitle);
        return header;
    }
    
    /**
     * Create tabbed interface
     */
    private TabPane createTabPane() {
        TabPane tabs = new TabPane();
        tabs.getStyleClass().add("main-tabs");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        
        // Dashboard Tab
        Tab dashboardTab = new Tab("Dashboard");
        dashboardTab.getStyleClass().add("dashboard-tab");
        dashboardView = new DashboardView(controller);
        dashboardTab.setContent(dashboardView.getRoot());
        
        // Inventory Tab
        Tab inventoryTab = new Tab("Inventory");
        inventoryTab.getStyleClass().add("inventory-tab");
        inventoryView = new InventoryView(controller);
        inventoryTab.setContent(inventoryView.getRoot());
        
        // Orders Tab
        Tab ordersTab = new Tab("Orders");
        ordersTab.getStyleClass().add("orders-tab");
        orderView = new OrderView(controller);
        ordersTab.setContent(orderView.getRoot());
        
        // Shipments Tab
        Tab shipmentsTab = new Tab("Shipments");
        shipmentsTab.getStyleClass().add("shipments-tab");
        shipmentView = new ShipmentView(controller);
        shipmentsTab.setContent(shipmentView.getRoot());
        
        tabs.getTabs().addAll(dashboardTab, inventoryTab, ordersTab, shipmentsTab);
        
        return tabs;
    }
    
    /**
     * Create status bar
     */
    private HBox createStatusBar() {
        HBox statusBar = new HBox();
        statusBar.getStyleClass().add("status-bar");
        statusBar.setPadding(new Insets(10));
        statusBar.setSpacing(20);
        statusBar.setAlignment(Pos.CENTER_LEFT);
        
        // Status label
        statusLabel = new Label("Ready");
        statusLabel.getStyleClass().add("status-label");
        statusLabel.setPrefWidth(300);
        
        // Progress bar
        progressBar = new ProgressBar();
        progressBar.getStyleClass().add("status-progress");
        progressBar.setPrefWidth(150);
        progressBar.setVisible(false);
        
        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Connection status
        connectionLabel = new Label("● Connected");
        connectionLabel.getStyleClass().add("connection-status");
        connectionLabel.setTextFill(Color.GREEN);
        
        // Time label
        timeLabel = new Label();
        timeLabel.getStyleClass().add("time-label");
        updateTime();
        
        statusBar.getChildren().addAll(statusLabel, progressBar, spacer, connectionLabel, timeLabel);
        
        return statusBar;
    }
    
    /**
     * Set up event handlers
     */
    private void setupEventHandlers() {
        // Tab change listener
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) {
                String tabText = newTab.getText();
                updateStatus("Switched to " + tabText + " view");
                
                // Refresh data when switching tabs
                switch (tabText) {
                    case "Dashboard" -> dashboardView.refreshData();
                    case "Inventory" -> inventoryView.refreshData();
                    case "Orders" -> orderView.refreshData();
                    case "Shipments" -> shipmentView.refreshData();
                }
            }
        });
        
        // Set controller event handlers
        controller.setEventHandlers(
            this::showError,
            this::showSuccess,
            this::showProgress
        );
    }
    
    /**
     * Initialize data loading
     */
    public void initializeData() {
        Platform.runLater(() -> {
            updateStatus("Loading data...");
            showProgress("Initializing application data...");
            
            // Load initial data for all views
            dashboardView.loadDashboardData();
            inventoryView.loadInventoryData();
            orderView.loadOrderData();
            shipmentView.loadShipmentData();
            
            hideProgress();
            updateStatus("Data loaded successfully");
            
            // Check database connection
            checkDatabaseConnection();
        });
    }
    
    /**
     * Check database connection status
     */
    private void checkDatabaseConnection() {
        Thread checkThread = new Thread(() -> {
            boolean connected;
            try {
                connected = DatabaseManager.getInstance().testConnection();
            } catch (Exception e) {
                connected = false;
            }

            boolean finalConnected = connected;
            Platform.runLater(() -> {
                if (finalConnected) {
                    connectionLabel.setText("Connected");
                    connectionLabel.setTextFill(Color.GREEN);
                } else {
                    connectionLabel.setText("Disconnected");
                    connectionLabel.setTextFill(Color.RED);
                }
            });
        }, "db-connection-check");
        checkThread.setDaemon(true);
        checkThread.start();
    }

    /**
     * Check database connection status (simulated - legacy).
     */
    private void checkDatabaseConnectionSimulated() {
        // This would typically check actual database connectivity
        // For now, we'll simulate it
        Platform.runLater(() -> {
            connectionLabel.setText("● Connected");
            connectionLabel.setTextFill(Color.GREEN);
        });
    }
    
    /**
     * Update status message
     */
    private void updateStatus(String message) {
        Platform.runLater(() -> {
            statusLabel.setText(message);
            statusLabel.getStyleClass().removeAll("status-error", "status-success", "status-info");
            statusLabel.getStyleClass().add("status-info");
        });
    }
    
    /**
     * Show error message
     */
    private void showError(String message) {
        Platform.runLater(() -> {
            statusLabel.setText("Error: " + message);
            statusLabel.getStyleClass().removeAll("status-error", "status-success", "status-info");
            statusLabel.getStyleClass().add("status-error");
            
            // Also show alert for important errors
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    /**
     * Show success message
     */
    private void showSuccess(String message) {
        Platform.runLater(() -> {
            statusLabel.setText(message);
            statusLabel.getStyleClass().removeAll("status-error", "status-success", "status-info");
            statusLabel.getStyleClass().add("status-success");
        });
    }
    
    /**
     * Show progress indicator
     */
    private void showProgress(String message) {
        Platform.runLater(() -> {
            statusLabel.setText(message);
            progressBar.setVisible(true);
            progressBar.setProgress(-1); // Indeterminate progress
        });
    }
    
    /**
     * Hide progress indicator
     */
    private void hideProgress() {
        Platform.runLater(() -> {
            progressBar.setVisible(false);
            progressBar.setProgress(0);
        });
    }
    
    /**
     * Start clock update
     */
    private void startClock() {
        clockTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateTime()));
        clockTimeline.setCycleCount(Timeline.INDEFINITE);
        clockTimeline.play();
    }
    
    /**
     * Update time display
     */
    private void updateTime() {
        Platform.runLater(() -> {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm:ss");
            timeLabel.setText(now.format(formatter));
        });
    }
    
    /**
     * Stop clock update
     */
    private void stopClock() {
        if (clockTimeline != null) {
            clockTimeline.stop();
        }
    }
    
    /**
     * Get root node
     */
    public BorderPane getRoot() {
        return root;
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        stopClock();
        
        // Cleanup individual views
        if (dashboardView != null) dashboardView.cleanup();
        if (inventoryView != null) inventoryView.cleanup();
        if (orderView != null) orderView.cleanup();
        if (shipmentView != null) shipmentView.cleanup();
    }
    
    /**
     * Switch to specific tab
     */
    public void switchToTab(String tabName) {
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getText().equalsIgnoreCase(tabName)) {
                tabPane.getSelectionModel().select(tab);
                break;
            }
        }
    }
    
    /**
     * Get current active tab name
     */
    public String getCurrentTab() {
        Tab selected = tabPane.getSelectionModel().getSelectedItem();
        return selected != null ? selected.getText() : "";
    }
}
