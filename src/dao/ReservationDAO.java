package dao;

import database.DatabaseConnection;
import model.Reservation;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReservationDAO extends AbstractDAO<Reservation> {

    private static final String BASE_SELECT =
        "SELECT r.reservation_id, r.customer_id, r.vehicle_id, v.vehicle_name, " +
        "       r.start_date, r.end_date, r.status, r.discount_id, d.event_name " +
        "FROM reservations r " +
        "INNER JOIN customers c ON r.customer_id = c.customer_id " +
        "INNER JOIN vehicles v ON r.vehicle_id = v.vehicle_id " +
        "LEFT JOIN discount_events d ON r.discount_id = d.discount_id";

    @Override protected String selectAllSql()  { return BASE_SELECT + " ORDER BY r.reservation_date DESC"; }
    @Override protected String[] searchColumns() { return new String[]{ "r.reservation_id", "c.first_name", "c.last_name", "c.first_name || ' ' || c.last_name", "v.vehicle_name", "r.status", "d.event_name", "r.start_date", "r.end_date" }; }
    @Override protected String selectByIdSql() { return BASE_SELECT + " WHERE r.reservation_id=?"; }
    @Override protected String deleteSql()     { return "DELETE FROM reservations WHERE reservation_id=?"; }

    @Override protected String insertSql() {
        return "INSERT INTO reservations(customer_id, vehicle_id, start_date, end_date, status, discount_id) " +
               "VALUES (?,?,?,?,?,?)";
    }
    @Override protected String updateSql() {
        return "UPDATE reservations SET customer_id=?, vehicle_id=?, start_date=?, end_date=?, status=?, discount_id=? " +
               "WHERE reservation_id=?";
    }

    @Override protected RowMapper<Reservation> mapper() {
        return rs -> {
            Reservation r = new Reservation();
            r.setId(rs.getInt("reservation_id"));
            r.setCustomerId(rs.getInt("customer_id"));
            r.setVehicleId(rs.getInt("vehicle_id"));
            r.setVehicleName(rs.getString("vehicle_name"));
            r.setStartDate(rs.getDate("start_date").toLocalDate());
            r.setEndDate(rs.getDate("end_date").toLocalDate());
            r.setStatus(rs.getString("status"));
            int discId = rs.getInt("discount_id");
            r.setDiscountId(rs.wasNull() ? null : discId);
            r.setDiscountEventName(rs.getString("event_name"));
            return r;
        };
    }

    @Override protected ParamBinder<Reservation> insertBinder() {
        return (ps, r) -> {
            ps.setInt(1, r.getCustomerId());
            ps.setInt(2, r.getVehicleId());
            ps.setDate(3, java.sql.Date.valueOf(r.getStartDate()));
            ps.setDate(4, java.sql.Date.valueOf(r.getEndDate()));
            ps.setString(5, r.getStatus());
            if (r.getDiscountId() != null) ps.setInt(6, r.getDiscountId()); else ps.setNull(6, Types.INTEGER);
        };
    }

    @Override protected ParamBinder<Reservation> updateBinder() {
        return (ps, r) -> {
            ps.setInt(1, r.getCustomerId());
            ps.setInt(2, r.getVehicleId());
            ps.setDate(3, java.sql.Date.valueOf(r.getStartDate()));
            ps.setDate(4, java.sql.Date.valueOf(r.getEndDate()));
            ps.setString(5, r.getStatus());
            if (r.getDiscountId() != null) ps.setInt(6, r.getDiscountId()); else ps.setNull(6, Types.INTEGER);
            ps.setInt(7, r.getId());
        };
    }

    /** SELECT ... WHERE ... ORDER BY with PreparedStatement parameters. */
    private List<Reservation> findWhere(String where, Object... params) {
        List<Reservation> list = new ArrayList<>();
        String sql = BASE_SELECT + " WHERE " + where + " ORDER BY r.reservation_date DESC";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapper().map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findWhere failed: " + e.getMessage(), e);
        }
        return list;
    }

    /** For CustomerFrame's "My Reservations" tab. */
    public List<Reservation> findByCustomer(int customerId) {
        return findWhere("r.customer_id = ?", customerId);
    }

    /** For the staff-side "Pending Customer Orders" screen. */
    public List<Reservation> findPending() {
        return findWhere("r.status = ?", "pending");
    }

    /** Approved reservations that have not been turned into a rental yet (used by the Rentals screen). */
    public List<Reservation> findApproved() {
        return findWhere("r.status = ?", "approved");
    }

    /** Customer portal search: only this customer's reservations, filtered by keyword (SQL LIKE). */
    public List<Reservation> searchByCustomer(int customerId, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return findByCustomer(customerId);
        return searchWhere("r.customer_id = ?", customerId, keyword);
    }

    /** Pending-orders screen search (SQL LIKE inside the pending list). */
    public List<Reservation> searchPending(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return findPending();
        return searchWhere("r.status = ?", "pending", keyword);
    }

    private List<Reservation> searchWhere(String where, Object param, String keyword) {
        String[] cols = searchColumns();
        String sql = BASE_SELECT + " WHERE " + where + " AND " + likeClause(cols) + " ORDER BY r.reservation_date DESC";
        List<Reservation> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, param);
            bindLike(ps, 2, cols.length, keyword);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapper().map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("search failed: " + e.getMessage(), e);
        }
        return list;
    }

    private boolean count(String where, Object... params) {
        String sql = "SELECT COUNT(*) FROM reservations r WHERE " + where;
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("overlap check failed: " + e.getMessage(), e);
        }
    }

    /** Duplicate check: does this customer already have a pending/approved booking of this vehicle in these dates? */
    public boolean customerHasOverlap(int customerId, int vehicleId, LocalDate start, LocalDate end) {
        return count("r.customer_id = ? AND r.vehicle_id = ? AND r.status IN ('pending','approved') " +
                     "AND r.start_date <= ? AND r.end_date >= ?",
                     customerId, vehicleId, java.sql.Date.valueOf(end), java.sql.Date.valueOf(start));
    }

    /** Is the vehicle already promised to somebody else (APPROVED booking) in these dates? */
    public boolean vehicleBookedBetween(int vehicleId, LocalDate start, LocalDate end, int excludeReservationId) {
        return count("r.vehicle_id = ? AND r.status = 'approved' AND r.reservation_id <> ? " +
                     "AND r.start_date <= ? AND r.end_date >= ?",
                     vehicleId, excludeReservationId, java.sql.Date.valueOf(end), java.sql.Date.valueOf(start));
    }
}
