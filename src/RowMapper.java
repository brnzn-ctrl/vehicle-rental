
import java.sql.ResultSet;
import java.sql.SQLException;

/** Turns one ResultSet row into an object. */
@FunctionalInterface
public interface RowMapper<T> {
    T map(ResultSet rs) throws SQLException;
}
