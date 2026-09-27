

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Generic CRUD skeleton (Template Method pattern). A concrete DAO only has to
 * supply 5 small things (table name, select query, row mapper, insert/update
 * SQL + binders) and gets findAll/findById/insert/update/delete for free.
 */
public abstract class AbstractDAO<T extends Entity> {

    protected abstract String selectAllSql();      // e.g. "SELECT * FROM vehicles"
    protected abstract String selectByIdSql();      // same + " WHERE vehicle_id=?"
    protected abstract String insertSql();          // "INSERT INTO vehicles(...) VALUES(...)"
    protected abstract String updateSql();          // "UPDATE vehicles SET ... WHERE vehicle_id=?"
    protected abstract String deleteSql();          // "DELETE FROM vehicles WHERE vehicle_id=?"
    protected abstract RowMapper<T> mapper();
    protected abstract ParamBinder<T> insertBinder();
    protected abstract ParamBinder<T> updateBinder(); // must also bind id LAST for the WHERE clause

    public List<T> findAll() {
        List<T> list = new ArrayList<>();
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(selectAllSql());
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapper().map(rs));
        } catch (SQLException e) {
            throw new RuntimeException("findAll failed: " + e.getMessage(), e);
        }
        return list;
    }

    public T findById(int id) {
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(selectByIdSql())) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapper().map(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("findById failed: " + e.getMessage(), e);
        }
    }

    /** Inserts and writes the generated id back into obj.id. */
    public void insert(T obj) {
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(insertSql(), Statement.RETURN_GENERATED_KEYS)) {
            insertBinder().bind(ps, obj);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) obj.id = keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("insert failed: " + e.getMessage(), e);
        }
    }

    public void update(T obj) {
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(updateSql())) {
            updateBinder().bind(ps, obj);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("update failed: " + e.getMessage(), e);
        }
    }

    public void delete(int id) {
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(deleteSql())) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("delete failed: " + e.getMessage(), e);
        }
    }
}

