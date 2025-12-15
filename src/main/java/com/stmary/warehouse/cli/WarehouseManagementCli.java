package com.stmary.warehouse.cli;

import com.stmary.warehouse.model.Inventory;
import com.stmary.warehouse.model.Order;
import com.stmary.warehouse.model.Shipment;
import com.stmary.warehouse.service.InventoryService;
import com.stmary.warehouse.service.OrderService;
import com.stmary.warehouse.service.ShipmentService;
import com.stmary.warehouse.util.ValidationUtils;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.CompletionException;

/**
 * Console-based, menu-driven interface for St Mary's Warehouse Management System.
 * Implements the assessment's Basic Requirement for a text UI without removing the existing GUI.
 */
public final class WarehouseManagementCli {
    private final Scanner scanner = new Scanner(System.in);

    private final InventoryService inventoryService = new InventoryService();
    private final OrderService orderService = new OrderService();
    private final ShipmentService shipmentService = new ShipmentService();

    public static void main(String[] args) {
        new WarehouseManagementCli().run();
    }

    private void run() {
        println("St Mary's Warehouse Management System (CLI)");
        println("Date format: YYYY-MM-DD");
        println("");

        try {
            mainLoop();
        } finally {
            shutdown();
        }
    }

    private void mainLoop() {
        while (true) {
            println("Main Menu");
            println("1) Manage Inventory");
            println("2) Process Orders");
            println("3) Track Shipments");
            println("0) Exit");

            int choice = readInt("Select option", 0, 3);
            switch (choice) {
                case 1 -> inventoryMenu();
                case 2 -> ordersMenu();
                case 3 -> shipmentsMenu();
                case 0 -> {
                    println("Goodbye.");
                    return;
                }
                default -> println("Invalid option.");
            }
        }
    }

    // ===============================
    // INVENTORY MENU
    // ===============================

    private void inventoryMenu() {
        while (true) {
            println("");
            println("Inventory Menu");
            println("1) List all items");
            println("2) Search by ID");
            println("3) Search by name");
            println("4) Search by location");
            println("5) Add item");
            println("6) Update item");
            println("7) Delete item");
            println("0) Back");

            int choice = readInt("Select option", 0, 7);
            switch (choice) {
                case 1 -> inventoryListAll();
                case 2 -> inventorySearchById();
                case 3 -> inventorySearchByName();
                case 4 -> inventorySearchByLocation();
                case 5 -> inventoryAdd();
                case 6 -> inventoryUpdate();
                case 7 -> inventoryDelete();
                case 0 -> {
                    return;
                }
                default -> println("Invalid option.");
            }
        }
    }

    private void inventoryListAll() {
        try {
            List<Inventory> items = inventoryService.getAllInventory();
            printInventory(items);
        } catch (RuntimeException e) {
            printError(e);
        }
    }

    private void inventorySearchById() {
        int id = readInt("Item ID", 1, Integer.MAX_VALUE);
        try {
            Optional<Inventory> item = inventoryService.findInventoryById(id);
            if (item.isPresent()) {
                printInventory(List.of(item.get()));
            } else {
                println("No inventory item found with ID: " + id);
            }
        } catch (RuntimeException e) {
            printError(e);
        }
    }

    private void inventorySearchByName() {
        String name = readNonEmpty("Name contains");
        try {
            List<Inventory> items = inventoryService.searchByNameAsync(name).join();
            printInventory(items);
        } catch (RuntimeException e) {
            printError(e);
        }
    }

    private void inventorySearchByLocation() {
        String location = readNonEmpty("Location contains");
        try {
            List<Inventory> items = inventoryService.searchByLocationAsync(location).join();
            printInventory(items);
        } catch (RuntimeException e) {
            printError(e);
        }
    }

    private void inventoryAdd() {
        try {
            String itemName = ValidationUtils.validateItemName(readNonEmpty("Item name"));
            int quantity = ValidationUtils.validateQuantity(readNonEmpty("Quantity (0+)"));
            String location = ValidationUtils.validateLocation(readNonEmpty("Location"));

            Inventory inventory = new Inventory();
            inventory.setItemName(itemName);
            inventory.setItemQuantity(quantity);
            inventory.setItemLocation(location);

            Inventory created = inventoryService.createInventory(inventory);
            println("Created: " + created);
        } catch (RuntimeException e) {
            printError(e);
        }
    }

