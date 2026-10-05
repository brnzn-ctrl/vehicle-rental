package dao;


import model.InventoryStock;

public class InventoryStockDAO extends AbstractDAO<InventoryStock> {

    private static final String BASE_SELECT =
        "SELECT i.stock_id, i.supply_id, s.supply_name, s.unit, i.quantity, i.unit_cost, i.date_received " +
        "FROM inventory_stock i INNER JOIN supplies s ON i.supply_id = s.supply_id";

    @Override protected String selectAllSql()  { return BASE_SELECT + " ORDER BY i.date_received DESC"; }
    @Override protected String[] searchColumns() { return new String[]{ "i.stock_id", "s.supply_name", "s.unit", "i.quantity", "i.unit_cost", "i.date_received" }; }
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
            i.setId(rs.getInt("stock_id"));
            i.setSupplyId(rs.getInt("supply_id"));
            i.setSupplyName(rs.getString("supply_name"));
            i.setUnit(rs.getString("unit"));
            i.setQuantity(rs.getInt("quantity"));
            i.setUnitCost(rs.getBigDecimal("unit_cost"));
            i.setDateReceived(rs.getTimestamp("date_received").toLocalDateTime());
            return i;
        };
    }

    @Override protected ParamBinder<InventoryStock> insertBinder() {
        return (ps, i) -> {
            ps.setInt(1, i.getSupplyId());
            ps.setInt(2, i.getQuantity());
            ps.setBigDecimal(3, i.getUnitCost());
        };
    }

    @Override protected ParamBinder<InventoryStock> updateBinder() {
        return (ps, i) -> {
            ps.setInt(1, i.getSupplyId());
            ps.setInt(2, i.getQuantity());
            ps.setBigDecimal(3, i.getUnitCost());
            ps.setInt(4, i.getId());
        };
    }
}
