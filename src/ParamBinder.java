
import java.sql.PreparedStatement;
import java.sql.SQLException;

/** Fills in the "?" placeholders of an INSERT/UPDATE statement from an object. */
@FunctionalInterface
public interface ParamBinder<T> {
    void bind(PreparedStatement ps, T obj) throws SQLException;
}
