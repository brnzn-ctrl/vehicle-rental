package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/** Keeps the stored supply stock_level in sync with received quantities and reorder thresholds. */
public final class StockLevelUpdater {
    private static final String STATUS_EXPRESSION =
        "CASE " +
        "WHEN COALESCE((SELECT SUM(i.quantity) FROM inventory_stock i WHERE i.supply_id = supplies.supply_id),0) <= 2 THEN 'critical stock' " +
        "WHEN COALESCE((SELECT SUM(i.quantity) FROM inventory_stock i WHERE i.supply_id = supplies.supply_id),0) <= supplies.reorder_threshold THEN 'low stock' " +
        "ELSE 'sufficient' END";

    private StockLevelUpdater() { }

    public static void refresh(Connection connection, int supplyId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE supplies SET stock_level = " + STATUS_EXPRESSION + " WHERE supply_id = ?")) {
            ps.setInt(1, supplyId);
            ps.executeUpdate();
        }
    }

    public static void refreshAll(Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE supplies SET stock_level = " + STATUS_EXPRESSION)) {
            ps.executeUpdate();
        }
    }
}
