package com.stmary.warehouse.view;

import com.stmary.warehouse.controller.MainController;
import com.stmary.warehouse.model.Inventory;
import com.stmary.warehouse.util.ValidationUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Optional;

/**
 * Inventory management view with full CRUD functionality
 * Implements search, filtering, and stock management features
 */
public class InventoryView {
    private final MainController controller;
    private BorderPane root;
    private TableView<Inventory> inventoryTable;
    private ObservableList<Inventory> inventoryData;
    private FilteredList<Inventory> filteredData;
    
    // Form controls
    private TextField itemNameField;
    private TextField quantityField;
    private TextField locationField;
    private Button addButton;
    private Button updateButton;
    private Button deleteButton;
    
    // Search and filter controls
    private TextField searchField;
    private ComboBox<String> locationFilter;
    private CheckBox lowStockOnlyCheckBox;
    
    // Stock management controls
    private TextField stockChangeField;
    private Button addStockButton;
    private Button removeStockButton;
    
    private Inventory selectedItem;
    
    public InventoryView(MainController controller) {
        this.controller = controller;
        this.inventoryData = FXCollections.observableArrayList();
        this.filteredData = new FilteredList<>(inventoryData);
        initializeView();
        setupEventHandlers();
    }
    
    /**
     * Initialize the inventory view layout
     */
    private void initializeView() {
        root = new BorderPane();
        root.getStyleClass().add("inventory-view");
        root.setPadding(new Insets(20));
        
        // Create toolbar
        HBox toolbar = createToolbar();
        root.setTop(toolbar);
        
        // Create main content with split pane
        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(createTableView(), createFormView());
        splitPane.setDividerPositions(0.7);
        root.setCenter(splitPane);
    }
    
    /**
     * Create toolbar with search and filter controls
     */
    private HBox createToolbar() {
        HBox toolbar = new HBox();
        toolbar.getStyleClass().add("toolbar");
        toolbar.setPadding(new Insets(0, 0, 15, 0));
        toolbar.setSpacing(15);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        
        // Search field
        Label searchLabel = new Label("Search:");
        searchField = new TextField();
        searchField.setPromptText("Search by item name...");
        searchField.setPrefWidth(250);
        searchField.getStyleClass().add("search-field");
        
        // Location filter
        Label locationLabel = new Label("Location:");
        locationFilter = new ComboBox<>();
        locationFilter.setPromptText("All locations");
        locationFilter.setPrefWidth(150);
        locationFilter.getStyleClass().add("filter-combo");
        
        // Low stock filter
        lowStockOnlyCheckBox = new CheckBox("Low stock only");
        lowStockOnlyCheckBox.getStyleClass().add("filter-checkbox");
        
        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Refresh button
        Button refreshButton = new Button("Refresh");
        refreshButton.getStyleClass().addAll("btn", "btn-primary");
        refreshButton.setOnAction(e -> refreshData());
        
        toolbar.getChildren().addAll(
            searchLabel, searchField,
            locationLabel, locationFilter,
            lowStockOnlyCheckBox,
            spacer, refreshButton
        );
        
        return toolbar;
    }
    
    /**
     * Create inventory table view
     */
    private VBox createTableView() {
        VBox tableContainer = new VBox();
        tableContainer.getStyleClass().add("left-panel");
        tableContainer.setSpacing(10);
        
        Label tableTitle = new Label("Inventory Items");
        tableTitle.getStyleClass().add("section-title");
        
        inventoryTable = new TableView<>();
        inventoryTable.getStyleClass().add("data-table");
        inventoryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // Create columns
        TableColumn<Inventory, Integer> idColumn = new TableColumn<>("Item ID");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("itemId"));
        idColumn.setPrefWidth(80);
        idColumn.getStyleClass().add("id-column");
        
        TableColumn<Inventory, String> nameColumn = new TableColumn<>("Item Name");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        nameColumn.setPrefWidth(200);
        nameColumn.getStyleClass().add("name-column");
        
