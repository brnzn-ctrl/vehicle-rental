package dao;

import util.UITheme;
import database.DatabaseConnection;
import model.Rental;
import model.ReturnRecord;
import model.Payment;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;

public class RentalDAO extends AbstractDAO<Rental> {

    private final ReturnDAO returnDAO = new ReturnDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();

    private static final String BASE_SELECT =
        "SELECT r.rental_id, r.reservation_id, r.customer_id, c.first_name AS cfn, c.last_name AS cln, " +
        "       r.vehicle_id, v.vehicle_name, r.staff_id, r.rent_out_date, r.due_date, " +
        "       r.daily_rate_snapshot, r.discount_percent_snapshot, r.status " +
        "FROM rentals r " +
        "INNER JOIN customers c ON r.customer_id = c.customer_id " +
        "INNER JOIN vehicles v ON r.vehicle_id = v.vehicle_id";

    @Override protected String selectAllSql()  { return BASE_SELECT + " ORDER BY r.rent_out_date DESC"; }
    @Override protected String[] searchColumns() { return new String[]{ "r.rental_id", "c.first_name", "c.last_name", "c.first_name || ' ' || c.last_name", "v.vehicle_name", "r.status", "r.due_date", "r.rent_out_date", "r.daily_rate_snapshot" }; }
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
            r.setId(rs.getInt("rental_id"));
            int resId = rs.getInt("reservation_id");
            r.setReservationId(rs.wasNull() ? null : resId);
            r.setCustomerId(rs.getInt("customer_id"));
            r.setCustomerName(rs.getString("cfn") + " " + rs.getString("cln"));
            r.setVehicleId(rs.getInt("vehicle_id"));
            r.setVehicleName(rs.getString("vehicle_name"));
            r.setStaffId(rs.getInt("staff_id"));
            r.setRentOutDate(rs.getTimestamp("rent_out_date").toLocalDateTime());
            r.setDueDate(rs.getDate("due_date").toLocalDate());
            r.setDailyRateSnapshot(rs.getBigDecimal("daily_rate_snapshot"));
            r.setDiscountPercentSnapshot(rs.getBigDecimal("discount_percent_snapshot"));
            r.setStatus(rs.getString("status"));
            return r;
        };
    }

    @Override protected ParamBinder<Rental> insertBinder() {
        return (ps, r) -> {
            if (r.getReservationId() != null) ps.setInt(1, r.getReservationId()); else ps.setNull(1, Types.INTEGER);
            ps.setInt(2, r.getCustomerId());
            ps.setInt(3, r.getVehicleId());
            ps.setInt(4, r.getStaffId());
            ps.setDate(5, Date.valueOf(r.getDueDate()));
            ps.setBigDecimal(6, r.getDailyRateSnapshot());
            ps.setBigDecimal(7, r.getDiscountPercentSnapshot());
            ps.setString(8, r.getStatus());
        };
    }

    @Override protected ParamBinder<Rental> updateBinder() {
        return (ps, r) -> {
            if (r.getReservationId() != null) ps.setInt(1, r.getReservationId()); else ps.setNull(1, Types.INTEGER);
            ps.setInt(2, r.getCustomerId());
            ps.setInt(3, r.getVehicleId());
            ps.setInt(4, r.getStaffId());
            ps.setDate(5, Date.valueOf(r.getDueDate()));
            ps.setBigDecimal(6, r.getDailyRateSnapshot());
            ps.setBigDecimal(7, r.getDiscountPercentSnapshot());
            ps.setString(8, r.getStatus());
            ps.setInt(9, r.getId());
        };
    }

    /**
     * Checks a vehicle out to a customer. INSERT rental + UPDATE vehicle run as ONE transaction
     * (commit / rollback), so a failure can never leave a rental without a "rented" vehicle.
     */
    public Rental checkOut(Rental r) {
        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(insertSql(), Statement.RETURN_GENERATED_KEYS)) {
                    insertBinder().bind(ps, r);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) r.setId(keys.getInt(1));
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE vehicles SET status='rented' WHERE vehicle_id=? AND status='available'")) {
                    ps.setInt(1, r.getVehicleId());
                    if (ps.executeUpdate() == 0) {
                        throw new SQLException("That vehicle is no longer available. Refresh the list and pick another.", "VR001");
                    }
                }
                if (r.getReservationId() != null) {
                    // the approved reservation becomes a rental: mark it "converted" (same transaction)
                    try (PreparedStatement ps = c.prepareStatement(
                            "UPDATE reservations SET status='converted' WHERE reservation_id=? AND status='approved'")) {
                        ps.setInt(1, r.getReservationId());
                        if (ps.executeUpdate() == 0) {
                            throw new SQLException("That reservation is no longer approved. Refresh the list.", "VR003");
                        }
                    }
                }
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
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
        long daysRented = java.time.temporal.ChronoUnit.DAYS.between(r.getRentOutDate().toLocalDate(), actualReturnDate);
        if (daysRented < 1) daysRented = 1;
        long lateDays = Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(r.getDueDate(), actualReturnDate));
        // every money value is rounded to 2 decimals so it always fits the DECIMAL(10,2) columns (and the receipt matches the DB)
        BigDecimal lateFee = r.getDailyRateSnapshot().multiply(BigDecimal.valueOf(lateDays)).setScale(2, java.math.RoundingMode.HALF_UP);

        BigDecimal subtotal = r.getDailyRateSnapshot().multiply(BigDecimal.valueOf(daysRented)).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal discountAmt = subtotal.multiply(r.getDiscountPercentSnapshot())
                                         .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        damageFee = damageFee.setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal total = subtotal.subtract(discountAmt).add(lateFee).add(damageFee);

        // rental status + returns row + payments row + vehicle status = ONE transaction (all or nothing)
        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE rentals SET status='returned' WHERE rental_id=? AND status='ongoing'")) {
                    ps.setInt(1, r.getId());
                    if (ps.executeUpdate() == 0) {
                        throw new SQLException("This rental is already closed.", "VR002");
                    }
                }
                // returns + payments rows are written by their own DAOs, on THIS connection (same transaction)
                ReturnRecord ret = new ReturnRecord();
                ret.setRentalId(r.getId());
                ret.setConditionNotes(conditionNotes);
                ret.setLateDays((int) lateDays);
                ret.setLateFee(lateFee);
                ret.setDamageFee(damageFee);
                ret.setReceivedBy(receivedByStaffId);
                returnDAO.insert(c, ret);

                Payment pay = new Payment();
                pay.setRentalId(r.getId());
                pay.setAmount(total);
                pay.setPaymentMethod(paymentMethod);
                pay.setProcessedBy(receivedByStaffId);
                paymentDAO.insert(c, pay);

                try (PreparedStatement ps = c.prepareStatement("UPDATE vehicles SET status='available' WHERE vehicle_id=?")) {
                    ps.setInt(1, r.getVehicleId());
                    ps.executeUpdate();
                }
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("returnAndPay failed: " + e.getMessage(), e);
        }

        return "RESTRO RENTALS — OFFICIAL RECEIPT\n" +
               "----------------------------------------\n" +
               "Rental #" + r.getId() + "   Vehicle: " + r.getVehicleName() + "\n" +
               "Customer: " + r.getCustomerName() + "\n" +
               "Rented: " + r.getRentOutDate().toLocalDate() + "   Returned: " + actualReturnDate + "\n" +
               "Days rented: " + daysRented + " @ " + UITheme.peso(r.getDailyRateSnapshot()) + "/day\n" +
               "Subtotal: " + UITheme.peso(subtotal) + "\n" +
               "Discount (" + r.getDiscountPercentSnapshot() + "%): -" + UITheme.peso(discountAmt) + "\n" +
               "Late fee (" + lateDays + " day/s): " + UITheme.peso(lateFee) + "\n" +
               "Damage fee: " + UITheme.peso(damageFee) + "\n" +
               "----------------------------------------\n" +
               "TOTAL DUE: " + UITheme.peso(total) + "\n" +
               "Paid via: " + paymentMethod;
    }

    /** Returned rentals that do not have a payment yet (fills the "Rental" box of the Payments screen). */
    public java.util.List<Rental> findReturnedWithoutPayment() {
        String sql = BASE_SELECT + " WHERE r.status = ? AND NOT EXISTS " +
                     "(SELECT 1 FROM payments p WHERE p.rental_id = r.rental_id) ORDER BY r.rental_id DESC";
        java.util.List<Rental> list = new java.util.ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, "returned");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapper().map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findReturnedWithoutPayment failed: " + e.getMessage(), e);
        }
        return list;
    }

    /** UPDATE ... WHERE: change the due date of a rental that is still ongoing. */
    public void updateDueDate(int rentalId, LocalDate newDue) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE rentals SET due_date=? WHERE rental_id=? AND status='ongoing'")) {
            ps.setDate(1, Date.valueOf(newDue));
            ps.setInt(2, rentalId);
            if (ps.executeUpdate() == 0) {
                throw new RuntimeException("That rental is no longer ongoing. Refresh the list.");
            }
        } catch (SQLException e) {
            throw new RuntimeException("updateDueDate failed: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes a rental that was created by mistake (status still 'ongoing', so no return or payment exists yet).
     * DELETE rental + vehicle back to 'available' + linked reservation back to 'approved' run as ONE transaction.
     * Closed rentals are never deleted: they are the billing history.
     */
    public void deleteOngoing(int rentalId) {
        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                int vehicleId = 0;
                Integer reservationId = null;
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT vehicle_id, reservation_id FROM rentals WHERE rental_id=? AND status='ongoing'")) {
                    ps.setInt(1, rentalId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("That rental is no longer ongoing. Refresh the list.", "VR004");
                        }
                        vehicleId = rs.getInt(1);
                        int resId = rs.getInt(2);
                        if (!rs.wasNull()) reservationId = resId;
                    }
                }
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM rentals WHERE rental_id=?")) {
                    ps.setInt(1, rentalId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE vehicles SET status='available' WHERE vehicle_id=? AND status='rented'")) {
                    ps.setInt(1, vehicleId);
                    ps.executeUpdate();
                }
                if (reservationId != null) {
                    try (PreparedStatement ps = c.prepareStatement(
                            "UPDATE reservations SET status='approved' WHERE reservation_id=? AND status='converted'")) {
                        ps.setInt(1, reservationId);
                        ps.executeUpdate();
                    }
                }
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("deleteOngoing failed: " + e.getMessage(), e);
        }
    }
}
