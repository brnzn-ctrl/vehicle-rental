package dao;

import util.DataAccessException;
import database.DatabaseConnection;
import java.math.BigDecimal;
import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Read-only queries for the Reports screen. Kept separate from AbstractDAO
 * since reports aren't single-table CRUD — they're joins + aggregates.
 */
public class ReportDAO {

    /** Total income per vehicle category, using an INNER JOIN + SUM (matches MP4 requirement). */
    public Map<String, BigDecimal> incomeByCategory() {
        String sql =
            "SELECT c.category_name, SUM(p.amount) AS total " +
            "FROM payments p " +
            "INNER JOIN rentals r ON p.rental_id = r.rental_id " +
            "INNER JOIN vehicles v ON r.vehicle_id = v.vehicle_id " +
            "INNER JOIN vehicle_categories c ON v.category_id = c.category_id " +
            "GROUP BY c.category_name " +
            "ORDER BY total DESC";

        Map<String, BigDecimal> result = new LinkedHashMap<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getString("category_name"), rs.getBigDecimal("total"));
            }
        } catch (SQLException e) {
            throw new DataAccessException("incomeByCategory", e);
        }
        return result;
    }

    /** Net income between two dates (inclusive): payments minus expenses and stock receipt costs. */
    public BigDecimal netIncomeBetween(java.time.LocalDate from, java.time.LocalDate to) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            BigDecimal revenue = sumBetween(conn,
                "SELECT COALESCE(SUM(amount), 0) FROM payments WHERE DATE(payment_date) BETWEEN ? AND ?", from, to);
            BigDecimal expenses = sumBetween(conn,
                "SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE expense_date BETWEEN ? AND ?", from, to);
            BigDecimal stockReceipts = sumBetween(conn,
                "SELECT COALESCE(SUM(quantity * unit_cost), 0) FROM inventory_stock WHERE DATE(date_received) BETWEEN ? AND ?", from, to);
            return revenue.subtract(expenses).subtract(stockReceipts);
        } catch (SQLException e) {
            throw new DataAccessException("netIncomeBetween", e);
        }
    }

    private BigDecimal sumBetween(Connection conn, String sql, java.time.LocalDate from, java.time.LocalDate to)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(from));
            ps.setDate(2, java.sql.Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        }
    }
}
