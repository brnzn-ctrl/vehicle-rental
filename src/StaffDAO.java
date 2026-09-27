

public class StaffDAO extends AbstractDAO<Staff> {

    @Override protected String selectAllSql()  { return "SELECT * FROM staff ORDER BY last_name"; }
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
            s.id = rs.getInt("staff_id");
            s.username = rs.getString("username");
            s.passwordHash = rs.getString("password_hash");
            s.firstName = rs.getString("first_name");
            s.lastName = rs.getString("last_name");
            s.role = rs.getString("role");
            s.active = rs.getBoolean("is_active");
            return s;
        };
    }

    @Override protected ParamBinder<Staff> insertBinder() {
        return (ps, s) -> {
            ps.setString(1, s.username); ps.setString(2, s.passwordHash);
            ps.setString(3, s.firstName); ps.setString(4, s.lastName);
            ps.setString(5, s.role); ps.setBoolean(6, s.active);
        };
    }

    @Override protected ParamBinder<Staff> updateBinder() {
        return (ps, s) -> {
            ps.setString(1, s.username); ps.setString(2, s.passwordHash);
            ps.setString(3, s.firstName); ps.setString(4, s.lastName);
            ps.setString(5, s.role); ps.setBoolean(6, s.active);
            ps.setInt(7, s.id);
        };
    }

    /** Used by the login screen. Returns null if the username/password don't match. */
    public Staff login(String username, String plainPassword) {
        String hashed = PasswordUtil.hash(plainPassword);
        for (Staff s : findAll()) {
            if (s.username.equalsIgnoreCase(username) && s.passwordHash.equals(hashed) && s.active) return s;
        }
        return null;
    }

    /** Used by "Add Staff" to enforce a unique username before insert. */
    public boolean usernameExists(String username) {
        for (Staff s : findAll()) if (s.username.equalsIgnoreCase(username)) return true;
        return false;
    }
}
