
public class SupplierDAO extends AbstractDAO<Supplier> {

    @Override protected String selectAllSql()  { return "SELECT * FROM suppliers ORDER BY supplier_name"; }
    @Override protected String selectByIdSql() { return "SELECT * FROM suppliers WHERE supplier_id=?"; }
    @Override protected String insertSql() {
        return "INSERT INTO suppliers(supplier_name, contact_number, address) VALUES (?,?,?)";
    }
    @Override protected String updateSql() {
        return "UPDATE suppliers SET supplier_name=?, contact_number=?, address=? WHERE supplier_id=?";
    }
    @Override protected String deleteSql() { return "DELETE FROM suppliers WHERE supplier_id=?"; }

    @Override protected RowMapper<Supplier> mapper() {
        return rs -> {
            Supplier s = new Supplier();
            s.id = rs.getInt("supplier_id");
            s.name = rs.getString("supplier_name");
            s.contactNumber = rs.getString("contact_number");
            s.address = rs.getString("address");
            return s;
        };
    }

    @Override protected ParamBinder<Supplier> insertBinder() {
        return (ps, s) -> {
            ps.setString(1, s.name);
            ps.setString(2, s.contactNumber);
            ps.setString(3, s.address);
        };
    }

    @Override protected ParamBinder<Supplier> updateBinder() {
        return (ps, s) -> {
            ps.setString(1, s.name);
            ps.setString(2, s.contactNumber);
            ps.setString(3, s.address);
            ps.setInt(4, s.id);
        };
    }
}
