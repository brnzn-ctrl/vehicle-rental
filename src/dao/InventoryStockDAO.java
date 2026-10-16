package dao;

import database.DatabaseConnection;
import database.StockLevelUpdater;
import model.InventoryStock;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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

    @Override public void insert(InventoryStock stock) {
        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            super.insert(c, stock);
            StockLevelUpdater.refresh(c, stock.getSupplyId());
            c.commit();
        } catch (SQLException e) {
            throw new RuntimeException("insert failed: " + e.getMessage(), e);
        }
    }

    @Override public void update(InventoryStock stock) {
        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            int oldSupplyId;
            try (PreparedStatement find = c.prepareStatement("SELECT supply_id FROM inventory_stock WHERE stock_id=?")) {
                find.setInt(1, stock.getId());
                try (ResultSet rs = find.executeQuery()) {
                    if (!rs.next()) throw new SQLException("That record no longer exists. Refresh the list and try again.", "VR404");
                    oldSupplyId = rs.getInt(1);
                }
            }
            try (PreparedStatement ps = c.prepareStatement(updateSql())) {
                updateBinder().bind(ps, stock);
                if (ps.executeUpdate() == 0) throw new SQLException("That record no longer exists. Refresh the list and try again.", "VR404");
            }
            StockLevelUpdater.refresh(c, oldSupplyId);
            if (oldSupplyId != stock.getSupplyId()) StockLevelUpdater.refresh(c, stock.getSupplyId());
            c.commit();
        } catch (SQLException e) {
            throw new RuntimeException("update failed: " + e.getMessage(), e);
        }
    }

    @Override public void delete(int id) {
        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            int supplyId;
            try (PreparedStatement find = c.prepareStatement("SELECT supply_id FROM inventory_stock WHERE stock_id=?")) {
                find.setInt(1, id);
                try (ResultSet rs = find.executeQuery()) {
                    if (!rs.next()) throw new SQLException("That record no longer exists. Refresh the list and try again.", "VR404");
                    supplyId = rs.getInt(1);
                }
            }
            try (PreparedStatement ps = c.prepareStatement(deleteSql())) {
                ps.setInt(1, id);
                if (ps.executeUpdate() == 0) throw new SQLException("That record no longer exists. Refresh the list and try again.", "VR404");
            }
            StockLevelUpdater.refresh(c, supplyId);
            c.commit();
        } catch (SQLException e) {
            throw new RuntimeException("delete failed: " + e.getMessage(), e);
        }
    }
}