    private void inventoryUpdate() {
        int id = readInt("Item ID to update", 1, Integer.MAX_VALUE);
        try {
            Optional<Inventory> existingOpt = inventoryService.findInventoryById(id);
            if (existingOpt.isEmpty()) {
                println("No inventory item found with ID: " + id);
                return;
            }

            Inventory existing = existingOpt.get();
            println("Current: " + existing);
            println("Press Enter to keep current value.");

            String newNameRaw = readLine("New name [" + existing.getItemName() + "]");
            String newQtyRaw = readLine("New quantity [" + existing.getItemQuantity() + "]");
            String newLocRaw = readLine("New location [" + existing.getItemLocation() + "]");

            if (!newNameRaw.isBlank()) {
                existing.setItemName(ValidationUtils.validateItemName(newNameRaw));
            }
            if (!newQtyRaw.isBlank()) {
                existing.setItemQuantity(ValidationUtils.validateQuantity(newQtyRaw));
            }
            if (!newLocRaw.isBlank()) {
                existing.setItemLocation(ValidationUtils.validateLocation(newLocRaw));
            }

            boolean updated = inventoryService.updateInventory(existing);
            println(updated ? "Updated successfully." : "No changes applied (not updated).");
        } catch (RuntimeException e) {
            printError(e);
        }
    }

    private void inventoryDelete() {
        int id = readInt("Item ID to delete", 1, Integer.MAX_VALUE);
        if (!confirm("Delete inventory item ID " + id + "?")) {
            println("Cancelled.");
            return;
        }

        try {
            boolean deleted = inventoryService.deleteInventory(id);
            println(deleted ? "Deleted successfully." : "Item not deleted (not found).");
        } catch (RuntimeException e) {
            printError(e);
        }
    }

    private void printInventory(List<Inventory> items) {
        println("");
        if (items == null || items.isEmpty()) {
            println("No inventory records.");
            return;
        }

        println("Inventory Records (" + items.size() + ")");
        println("ID\tName\tQuantity\tLocation");
        for (Inventory i : items) {
            println(i.getItemId() + "\t" + i.getItemName() + "\t" + i.getItemQuantity() + "\t\t" + i.getItemLocation());
        }
    }

    // ===============================
    // ORDERS MENU
    // ===============================

    private void ordersMenu() {
        while (true) {
            println("");
            println("Orders Menu");
            println("1) List all orders");
            println("2) Search by ID");
            println("3) Search by customer name");
            println("4) Filter by status");
            println("5) Add order");
            println("6) Update order");
            println("7) Update status (workflow)");
            println("8) Delete order");
            println("0) Back");

            int choice = readInt("Select option", 0, 8);
            switch (choice) {
                case 1 -> ordersListAll();
                case 2 -> ordersSearchById();
                case 3 -> ordersSearchByCustomer();
                case 4 -> ordersFilterByStatus();
                case 5 -> ordersAdd();
                case 6 -> ordersUpdate();
                case 7 -> ordersWorkflowUpdate();
                case 8 -> ordersDelete();
                case 0 -> {
                    return;
                }
                default -> println("Invalid option.");
            }
        }
    }

    private void ordersListAll() {
        try {
            List<Order> orders = orderService.getAllOrders();
            printOrders(orders);
        } catch (RuntimeException e) {
            printError(e);
        }
    }

    private void ordersSearchById() {
        int id = readInt("Order ID", 1, Integer.MAX_VALUE);
        try {
            Optional<Order> order = orderService.findOrderById(id);
            if (order.isPresent()) {
                printOrders(List.of(order.get()));
            } else {
                println("No order found with ID: " + id);
            }
        } catch (RuntimeException e) {
            printError(e);
        }
    }

    private void ordersSearchByCustomer() {
        String customer = readNonEmpty("Customer name contains");
        try {
            List<Order> orders = orderService.searchByCustomerAsync(customer).join();
            printOrders(orders);
        } catch (RuntimeException e) {
            printError(e);
        }
    }

    private void ordersFilterByStatus() {
        Order.OrderStatus status = readOrderStatus("Select status");
        try {
            List<Order> orders = orderService.getOrdersByStatusAsync(status).join();
            printOrders(orders);
        } catch (RuntimeException e) {
            printError(e);
        }
    }

    private void ordersAdd() {
        try {
            LocalDate date = readDateOrDefault("Order date (YYYY-MM-DD, blank=today)", LocalDate.now());
            String customer = ValidationUtils.validateCustomerName(readNonEmpty("Customer name"));
            Order.OrderStatus status = readOrderStatusOrDefault("Initial status (blank=Pending)", Order.OrderStatus.PENDING);

            Order order = new Order();
            order.setOrderDate(date);
            order.setCustomerName(customer);
            order.setOrderStatus(status);

            Order created = orderService.createOrder(order);
            println("Created: " + created);
        } catch (RuntimeException e) {
            printError(e);
        }
    }

