public class CustomerDAO extends AbstractDAO<Customer> {

    @Override protected String selectAllSql()  { return "SELECT * FROM customers ORDER BY last_name"; }
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
            c.id = rs.getInt("customer_id");
            c.username = rs.getString("username");
            c.passwordHash = rs.getString("password_hash");
            c.firstName = rs.getString("first_name");
            c.lastName = rs.getString("last_name");
            c.email = rs.getString("email");
            c.phone = rs.getString("phone");
            c.address = rs.getString("address");
            c.licenseNumber = rs.getString("license_number");
            c.active = rs.getBoolean("is_active");
            return c;
        };
    }

    @Override protected ParamBinder<Customer> insertBinder() {
        return (ps, c) -> {
            ps.setString(1, c.username); ps.setString(2, c.passwordHash);
            ps.setString(3, c.firstName); ps.setString(4, c.lastName);
            ps.setString(5, c.email); ps.setString(6, c.phone);
            ps.setString(7, c.address); ps.setString(8, c.licenseNumber);
            ps.setBoolean(9, c.active);
        };
    }

    @Override protected ParamBinder<Customer> updateBinder() {
        return (ps, c) -> {
            ps.setString(1, c.username); ps.setString(2, c.passwordHash);
            ps.setString(3, c.firstName); ps.setString(4, c.lastName);
            ps.setString(5, c.email); ps.setString(6, c.phone);
            ps.setString(7, c.address); ps.setString(8, c.licenseNumber);
            ps.setBoolean(9, c.active);
            ps.setInt(10, c.id);
        };
    }

    /** Used by the customer login screen. Returns null if the credentials don't match. */
    public Customer login(String username, String plainPassword) {
        String hashed = PasswordUtil.hash(plainPassword);
        for (Customer c : findAll()) {
            if (c.username.equalsIgnoreCase(username) && c.passwordHash.equals(hashed) && c.active) return c;
        }
        return null;
    }

    /** Systematic duplicate check — no two people can register the same username. */
    public boolean usernameExists(String username) {
        for (Customer c : findAll()) if (c.username.equalsIgnoreCase(username)) return true;
        return false;
    }
}
