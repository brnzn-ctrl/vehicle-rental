import java.sql.Types;

public class SupplyDAO extends AbstractDAO<Supply> {

    private static final String BASE_SELECT =
        "SELECT s.supply_id, s.supply_name, s.unit, s.supplier_id, sup.supplier_name, s.reorder_level " +
        "FROM supplies s LEFT JOIN suppliers sup ON s.supplier_id = sup.supplier_id";

    @Override protected String selectAllSql()  { return BASE_SELECT + " ORDER BY s.supply_name"; }
    @Override protected String selectByIdSql() { return BASE_SELECT + " WHERE s.supply_id=?"; }
    @Override protected String deleteSql()     { return "DELETE FROM supplies WHERE supply_id=?"; }

    @Override protected String insertSql() {
        return "INSERT INTO supplies(supply_name, unit, supplier_id, reorder_level) VALUES (?,?,?,?)";
    }
    @Override protected String updateSql() {
        return "UPDATE supplies SET supply_name=?, unit=?, supplier_id=?, reorder_level=? WHERE supply_id=?";
    }

    @Override protected RowMapper<Supply> mapper() {
        return rs -> {
            Supply s = new Supply();
            s.id = rs.getInt("supply_id");
            s.name = rs.getString("supply_name");
            s.unit = rs.getString("unit");
            int supId = rs.getInt("supplier_id");
            s.supplierId = rs.wasNull() ? null : supId;
            s.supplierName = rs.getString("supplier_name");
            s.reorderLevel = rs.getInt("reorder_level");
            return s;
        };
    }

    @Override protected ParamBinder<Supply> insertBinder() {
        return (ps, s) -> {
            ps.setString(1, s.name);
            ps.setString(2, s.unit);
            if (s.supplierId != null) ps.setInt(3, s.supplierId); else ps.setNull(3, Types.INTEGER);
            ps.setInt(4, s.reorderLevel);
        };
    }

    @Override protected ParamBinder<Supply> updateBinder() {
        return (ps, s) -> {
            ps.setString(1, s.name);
            ps.setString(2, s.unit);
            if (s.supplierId != null) ps.setInt(3, s.supplierId); else ps.setNull(3, Types.INTEGER);
            ps.setInt(4, s.reorderLevel);
            ps.setInt(5, s.id);
        };
    }

    /** Current total quantity on hand for a supply (sum of all stock receipts). */
    public int currentQuantity(int supplyId) {
        try (java.sql.Connection c = DB.get();
             java.sql.PreparedStatement ps = c.prepareStatement(
                 "SELECT COALESCE(SUM(quantity),0) AS qty FROM inventory_stock WHERE supply_id=?")) {
            ps.setInt(1, supplyId);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("qty") : 0;
            }
        } catch (java.sql.SQLException e) {
            throw new RuntimeException("currentQuantity failed: " + e.getMessage(), e);
        }
    }
}