    private void ordersUpdate() {
        int id = readInt("Order ID to update", 1, Integer.MAX_VALUE);
        try {
            Optional<Order> existingOpt = orderService.findOrderById(id);
            if (existingOpt.isEmpty()) {
                println("No order found with ID: " + id);
                return;
            }

            Order existing = existingOpt.get();
            println("Current: " + existing);
            println("Press Enter to keep current value.");

            String newDateRaw = readLine("New date [" + existing.getOrderDate() + "]");
            String newCustomerRaw = readLine("New customer [" + existing.getCustomerName() + "]");
            String newStatusRaw = readLine("New status [" + existing.getOrderStatus().getDisplayName() + "]");

            if (!newDateRaw.isBlank()) {
                existing.setOrderDate(parseDate(newDateRaw));
            }
            if (!newCustomerRaw.isBlank()) {
                existing.setCustomerName(ValidationUtils.validateCustomerName(newCustomerRaw));
            }
            if (!newStatusRaw.isBlank()) {
                existing.setOrderStatus(Order.OrderStatus.fromString(newStatusRaw));
            }

            boolean updated = orderService.updateOrder(existing);
            println(updated ? "Updated successfully." : "No changes applied (not updated).");
        } catch (RuntimeException e) {
            printError(e);
        }
    }

    private void ordersWorkflowUpdate() {
        int id = readInt("Order ID", 1, Integer.MAX_VALUE);
        println("Workflow actions:");
        println("1) Confirm");
        println("2) Process");
        println("3) Ship");
        println("4) Deliver");
        println("5) Cancel");
        println("0) Back");

        int choice = readInt("Select action", 0, 5);
        if (choice == 0) return;

        try {
            boolean success = switch (choice) {
                case 1 -> orderService.confirmOrderAsync(id).join();
                case 2 -> orderService.processOrderAsync(id).join();
                case 3 -> orderService.shipOrderAsync(id).join();
                case 4 -> orderService.deliverOrderAsync(id).join();
                case 5 -> orderService.cancelOrderAsync(id).join();
                default -> false;
            };
            println(success ? "Status updated." : "Status not updated (order not found or invalid transition).");
        } catch (RuntimeException e) {
            printError(e);
        }
    }

    private void ordersDelete() {
        int id = readInt("Order ID to delete", 1, Integer.MAX_VALUE);
        if (!confirm("Delete order ID " + id + "?")) {
            println("Cancelled.");
            return;
        }
        try {
            boolean deleted = orderService.deleteOrder(id);
            println(deleted ? "Deleted successfully." : "Order not deleted (not found).");
        } catch (RuntimeException e) {
            printError(e);
        }
    }

    private void printOrders(List<Order> orders) {
        println("");
        if (orders == null || orders.isEmpty()) {
            println("No order records.");
            return;
        }

        println("Order Records (" + orders.size() + ")");
        println("ID\tDate\t\tCustomer\tStatus");
        for (Order o : orders) {
            println(o.getOrderId() + "\t" + o.getOrderDate() + "\t" + o.getCustomerName() + "\t\t" + o.getOrderStatusString());
        }
    }

    // ===============================
    // SHIPMENTS MENU
    // ===============================

    private void shipmentsMenu() {
        while (true) {
            println("");
            println("Shipments Menu");
            println("1) List all shipments");
            println("2) Search by ID");
            println("3) Search by destination");
            println("4) Filter by status");
            println("5) Add shipment");
            println("6) Update shipment");
            println("7) Update status (workflow)");
            println("8) Delete shipment");
            println("0) Back");

            int choice = readInt("Select option", 0, 8);
            switch (choice) {
                case 1 -> shipmentsListAll();
                case 2 -> shipmentsSearchById();
                case 3 -> shipmentsSearchByDestination();
                case 4 -> shipmentsFilterByStatus();
                case 5 -> shipmentsAdd();
                case 6 -> shipmentsUpdate();
                case 7 -> shipmentsWorkflowUpdate();
                case 8 -> shipmentsDelete();
                case 0 -> {
                    return;
                }
                default -> println("Invalid option.");
            }
        }
    }

    private void shipmentsListAll() {
        try {
            List<Shipment> shipments = shipmentService.getAllShipments();
            printShipments(shipments);
        } catch (RuntimeException e) {
            printError(e);
        }
    }

