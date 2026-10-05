package dao;

import database.DatabaseConnection;
import model.Payment;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** CRUD for the "payments" table (JOINs rentals + vehicles so the vehicle name can be shown). */
public class PaymentDAO extends AbstractDAO<Payment> {

    private static final String BASE_SELECT =
        "SELECT p.payment_id, p.rental_id, c.first_name || ' ' || c.last_name AS customer_name, v.vehicle_name, " +
        "       p.amount, p.payment_method, p.payment_date, p.processed_by, " +
        "       s.first_name || ' ' || s.last_name AS processed_name " +
        "FROM payments p " +
        "INNER JOIN rentals r ON p.rental_id = r.rental_id " +
        "INNER JOIN customers c ON r.customer_id = c.customer_id " +
        "INNER JOIN vehicles v ON r.vehicle_id = v.vehicle_id " +
        "INNER JOIN staff s ON p.processed_by = s.staff_id";

    @Override protected String selectAllSql()  { return BASE_SELECT + " ORDER BY p.payment_date DESC"; }
    @Override protected String selectByIdSql() { return BASE_SELECT + " WHERE p.payment_id=?"; }
    @Override protected String[] searchColumns() {
        return new String[]{ "p.payment_id", "p.rental_id", "c.first_name || ' ' || c.last_name", "v.vehicle_name",
                             "p.payment_method", "p.amount", "p.payment_date", "s.first_name || ' ' || s.last_name" };
    }

    @Override protected String insertSql() {
        return "INSERT INTO payments(rental_id, amount, payment_method, processed_by) VALUES (?,?,?,?)";
    }
    @Override protected String updateSql() {
        return "UPDATE payments SET rental_id=?, amount=?, payment_method=?, processed_by=? WHERE payment_id=?";
    }
    @Override protected String deleteSql() { return "DELETE FROM payments WHERE payment_id=?"; }

    @Override protected RowMapper<Payment> mapper() {
        return rs -> {
            Payment p = new Payment();
            p.setId(rs.getInt("payment_id"));
            p.setRentalId(rs.getInt("rental_id"));
            p.setCustomerName(rs.getString("customer_name"));
            p.setVehicleName(rs.getString("vehicle_name"));
            p.setProcessedByName(rs.getString("processed_name"));
            p.setAmount(rs.getBigDecimal("amount"));
            p.setPaymentMethod(rs.getString("payment_method"));
            Timestamp when = rs.getTimestamp("payment_date");
            p.setPaymentDate(when == null ? null : when.toLocalDateTime());
            p.setProcessedBy(rs.getInt("processed_by"));
            return p;
        };
    }

    @Override protected ParamBinder<Payment> insertBinder() {
        return (ps, p) -> {
            ps.setInt(1, p.getRentalId());
            ps.setBigDecimal(2, p.getAmount());
            ps.setString(3, p.getPaymentMethod());
            ps.setInt(4, p.getProcessedBy());
        };
    }

    @Override protected ParamBinder<Payment> updateBinder() {
        return (ps, p) -> {
            ps.setInt(1, p.getRentalId());
            ps.setBigDecimal(2, p.getAmount());
            ps.setString(3, p.getPaymentMethod());
            ps.setInt(4, p.getProcessedBy());
            ps.setInt(5, p.getId());
        };
    }

    /** All payments of one rental. */
    public List<Payment> findByRental(int rentalId) {
        List<Payment> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(BASE_SELECT + " WHERE p.rental_id=? ORDER BY p.payment_date DESC")) {
            ps.setInt(1, rentalId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapper().map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByRental failed: " + e.getMessage(), e);
        }
        return list;
    }

    /** Duplicate check: a returned rental is paid once (Return & Pay already creates that payment). */
    public boolean existsForRental(int rentalId) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM payments WHERE rental_id = ?")) {
            ps.setInt(1, rentalId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("existsForRental failed: " + e.getMessage(), e);
        }
    }

    /** Total money received so far (used by the dashboard). */
    public BigDecimal totalIncome() {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COALESCE(SUM(amount),0) FROM payments");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
        } catch (SQLException e) {
            throw new RuntimeException("totalIncome failed: " + e.getMessage(), e);
        }
    }
}
