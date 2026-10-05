package dao;


import model.Supplier;


public class SupplierDAO extends AbstractDAO<Supplier> {

    @Override protected String selectAllSql()  { return "SELECT * FROM suppliers ORDER BY supplier_name"; }
    @Override protected String[] searchColumns() { return new String[]{ "supplier_id", "supplier_name", "contact_number", "address" }; }
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
            s.setId(rs.getInt("supplier_id"));
            s.setName(rs.getString("supplier_name"));
            s.setContactNumber(rs.getString("contact_number"));
            s.setAddress(rs.getString("address"));
            return s;
        };
    }

    @Override protected ParamBinder<Supplier> insertBinder() {
        return (ps, s) -> {
            ps.setString(1, s.getName());
            ps.setString(2, s.getContactNumber());
            ps.setString(3, s.getAddress());
        };
    }

    @Override protected ParamBinder<Supplier> updateBinder() {
        return (ps, s) -> {
            ps.setString(1, s.getName());
            ps.setString(2, s.getContactNumber());
            ps.setString(3, s.getAddress());
            ps.setInt(4, s.getId());
        };
    }
}
