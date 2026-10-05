package util;

import database.DatabaseConnection;
import model.Staff;
import javax.swing.*;
import java.awt.Component;
import java.sql.*;

/** Role check used by every screen that deletes records ("Delete records when authorized"). */
public final class AccessControl {

    private AccessControl() { }

    /** True only when the logged-in staff member has role = 'admin' in the STAFF table. */
    public static boolean isAdmin(Staff staff) {
        if (staff == null) return false;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT role FROM staff WHERE staff_id = ?")) {
            ps.setInt(1, staff.getId());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && "admin".equalsIgnoreCase(rs.getString(1));
            }
        } catch (SQLException e) {
            return false;   // when in doubt, deny
        }
    }

    /** Shows "Access Denied" and returns false unless the user is an administrator. */
    public static boolean requireAdmin(Component parent, Staff staff, String action) {
        if (isAdmin(staff)) return true;
        JOptionPane.showMessageDialog(parent, "Only an administrator can " + action + ".",
                "Access Denied", JOptionPane.WARNING_MESSAGE);
        return false;
    }
}
