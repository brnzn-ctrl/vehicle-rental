

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;

public class VehicleDAO extends AbstractDAO<Vehicle> {

    private static final String BASE_SELECT =
        "SELECT v.vehicle_id, v.plate_number, v.vehicle_name, v.category_id, c.category_name, " +
        "       v.daily_rate, v.status, v.image_filename " +
        "FROM vehicles v INNER JOIN vehicle_categories c ON v.category_id = c.category_id";

    @Override protected String selectAllSql()  { return BASE_SELECT + " ORDER BY v.vehicle_name"; }
    @Override protected String selectByIdSql() { return BASE_SELECT + " WHERE v.vehicle_id=?"; }
    @Override protected String deleteSql()     { return "DELETE FROM vehicles WHERE vehicle_id=?"; }

    @Override protected String insertSql() {
        return "INSERT INTO vehicles(plate_number, vehicle_name, category_id, daily_rate, status, image_filename) " +
               "VALUES (?,?,?,?,?,?)";
    }

    @Override protected String updateSql() {
        return "UPDATE vehicles SET plate_number=?, vehicle_name=?, category_id=?, daily_rate=?, status=?, image_filename=? " +
               "WHERE vehicle_id=?";
    }

    @Override protected RowMapper<Vehicle> mapper() {
        return rs -> {
            Vehicle v = new Vehicle();
            v.id            = rs.getInt("vehicle_id");
            v.plateNumber   = rs.getString("plate_number");
            v.name          = rs.getString("vehicle_name");
            v.categoryId    = rs.getInt("category_id");
            v.categoryName  = rs.getString("category_name");
            v.dailyRate     = rs.getBigDecimal("daily_rate");
            v.status        = rs.getString("status");
            v.imageFilename = rs.getString("image_filename");
            return v;
        };
    }

    @Override protected ParamBinder<Vehicle> insertBinder() {
        return (ps, v) -> {
            ps.setString(1, v.plateNumber);
            ps.setString(2, v.name);
            ps.setInt(3, v.categoryId);
            ps.setBigDecimal(4, v.dailyRate);
            ps.setString(5, v.status);
            ps.setString(6, v.imageFilename);
        };
    }

    @Override protected ParamBinder<Vehicle> updateBinder() {
        return (ps, v) -> {
            ps.setString(1, v.plateNumber);
            ps.setString(2, v.name);
            ps.setInt(3, v.categoryId);
            ps.setBigDecimal(4, v.dailyRate);
            ps.setString(5, v.status);
            ps.setString(6, v.imageFilename);
            ps.setInt(7, v.id); // WHERE vehicle_id=?
        };
    }
}
