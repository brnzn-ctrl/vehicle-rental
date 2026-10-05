package dao;

import model.DiscountEvent;
import java.math.BigDecimal;
import java.sql.*;

public class DiscountEventDAO extends AbstractDAO<DiscountEvent> {

    @Override protected String selectAllSql()  { return "SELECT * FROM discount_events ORDER BY start_date"; }
    @Override protected String[] searchColumns() { return new String[]{ "discount_id", "event_name", "start_date", "end_date", "discount_percent" }; }
    @Override protected String selectByIdSql() { return "SELECT * FROM discount_events WHERE discount_id=?"; }
    @Override protected String insertSql() {
        return "INSERT INTO discount_events(event_name, start_date, end_date, discount_percent, is_active) VALUES (?,?,?,?,?)";
    }
    @Override protected String updateSql() {
        return "UPDATE discount_events SET event_name=?, start_date=?, end_date=?, discount_percent=?, is_active=? WHERE discount_id=?";
    }
    @Override protected String deleteSql() { return "DELETE FROM discount_events WHERE discount_id=?"; }

    @Override protected RowMapper<DiscountEvent> mapper() {
        return rs -> {
            DiscountEvent d = new DiscountEvent();
            d.setId(rs.getInt("discount_id"));
            d.setEventName(rs.getString("event_name"));
            d.setStartDate(rs.getDate("start_date").toLocalDate());
            d.setEndDate(rs.getDate("end_date").toLocalDate());
            d.setDiscountPercent(rs.getBigDecimal("discount_percent"));
            d.setActive(rs.getBoolean("is_active"));
            return d;
        };
    }

    @Override protected ParamBinder<DiscountEvent> insertBinder() {
        return (ps, d) -> {
            ps.setString(1, d.getEventName());
            ps.setDate(2, java.sql.Date.valueOf(d.getStartDate()));
            ps.setDate(3, java.sql.Date.valueOf(d.getEndDate()));
            ps.setBigDecimal(4, d.getDiscountPercent());
            ps.setBoolean(5, d.isActive());
        };
    }

    @Override protected ParamBinder<DiscountEvent> updateBinder() {
        return (ps, d) -> {
            ps.setString(1, d.getEventName());
            ps.setDate(2, java.sql.Date.valueOf(d.getStartDate()));
            ps.setDate(3, java.sql.Date.valueOf(d.getEndDate()));
            ps.setBigDecimal(4, d.getDiscountPercent());
            ps.setBoolean(5, d.isActive());
            ps.setInt(6, d.getId());
        };
    }

    /** Returns the active holiday discount covering this date, or null if none applies (SQL WHERE, best discount first). */
    public DiscountEvent findActiveForDate(java.time.LocalDate date) {
        String sql = "SELECT * FROM discount_events WHERE is_active = TRUE AND start_date <= ? AND end_date >= ? " +
                     "ORDER BY discount_percent DESC";
        try (Connection c = database.DatabaseConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(date));
            ps.setDate(2, java.sql.Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapper().map(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("findActiveForDate failed: " + e.getMessage(), e);
        }
    }
}
