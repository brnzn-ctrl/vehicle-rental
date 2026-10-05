package dao;


import util.PasswordHasher;
import database.DatabaseConnection;
import java.sql.*;
import model.Customer;

public class CustomerDAO extends AbstractDAO<Customer> {

    @Override protected String selectAllSql()  { return "SELECT * FROM customers ORDER BY last_name"; }
    @Override protected String[] searchColumns() { return new String[]{ "customer_id", "username", "first_name", "last_name", "first_name || ' ' || last_name", "email", "phone", "address", "license_number" }; }
    @Override protected String selectByIdSql() { return "SELECT * FROM customers WHERE customer_id=?"; }
    @Override protected String insertSql() {
        return "INSERT INTO customers(username, password_hash, first_name, last_name, email, phone, address, license_number, is_active) "
             + "VALUES (?,?,?,?,?,?,?,?,?)";
    }
    @Override protected String updateSql() {
        return "UPDATE customers SET username=?, password_hash=?, first_name=?, last_name=?, email=?, phone=?, address=?, license_number=?, is_active=? "
             + "WHERE customer_id=?";
    }
    @Override protected String deleteSql() { return "DELETE FROM customers WHERE customer_id=?"; }

    @Override protected RowMapper<Customer> mapper() {
        return rs -> {
            Customer c = new Customer();
            c.setId(rs.getInt("customer_id"));
            c.setUsername(rs.getString("username"));
            c.setPasswordHash(rs.getString("password_hash"));
            c.setFirstName(rs.getString("first_name"));
            c.setLastName(rs.getString("last_name"));
            c.setEmail(rs.getString("email"));
            c.setPhone(rs.getString("phone"));
            c.setAddress(rs.getString("address"));
            c.setLicenseNumber(rs.getString("license_number"));
            c.setActive(rs.getBoolean("is_active"));
            return c;
        };
    }

    @Override protected ParamBinder<Customer> insertBinder() {
        return (ps, c) -> {
            ps.setString(1, c.getUsername()); ps.setString(2, c.getPasswordHash());
            ps.setString(3, c.getFirstName()); ps.setString(4, c.getLastName());
            ps.setString(5, c.getEmail()); ps.setString(6, c.getPhone());
            ps.setString(7, c.getAddress()); ps.setString(8, c.getLicenseNumber());
            ps.setBoolean(9, c.isActive());
        };
    }

    @Override protected ParamBinder<Customer> updateBinder() {
        return (ps, c) -> {
            ps.setString(1, c.getUsername()); ps.setString(2, c.getPasswordHash());
            ps.setString(3, c.getFirstName()); ps.setString(4, c.getLastName());
            ps.setString(5, c.getEmail()); ps.setString(6, c.getPhone());
            ps.setString(7, c.getAddress()); ps.setString(8, c.getLicenseNumber());
            ps.setBoolean(9, c.isActive());
            ps.setInt(10, c.getId());
        };
    }

    /**
     * Database authentication: SELECT ... WHERE username = ? AND is_active (PreparedStatement), then the password is
     * checked against the salted hash. An old unsalted hash is re-saved in the new salted format after a successful
     * login. Returns null if the username or password is wrong.
     */
    public Customer login(String username, String plainPassword) {
        String sql = "SELECT * FROM customers WHERE LOWER(username) = ? AND is_active = TRUE";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Customer found = mapper().map(rs);
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
        try (PreparedStatement up = c.prepareStatement("UPDATE customers SET password_hash = ? WHERE customer_id = ?")) {
            up.setString(1, PasswordHasher.hash(plainPassword));
            up.setInt(2, id);
            up.executeUpdate();
        } catch (SQLException ignored) {
            // the old hash still works; the upgrade will be tried again at the next login
        }
    }

    /** Duplicate check done by the database (COUNT ... WHERE), not by loading every row. */
    public boolean usernameExists(String username) {
        String sql = "SELECT COUNT(*) FROM customers WHERE LOWER(username) = ?";
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
