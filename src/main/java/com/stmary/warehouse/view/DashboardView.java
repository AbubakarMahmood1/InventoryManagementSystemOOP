package com.stmary.warehouse.view;

import com.stmary.warehouse.controller.MainController;
import com.stmary.warehouse.service.InventoryService;
import com.stmary.warehouse.service.OrderService;
import com.stmary.warehouse.service.ShipmentService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

/**
 * Dashboard view providing overview of warehouse operations
 * Features statistics, charts, and quick status indicators
 */
public class DashboardView {
    private final MainController controller;
    private BorderPane root;
    
    // Dashboard cards
    private Label totalInventoryLabel;
    private Label lowStockLabel;
    private Label pendingOrdersLabel;
    private Label inTransitShipmentsLabel;
    
    // Charts
    private PieChart inventoryStatusChart;
    private BarChart<String, Number> monthlyOrdersChart;
    private LineChart<String, Number> shipmentTrendsChart;
    
    public DashboardView(MainController controller) {
        this.controller = controller;
        initializeView();
    }
    
    /**
     * Initialize dashboard view
     */
    private void initializeView() {
        root = new BorderPane();
        root.getStyleClass().add("dashboard-view");
        root.setPadding(new Insets(20));
        
        // Create main sections
        VBox topSection = createTopSection();
        VBox centerSection = createCenterSection();
        
        root.setTop(topSection);
        root.setCenter(centerSection);
    }
    
    /**
     * Create top section with summary cards
     */
    private VBox createTopSection() {
        VBox topSection = new VBox();
        topSection.setSpacing(20);
        
        // Title
        Label dashboardTitle = new Label("Dashboard Overview");
        dashboardTitle.getStyleClass().add("section-title");
        dashboardTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        
        // Summary cards grid
        GridPane cardsGrid = createSummaryCards();
        
        topSection.getChildren().addAll(dashboardTitle, cardsGrid);
        return topSection;
    }
    
    /**
     * Create summary cards grid
     */
    private GridPane createSummaryCards() {
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setPadding(new Insets(10));
        
        // Total Inventory Card
        VBox inventoryCard = createDashboardCard(
            "Total Inventory Items", 
            "0", 
            "Items in warehouse",
            "#3498db"
        );
        totalInventoryLabel = (Label) ((VBox) inventoryCard.getChildren().get(1)).getChildren().get(0);
        
        // Low Stock Card
        VBox lowStockCard = createDashboardCard(
            "Low Stock Alerts", 
            "0", 
            "Items need restocking",
            "#e74c3c"
        );
        lowStockLabel = (Label) ((VBox) lowStockCard.getChildren().get(1)).getChildren().get(0);
        
        // Pending Orders Card
        VBox ordersCard = createDashboardCard(
            "Pending Orders", 
            "0", 
            "Orders awaiting processing",
            "#f39c12"
        );
        pendingOrdersLabel = (Label) ((VBox) ordersCard.getChildren().get(1)).getChildren().get(0);
        
        // In Transit Shipments Card
        VBox shipmentsCard = createDashboardCard(
            "In Transit Shipments", 
            "0", 
            "Active shipments",
            "#9b59b6"
        );
        inTransitShipmentsLabel = (Label) ((VBox) shipmentsCard.getChildren().get(1)).getChildren().get(0);
        
        // Add cards to grid
        grid.add(inventoryCard, 0, 0);
        grid.add(lowStockCard, 1, 0);
        grid.add(ordersCard, 2, 0);
        grid.add(shipmentsCard, 3, 0);
        
        // Set column constraints for equal width
        for (int i = 0; i < 4; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(25);
            grid.getColumnConstraints().add(col);
        }
        
        return grid;
    }
    
    /**
     * Create individual dashboard card
     */
    private VBox createDashboardCard(String title, String value, String subtitle, String color) {
        VBox card = new VBox();
        card.getStyleClass().add("dashboard-card");
        card.setPadding(new Insets(20));
        card.setSpacing(10);
        card.setPrefHeight(120);
        card.setAlignment(Pos.CENTER);
        
        // Title
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("card-title");
        
        // Value container
        VBox valueContainer = new VBox();
        valueContainer.setAlignment(Pos.CENTER);
        
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("card-value");
        valueLabel.setTextFill(Color.web(color));
        
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("card-subtitle");
        
        valueContainer.getChildren().addAll(valueLabel, subtitleLabel);
        
        card.getChildren().addAll(titleLabel, valueContainer);
        
        return card;
    }
    
    /**
     * Create center section with charts
     */
    private VBox createCenterSection() {
        VBox centerSection = new VBox();
        centerSection.setSpacing(20);
        centerSection.setPadding(new Insets(20, 0, 0, 0));
        
        // Charts title
        Label chartsTitle = new Label("Analytics & Trends");
        chartsTitle.getStyleClass().add("section-title");
        chartsTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        
        // Charts grid
        GridPane chartsGrid = createChartsGrid();
        
        centerSection.getChildren().addAll(chartsTitle, chartsGrid);
        VBox.setVgrow(chartsGrid, Priority.ALWAYS);
        
        return centerSection;
    }
    
    /**
     * Create charts grid
     */
    private GridPane createChartsGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setPrefHeight(400);
        
        // Inventory Status Chart
        inventoryStatusChart = createInventoryStatusChart();
        VBox inventoryChartContainer = createChartContainer("Inventory Status", inventoryStatusChart);
        
