import java.sql.*;

public class ReservationDAO extends AbstractDAO<Reservation> {

    private static final String BASE_SELECT =
        "SELECT r.reservation_id, r.customer_id, r.vehicle_id, v.vehicle_name, " +
        "       r.start_date, r.end_date, r.status, r.discount_id, d.event_name " +
        "FROM reservations r " +
        "INNER JOIN vehicles v ON r.vehicle_id = v.vehicle_id " +
        "LEFT JOIN discount_events d ON r.discount_id = d.discount_id";

    @Override protected String selectAllSql()  { return BASE_SELECT + " ORDER BY r.reservation_date DESC"; }
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
            r.id = rs.getInt("reservation_id");
            r.customerId = rs.getInt("customer_id");
            r.vehicleId = rs.getInt("vehicle_id");
            r.vehicleName = rs.getString("vehicle_name");
            r.startDate = rs.getDate("start_date").toLocalDate();
            r.endDate = rs.getDate("end_date").toLocalDate();
            r.status = rs.getString("status");
            int discId = rs.getInt("discount_id");
            r.discountId = rs.wasNull() ? null : discId;
            r.discountEventName = rs.getString("event_name");
            return r;
        };
    }

    @Override protected ParamBinder<Reservation> insertBinder() {
        return (ps, r) -> {
            ps.setInt(1, r.customerId);
            ps.setInt(2, r.vehicleId);
            ps.setDate(3, java.sql.Date.valueOf(r.startDate));
            ps.setDate(4, java.sql.Date.valueOf(r.endDate));
            ps.setString(5, r.status);
            if (r.discountId != null) ps.setInt(6, r.discountId); else ps.setNull(6, Types.INTEGER);
        };
    }

    @Override protected ParamBinder<Reservation> updateBinder() {
        return (ps, r) -> {
            ps.setInt(1, r.customerId);
            ps.setInt(2, r.vehicleId);
            ps.setDate(3, java.sql.Date.valueOf(r.startDate));
            ps.setDate(4, java.sql.Date.valueOf(r.endDate));
            ps.setString(5, r.status);
            if (r.discountId != null) ps.setInt(6, r.discountId); else ps.setNull(6, Types.INTEGER);
            ps.setInt(7, r.id);
        };
    }

    /** For CustomerFrame's "My Reservations" tab. */
    public java.util.List<Reservation> findByCustomer(int customerId) {
        return findAll().stream().filter(r -> r.customerId == customerId).toList();
    }

    /** For the staff-side "Pending Customer Orders" screen. */
    public java.util.List<Reservation> findPending() {
        return findAll().stream().filter(r -> "pending".equals(r.status)).toList();
    }
}
