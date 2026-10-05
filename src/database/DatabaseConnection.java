package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * The one place that knows how to open a connection to Apache Derby.
 * Java application -> JDBC -> Apache Derby -> database tables.
 * Every DAO calls DatabaseConnection.getConnection() instead of repeating connection code.
 */
public final class DatabaseConnection {

    // "create=true" tells Derby to create the database folder the first time it doesn't exist yet.
    // This is the EMBEDDED driver, so no server has to be started (a network URL such as
    // jdbc:derby://localhost:1527/VehicleRentalDB would also work with Derby's network server).
    private static final String URL = "jdbc:derby:VehicleRentalDB;create=true";

    private DatabaseConnection() { }

    public static Connection getConnection() throws SQLException {
        try {
            return DriverManager.getConnection(URL);
        } catch (NoClassDefFoundError | ExceptionInInitializerError e) {
            // Derby 10.15+ is split in two jars: derby.jar needs derbyshared.jar next to it.
            // Report it as a normal SQLException (state "VRLIB") so every screen shows the same clear message.
            throw new SQLException("Derby is missing a library file. Add derbyshared.jar (from the lib folder of your "
                    + "Apache Derby download, the same folder as derby.jar) to the project's Libraries, "
                    + "or copy it into dist/lib next to derby.jar, then Clean and Build.", "VRLIB", e);
        }
    }

    /** Short name kept so older code that says get() keeps working. */
    public static Connection get() throws SQLException { return getConnection(); }

    /** Cleanly shuts down the embedded Derby engine; call this when the app exits. */
    public static void shutdown() {
        try {
            DriverManager.getConnection("jdbc:derby:;shutdown=true");
        } catch (SQLException e) {
            // Derby always throws an exception on a successful shutdown: that's normal, not an error.
        }
    }
}
