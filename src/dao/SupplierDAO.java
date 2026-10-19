package dao;


import model.Supplier;
import database.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


public class SupplierDAO extends AbstractDAO<Supplier> {

    @Override protected String selectAllSql()  { return "SELECT * FROM suppliers ORDER BY supplier_name"; }
    @Override protected String[] searchColumns() { return new String[]{ "supplier_id", "supplier_name", "contact_number", "address" }; }
    @Override protected String selectByIdSql() { return "SELECT * FROM suppliers WHERE supplier_id=?"; }
    @Override protected String insertSql() {
        return "INSERT INTO suppliers(supplier_name, contact_number, address, supplier_type) VALUES (?,?,?,?)";
    }
    @Override protected String updateSql() {
        return "UPDATE suppliers SET supplier_name=?, contact_number=?, address=?, supplier_type=? WHERE supplier_id=?";
    }
    @Override protected String deleteSql() { return "DELETE FROM suppliers WHERE supplier_id=?"; }

    @Override protected RowMapper<Supplier> mapper() {
        return rs -> {
            Supplier s = new Supplier();
            s.setId(rs.getInt("supplier_id"));
            s.setName(rs.getString("supplier_name"));
            s.setContactNumber(rs.getString("contact_number"));
            s.setAddress(rs.getString("address"));
            s.setType(rs.getString("supplier_type"));
            return s;
        };
    }

    @Override protected ParamBinder<Supplier> insertBinder() {
        return (ps, s) -> {
            ps.setString(1, s.getName());
            ps.setString(2, s.getContactNumber());
            ps.setString(3, s.getAddress());
            ps.setString(4, s.getType());
        };
    }

    @Override protected ParamBinder<Supplier> updateBinder() {
        return (ps, s) -> {
            ps.setString(1, s.getName());
            ps.setString(2, s.getContactNumber());
            ps.setString(3, s.getAddress());
            ps.setString(4, s.getType());
            ps.setInt(5, s.getId());
        };
    }

    public List<Supplier> searchByType(String type, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return findByType(type);
        String[] cols = searchColumns();
        String sql = "SELECT * FROM suppliers WHERE supplier_type=? AND " + likeClause(cols)
                + " ORDER BY supplier_name";
        List<Supplier> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, type);
            bindLike(ps, 2, cols.length, keyword);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapper().map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("searchByType failed: " + e.getMessage(), e);
        }
        return list;
    }

    public List<Supplier> findByType(String type) {
        List<Supplier> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT * FROM suppliers WHERE supplier_type=? ORDER BY supplier_name")) {
            ps.setString(1, type);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapper().map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByType failed: " + e.getMessage(), e);
        }
        return list;
    }
}
