package com.stmary.warehouse.view;

import com.stmary.warehouse.controller.MainController;
import com.stmary.warehouse.model.Order;
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

import java.time.LocalDate;
import java.util.List;

/**
 * Order management view - simplified version
 * Follows same pattern as InventoryView
 */
public class OrderView {
    private final MainController controller;
    private BorderPane root;
    private TableView<Order> orderTable;
    private ObservableList<Order> orderData;
    private FilteredList<Order> filteredData;
    
    // Form controls
    private TextField customerField;
    private DatePicker orderDatePicker;
    private ComboBox<Order.OrderStatus> statusCombo;
    private Button addButton;
    private Button updateButton;
    private Button deleteButton;
    
    // Search controls
    private TextField searchField;
    private ComboBox<Order.OrderStatus> statusFilter;
    
    // Quick action buttons (add these as class fields at the top)
    private Button confirmButton;
    private Button processButton;
    private Button shipButton;
    private Button cancelButton;
    
    private Order selectedOrder;
    
    public OrderView(MainController controller) {
        this.controller = controller;
        this.orderData = FXCollections.observableArrayList();
        this.filteredData = new FilteredList<>(orderData);
        initializeView();
        setupEventHandlers();
    }
    
    private void initializeView() {
        root = new BorderPane();
        root.getStyleClass().add("order-view");
        root.setPadding(new Insets(20));
        
        // Create toolbar
        HBox toolbar = createToolbar();
        root.setTop(toolbar);
        
        // Create main content
        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(createTableView(), createFormView());
        splitPane.setDividerPositions(0.7);
        root.setCenter(splitPane);
    }
    
    private HBox createToolbar() {
        HBox toolbar = new HBox();
        toolbar.getStyleClass().add("toolbar");
        toolbar.setPadding(new Insets(0, 0, 15, 0));
        toolbar.setSpacing(15);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        
        // Search field
        Label searchLabel = new Label("Search Customer:");
        searchField = new TextField();
        searchField.setPromptText("Search by customer name...");
        searchField.setPrefWidth(250);
        searchField.getStyleClass().add("search-field");
        
        // Status filter
        Label statusLabel = new Label("Status:");
        statusFilter = new ComboBox<>();
        statusFilter.getItems().add(null); // All statuses
        statusFilter.getItems().addAll(Order.OrderStatus.values());
        statusFilter.setPromptText("All statuses");
        statusFilter.setPrefWidth(150);
        
        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Refresh button
        Button refreshButton = new Button("Refresh");
        refreshButton.getStyleClass().addAll("btn", "btn-primary");
        refreshButton.setOnAction(e -> refreshData());
        
        toolbar.getChildren().addAll(
            searchLabel, searchField,
            statusLabel, statusFilter,
            spacer, refreshButton
        );
        
        return toolbar;
    }
    
    private VBox createTableView() {
        VBox tableContainer = new VBox();
        tableContainer.getStyleClass().add("left-panel");
        tableContainer.setSpacing(10);
        
        Label tableTitle = new Label("Orders");
        tableTitle.getStyleClass().add("section-title");
        
        orderTable = new TableView<>();
        orderTable.getStyleClass().add("data-table");
        orderTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // Create columns
        TableColumn<Order, Integer> idColumn = new TableColumn<>("Order ID");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("orderId"));
        idColumn.setPrefWidth(80);
        
