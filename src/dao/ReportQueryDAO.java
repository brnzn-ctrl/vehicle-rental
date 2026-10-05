package dao;

import database.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Read-only report queries (JOIN / WHERE / ORDER BY / GROUP BY) returning plain table data. */
public class ReportQueryDAO {

    public static final String[] REPORT_NAMES = {
        "Customer List", "Vehicle List", "User (Staff) List", "Rental / Transaction History",
        "Inventory Status", "Daily Transactions", "Monthly Transactions"
    };

    public static class Result {
        public String[] columns;
        public List<Object[]> rows = new ArrayList<>();
    }

    /** @param date only used by "Daily Transactions" (already validated by the caller). */
    public Result run(String reportName, java.time.LocalDate date) {
        switch (reportName) {
            case "Customer List":
                return query("SELECT customer_id AS ID, username AS Username, first_name AS First_Name, last_name AS Last_Name, " +
                             "email AS Email, phone AS Phone, license_number AS License, is_active AS Active " +
                             "FROM customers ORDER BY last_name, first_name");
            case "Vehicle List":
                return query("SELECT v.vehicle_id AS ID, v.plate_number AS Plate, v.vehicle_name AS Vehicle, " +
                             "c.category_name AS Category, v.daily_rate AS Daily_Rate, v.status AS Status " +
                             "FROM vehicles v INNER JOIN vehicle_categories c ON v.category_id = c.category_id " +
                             "ORDER BY c.category_name, v.vehicle_name");
            case "User (Staff) List":
                return query("SELECT staff_id AS ID, username AS Username, first_name AS First_Name, last_name AS Last_Name, " +
                             "role AS Role, is_active AS Active FROM staff ORDER BY role, last_name");
            case "Rental / Transaction History":
                return query("SELECT r.rental_id AS Rental_ID, c.first_name || ' ' || c.last_name AS Customer, " +
                             "v.vehicle_name AS Vehicle, s.username AS Staff, r.rent_out_date AS Rented_On, " +
                             "r.due_date AS Due_Date, r.status AS Status " +
                             "FROM rentals r " +
                             "INNER JOIN customers c ON r.customer_id = c.customer_id " +
                             "INNER JOIN vehicles v ON r.vehicle_id = v.vehicle_id " +
                             "INNER JOIN staff s ON r.staff_id = s.staff_id " +
                             "ORDER BY r.rent_out_date DESC");
            case "Inventory Status":
                return query("SELECT s.supply_name AS Supply, s.unit AS Unit, sup.supplier_name AS Supplier, " +
                             "COALESCE(SUM(i.quantity),0) AS On_Hand, s.reorder_level AS Reorder_Level, " +
                             "CAST(CASE WHEN COALESCE(SUM(i.quantity),0) <= s.reorder_level THEN 'LOW' ELSE 'OK' END AS VARCHAR(5)) AS Stock_Status " +
                             "FROM supplies s " +
                             "LEFT JOIN suppliers sup ON s.supplier_id = sup.supplier_id " +
                             "LEFT JOIN inventory_stock i ON i.supply_id = s.supply_id " +
                             "GROUP BY s.supply_id, s.supply_name, s.unit, sup.supplier_name, s.reorder_level " +
                             "ORDER BY s.supply_name");
            case "Daily Transactions":
                return query("SELECT p.payment_id AS Payment_ID, p.rental_id AS Rental_ID, " +
                             "c.first_name || ' ' || c.last_name AS Customer, p.amount AS Amount, " +
                             "p.payment_method AS Method, p.payment_date AS Paid_On " +
                             "FROM payments p " +
                             "INNER JOIN rentals r ON p.rental_id = r.rental_id " +
                             "INNER JOIN customers c ON r.customer_id = c.customer_id " +
                             "WHERE DATE(p.payment_date) = ? ORDER BY p.payment_date", java.sql.Date.valueOf(date));
            case "Monthly Transactions":
                return query("SELECT yr AS Year_, mo AS Month_, COUNT(*) AS Payments, SUM(amount) AS Total " +
                             "FROM (SELECT YEAR(payment_date) AS yr, MONTH(payment_date) AS mo, amount FROM payments) t " +
                             "GROUP BY yr, mo ORDER BY yr DESC, mo DESC");
            default:
                throw new IllegalArgumentException("Unknown report: " + reportName);
        }
    }

    /** FIRST_NAME -> First Name */
    private static String pretty(String label) {
        StringBuilder sb = new StringBuilder();
        for (String w : label.toLowerCase().split("_")) {
            if (w.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return sb.toString();
    }

    private Result query(String sql, Object... params) {
        Result res = new Result();
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                int n = md.getColumnCount();
                res.columns = new String[n];
                for (int i = 0; i < n; i++) res.columns[i] = pretty(md.getColumnLabel(i + 1));
                while (rs.next()) {
                    Object[] row = new Object[n];
                    for (int i = 0; i < n; i++) row[i] = rs.getObject(i + 1);
                    res.rows.add(row);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("report failed: " + e.getMessage(), e);
        }
        return res;
    }
}
