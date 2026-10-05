package database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Small, safe, repeatable upgrade that runs at start-up (after SchemaInitializer).
 * Salted password hashes are longer than the old 64-character SHA-256 text, so the password_hash
 * columns are widened to VARCHAR(255) if they are narrower. Existing data is kept. Running it again does nothing.
 */
public final class SchemaUpgrade {
    private static final int WANTED = 255;

    private SchemaUpgrade() { }

    /**
     * @return null when everything is fine, otherwise a plain-English warning (the program keeps working: it simply
     *         keeps using the old password format until the column can be widened).
     */
    public static String run() {
        try (Connection c = DatabaseConnection.getConnection()) {
            widenPasswordColumn(c, "STAFF");
            widenPasswordColumn(c, "CUSTOMERS");
            return null;
        } catch (SQLException e) {
            return "The password column could not be widened for salted passwords (" + e.getMessage() + ").\n"
                 + "The system will keep using the old password format until this is fixed.";
        }
    }

    private static void widenPasswordColumn(Connection c, String table) throws SQLException {
        int size = -1;
        try (ResultSet rs = c.getMetaData().getColumns(null, null, table, "PASSWORD_HASH")) {
            if (rs.next()) size = rs.getInt("COLUMN_SIZE");
        }
        if (size < 0 || size >= WANTED) return;       // table missing, or already wide enough
        try (Statement st = c.createStatement()) {
            st.executeUpdate("ALTER TABLE " + table + " ALTER COLUMN password_hash SET DATA TYPE VARCHAR(" + WANTED + ")");
        }
    }
}
