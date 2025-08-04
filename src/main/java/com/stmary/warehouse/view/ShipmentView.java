package com.stmary.warehouse.view;

import com.stmary.warehouse.controller.MainController;
import com.stmary.warehouse.model.Shipment;
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

/**
 * Shipment tracking view - simplified version
 * Follows same pattern as other views
 */
public class ShipmentView {
    private final MainController controller;
    private BorderPane root;
    private TableView<Shipment> shipmentTable;
    private ObservableList<Shipment> shipmentData;
    private FilteredList<Shipment> filteredData;
    
    // Form controls
    private TextField destinationField;
    private DatePicker shipmentDatePicker;
    private ComboBox<Shipment.ShipmentStatus> statusCombo;
    private Button addButton;
    private Button updateButton;
    private Button deleteButton;
    
    // Search controls
    private TextField searchField;
    private ComboBox<Shipment.ShipmentStatus> statusFilter;
    
    // Tracking action buttons (add these as class fields at the top)
    private Button shipTrackingButton;
    private Button outForDeliveryButton;
    private Button deliverTrackingButton;
    private Button returnTrackingButton;
    
    private Shipment selectedShipment;
    
    public ShipmentView(MainController controller) {
        this.controller = controller;
        this.shipmentData = FXCollections.observableArrayList();
        this.filteredData = new FilteredList<>(shipmentData);
        initializeView();
        setupEventHandlers();
    }
    
    private void initializeView() {
        root = new BorderPane();
        root.getStyleClass().add("shipment-view");
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
        Label searchLabel = new Label("Search Destination:");
        searchField = new TextField();
        searchField.setPromptText("Search by destination...");
        searchField.setPrefWidth(250);
        searchField.getStyleClass().add("search-field");
        
        // Status filter
        Label statusLabel = new Label("Status:");
        statusFilter = new ComboBox<>();
        statusFilter.getItems().add(null); // All statuses
        statusFilter.getItems().addAll(Shipment.ShipmentStatus.values());
        statusFilter.setPromptText("All statuses");
        statusFilter.setPrefWidth(150);
        
        // Show trackable only
        CheckBox trackableOnly = new CheckBox("Trackable only");
        trackableOnly.getStyleClass().add("filter-checkbox");
        
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
            trackableOnly,
            spacer, refreshButton
        );
        