        TableColumn<Inventory, Integer> quantityColumn = new TableColumn<>("Quantity");
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("itemQuantity"));
        quantityColumn.setPrefWidth(100);
        quantityColumn.setCellFactory(column -> new TableCell<Inventory, Integer>() {
            @Override
            protected void updateItem(Integer quantity, boolean empty) {
                super.updateItem(quantity, empty);
                if (empty || quantity == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(quantity.toString());
                    
                    // Color coding for stock levels
                    if (quantity <= 10) {
                        setTextFill(Color.RED);
                        setStyle("-fx-font-weight: bold;");
                    } else if (quantity <= 50) {
                        setTextFill(Color.ORANGE);
                        setStyle("-fx-font-weight: bold;");
                    } else {
                        setTextFill(Color.GREEN);
                        setStyle("-fx-font-weight: bold;");
                    }
                }
            }
        });
        
        TableColumn<Inventory, String> locationColumn = new TableColumn<>("Location");
        locationColumn.setCellValueFactory(new PropertyValueFactory<>("itemLocation"));
        locationColumn.setPrefWidth(150);
        locationColumn.getStyleClass().add("location-column");
        
        TableColumn<Inventory, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setPrefWidth(100);
        statusColumn.setCellFactory(column -> new TableCell<Inventory, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Inventory inventory = getTableView().getItems().get(getIndex());
                    if (inventory != null) {
                        Label statusLabel = new Label();
                        if (inventory.getItemQuantity() == 0) {
                            statusLabel.setText("Out of Stock");
                            statusLabel.getStyleClass().add("status-low");
                        } else if (inventory.getItemQuantity() <= 10) {
                            statusLabel.setText("Low Stock");
                            statusLabel.getStyleClass().add("status-medium");
                        } else {
                            statusLabel.setText("In Stock");
                            statusLabel.getStyleClass().add("status-high");
                        }
                        setGraphic(statusLabel);
                    }
                }
            }
        });
        
        inventoryTable.getColumns().addAll(idColumn, nameColumn, quantityColumn, locationColumn, statusColumn);
        
        // Set up sorting and filtering
        SortedList<Inventory> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(inventoryTable.comparatorProperty());
        inventoryTable.setItems(sortedData);
        
        // Summary label
        Label summaryLabel = new Label();
        summaryLabel.getStyleClass().add("summary-label");
        updateSummaryLabel(summaryLabel);
        
        tableContainer.getChildren().addAll(tableTitle, inventoryTable, summaryLabel);
        VBox.setVgrow(inventoryTable, Priority.ALWAYS);
        
        return tableContainer;
    }
    
    /**
     * Create form view for inventory management
     */
    private VBox createFormView() {
        VBox formContainer = new VBox();
        formContainer.getStyleClass().add("right-panel");
        formContainer.setSpacing(15);
        formContainer.setPadding(new Insets(0, 0, 0, 20));
        formContainer.setPrefWidth(300);
        
        Label formTitle = new Label("Item Details");
        formTitle.getStyleClass().add("section-title");
        
        // Form fields
        VBox form = new VBox();
        form.setSpacing(10);
        form.getStyleClass().add("form-container");
        
        // Item Name
        Label nameLabel = new Label("Item Name:");
        itemNameField = new TextField();
        itemNameField.setPromptText("Enter item name");
        itemNameField.getStyleClass().add("form-field");
        
        // Quantity
        Label quantityLabel = new Label("Quantity:");
        quantityField = new TextField();
        quantityField.setPromptText("Enter quantity");
        quantityField.getStyleClass().add("form-field");
        
        // Location
        Label locationLabel = new Label("Location:");
        locationField = new TextField();
        locationField.setPromptText("Enter location (e.g., A1-B2)");
        locationField.getStyleClass().add("form-field");
        
        form.getChildren().addAll(
            nameLabel, itemNameField,
            quantityLabel, quantityField,
            locationLabel, locationField
        );
        
        // CRUD Buttons
        HBox crudButtons = new HBox();
        crudButtons.setSpacing(10);
        crudButtons.setAlignment(Pos.CENTER);
        
        addButton = new Button("Add Item");
        addButton.getStyleClass().addAll("btn", "btn-success");
        addButton.setPrefWidth(90);
        
        updateButton = new Button("Update");
        updateButton.getStyleClass().addAll("btn", "btn-primary");
        updateButton.setPrefWidth(90);
        updateButton.setDisable(true);
        
        deleteButton = new Button("Delete");
        deleteButton.getStyleClass().addAll("btn", "btn-danger");
        deleteButton.setPrefWidth(90);
        deleteButton.setDisable(true);
        
        crudButtons.getChildren().addAll(addButton, updateButton, deleteButton);
        
        // Stock management section
        VBox stockSection = new VBox();
        stockSection.setSpacing(10);
        
        Label stockTitle = new Label("Stock Management");
        stockTitle.getStyleClass().add("section-title");
        stockTitle.setStyle("-fx-font-size: 14px;");
        
        Label stockChangeLabel = new Label("Quantity Change:");
        stockChangeField = new TextField();
        stockChangeField.setPromptText("Enter quantity");
        stockChangeField.getStyleClass().add("form-field");
        stockChangeField.setDisable(true);
        
        HBox stockButtons = new HBox();
        stockButtons.setSpacing(10);
        stockButtons.setAlignment(Pos.CENTER);
        
        addStockButton = new Button("Add Stock");
        addStockButton.getStyleClass().addAll("btn", "btn-success");
        addStockButton.setPrefWidth(120);
        addStockButton.setDisable(true);
        
        removeStockButton = new Button("Remove Stock");
        removeStockButton.getStyleClass().addAll("btn", "btn-warning");
        removeStockButton.setPrefWidth(120);
        removeStockButton.setDisable(true);
        
        stockButtons.getChildren().addAll(addStockButton, removeStockButton);
        
        stockSection.getChildren().addAll(stockTitle, stockChangeLabel, stockChangeField, stockButtons);
        
        formContainer.getChildren().addAll(formTitle, form, crudButtons, new Separator(), stockSection);
        
        return formContainer;
    }
    
    /**
     * Set up event handlers
     */
    private void setupEventHandlers() {
        // Table selection handler
        inventoryTable.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
            selectedItem = newItem;
            if (newItem != null) {
                populateForm(newItem);
                updateButton.setDisable(false);
                deleteButton.setDisable(false);
                stockChangeField.setDisable(false);
                addStockButton.setDisable(false);
                removeStockButton.setDisable(false);
            } else {
                clearForm();
                updateButton.setDisable(true);
                deleteButton.setDisable(true);
                stockChangeField.setDisable(true);
                addStockButton.setDisable(true);
                removeStockButton.setDisable(true);
            }
        });
        
        // Search and filter handlers
        searchField.textProperty().addListener((obs, oldText, newText) -> updateFilters());
        locationFilter.valueProperty().addListener((obs, oldVal, newVal) -> updateFilters());
        lowStockOnlyCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> updateFilters());
        
        // CRUD button handlers
        addButton.setOnAction(e -> addInventoryItem());
        updateButton.setOnAction(e -> updateInventoryItem());
        deleteButton.setOnAction(e -> deleteInventoryItem());
        
        // Stock management handlers
        addStockButton.setOnAction(e -> addStock());
        removeStockButton.setOnAction(e -> removeStock());
        
        // Input validation for numeric fields
        quantityField.textProperty().addListener((obs, oldText, newText) -> {
            if (!newText.matches("\\d*")) {
                quantityField.setText(newText.replaceAll("[^\\d]", ""));
            }
        });
        
        stockChangeField.textProperty().addListener((obs, oldText, newText) -> {
            if (!newText.matches("\\d*")) {
                stockChangeField.setText(newText.replaceAll("[^\\d]", ""));
            }
        });
    }
    
    /**
     * Update filters based on search and filter criteria
     */
    private void updateFilters() {
        filteredData.setPredicate(inventory -> {
            // Search filter
            String searchText = searchField.getText();
            if (searchText != null && !searchText.isEmpty()) {
                if (!inventory.getItemName().toLowerCase().contains(searchText.toLowerCase())) {
                    return false;
                }
            }
            
            // Location filter
            String selectedLocation = locationFilter.getValue();
            if (selectedLocation != null && !selectedLocation.isEmpty()) {
                if (!inventory.getItemLocation().equals(selectedLocation)) {
                    return false;
                }
            }
            
            // Low stock filter
            if (lowStockOnlyCheckBox.isSelected()) {
                if (inventory.getItemQuantity() > 10) {
                    return false;
                }
            }
            
            return true;
        });
        
        // Update summary
        Label summaryLabel = (Label) ((VBox) inventoryTable.getParent()).getChildren().get(2);
        updateSummaryLabel(summaryLabel);
    }
    
    /**
     * Add new inventory item
     */
    private void addInventoryItem() {
        try {
            // Validate inputs
            String itemName = ValidationUtils.validateItemName(itemNameField.getText());
            int quantity = ValidationUtils.validateQuantity(quantityField.getText());
            String location = ValidationUtils.validateLocation(locationField.getText());
            
            // Create new inventory item
            Inventory newItem = new Inventory(itemName, quantity, location);
            
            controller.createInventory(newItem, createdItem -> {
                inventoryData.add(createdItem);
                updateLocationFilter();
                clearForm();
                inventoryTable.getSelectionModel().select(createdItem);
            });
            
        } catch (IllegalArgumentException e) {
            showError("Input Error", e.getMessage());
        }
    }
    
    /**
     * Update selected inventory item
     */
    private void updateInventoryItem() {
        if (selectedItem == null) return;
        
        try {
            // Validate inputs
            String itemName = ValidationUtils.validateItemName(itemNameField.getText());
            int quantity = ValidationUtils.validateQuantity(quantityField.getText());
            String location = ValidationUtils.validateLocation(locationField.getText());
            
            // Update item
            selectedItem.setItemName(itemName);
            selectedItem.setItemQuantity(quantity);
            selectedItem.setItemLocation(location);
            
            controller.updateInventory(selectedItem, success -> {
                if (success) {
                    inventoryTable.refresh();
                    updateLocationFilter();
                }
            });
            
        } catch (IllegalArgumentException e) {
            showError("Input Error", e.getMessage());
        }
    }
    
    /**
     * Delete selected inventory item
     */
    private void deleteInventoryItem() {
        if (selectedItem == null) return;
        
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Delete Confirmation");
        confirmDialog.setHeaderText("Delete Inventory Item");
        confirmDialog.setContentText("Are you sure you want to delete '" + selectedItem.getItemName() + "'?");
        
        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                controller.deleteInventory(selectedItem.getItemId(), success -> {
                    if (success) {
                        inventoryData.remove(selectedItem);
                        updateLocationFilter();
                        clearForm();
                    }
                });
            }
        });
    }
    
    /**
     * Add stock to selected item
     */
    private void addStock() {
        if (selectedItem == null) return;
        
        try {
            int quantityToAdd = ValidationUtils.validateQuantity(stockChangeField.getText());
            if (quantityToAdd <= 0) {
                showError("Input Error", "Quantity must be greater than 0");
                return;
            }
            
            int newQuantity = selectedItem.getItemQuantity() + quantityToAdd;
            
            controller.updateStock(selectedItem.getItemId(), newQuantity, success -> {
                if (success) {
                    selectedItem.setItemQuantity(newQuantity);
                    inventoryTable.refresh();
                    stockChangeField.clear();
                    populateForm(selectedItem);
                }
            });
            
        } catch (IllegalArgumentException e) {
            showError("Input Error", e.getMessage());
        }
    }
    
    /**
     * Remove stock from selected item
     */
    private void removeStock() {
        if (selectedItem == null) return;
        
        try {
            int quantityToRemove = ValidationUtils.validateQuantity(stockChangeField.getText());
            if (quantityToRemove <= 0) {
                showError("Input Error", "Quantity must be greater than 0");
                return;
            }
            
            int newQuantity = selectedItem.getItemQuantity() - quantityToRemove;
            if (newQuantity < 0) {
                showError("Stock Error", "Insufficient stock. Available: " + selectedItem.getItemQuantity());
                return;
            }
            
            controller.updateStock(selectedItem.getItemId(), newQuantity, success -> {
                if (success) {
                    selectedItem.setItemQuantity(newQuantity);
                    inventoryTable.refresh();
                    stockChangeField.clear();
                    populateForm(selectedItem);
                }
            });
            
        } catch (IllegalArgumentException e) {
            showError("Input Error", e.getMessage());
        }
    }
    
    /**
     * Populate form with selected item data
     */
    private void populateForm(Inventory item) {
        itemNameField.setText(item.getItemName());
        quantityField.setText(String.valueOf(item.getItemQuantity()));
        locationField.setText(item.getItemLocation());
        stockChangeField.clear();
    }
    
    /**
     * Clear form fields
     */
    private void clearForm() {
        itemNameField.clear();
        quantityField.clear();
        locationField.clear();
        stockChangeField.clear();
        selectedItem = null;
        inventoryTable.getSelectionModel().clearSelection();
    }
    
    /**
     * Update location filter combo box
     */
    private void updateLocationFilter() {
        String currentSelection = locationFilter.getValue();
        
        List<String> locations = inventoryData.stream()
            .map(Inventory::getItemLocation)
            .distinct()
            .sorted()
            .toList();
        
        locationFilter.getItems().clear();
        locationFilter.getItems().add(""); // Empty for all locations
        locationFilter.getItems().addAll(locations);
        
        if (currentSelection != null && locationFilter.getItems().contains(currentSelection)) {
            locationFilter.setValue(currentSelection);
        }
    }
    
    /**
     * Update summary label
     */
    private void updateSummaryLabel(Label label) {
        int totalItems = filteredData.size();
        int totalQuantity = filteredData.stream()
            .mapToInt(Inventory::getItemQuantity)
            .sum();
        int lowStockItems = (int) filteredData.stream()
            .filter(item -> item.getItemQuantity() <= 10)
            .count();
        
        label.setText(String.format("Total Items: %d | Total Quantity: %d | Low Stock Items: %d",
            totalItems, totalQuantity, lowStockItems));
    }
    
    /**
     * Show error dialog
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Load inventory data
     */
    public void loadInventoryData() {
        controller.loadAllInventory(data -> {
            inventoryData.setAll(data);
            updateLocationFilter();
        });
    }
    
    /**
     * Refresh data
     */
    public void refreshData() {
        loadInventoryData();
        clearForm();
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
        // Cleanup resources if needed
    }
}