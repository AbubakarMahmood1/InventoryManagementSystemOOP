package com.stmary.warehouse;

import com.stmary.warehouse.controller.MainController;
import com.stmary.warehouse.view.MainView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * Main JavaFX Application for St Mary's Warehouse Management System
 * Entry point with proper initialization and shutdown handling
 */
public class WarehouseManagementApp extends Application {
    
    private MainController controller;
    private MainView mainView;
    
    @Override
    public void start(Stage primaryStage) {
        try {
            // Initialize controller
            controller = new MainController();
            
            // Initialize main view
            mainView = new MainView(controller);
            
            // Create scene
            Scene scene = new Scene(mainView.getRoot(), 1400, 900);
            
            // Load CSS styling
            scene.getStylesheets().add(getClass().getResource("/styles/warehouse.css").toExternalForm());
            
            // Configure primary stage
            primaryStage.setTitle("St Mary's Warehouse Management System");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1200);
            primaryStage.setMinHeight(800);
            
            // Set application icon
            try {
                primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/icons/warehouse.png")));
            } catch (Exception e) {
                // Icon not found, continue without it
                System.out.println("Warning: Application icon not found");
            }
            
            // Configure close behavior
            primaryStage.setOnCloseRequest(event -> {
                event.consume(); // Prevent immediate close
                handleApplicationExit();
            });
            
            // Show the stage
            primaryStage.show();
            
            // Initialize data loading
            mainView.initializeData();
            
        } catch (Exception e) {
            showErrorDialog("Application Startup Error", 
                "Failed to start the application: " + e.getMessage());
            Platform.exit();
        }
    }
    
    /**
     * Handle application exit with proper cleanup
     */
    private void handleApplicationExit() {
        try {
            // Confirm exit
            Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
            confirmDialog.setTitle("Exit Confirmation");
            confirmDialog.setHeaderText("Exit Application");
            confirmDialog.setContentText("Are you sure you want to exit the Warehouse Management System?");
            
            confirmDialog.showAndWait().ifPresent(response -> {
                if (response == javafx.scene.control.ButtonType.OK) {
                    performCleanup();
                    Platform.exit();
                }
            });
            
        } catch (Exception e) {
            System.err.println("Error during application exit: " + e.getMessage());
            Platform.exit();
        }
    }
    
    /**
     * Perform cleanup operations
     */
    private void performCleanup() {
        try {
            if (controller != null) {
                controller.shutdown();
            }
            
            if (mainView != null) {
                mainView.cleanup();
            }
            
        } catch (Exception e) {
            System.err.println("Error during cleanup: " + e.getMessage());
        }
    }
    
    /**
     * Show error dialog
     */
    private void showErrorDialog(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    /**
     * Application entry point
     */
    public static void main(String[] args) {
        // Set system properties for better JavaFX performance
        System.setProperty("javafx.animation.fullspeed", "true");
        System.setProperty("javafx.animation.pulse", "60");
        System.setProperty("prism.lcdtext", "false");
        System.setProperty("prism.text", "t2k");
        
        // Handle uncaught exceptions
        Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {
            System.err.println("Uncaught exception in thread " + thread.getName() + ": " + exception.getMessage());
            exception.printStackTrace();
            
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Unexpected Error");
                alert.setHeaderText("An unexpected error occurred");
                alert.setContentText("Error: " + exception.getMessage() + "\n\nThe application may need to be restarted.");
                alert.showAndWait();
            });
        });
        
        // Launch JavaFX application
        launch(args);
    }
}