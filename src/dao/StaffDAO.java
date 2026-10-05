package dao;


import util.PasswordHasher;
import database.DatabaseConnection;
import java.sql.*;
import model.Staff;



public class StaffDAO extends AbstractDAO<Staff> {

    @Override protected String selectAllSql()  { return "SELECT * FROM staff ORDER BY last_name"; }
    @Override protected String[] searchColumns() { return new String[]{ "staff_id", "username", "first_name", "last_name", "first_name || ' ' || last_name", "role" }; }
    @Override protected String selectByIdSql() { return "SELECT * FROM staff WHERE staff_id=?"; }
    @Override protected String insertSql() {
        return "INSERT INTO staff(username, password_hash, first_name, last_name, role, is_active) VALUES (?,?,?,?,?,?)";
    }
    @Override protected String updateSql() {
        return "UPDATE staff SET username=?, password_hash=?, first_name=?, last_name=?, role=?, is_active=? WHERE staff_id=?";
    }
    @Override protected String deleteSql() { return "DELETE FROM staff WHERE staff_id=?"; }

    @Override protected RowMapper<Staff> mapper() {
        return rs -> {
            Staff s = new Staff();
            s.setId(rs.getInt("staff_id"));
            s.setUsername(rs.getString("username"));
            s.setPasswordHash(rs.getString("password_hash"));
            s.setFirstName(rs.getString("first_name"));
            s.setLastName(rs.getString("last_name"));
            s.setRole(rs.getString("role"));
            s.setActive(rs.getBoolean("is_active"));
            return s;
        };
    }

    @Override protected ParamBinder<Staff> insertBinder() {
        return (ps, s) -> {
            ps.setString(1, s.getUsername()); ps.setString(2, s.getPasswordHash());
            ps.setString(3, s.getFirstName()); ps.setString(4, s.getLastName());
            ps.setString(5, s.getRole()); ps.setBoolean(6, s.isActive());
        };
    }

    @Override protected ParamBinder<Staff> updateBinder() {
        return (ps, s) -> {
            ps.setString(1, s.getUsername()); ps.setString(2, s.getPasswordHash());
            ps.setString(3, s.getFirstName()); ps.setString(4, s.getLastName());
            ps.setString(5, s.getRole()); ps.setBoolean(6, s.isActive());
            ps.setInt(7, s.getId());
        };
    }

    /**
     * Database authentication: SELECT ... WHERE username = ? AND is_active (PreparedStatement), then the password is
     * checked against the salted hash. An old unsalted hash is re-saved in the new salted format after a successful
     * login. Returns null if the username or password is wrong.
     */
    public Staff login(String username, String plainPassword) {
        String sql = "SELECT * FROM staff WHERE LOWER(username) = ? AND is_active = TRUE";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Staff found = mapper().map(rs);
                if (!PasswordHasher.matches(plainPassword, found.getPasswordHash())) return null;
                if (PasswordHasher.isSaltedEnabled() && PasswordHasher.isLegacy(found.getPasswordHash())) upgradeHash(c, found.getId(), plainPassword);
                return found;
            }
        } catch (SQLException e) {
            throw new RuntimeException("login failed: " + e.getMessage(), e);
        }
    }

    /** Re-saves a password in the salted format. A failure here must never block a valid login. */
    private void upgradeHash(Connection c, int id, String plainPassword) {
        try (PreparedStatement up = c.prepareStatement("UPDATE staff SET password_hash = ? WHERE staff_id = ?")) {
            up.setString(1, PasswordHasher.hash(plainPassword));
            up.setInt(2, id);
            up.executeUpdate();
        } catch (SQLException ignored) {
            // the old hash still works; the upgrade will be tried again at the next login
        }
    }

    /** Duplicate check done by the database (COUNT ... WHERE), not by loading every row. */
    public boolean usernameExists(String username) {
        String sql = "SELECT COUNT(*) FROM staff WHERE LOWER(username) = ?";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("usernameExists failed: " + e.getMessage(), e);
        }
    }
}