        return toolbar;
    }
    
    private VBox createTableView() {
        VBox tableContainer = new VBox();
        tableContainer.getStyleClass().add("left-panel");
        tableContainer.setSpacing(10);
        
        Label tableTitle = new Label("Shipments");
        tableTitle.getStyleClass().add("section-title");
        
        shipmentTable = new TableView<>();
        shipmentTable.getStyleClass().add("data-table");
        shipmentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // Create columns
        TableColumn<Shipment, Integer> idColumn = new TableColumn<>("Shipment ID");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("shipmentId"));
        idColumn.setPrefWidth(100);
        
        TableColumn<Shipment, String> destinationColumn = new TableColumn<>("Destination");
        destinationColumn.setCellValueFactory(new PropertyValueFactory<>("destination"));
        destinationColumn.setPrefWidth(200);
        
        TableColumn<Shipment, LocalDate> dateColumn = new TableColumn<>("Ship Date");
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("shipmentDate"));
        dateColumn.setPrefWidth(120);
        
        TableColumn<Shipment, Shipment.ShipmentStatus> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("shipmentStatus"));
        statusColumn.setPrefWidth(150);
        statusColumn.setCellFactory(column -> new TableCell<Shipment, Shipment.ShipmentStatus>() {
            @Override
            protected void updateItem(Shipment.ShipmentStatus status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status.getDisplayName());
                    switch (status) {
                        case PREPARING -> setTextFill(Color.ORANGE);
                        case IN_TRANSIT -> setTextFill(Color.BLUE);
                        case OUT_FOR_DELIVERY -> setTextFill(Color.PURPLE);
                        case DELIVERED -> setTextFill(Color.GREEN);
                        case RETURNED -> setTextFill(Color.RED);
                        case CANCELLED -> setTextFill(Color.DARKRED);
                    }
                    setStyle("-fx-font-weight: bold;");
                }
            }
        });
        
        // Days in transit column
        TableColumn<Shipment, String> daysColumn = new TableColumn<>("Days");
        daysColumn.setCellFactory(column -> new TableCell<Shipment, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    Shipment shipment = getTableView().getItems().get(getIndex());
                    if (shipment != null) {
                        long days = shipment.getDaysInTransit();
                        setText(days + " days");
                        if (days > 7 && !shipment.isCompleted()) {
                            setTextFill(Color.RED);
                            setStyle("-fx-font-weight: bold;");
                        } else {
                            setTextFill(Color.BLACK);
                            setStyle("");
                        }
                    }
                }
            }
        });
        daysColumn.setPrefWidth(80);
        
        shipmentTable.getColumns().addAll(idColumn, destinationColumn, dateColumn, statusColumn, daysColumn);
        
        // Set up sorting and filtering
        SortedList<Shipment> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(shipmentTable.comparatorProperty());
        shipmentTable.setItems(sortedData);
        
        tableContainer.getChildren().addAll(tableTitle, shipmentTable);
        VBox.setVgrow(shipmentTable, Priority.ALWAYS);
        
        return tableContainer;
    }
    
    private VBox createFormView() {
        VBox formContainer = new VBox();
        formContainer.getStyleClass().add("right-panel");
        formContainer.setSpacing(15);
        formContainer.setPadding(new Insets(0, 0, 0, 20));
        formContainer.setPrefWidth(300);
        
        Label formTitle = new Label("Shipment Details");
        formTitle.getStyleClass().add("section-title");
        
        // Form fields
        VBox form = new VBox();
        form.setSpacing(10);
        form.getStyleClass().add("form-container");
        
        // Destination
        Label destinationLabel = new Label("Destination:");
        destinationField = new TextField();
        destinationField.setPromptText("Enter destination");
        destinationField.getStyleClass().add("form-field");
        
        // Shipment Date
        Label dateLabel = new Label("Shipment Date:");
        shipmentDatePicker = new DatePicker(LocalDate.now());
        shipmentDatePicker.getStyleClass().add("form-field");
        
        // Status
        Label statusLabel = new Label("Status:");
        statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll(Shipment.ShipmentStatus.values());
        statusCombo.setValue(Shipment.ShipmentStatus.PREPARING);
        statusCombo.getStyleClass().add("form-field");
        
        form.getChildren().addAll(
            destinationLabel, destinationField,
            dateLabel, shipmentDatePicker,
            statusLabel, statusCombo
        );
        
        // Buttons
        HBox buttons = new HBox();
        buttons.setSpacing(10);
        buttons.setAlignment(Pos.CENTER);
        
        addButton = new Button("Add Shipment");
        addButton.getStyleClass().addAll("btn", "btn-success");
        addButton.setPrefWidth(110);
        
        updateButton = new Button("Update");
        updateButton.getStyleClass().addAll("btn", "btn-primary");
        updateButton.setPrefWidth(110);
        updateButton.setDisable(true);
        
        deleteButton = new Button("Delete");
        deleteButton.getStyleClass().addAll("btn", "btn-danger");
        deleteButton.setPrefWidth(110);
        deleteButton.setDisable(true);
        
        buttons.getChildren().addAll(addButton, updateButton, deleteButton);
        
        // Tracking section
        VBox tracking = new VBox();
        tracking.setSpacing(8);
        
        Label trackingLabel = new Label("Tracking Actions:");
        trackingLabel.getStyleClass().add("section-title");
        trackingLabel.setStyle("-fx-font-size: 14px;");
        
        Button shipBtn = new Button("Ship");
        shipBtn.getStyleClass().addAll("btn", "btn-warning");
        shipBtn.setPrefWidth(140);
        shipBtn.setDisable(true);
        shipBtn.setOnAction(e -> shipSelectedShipment());
        
        Button outForDeliveryBtn = new Button("Out for Delivery");
        outForDeliveryBtn.getStyleClass().addAll("btn", "btn-warning");
        outForDeliveryBtn.setPrefWidth(140);
        outForDeliveryBtn.setDisable(true);
        outForDeliveryBtn.setOnAction(e -> outForDeliverySelected());
        
        Button deliverBtn = new Button("Mark Delivered");
        deliverBtn.getStyleClass().addAll("btn", "btn-success");
        deliverBtn.setPrefWidth(140);
        deliverBtn.setDisable(true);
        deliverBtn.setOnAction(e -> deliverSelectedShipment());
        
        Button returnBtn = new Button("Mark Returned");
        returnBtn.getStyleClass().addAll("btn", "btn-danger");
        returnBtn.setPrefWidth(140);
        returnBtn.setDisable(true);
        returnBtn.setOnAction(e -> returnSelectedShipment());
        
        tracking.getChildren().addAll(trackingLabel, shipBtn, outForDeliveryBtn, deliverBtn, returnBtn);
        
        // Store references
        this.shipTrackingButton = shipBtn;
        this.outForDeliveryButton = outForDeliveryBtn;
        this.deliverTrackingButton = deliverBtn;
        this.returnTrackingButton = returnBtn;
        
        formContainer.getChildren().addAll(formTitle, form, buttons, tracking);
        
        return formContainer;
    }
    
    private void setupEventHandlers() {
        // Table selection
        shipmentTable.getSelectionModel().selectedItemProperty().addListener((obs, oldShipment, newShipment) -> {
            selectedShipment = newShipment;
            if (newShipment != null) {
                populateForm(newShipment);
                updateButton.setDisable(false);
                deleteButton.setDisable(false);
                updateTrackingButtons(newShipment);
            } else {
                clearForm();
                updateButton.setDisable(true);
                deleteButton.setDisable(true);
                disableTrackingButtons();
            }
        });
        
        // Search and filter
        searchField.textProperty().addListener((obs, oldText, newText) -> updateFilters());
        statusFilter.valueProperty().addListener((obs, oldStatus, newStatus) -> updateFilters());
        
        // Button actions
        addButton.setOnAction(e -> addShipment());
        updateButton.setOnAction(e -> updateShipment());
        deleteButton.setOnAction(e -> deleteShipment());
    }
    
    private void updateFilters() {
        filteredData.setPredicate(shipment -> {
            // Search filter
            String searchText = searchField.getText();
            if (searchText != null && !searchText.isEmpty()) {
                if (!shipment.getDestination().toLowerCase().contains(searchText.toLowerCase())) {
                    return false;
                }
            }
            
            // Status filter
            Shipment.ShipmentStatus selectedStatus = statusFilter.getValue();
            if (selectedStatus != null && shipment.getShipmentStatus() != selectedStatus) {
                return false;
            }
            
            return true;
        });
    }
    
    private void addShipment() {
        try {
            Shipment newShipment = new Shipment(
                destinationField.getText().trim(),
                shipmentDatePicker.getValue(),
                statusCombo.getValue()
            );
            
            controller.createShipment(newShipment, createdShipment -> {
                shipmentData.add(createdShipment);
                clearForm();
                shipmentTable.getSelectionModel().select(createdShipment);
            });
        } catch (Exception e) {
            showError("Error creating shipment: " + e.getMessage());
        }
    }
    
    private void updateShipment() {
        if (selectedShipment == null) return;
        
        try {
            selectedShipment.setDestination(destinationField.getText().trim());
            selectedShipment.setShipmentDate(shipmentDatePicker.getValue());
            selectedShipment.setShipmentStatus(statusCombo.getValue());
            
            controller.updateShipment(selectedShipment, success -> {
                if (success) {
                    shipmentTable.refresh();
                }
            });
        } catch (Exception e) {
            showError("Error updating shipment: " + e.getMessage());
        }
    }
    
    private void deleteShipment() {
        if (selectedShipment == null) return;
        
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Delete Confirmation");
        confirmDialog.setHeaderText("Delete Shipment");
        confirmDialog.setContentText("Are you sure you want to delete shipment to '" + 
            selectedShipment.getDestination() + "'?");
        
        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                controller.deleteShipment(selectedShipment.getShipmentId(), success -> {
                    if (success) {
                        shipmentData.remove(selectedShipment);
                        clearForm();
                    }
                });
            }
        });
    }
    
    private void populateForm(Shipment shipment) {
        destinationField.setText(shipment.getDestination());
        shipmentDatePicker.setValue(shipment.getShipmentDate());
        statusCombo.setValue(shipment.getShipmentStatus());
    }
    
    private void clearForm() {
        destinationField.clear();
        shipmentDatePicker.setValue(LocalDate.now());
        statusCombo.setValue(Shipment.ShipmentStatus.PREPARING);
        selectedShipment = null;
        shipmentTable.getSelectionModel().clearSelection();
    }
    
    private void updateTrackingButtons(Shipment shipment) {
        Shipment.ShipmentStatus status = shipment.getShipmentStatus();
        
        shipTrackingButton.setDisable(status != Shipment.ShipmentStatus.PREPARING);
        outForDeliveryButton.setDisable(status != Shipment.ShipmentStatus.IN_TRANSIT);
        deliverTrackingButton.setDisable(status != Shipment.ShipmentStatus.OUT_FOR_DELIVERY);
        returnTrackingButton.setDisable(shipment.isCompleted());
    }
    
    private void disableTrackingButtons() {
        shipTrackingButton.setDisable(true);
        outForDeliveryButton.setDisable(true);
        deliverTrackingButton.setDisable(true);
        returnTrackingButton.setDisable(true);
    }
    
    // Tracking action methods
    private void shipSelectedShipment() {
        if (selectedShipment != null) {
            controller.shipShipment(selectedShipment.getShipmentId(), success -> {
                if (success) {
                    selectedShipment.setShipmentStatus(Shipment.ShipmentStatus.IN_TRANSIT);
                    shipmentTable.refresh();
                    updateTrackingButtons(selectedShipment);
                }
            });
        }
    }
    
    private void outForDeliverySelected() {
        if (selectedShipment != null) {
            controller.outForDelivery(selectedShipment.getShipmentId(), success -> {
                if (success) {
                    selectedShipment.setShipmentStatus(Shipment.ShipmentStatus.OUT_FOR_DELIVERY);
                    shipmentTable.refresh();
                    updateTrackingButtons(selectedShipment);
                }
            });
        }
    }
    
    private void deliverSelectedShipment() {
        if (selectedShipment != null) {
            controller.deliverShipment(selectedShipment.getShipmentId(), success -> {
                if (success) {
                    selectedShipment.setShipmentStatus(Shipment.ShipmentStatus.DELIVERED);
                    shipmentTable.refresh();
                    updateTrackingButtons(selectedShipment);
                }
            });
        }
    }
    
    private void returnSelectedShipment() {
        if (selectedShipment != null) {
            Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
            confirmDialog.setTitle("Return Shipment");
            confirmDialog.setHeaderText("Return Shipment Confirmation");
            confirmDialog.setContentText("Are you sure you want to mark this shipment as returned?");
            
            confirmDialog.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    controller.returnShipment(selectedShipment.getShipmentId(), success -> {
                        if (success) {
                            selectedShipment.setShipmentStatus(Shipment.ShipmentStatus.RETURNED);
                            shipmentTable.refresh();
                            updateTrackingButtons(selectedShipment);
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
    
    public void loadShipmentData() {
        controller.loadAllShipments(data -> shipmentData.setAll(data));
    }
    
    public void refreshData() {
        loadShipmentData();
        clearForm();
    }
    
    public BorderPane getRoot() {
        return root;
    }
    
    public void cleanup() {
        // Cleanup resources if needed
    }
}