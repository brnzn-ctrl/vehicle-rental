package dao;

import database.DatabaseConnection;
import java.sql.*;


public class UniqueValueChecker {

    /** Is this e-mail already used by another customer? (excludeCustomerId = 0 when adding a new one) */
    public boolean customerEmailExists(String email, int excludeCustomerId) {
        return exists("SELECT COUNT(*) FROM customers WHERE UPPER(email) = ? AND customer_id <> ?", email, excludeCustomerId);
    }

    /** Is this driver's license number already used by another customer? */
    public boolean customerLicenseExists(String license, int excludeCustomerId) {
        return exists("SELECT COUNT(*) FROM customers WHERE UPPER(license_number) = ? AND customer_id <> ?", license, excludeCustomerId);
    }

    /** Is this vehicle category name already used by another category? */
    public boolean categoryNameExists(String name, int excludeId) {
        return exists("SELECT COUNT(*) FROM vehicle_categories WHERE UPPER(category_name) = ? AND category_id <> ?", name, excludeId);
    }

    /** Is this supplier name already used by another supplier? */
    public boolean supplierNameExists(String name, int excludeId) {
        return exists("SELECT COUNT(*) FROM suppliers WHERE UPPER(supplier_name) = ? AND supplier_id <> ?", name, excludeId);
    }

    /** Is this supply name already used by another supply item? */
    public boolean supplyNameExists(String name, int excludeId) {
        return exists("SELECT COUNT(*) FROM supplies WHERE UPPER(supply_name) = ? AND supply_id <> ?", name, excludeId);
    }

    /** Is this discount event name already used by another event? */
    public boolean discountEventNameExists(String name, int excludeId) {
        return exists("SELECT COUNT(*) FROM discount_events WHERE UPPER(event_name) = ? AND discount_id <> ?", name, excludeId);
    }

    private boolean exists(String sql, String value, int excludeId) {
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, value.trim().toUpperCase());
            ps.setInt(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("duplicate check failed: " + e.getMessage(), e);
        }
    }
}