        // Monthly Orders Chart
        monthlyOrdersChart = createMonthlyOrdersChart();
        VBox ordersChartContainer = createChartContainer("Monthly Orders", monthlyOrdersChart);
        
        // Shipment Trends Chart
        shipmentTrendsChart = createShipmentTrendsChart();
        VBox shipmentsChartContainer = createChartContainer("Shipment Trends", shipmentTrendsChart);
        
        // Add charts to grid
        grid.add(inventoryChartContainer, 0, 0);
        grid.add(ordersChartContainer, 1, 0);
        grid.add(shipmentsChartContainer, 0, 1, 2, 1);
        
        // Set constraints
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(col1, col2);
        
        RowConstraints row1 = new RowConstraints();
        row1.setPercentHeight(50);
        RowConstraints row2 = new RowConstraints();
        row2.setPercentHeight(50);
        grid.getRowConstraints().addAll(row1, row2);
        
        return grid;
    }
    
    /**
     * Create chart container with title
     */
    private VBox createChartContainer(String title, javafx.scene.Node chart) {
        VBox container = new VBox();
        container.getStyleClass().add("chart");
        container.setSpacing(10);
        
        Label chartTitle = new Label(title);
        chartTitle.getStyleClass().add("chart-title");
        
        container.getChildren().addAll(chartTitle, chart);
        VBox.setVgrow(chart, Priority.ALWAYS);
        
        return container;
    }
    
    /**
     * Create inventory status pie chart
     */
    private PieChart createInventoryStatusChart() {
        PieChart chart = new PieChart();
        chart.setLegendVisible(true);
        chart.setLabelsVisible(true);
        chart.setPrefHeight(250);
        
        // Initial data
        chart.getData().addAll(
            new PieChart.Data("In Stock", 85),
            new PieChart.Data("Low Stock", 12),
            new PieChart.Data("Out of Stock", 3)
        );
        
        return chart;
    }
    
    /**
     * Create monthly orders bar chart
     */
    private BarChart<String, Number> createMonthlyOrdersChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Month");
        yAxis.setLabel("Orders");
        
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("Orders by Month");
        chart.setPrefHeight(250);
        
        // Sample data
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Orders");
        series.getData().addAll(
            new XYChart.Data<>("Jan", 23),
            new XYChart.Data<>("Feb", 14),
            new XYChart.Data<>("Mar", 15),
            new XYChart.Data<>("Apr", 24),
            new XYChart.Data<>("May", 34),
            new XYChart.Data<>("Jun", 36)
        );
        
        chart.getData().add(series);
        return chart;
    }
    
    /**
     * Create shipment trends line chart
     */
    private LineChart<String, Number> createShipmentTrendsChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Week");
        yAxis.setLabel("Shipments");
        
        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Weekly Shipment Trends");
        chart.setPrefHeight(200);
        
        // Sample data
        XYChart.Series<String, Number> series1 = new XYChart.Series<>();
        series1.setName("Outbound");
        series1.getData().addAll(
            new XYChart.Data<>("Week 1", 12),
            new XYChart.Data<>("Week 2", 18),
            new XYChart.Data<>("Week 3", 15),
            new XYChart.Data<>("Week 4", 22)
        );
        
        XYChart.Series<String, Number> series2 = new XYChart.Series<>();
        series2.setName("Delivered");
        series2.getData().addAll(
            new XYChart.Data<>("Week 1", 10),
            new XYChart.Data<>("Week 2", 16),
            new XYChart.Data<>("Week 3", 13),
            new XYChart.Data<>("Week 4", 20)
        );
        
        chart.getData().addAll(series1, series2);
        return chart;
    }
    
    /**
     * Load dashboard data
     */
    public void loadDashboardData() {
        // Load inventory stats
        controller.getInventoryStats(stats -> {
            totalInventoryLabel.setText(String.valueOf(stats.getTotalItems()));
            lowStockLabel.setText(String.valueOf(stats.getLowStockItems()));
            updateInventoryChart(stats);
        });
        
        // Load order stats
        controller.getOrderStats(stats -> {
            // Count pending orders
            int pendingCount = stats.getStatusCounts().stream()
                .filter(sc -> "Pending".equals(sc.getStatus()))
                .mapToInt(sc -> sc.getCount())
                .sum();
            pendingOrdersLabel.setText(String.valueOf(pendingCount));
        });
        
        // Load shipment stats
        controller.getShipmentStats(stats -> {
            // Count in-transit shipments
            int inTransitCount = stats.getStatusCounts().stream()
                .filter(sc -> "In Transit".equals(sc.getStatus()) || "Out for Delivery".equals(sc.getStatus()))
                .mapToInt(sc -> sc.getCount())
                .sum();
            inTransitShipmentsLabel.setText(String.valueOf(inTransitCount));
        });
    }
    
    /**
     * Update inventory chart with real data
     */
    private void updateInventoryChart(InventoryService.InventoryStats stats) {
        inventoryStatusChart.getData().clear();
        
        int totalItems = stats.getTotalItems();
        int lowStockItems = stats.getLowStockItems();
        int inStockItems = totalItems - lowStockItems;
        
        if (totalItems > 0) {
            inventoryStatusChart.getData().addAll(
                new PieChart.Data("In Stock (" + inStockItems + ")", inStockItems),
                new PieChart.Data("Low Stock (" + lowStockItems + ")", lowStockItems)
            );
        }
    }
    
    /**
     * Refresh dashboard data
     */
    public void refreshData() {
        loadDashboardData();
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
        // Cleanup chart data if needed
    }
}