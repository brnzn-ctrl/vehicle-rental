import java.math.BigDecimal;
import java.sql.*;

public class DiscountEventDAO extends AbstractDAO<DiscountEvent> {

    @Override protected String selectAllSql()  { return "SELECT * FROM discount_events ORDER BY start_date"; }
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
            d.id = rs.getInt("discount_id");
            d.eventName = rs.getString("event_name");
            d.startDate = rs.getDate("start_date").toLocalDate();
            d.endDate = rs.getDate("end_date").toLocalDate();
            d.discountPercent = rs.getBigDecimal("discount_percent");
            d.active = rs.getBoolean("is_active");
            return d;
        };
    }

    @Override protected ParamBinder<DiscountEvent> insertBinder() {
        return (ps, d) -> {
            ps.setString(1, d.eventName);
            ps.setDate(2, java.sql.Date.valueOf(d.startDate));
            ps.setDate(3, java.sql.Date.valueOf(d.endDate));
            ps.setBigDecimal(4, d.discountPercent);
            ps.setBoolean(5, d.active);
        };
    }

    @Override protected ParamBinder<DiscountEvent> updateBinder() {
        return (ps, d) -> {
            ps.setString(1, d.eventName);
            ps.setDate(2, java.sql.Date.valueOf(d.startDate));
            ps.setDate(3, java.sql.Date.valueOf(d.endDate));
            ps.setBigDecimal(4, d.discountPercent);
            ps.setBoolean(5, d.active);
            ps.setInt(6, d.id);
        };
    }

    /** Returns the active holiday discount covering this date, or null if none applies. */
    public DiscountEvent findActiveForDate(java.time.LocalDate date) {
        for (DiscountEvent d : findAll()) {
            if (d.active && !date.isBefore(d.startDate) && !date.isAfter(d.endDate)) return d;
        }
        return null;
    }
}
