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
        try (Connection conn = DB.get();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getString("category_name"), rs.getBigDecimal("total"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("incomeByCategory failed: " + e.getMessage(), e);
        }
        return result;
    }

    /** Total income between two dates (inclusive) — for the MP5 date-range requirement. */
    public BigDecimal totalIncomeBetween(java.time.LocalDate from, java.time.LocalDate to) {
        String sql =
            "SELECT COALESCE(SUM(p.amount),0) AS total FROM payments p " +
            "WHERE DATE(p.payment_date) BETWEEN ? AND ?";
        try (Connection conn = DB.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(from));
            ps.setDate(2, java.sql.Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal("total") : BigDecimal.ZERO;
            }
        } catch (SQLException e) {
            throw new RuntimeException("totalIncomeBetween failed: " + e.getMessage(), e);
        }
    }
}
