package in.strikes;

import java.sql.*;
import java.util.Scanner;

public class OrderManagement {

    private static final String URL = "jdbc:mysql://localhost:3306/ecommerce_db";
    private static final String USER = "root";
    private static final String PASSWORD = "Saksham15@";

    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // (a) Insert Order Records using PreparedStatement
    public static void insertOrders(Scanner sc) {
        String sql = "INSERT INTO Orders (order_id, customer_name, product_name, quantity, unit_price, status) VALUES (?, ?, ?, ?, ?, ?)";

        System.out.print("How many orders do you want to insert (minimum 3)? ");
        int count = sc.nextInt();
        sc.nextLine(); // consume newline

        if (count < 3) {
            System.out.println("Minimum 3 records required. Setting count to 3.");
            count = 3;
        }

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (int i = 1; i <= count; i++) {
                System.out.println("\n--- Enter details for Order " + i + " ---");
                System.out.print("Order ID: ");
                int id = sc.nextInt();
                sc.nextLine();

                System.out.print("Customer Name: ");
                String customerName = sc.nextLine();

                System.out.print("Product Name: ");
                String productName = sc.nextLine();

                System.out.print("Quantity: ");
                int quantity = sc.nextInt();

                System.out.print("Unit Price: ");
                double unitPrice = sc.nextDouble();
                sc.nextLine();

                System.out.print("Status (Pending/Shipped/Delivered/Cancelled): ");
                String status = sc.nextLine().trim();

                pstmt.setInt(1, id);
                pstmt.setString(2, customerName);
                pstmt.setString(3, productName);
                pstmt.setInt(4, quantity);
                pstmt.setDouble(5, unitPrice);
                pstmt.setString(6, status);

                try {
                    int rows = pstmt.executeUpdate();
                    if (rows > 0) {
                        System.out.println("Order ID " + id + " inserted successfully.");
                    }
                } catch (SQLIntegrityConstraintViolationException e) {
                    System.out.println("Error: Order ID " + id + " already exists! Skipping duplicate record.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error during insertion: " + e.getMessage());
        }
    }

    // (b) Conditional Retrieval and Sorting
    public static void getOrdersByStatus(Scanner sc) {
        System.out.print("Enter order status to search: ");
        String status = sc.nextLine().trim();

        String sql = "SELECT order_id, customer_name, product_name, quantity, unit_price, status " +
                "FROM Orders WHERE status = ? AND quantity > 2 ORDER BY unit_price DESC";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, status);
            try (ResultSet rs = pstmt.executeQuery()) {
                boolean found = false;
                System.out.println("\n--- Orders with Status '" + status + "' and Quantity > 2 (Sorted by Unit Price DESC) ---");
                while (rs.next()) {
                    found = true;
                    printOrder(rs);
                }
                if (!found) {
                    System.out.println("No matching orders found with this status and quantity > 2.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error during retrieval: " + e.getMessage());
        }
    }

    // (c) Increase Order Quantity
    public static void increaseOrderQuantity(Scanner sc) {
        System.out.print("Enter Order ID to increase quantity: ");
        int id = sc.nextInt();
        System.out.print("Enter additional quantity to add: ");
        int addedQty = sc.nextInt();
        sc.nextLine();

        String sql = "UPDATE Orders SET quantity = quantity + ? WHERE order_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, addedQty);
            pstmt.setInt(2, id);

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Quantity updated successfully for Order ID " + id + ".");
            } else {
                System.out.println("Order with ID " + id + " does not exist.");
            }
        } catch (SQLException e) {
            System.out.println("Database error during quantity update: " + e.getMessage());
        }
    }

    // (d) Search and Modify Order Status
    public static void searchAndModifyStatus(Scanner sc) {
        System.out.print("Enter Order ID: ");
        int id = sc.nextInt();
        sc.nextLine();
        System.out.print("Enter new status (Pending, Shipped, Delivered): ");
        String newStatus = sc.nextLine().trim();

        if (!newStatus.equalsIgnoreCase("Pending") &&
                !newStatus.equalsIgnoreCase("Shipped") &&
                !newStatus.equalsIgnoreCase("Delivered")) {
            System.out.println("Invalid status. New status must be Pending, Shipped, or Delivered.");
            return;
        }

        // Normalize capitalization (e.g., "shipped" -> "Shipped")
        String formattedStatus = newStatus.substring(0, 1).toUpperCase() + newStatus.substring(1).toLowerCase();

        String selectSql = "SELECT * FROM Orders WHERE order_id = ?";
        String updateSql = "UPDATE Orders SET status = ? WHERE order_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement selectStmt = conn.prepareStatement(selectSql);
             PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {

            selectStmt.setInt(1, id);
            try (ResultSet rs = selectStmt.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("Order with ID " + id + " does not exist.");
                    return;
                }
                System.out.println("\nCurrent Order Details:");
                printOrder(rs);
            }

            updateStmt.setString(1, formattedStatus);
            updateStmt.setInt(2, id);
            updateStmt.executeUpdate();

            System.out.println("\nStatus updated! Updated Order Details:");
            try (ResultSet updatedRs = selectStmt.executeQuery()) {
                if (updatedRs.next()) {
                    printOrder(updatedRs);
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error during status modification: " + e.getMessage());
        }
    }

    // (e) Aggregate Query and Conditional Deletion
    public static void calculateValueAndDeleteCancelled(Scanner sc) {
        String sumSql = "SELECT SUM(quantity * unit_price) AS total_value FROM Orders";
        String checkStatusSql = "SELECT status FROM Orders WHERE order_id = ?";
        String deleteSql = "DELETE FROM Orders WHERE order_id = ? AND status = 'Cancelled'";

        try (Connection conn = getConnection();
             PreparedStatement sumStmt = conn.prepareStatement(sumSql);
             ResultSet sumRs = sumStmt.executeQuery()) {

            if (sumRs.next()) {
                double totalValue = sumRs.getDouble("total_value");
                System.out.printf("Total Order Value: %.2f%n", totalValue);
            }

            System.out.print("\nEnter Order ID to delete (only deletes if status is Cancelled): ");
            int id = sc.nextInt();
            sc.nextLine();

            try (PreparedStatement checkStmt = conn.prepareStatement(checkStatusSql)) {
                checkStmt.setInt(1, id);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (!rs.next()) {
                        System.out.println("Cannot delete: Order ID " + id + " does not exist.");
                        return;
                    }
                    String status = rs.getString("status");
                    if (!status.equalsIgnoreCase("Cancelled")) {
                        System.out.println("Cannot delete: Order ID " + id + " has status '" + status + "'. Only 'Cancelled' orders can be deleted.");
                        return;
                    }
                }
            }

            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                deleteStmt.setInt(1, id);
                int deleted = deleteStmt.executeUpdate();
                if (deleted > 0) {
                    System.out.println("Order ID " + id + " deleted successfully.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error during aggregate/delete operation: " + e.getMessage());
        }
    }

    private static void printOrder(ResultSet rs) throws SQLException {
        System.out.printf("ID: %d | Customer: %s | Product: %s | Quantity: %d | Unit Price: %.2f | Status: %s%n",
                rs.getInt("order_id"),
                rs.getString("customer_name"),
                rs.getString("product_name"),
                rs.getInt("quantity"),
                rs.getDouble("unit_price"),
                rs.getString("status"));
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int choice;

        do {
            System.out.println("\n========= E-COMMERCE ORDER MANAGEMENT SYSTEM =========");
            System.out.println("1. Insert Order Records");
            System.out.println("2. Search Orders by Status (Quantity > 2, Unit Price DESC)");
            System.out.println("3. Increase Order Quantity");
            System.out.println("4. Search and Modify Order Status");
            System.out.println("5. Display Total Order Value & Delete Cancelled Order");
            System.out.println("6. Exit");
            System.out.print("Enter your choice: ");

            choice = sc.nextInt();
            sc.nextLine(); // consume newline

            switch (choice) {
                case 1 -> insertOrders(sc);
                case 2 -> getOrdersByStatus(sc);
                case 3 -> increaseOrderQuantity(sc);
                case 4 -> searchAndModifyStatus(sc);
                case 5 -> calculateValueAndDeleteCancelled(sc);
                case 6 -> System.out.println("Exiting system. Goodbye!");
                default -> System.out.println("Invalid choice. Please try again.");
            }
        } while (choice != 6);

        sc.close();
    }
}
