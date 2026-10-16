package dao;

import database.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Extra READ-ONLY queries that feed the dashboard charts (income per month, rentals per status, latest rentals).
 * Nothing here inserts, updates or deletes; the existing DashboardDAO still supplies the summary numbers.
 * A failed chart query returns an empty result, so a chart stays empty instead of blocking the whole dashboard.
 */
public class DashboardInsightsDAO {

    /** Month number (1-12) -> total payments received in that month of the given year. */
    public Map<Integer, BigDecimal> incomeByMonth(int year) {
        return byMonth("SELECT MONTH(payment_date), SUM(amount) FROM payments WHERE YEAR(payment_date) = ? GROUP BY MONTH(payment_date)", year);
    }

    /** Month number (1-12) -> total expenses recorded in that month of the given year. */
    public Map<Integer, BigDecimal> expensesByMonth(int year) {
        return byMonth("SELECT MONTH(expense_date), SUM(amount) FROM expenses WHERE YEAR(expense_date) = ? GROUP BY MONTH(expense_date)", year);
    }

    private Map<Integer, BigDecimal> byMonth(String sql, int year) {
        Map<Integer, BigDecimal> m = new LinkedHashMap<>();
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, year);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) m.put(rs.getInt(1), rs.getBigDecimal(2));
            }
        } catch (SQLException e) {
            m.clear();
        }
        return m;
    }

    /** "ongoing" / "returned" / "cancelled" -> number of rentals. */
    public Map<String, Integer> rentalsByStatus() {
        Map<String, Integer> m = new LinkedHashMap<>();
        String sql = "SELECT status, COUNT(*) FROM rentals GROUP BY status";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) m.put(rs.getString(1), rs.getInt(2));
        } catch (SQLException e) {
            m.clear();
        }
        return m;
    }

    /** Newest rentals first: {rentalId, customer, vehicle, rentedOut, due, ratePerDay, status}. */
    public List<Object[]> recentRentals(int limit) {
        List<Object[]> rows = new ArrayList<>();
        String sql = "SELECT r.rental_id, c.first_name || ' ' || c.last_name, v.vehicle_name, r.rent_out_date, r.due_date, "
                   + "       r.daily_rate_snapshot, r.status "
                   + "FROM rentals r "
                   + "INNER JOIN customers c ON r.customer_id = c.customer_id "
                   + "INNER JOIN vehicles v ON r.vehicle_id = v.vehicle_id "
                   + "ORDER BY r.rent_out_date DESC, r.rental_id DESC";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setMaxRows(limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Object[] { rs.getInt(1), rs.getString(2), rs.getString(3), rs.getTimestamp(4), rs.getDate(5), rs.getBigDecimal(6), rs.getString(7) });
                }
            }
        } catch (SQLException e) {
            rows.clear();
        }
        return rows;
    }
}
