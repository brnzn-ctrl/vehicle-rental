package dao;

import database.DatabaseConnection;
import model.Entity;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public abstract class AbstractDAO<T extends Entity> {

    protected abstract String selectAllSql();      // e.g. "SELECT * FROM vehicles"
    protected abstract String selectByIdSql();      // same + " WHERE vehicle_id=?"
    protected abstract String insertSql();          // "INSERT INTO vehicles(...) VALUES(...)"
    protected abstract String updateSql();          // "UPDATE vehicles SET ... WHERE vehicle_id=?"
    protected abstract String deleteSql();          // "DELETE FROM vehicles WHERE vehicle_id=?"
    protected abstract RowMapper<T> mapper();
    protected abstract ParamBinder<T> insertBinder();
    protected abstract ParamBinder<T> updateBinder(); // must also bind id LAST for the WHERE clause
    protected abstract String[] searchColumns();    // SQL columns the keyword search looks in, e.g. {"v.plate_number", ...}

    public List<T> findAll() {
        List<T> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(selectAllSql());
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapper().map(rs));
        } catch (SQLException e) {
            throw new RuntimeException("findAll failed: " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * SQL search: SELECT ... WHERE (col1 LIKE ? OR col2 LIKE ? ...) ORDER BY ... (PreparedStatement).
     * The keyword is matched anywhere inside any of searchColumns(), case-insensitive. Empty keyword = everything.
     */
    public List<T> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return findAll();
        String sql = selectAllSql();
        int at = sql.lastIndexOf(" ORDER BY ");
        String base = at < 0 ? sql : sql.substring(0, at);
        String order = at < 0 ? "" : sql.substring(at);
        String[] cols = searchColumns();
        List<T> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(base + " WHERE " + likeClause(cols) + order)) {
            bindLike(ps, 1, cols.length, keyword);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapper().map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("search failed: " + e.getMessage(), e);
        }
        return list;
    }

    /** "(LOWER(CAST(a AS CHAR(200))) LIKE ? ESCAPE '!' OR ...)": works for text, numbers and dates alike. */
    protected static String likeClause(String[] cols) {
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < cols.length; i++) {
            if (i > 0) sb.append(" OR ");
            sb.append("LOWER(CAST(").append(cols[i]).append(" AS CHAR(200))) LIKE ? ESCAPE '!'");
        }
        return sb.append(")").toString();
    }

    /** Binds the same "%keyword%" pattern to `count` placeholders starting at index `from`. */
    protected static void bindLike(PreparedStatement ps, int from, int count, String keyword) throws SQLException {
        String k = keyword.trim().toLowerCase().replace("!", "!!").replace("%", "!%").replace("_", "!_");
        for (int i = 0; i < count; i++) ps.setString(from + i, "%" + k + "%");
    }

    public T findById(int id) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(selectByIdSql())) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapper().map(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("findById failed: " + e.getMessage(), e);
        }
    }

    /** Inserts and writes the generated id back into the object (opens and closes its own connection). */
    public void insert(T obj) {
        try (Connection c = DatabaseConnection.getConnection()) {
            insert(c, obj);
        } catch (SQLException e) {
            throw new RuntimeException("insert failed: " + e.getMessage(), e);
        }
    }

    /**
     * Same INSERT, but on a connection the caller already opened. RentalDAO uses this so the rental, return and
     * payment rows are written inside ONE transaction (the caller commits or rolls back).
     */
    public void insert(Connection c, T obj) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(insertSql(), Statement.RETURN_GENERATED_KEYS)) {
            insertBinder().bind(ps, obj);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) obj.setId(keys.getInt(1));
            }
        }
    }

    public void update(T obj) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(updateSql())) {
            updateBinder().bind(ps, obj);
            if (ps.executeUpdate() == 0) throw missingRecord();
        } catch (SQLException e) {
            throw new RuntimeException("update failed: " + e.getMessage(), e);
        }
    }

    public void delete(int id) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(deleteSql())) {
            ps.setInt(1, id);
            if (ps.executeUpdate() == 0) throw missingRecord();
        } catch (SQLException e) {
            throw new RuntimeException("delete failed: " + e.getMessage(), e);
        }
    }

    /** "Missing record" error: the row was deleted by someone else after the list was loaded. */
    private static SQLException missingRecord() {
        return new SQLException("That record no longer exists. Refresh the list and try again.", "VR404");
    }
}
