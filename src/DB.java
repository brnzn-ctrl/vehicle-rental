
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Single place that knows how to open a MySQL connection. */
public final class DB {

    private static final String URL  = "jdbc:mysql://localhost:3306/vehicle_rental_system?useSSL=false&serverTimezone=Asia/Manila";
    private static final String USER = "root";
    private static final String PASS = "";          // <-- put your MySQL password here

    private DB() { }

    public static Connection get() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}
