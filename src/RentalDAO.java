import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;

public class RentalDAO extends AbstractDAO<Rental> {

    private static final String BASE_SELECT =
        "SELECT r.rental_id, r.reservation_id, r.customer_id, c.first_name AS cfn, c.last_name AS cln, " +
        "       r.vehicle_id, v.vehicle_name, r.staff_id, r.rent_out_date, r.due_date, " +
        "       r.daily_rate_snapshot, r.discount_percent_snapshot, r.status " +
        "FROM rentals r " +
        "INNER JOIN customers c ON r.customer_id = c.customer_id " +
        "INNER JOIN vehicles v ON r.vehicle_id = v.vehicle_id";

    @Override protected String selectAllSql()  { return BASE_SELECT + " ORDER BY r.rent_out_date DESC"; }
    @Override protected String selectByIdSql() { return BASE_SELECT + " WHERE r.rental_id=?"; }
    @Override protected String deleteSql()     { return "DELETE FROM rentals WHERE rental_id=?"; }

    @Override protected String insertSql() {
        return "INSERT INTO rentals(reservation_id, customer_id, vehicle_id, staff_id, due_date, " +
               "daily_rate_snapshot, discount_percent_snapshot, status) VALUES (?,?,?,?,?,?,?,?)";
    }
    @Override protected String updateSql() {
        return "UPDATE rentals SET reservation_id=?, customer_id=?, vehicle_id=?, staff_id=?, due_date=?, " +
               "daily_rate_snapshot=?, discount_percent_snapshot=?, status=? WHERE rental_id=?";
    }

    @Override protected RowMapper<Rental> mapper() {
        return rs -> {
            Rental r = new Rental();
            r.id = rs.getInt("rental_id");
            int resId = rs.getInt("reservation_id");
            r.reservationId = rs.wasNull() ? null : resId;
            r.customerId = rs.getInt("customer_id");
            r.customerName = rs.getString("cfn") + " " + rs.getString("cln");
            r.vehicleId = rs.getInt("vehicle_id");
            r.vehicleName = rs.getString("vehicle_name");
            r.staffId = rs.getInt("staff_id");
            r.rentOutDate = rs.getTimestamp("rent_out_date").toLocalDateTime();
            r.dueDate = rs.getDate("due_date").toLocalDate();
            r.dailyRateSnapshot = rs.getBigDecimal("daily_rate_snapshot");
            r.discountPercentSnapshot = rs.getBigDecimal("discount_percent_snapshot");
            r.status = rs.getString("status");
            return r;
        };
    }

    @Override protected ParamBinder<Rental> insertBinder() {
        return (ps, r) -> {
            if (r.reservationId != null) ps.setInt(1, r.reservationId); else ps.setNull(1, Types.INTEGER);
            ps.setInt(2, r.customerId);
            ps.setInt(3, r.vehicleId);
            ps.setInt(4, r.staffId);
            ps.setDate(5, Date.valueOf(r.dueDate));
            ps.setBigDecimal(6, r.dailyRateSnapshot);
            ps.setBigDecimal(7, r.discountPercentSnapshot);
            ps.setString(8, r.status);
        };
    }

    @Override protected ParamBinder<Rental> updateBinder() {
        return (ps, r) -> {
            if (r.reservationId != null) ps.setInt(1, r.reservationId); else ps.setNull(1, Types.INTEGER);
            ps.setInt(2, r.customerId);
            ps.setInt(3, r.vehicleId);
            ps.setInt(4, r.staffId);
            ps.setDate(5, Date.valueOf(r.dueDate));
            ps.setBigDecimal(6, r.dailyRateSnapshot);
            ps.setBigDecimal(7, r.discountPercentSnapshot);
            ps.setString(8, r.status);
            ps.setInt(9, r.id);
        };
    }

    /** Checks a vehicle out to a customer: inserts the rental and flips the vehicle to "rented". */
    public Rental checkOut(Rental r) {
        insert(r);
        try (Connection c = DB.get(); PreparedStatement ps =
                c.prepareStatement("UPDATE vehicles SET status='rented' WHERE vehicle_id=?")) {
            ps.setInt(1, r.vehicleId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("checkOut failed: " + e.getMessage(), e);
        }
        return r;
    }

    /**
     * Closes out a rental: computes the bill (days * rate - discount + late/damage fees),
     * writes a "returns" row, a "payments" row, flips status back to returned/available,
     * and returns a printable receipt.
     */
    public String returnAndPay(Rental r, LocalDate actualReturnDate, String conditionNotes,
                                BigDecimal damageFee, int receivedByStaffId, String paymentMethod) {
        long daysRented = java.time.temporal.ChronoUnit.DAYS.between(r.rentOutDate.toLocalDate(), actualReturnDate);
        if (daysRented < 1) daysRented = 1;
        long lateDays = Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(r.dueDate, actualReturnDate));
        BigDecimal lateFee = r.dailyRateSnapshot.multiply(BigDecimal.valueOf(lateDays));

        BigDecimal subtotal = r.dailyRateSnapshot.multiply(BigDecimal.valueOf(daysRented));
        BigDecimal discountAmt = subtotal.multiply(r.discountPercentSnapshot).divide(BigDecimal.valueOf(100));
        BigDecimal total = subtotal.subtract(discountAmt).add(lateFee).add(damageFee);

        try (Connection c = DB.get()) {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO returns(rental_id, condition_notes, late_days, late_fee, damage_fee, received_by) " +
                    "VALUES (?,?,?,?,?,?)")) {
                ps.setInt(1, r.id);
                ps.setString(2, conditionNotes);
                ps.setInt(3, (int) lateDays);
                ps.setBigDecimal(4, lateFee);
                ps.setBigDecimal(5, damageFee);
                ps.setInt(6, receivedByStaffId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO payments(rental_id, amount, payment_method, processed_by) VALUES (?,?,?,?)")) {
                ps.setInt(1, r.id);
                ps.setBigDecimal(2, total);
                ps.setString(3, paymentMethod);
                ps.setInt(4, receivedByStaffId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement("UPDATE rentals SET status='returned' WHERE rental_id=?")) {
                ps.setInt(1, r.id);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement("UPDATE vehicles SET status='available' WHERE vehicle_id=?")) {
                ps.setInt(1, r.vehicleId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("returnAndPay failed: " + e.getMessage(), e);
        }

        return "OCEAN CREST RENTALS — OFFICIAL RECEIPT\n" +
               "----------------------------------------\n" +
               "Rental #" + r.id + "   Vehicle: " + r.vehicleName + "\n" +
               "Customer: " + r.customerName + "\n" +
               "Rented: " + r.rentOutDate.toLocalDate() + "   Returned: " + actualReturnDate + "\n" +
               "Days rented: " + daysRented + " @ " + UITheme.peso(r.dailyRateSnapshot) + "/day\n" +
               "Subtotal: " + UITheme.peso(subtotal) + "\n" +
               "Discount (" + r.discountPercentSnapshot + "%): -" + UITheme.peso(discountAmt) + "\n" +
               "Late fee (" + lateDays + " day/s): " + UITheme.peso(lateFee) + "\n" +
               "Damage fee: " + UITheme.peso(damageFee) + "\n" +
               "----------------------------------------\n" +
               "TOTAL DUE: " + UITheme.peso(total) + "\n" +
               "Paid via: " + paymentMethod;
    }
}
