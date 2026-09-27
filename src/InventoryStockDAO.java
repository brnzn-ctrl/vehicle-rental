public class InventoryStockDAO extends AbstractDAO<InventoryStock> {

    private static final String BASE_SELECT =
        "SELECT i.stock_id, i.supply_id, s.supply_name, s.unit, i.quantity, i.unit_cost, i.date_received " +
        "FROM inventory_stock i INNER JOIN supplies s ON i.supply_id = s.supply_id";

    @Override protected String selectAllSql()  { return BASE_SELECT + " ORDER BY i.date_received DESC"; }
    @Override protected String selectByIdSql() { return BASE_SELECT + " WHERE i.stock_id=?"; }
    @Override protected String deleteSql()     { return "DELETE FROM inventory_stock WHERE stock_id=?"; }

    @Override protected String insertSql() {
        return "INSERT INTO inventory_stock(supply_id, quantity, unit_cost) VALUES (?,?,?)";
    }
    @Override protected String updateSql() {
        return "UPDATE inventory_stock SET supply_id=?, quantity=?, unit_cost=? WHERE stock_id=?";
    }

    @Override protected RowMapper<InventoryStock> mapper() {
        return rs -> {
            InventoryStock i = new InventoryStock();
            i.id = rs.getInt("stock_id");
            i.supplyId = rs.getInt("supply_id");
            i.supplyName = rs.getString("supply_name");
            i.unit = rs.getString("unit");
            i.quantity = rs.getInt("quantity");
            i.unitCost = rs.getBigDecimal("unit_cost");
            i.dateReceived = rs.getTimestamp("date_received").toLocalDateTime();
            return i;
        };
    }

    @Override protected ParamBinder<InventoryStock> insertBinder() {
        return (ps, i) -> {
            ps.setInt(1, i.supplyId);
            ps.setInt(2, i.quantity);
            ps.setBigDecimal(3, i.unitCost);
        };
    }

    @Override protected ParamBinder<InventoryStock> updateBinder() {
        return (ps, i) -> {
            ps.setInt(1, i.supplyId);
            ps.setInt(2, i.quantity);
            ps.setBigDecimal(3, i.unitCost);
            ps.setInt(4, i.id);
        };
    }
}