        TableColumn<Order, LocalDate> dateColumn = new TableColumn<>("Date");
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("orderDate"));
        dateColumn.setPrefWidth(120);
        
        TableColumn<Order, String> customerColumn = new TableColumn<>("Customer");
        customerColumn.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        customerColumn.setPrefWidth(200);
        
        TableColumn<Order, Order.OrderStatus> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("orderStatus"));
        statusColumn.setPrefWidth(120);
        statusColumn.setCellFactory(column -> new TableCell<Order, Order.OrderStatus>() {
            @Override
            protected void updateItem(Order.OrderStatus status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status.getDisplayName());
                    switch (status) {
                        case PENDING -> setTextFill(Color.ORANGE);
                        case CONFIRMED -> setTextFill(Color.BLUE);
                        case PROCESSING -> setTextFill(Color.PURPLE);
                        case SHIPPED -> setTextFill(Color.GREEN);
                        case DELIVERED -> setTextFill(Color.DARKGREEN);
                        case CANCELLED -> setTextFill(Color.RED);
                    }
                    setStyle("-fx-font-weight: bold;");
                }
            }
        });
        
        orderTable.getColumns().addAll(idColumn, dateColumn, customerColumn, statusColumn);
        
        // Set up sorting and filtering
        SortedList<Order> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(orderTable.comparatorProperty());
        orderTable.setItems(sortedData);
        
        tableContainer.getChildren().addAll(tableTitle, orderTable);
        VBox.setVgrow(orderTable, Priority.ALWAYS);
        
        return tableContainer;
    }
    
    private VBox createFormView() {
        VBox formContainer = new VBox();
        formContainer.getStyleClass().add("right-panel");
        formContainer.setSpacing(15);
        formContainer.setPadding(new Insets(0, 0, 0, 20));
        formContainer.setPrefWidth(300);
        
        Label formTitle = new Label("Order Details");
        formTitle.getStyleClass().add("section-title");
        
        // Form fields
        VBox form = new VBox();
        form.setSpacing(10);
        form.getStyleClass().add("form-container");
        
        // Customer Name
        Label customerLabel = new Label("Customer Name:");
        customerField = new TextField();
        customerField.setPromptText("Enter customer name");
        customerField.getStyleClass().add("form-field");
        
        // Order Date
        Label dateLabel = new Label("Order Date:");
        orderDatePicker = new DatePicker(LocalDate.now());
        orderDatePicker.getStyleClass().add("form-field");
        
        // Status
        Label statusLabel = new Label("Status:");
        statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll(Order.OrderStatus.values());
        statusCombo.setValue(Order.OrderStatus.PENDING);
        statusCombo.getStyleClass().add("form-field");
        
        form.getChildren().addAll(
            customerLabel, customerField,
            dateLabel, orderDatePicker,
            statusLabel, statusCombo
        );
        
        // Buttons
        HBox buttons = new HBox();
        buttons.setSpacing(10);
        buttons.setAlignment(Pos.CENTER);
        
        addButton = new Button("Add Order");
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
        
        buttons.getChildren().addAll(addButton, updateButton, deleteButton);
        
        // Quick actions
        VBox quickActions = new VBox();
        quickActions.setSpacing(8);
        
        Label actionsLabel = new Label("Quick Actions:");
        actionsLabel.getStyleClass().add("section-title");
        actionsLabel.setStyle("-fx-font-size: 14px;");
        
        Button confirmBtn = new Button("Confirm Order");
        confirmBtn.getStyleClass().addAll("btn", "btn-warning");
        confirmBtn.setPrefWidth(140);
        confirmBtn.setDisable(true);
        confirmBtn.setOnAction(e -> confirmSelectedOrder());
        
        Button processBtn = new Button("Process Order");
        processBtn.getStyleClass().addAll("btn", "btn-warning");
        processBtn.setPrefWidth(140);
        processBtn.setDisable(true);
        processBtn.setOnAction(e -> processSelectedOrder());
        
        Button shipBtn = new Button("Ship Order");
        shipBtn.getStyleClass().addAll("btn", "btn-warning");
        shipBtn.setPrefWidth(140);
        shipBtn.setDisable(true);
        shipBtn.setOnAction(e -> shipSelectedOrder());
        
        Button cancelBtn = new Button("Cancel Order");
        cancelBtn.getStyleClass().addAll("btn", "btn-danger");
        cancelBtn.setPrefWidth(140);
        cancelBtn.setDisable(true);
        cancelBtn.setOnAction(e -> cancelSelectedOrder());
        
        quickActions.getChildren().addAll(actionsLabel, confirmBtn, processBtn, shipBtn, cancelBtn);
        
        // Store references for enabling/disabling
        this.confirmButton = confirmBtn;
        this.processButton = processBtn;
        this.shipButton = shipBtn;
        this.cancelButton = cancelBtn;
        
        formContainer.getChildren().addAll(formTitle, form, buttons, quickActions);
        
        return formContainer;
    }
    
    private void setupEventHandlers() {
        // Table selection
        orderTable.getSelectionModel().selectedItemProperty().addListener((obs, oldOrder, newOrder) -> {
            selectedOrder = newOrder;
            if (newOrder != null) {
                populateForm(newOrder);
                updateButton.setDisable(false);
                deleteButton.setDisable(false);
                updateQuickActionButtons(newOrder);
            } else {
                clearForm();
                updateButton.setDisable(true);
                deleteButton.setDisable(true);
                disableQuickActions();
            }
        });
        
        // Search and filter
        searchField.textProperty().addListener((obs, oldText, newText) -> updateFilters());
        statusFilter.valueProperty().addListener((obs, oldStatus, newStatus) -> updateFilters());
        
        // Button actions
        addButton.setOnAction(e -> addOrder());
        updateButton.setOnAction(e -> updateOrder());
        deleteButton.setOnAction(e -> deleteOrder());
    }
    
    private void updateFilters() {
        filteredData.setPredicate(order -> {
            // Search filter
            String searchText = searchField.getText();
            if (searchText != null && !searchText.isEmpty()) {
                if (!order.getCustomerName().toLowerCase().contains(searchText.toLowerCase())) {
                    return false;
                }
            }
            
            // Status filter
            Order.OrderStatus selectedStatus = statusFilter.getValue();
            if (selectedStatus != null && order.getOrderStatus() != selectedStatus) {
                return false;
            }
            
            return true;
        });
    }
    
    private void addOrder() {
        try {
            Order newOrder = new Order(
                orderDatePicker.getValue(),
                customerField.getText().trim(),
                statusCombo.getValue()
            );
            
            controller.createOrder(newOrder, createdOrder -> {
                orderData.add(createdOrder);
                clearForm();
                orderTable.getSelectionModel().select(createdOrder);
            });
        } catch (Exception e) {
            showError("Error creating order: " + e.getMessage());
        }
    }
    
    private void updateOrder() {
        if (selectedOrder == null) return;
        
        try {
            selectedOrder.setCustomerName(customerField.getText().trim());
            selectedOrder.setOrderDate(orderDatePicker.getValue());
            selectedOrder.setOrderStatus(statusCombo.getValue());
            
            controller.updateOrder(selectedOrder, success -> {
                if (success) {
                    orderTable.refresh();
                }
            });
        } catch (Exception e) {
            showError("Error updating order: " + e.getMessage());
        }
    }
    
    private void deleteOrder() {
        if (selectedOrder == null) return;
        
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Delete Confirmation");
        confirmDialog.setHeaderText("Delete Order");
        confirmDialog.setContentText("Are you sure you want to delete order for '" + 
            selectedOrder.getCustomerName() + "'?");
        
        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                controller.deleteOrder(selectedOrder.getOrderId(), success -> {
                    if (success) {
                        orderData.remove(selectedOrder);
                        clearForm();
                    }
                });
            }
        });
    }
    
    private void populateForm(Order order) {
        customerField.setText(order.getCustomerName());
        orderDatePicker.setValue(order.getOrderDate());
        statusCombo.setValue(order.getOrderStatus());
    }
    
    private void clearForm() {
        customerField.clear();
        orderDatePicker.setValue(LocalDate.now());
        statusCombo.setValue(Order.OrderStatus.PENDING);
        selectedOrder = null;
        orderTable.getSelectionModel().clearSelection();
    }
    
    private void updateQuickActionButtons(Order order) {
        // Enable buttons based on current status and business rules
        Order.OrderStatus status = order.getOrderStatus();
        
        confirmButton.setDisable(status != Order.OrderStatus.PENDING);
        processButton.setDisable(status != Order.OrderStatus.CONFIRMED);
        shipButton.setDisable(status != Order.OrderStatus.PROCESSING);
        cancelButton.setDisable(!order.canBeCancelled());
    }
    
    private void disableQuickActions() {
        confirmButton.setDisable(true);
        processButton.setDisable(true);
        shipButton.setDisable(true);
        cancelButton.setDisable(true);
    }
    
    // Quick action methods
    private void confirmSelectedOrder() {
        if (selectedOrder != null) {
            controller.confirmOrder(selectedOrder.getOrderId(), success -> {
                if (success) {
                    selectedOrder.setOrderStatus(Order.OrderStatus.CONFIRMED);
                    orderTable.refresh();
                    updateQuickActionButtons(selectedOrder);
                }
            });
        }
    }
    
    private void processSelectedOrder() {
        if (selectedOrder != null) {
            controller.processOrder(selectedOrder.getOrderId(), success -> {
                if (success) {
                    selectedOrder.setOrderStatus(Order.OrderStatus.PROCESSING);
                    orderTable.refresh();
                    updateQuickActionButtons(selectedOrder);
                }
            });
        }
    }
    
    private void shipSelectedOrder() {
        if (selectedOrder != null) {
            controller.shipOrder(selectedOrder.getOrderId(), success -> {
                if (success) {
                    selectedOrder.setOrderStatus(Order.OrderStatus.SHIPPED);
                    orderTable.refresh();
                    updateQuickActionButtons(selectedOrder);
                }
            });
        }
    }
    
    private void cancelSelectedOrder() {
        if (selectedOrder != null) {
            Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
            confirmDialog.setTitle("Cancel Order");
            confirmDialog.setHeaderText("Cancel Order Confirmation");
            confirmDialog.setContentText("Are you sure you want to cancel this order?");
            
            confirmDialog.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    controller.cancelOrder(selectedOrder.getOrderId(), success -> {
                        if (success) {
                            selectedOrder.setOrderStatus(Order.OrderStatus.CANCELLED);
                            orderTable.refresh();
                            updateQuickActionButtons(selectedOrder);
                        }
                    });
                }
            });
        }
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    public void loadOrderData() {
        controller.loadAllOrders(data -> orderData.setAll(data));
    }
    
    public void refreshData() {
        loadOrderData();
        clearForm();
    }
    
    public BorderPane getRoot() {
        return root;
    }
    
    public void cleanup() {
        // Cleanup resources if needed
    }
}