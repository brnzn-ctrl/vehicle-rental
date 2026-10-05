package dao;

import database.DatabaseConnection;
import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

/** Summary numbers for the dashboard (COUNT / SUM with WHERE, all PreparedStatements). */
public class DashboardDAO {

    public Map<String, String> summary() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("Total Vehicles", one("SELECT COUNT(*) FROM vehicles"));
        m.put("Available Vehicles", one("SELECT COUNT(*) FROM vehicles WHERE status = ?", "available"));
        m.put("Rented Vehicles", one("SELECT COUNT(*) FROM vehicles WHERE status = ?", "rented"));
        m.put("Customers", one("SELECT COUNT(*) FROM customers WHERE is_active = TRUE"));
        m.put("System Users (Staff)", one("SELECT COUNT(*) FROM staff WHERE is_active = TRUE"));
        m.put("Ongoing Rentals", one("SELECT COUNT(*) FROM rentals WHERE status = ?", "ongoing"));
        m.put("Pending Reservations", one("SELECT COUNT(*) FROM reservations WHERE status = ?", "pending"));
        m.put("Today's Rentals", one("SELECT COUNT(*) FROM rentals WHERE DATE(rent_out_date) = CURRENT_DATE"));
        m.put("Today's Payments (PHP)", one("SELECT COALESCE(SUM(amount),0) FROM payments WHERE DATE(payment_date) = CURRENT_DATE"));
        m.put("Total Income (PHP)", String.valueOf(new PaymentDAO().totalIncome()));
        m.put("Total Expenses (PHP)", one("SELECT COALESCE(SUM(amount),0) FROM expenses"));
        m.put("Low-Stock Supplies", one(
            "SELECT COUNT(*) FROM supplies s WHERE " +
            "COALESCE((SELECT SUM(i.quantity) FROM inventory_stock i WHERE i.supply_id = s.supply_id),0) <= s.reorder_level"));
        return m;
    }

    private String one(String sql, Object... params) {
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? String.valueOf(rs.getObject(1)) : "0";
            }
        } catch (SQLException e) {
            throw new RuntimeException("dashboard query failed: " + e.getMessage(), e);
        }
    }
}
