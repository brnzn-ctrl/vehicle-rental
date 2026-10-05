package dao;

import database.DatabaseConnection;
import model.ReturnRecord;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** CRUD for the "returns" table (JOINs rentals + vehicles so the vehicle name can be shown). */
public class ReturnDAO extends AbstractDAO<ReturnRecord> {

    private static final String BASE_SELECT =
        "SELECT rt.return_id, rt.rental_id, c.first_name || ' ' || c.last_name AS customer_name, v.vehicle_name, " +
        "       rt.return_date, rt.condition_notes, rt.late_days, rt.late_fee, rt.damage_fee, rt.received_by, " +
        "       s.first_name || ' ' || s.last_name AS received_name " +
        "FROM returns rt " +
        "INNER JOIN rentals r ON rt.rental_id = r.rental_id " +
        "INNER JOIN customers c ON r.customer_id = c.customer_id " +
        "INNER JOIN vehicles v ON r.vehicle_id = v.vehicle_id " +
        "INNER JOIN staff s ON rt.received_by = s.staff_id";

    @Override protected String selectAllSql()  { return BASE_SELECT + " ORDER BY rt.return_date DESC"; }
    @Override protected String selectByIdSql() { return BASE_SELECT + " WHERE rt.return_id=?"; }
    @Override protected String[] searchColumns() {
        return new String[]{ "rt.return_id", "rt.rental_id", "c.first_name || ' ' || c.last_name", "v.vehicle_name",
                             "rt.return_date", "rt.condition_notes", "s.first_name || ' ' || s.last_name" };
    }

    @Override protected String insertSql() {
        return "INSERT INTO returns(rental_id, condition_notes, late_days, late_fee, damage_fee, received_by) VALUES (?,?,?,?,?,?)";
    }
    @Override protected String updateSql() {
        return "UPDATE returns SET rental_id=?, condition_notes=?, late_days=?, late_fee=?, damage_fee=?, received_by=? WHERE return_id=?";
    }
    @Override protected String deleteSql() { return "DELETE FROM returns WHERE return_id=?"; }

    @Override protected RowMapper<ReturnRecord> mapper() {
        return rs -> {
            ReturnRecord r = new ReturnRecord();
            r.setId(rs.getInt("return_id"));
            r.setRentalId(rs.getInt("rental_id"));
            r.setCustomerName(rs.getString("customer_name"));
            r.setVehicleName(rs.getString("vehicle_name"));
            r.setReceivedByName(rs.getString("received_name"));
            Timestamp when = rs.getTimestamp("return_date");
            r.setReturnDate(when == null ? null : when.toLocalDateTime());
            r.setConditionNotes(rs.getString("condition_notes"));
            r.setLateDays(rs.getInt("late_days"));
            r.setLateFee(rs.getBigDecimal("late_fee"));
            r.setDamageFee(rs.getBigDecimal("damage_fee"));
            r.setReceivedBy(rs.getInt("received_by"));
            return r;
        };
    }

    @Override protected ParamBinder<ReturnRecord> insertBinder() {
        return (ps, r) -> {
            ps.setInt(1, r.getRentalId());
            ps.setString(2, r.getConditionNotes());
            ps.setInt(3, r.getLateDays());
            ps.setBigDecimal(4, r.getLateFee());
            ps.setBigDecimal(5, r.getDamageFee());
            ps.setInt(6, r.getReceivedBy());
        };
    }

    @Override protected ParamBinder<ReturnRecord> updateBinder() {
        return (ps, r) -> {
            ps.setInt(1, r.getRentalId());
            ps.setString(2, r.getConditionNotes());
            ps.setInt(3, r.getLateDays());
            ps.setBigDecimal(4, r.getLateFee());
            ps.setBigDecimal(5, r.getDamageFee());
            ps.setInt(6, r.getReceivedBy());
            ps.setInt(7, r.getId());
        };
    }

    /** All return records of one rental (normally zero or one). */
    public List<ReturnRecord> findByRental(int rentalId) {
        List<ReturnRecord> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(BASE_SELECT + " WHERE rt.rental_id=? ORDER BY rt.return_date DESC")) {
            ps.setInt(1, rentalId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapper().map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByRental failed: " + e.getMessage(), e);
        }
        return list;
    }
}