    private void shipmentsSearchById() {
        int id = readInt("Shipment ID", 1, Integer.MAX_VALUE);
        try {
            Optional<Shipment> shipment = shipmentService.findShipmentById(id);
            if (shipment.isPresent()) {
                printShipments(List.of(shipment.get()));
            } else {
                println("No shipment found with ID: " + id);
            }
        } catch (RuntimeException e) {
            printError(e);
        }
    }

    private void shipmentsSearchByDestination() {
        String destination = readNonEmpty("Destination contains");
        try {
            List<Shipment> shipments = shipmentService.searchByDestinationAsync(destination).join();
            printShipments(shipments);
        } catch (RuntimeException e) {
            printError(e);
        }
    }

    private void shipmentsFilterByStatus() {
        Shipment.ShipmentStatus status = readShipmentStatus("Select status");
        try {
            List<Shipment> shipments = shipmentService.getShipmentsByStatusAsync(status).join();
            printShipments(shipments);
        } catch (RuntimeException e) {
            printError(e);
        }
    }

    private void shipmentsAdd() {
        try {
            String destination = ValidationUtils.sanitizeInput(readNonEmpty("Destination"));
            LocalDate date = readDateOrDefault("Shipment date (YYYY-MM-DD, blank=today)", LocalDate.now());
            Shipment.ShipmentStatus status = readShipmentStatusOrDefault("Initial status (blank=Preparing)", Shipment.ShipmentStatus.PREPARING);

            Shipment shipment = new Shipment();
            shipment.setDestination(destination);
            shipment.setShipmentDate(date);
            shipment.setShipmentStatus(status);

            Shipment created = shipmentService.createShipment(shipment);
            println("Created: " + created);
        } catch (RuntimeException e) {
            printError(e);
        }
    }

    private void shipmentsUpdate() {
        int id = readInt("Shipment ID to update", 1, Integer.MAX_VALUE);
        try {
            Optional<Shipment> existingOpt = shipmentService.findShipmentById(id);
            if (existingOpt.isEmpty()) {
                println("No shipment found with ID: " + id);
                return;
            }

            Shipment existing = existingOpt.get();
            println("Current: " + existing);
            println("Press Enter to keep current value.");

            String newDestinationRaw = readLine("New destination [" + existing.getDestination() + "]");
            String newDateRaw = readLine("New date [" + existing.getShipmentDate() + "]");
            String newStatusRaw = readLine("New status [" + existing.getShipmentStatus().getDisplayName() + "]");

            if (!newDestinationRaw.isBlank()) {
                existing.setDestination(ValidationUtils.sanitizeInput(newDestinationRaw));
            }
            if (!newDateRaw.isBlank()) {
                existing.setShipmentDate(parseDate(newDateRaw));
            }
            if (!newStatusRaw.isBlank()) {
                existing.setShipmentStatus(Shipment.ShipmentStatus.fromString(newStatusRaw));
            }

            boolean updated = shipmentService.updateShipment(existing);
            println(updated ? "Updated successfully." : "No changes applied (not updated).");
        } catch (RuntimeException e) {
            printError(e);
        }
    }

    private void shipmentsWorkflowUpdate() {
        int id = readInt("Shipment ID", 1, Integer.MAX_VALUE);
        println("Workflow actions:");
        println("1) Ship (Preparing -> In Transit)");
        println("2) Out for Delivery (In Transit -> Out for Delivery)");
        println("3) Deliver (Out for Delivery -> Delivered)");
        println("4) Return (Mark Returned)");
        println("5) Cancel");
        println("0) Back");

        int choice = readInt("Select action", 0, 5);
        if (choice == 0) return;

        try {
            boolean success = switch (choice) {
                case 1 -> shipmentService.shipAsync(id).join();
                case 2 -> shipmentService.outForDeliveryAsync(id).join();
                case 3 -> shipmentService.deliverAsync(id).join();
                case 4 -> shipmentService.returnShipmentAsync(id).join();
                case 5 -> shipmentService.cancelShipmentAsync(id).join();
                default -> false;
            };
            println(success ? "Status updated." : "Status not updated (shipment not found or invalid transition).");
        } catch (RuntimeException e) {
            printError(e);
        }
    }

    private void shipmentsDelete() {
        int id = readInt("Shipment ID to delete", 1, Integer.MAX_VALUE);
        if (!confirm("Delete shipment ID " + id + "?")) {
            println("Cancelled.");
            return;
        }
        try {
            boolean deleted = shipmentService.deleteShipment(id);
            println(deleted ? "Deleted successfully." : "Shipment not deleted (not found).");
        } catch (RuntimeException e) {
            printError(e);
        }
    }

    private void printShipments(List<Shipment> shipments) {
        println("");
        if (shipments == null || shipments.isEmpty()) {
            println("No shipment records.");
            return;
        }

        println("Shipment Records (" + shipments.size() + ")");
        println("ID\tDate\t\tDestination\tStatus");
        for (Shipment s : shipments) {
            println(s.getShipmentId() + "\t" + s.getShipmentDate() + "\t" + s.getDestination() + "\t\t" + s.getShipmentStatusString());
        }
    }

    // ===============================
    // INPUT / HELPERS
    // ===============================

    private int readInt(String label, int minInclusive, int maxInclusive) {
        while (true) {
            String raw = readLine(label + " [" + minInclusive + "-" + maxInclusive + "]");
            try {
                int value = Integer.parseInt(raw.trim());
                if (value < minInclusive || value > maxInclusive) {
                    println("Please enter a number between " + minInclusive + " and " + maxInclusive + ".");
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                println("Please enter a valid number.");
            }
        }
    }

    private String readNonEmpty(String label) {
        while (true) {
            String value = readLine(label);
            if (!value.isBlank()) return value;
            println("Value cannot be empty.");
        }
    }

    private LocalDate readDateOrDefault(String label, LocalDate defaultValue) {
        while (true) {
            String raw = readLine(label);
            if (raw.isBlank()) return defaultValue;
            try {
                return parseDate(raw);
            } catch (RuntimeException e) {
                println("Invalid date. Use YYYY-MM-DD.");
            }
        }
    }

    private LocalDate parseDate(String raw) {
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format: " + raw + " (expected YYYY-MM-DD)", e);
        }
    }

    private boolean confirm(String question) {
        while (true) {
            String raw = readLine(question + " (y/n)").trim().toLowerCase();
            if (raw.equals("y") || raw.equals("yes")) return true;
            if (raw.equals("n") || raw.equals("no")) return false;
            println("Please answer y/n.");
        }
    }

    private Order.OrderStatus readOrderStatus(String label) {
        return readEnum(label, Order.OrderStatus.values());
    }

    private Order.OrderStatus readOrderStatusOrDefault(String label, Order.OrderStatus defaultValue) {
        return readEnumOrDefault(label, Order.OrderStatus.values(), defaultValue);
    }

    private Shipment.ShipmentStatus readShipmentStatus(String label) {
        return readEnum(label, Shipment.ShipmentStatus.values());
    }

    private Shipment.ShipmentStatus readShipmentStatusOrDefault(String label, Shipment.ShipmentStatus defaultValue) {
        return readEnumOrDefault(label, Shipment.ShipmentStatus.values(), defaultValue);
    }

    private <T extends Enum<T>> T readEnum(String label, T[] values) {
        while (true) {
            println(label + ":");
            for (int i = 0; i < values.length; i++) {
                println((i + 1) + ") " + values[i].name());
            }
            int choice = readInt("Select", 1, values.length);
            return values[choice - 1];
        }
    }

    private <T extends Enum<T>> T readEnumOrDefault(String label, T[] values, T defaultValue) {
        while (true) {
            println(label + ":");
            for (int i = 0; i < values.length; i++) {
                println((i + 1) + ") " + values[i].name());
            }
            String raw = readLine("Select (1-" + values.length + ", blank=default)");
            if (raw.isBlank()) return defaultValue;
            try {
                int choice = Integer.parseInt(raw.trim());
                if (choice < 1 || choice > values.length) {
                    println("Please enter a number between 1 and " + values.length + ".");
                    continue;
                }
                return values[choice - 1];
            } catch (NumberFormatException e) {
                println("Please enter a valid number.");
            }
        }
    }

    private void shutdown() {
        inventoryService.shutdown();
        orderService.shutdown();
        shipmentService.shutdown();
    }

    private void printError(Throwable e) {
        Throwable t = e;
        while (t instanceof CompletionException && t.getCause() != null) {
            t = t.getCause();
        }
        String message = (t.getMessage() != null && !t.getMessage().isBlank()) ? t.getMessage() : t.toString();
        println("Error: " + message);
    }

    private String readLine(String label) {
        print(label + ": ");
        return scanner.nextLine();
    }

    private void print(String s) {
        System.out.print(s);
    }

    private void println(String s) {
        System.out.println(s);
    }
}
