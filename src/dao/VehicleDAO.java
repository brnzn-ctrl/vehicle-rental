package dao;



import model.Vehicle;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;

public class VehicleDAO extends AbstractDAO<Vehicle> {

    private static final String BASE_SELECT =
        "SELECT v.vehicle_id, v.plate_number, v.vehicle_name, v.category_id, c.category_name, " +
        "       v.daily_rate, v.status, v.image_filename " +
        "FROM vehicles v INNER JOIN vehicle_categories c ON v.category_id = c.category_id";

    @Override protected String selectAllSql()  { return BASE_SELECT + " ORDER BY v.vehicle_name"; }
    @Override protected String[] searchColumns() { return new String[]{ "v.vehicle_id", "v.plate_number", "v.vehicle_name", "c.category_name", "v.daily_rate", "v.status" }; }
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
            v.setId(rs.getInt("vehicle_id"));
            v.setPlateNumber(rs.getString("plate_number"));
            v.setName(rs.getString("vehicle_name"));
            v.setCategoryId(rs.getInt("category_id"));
            v.setCategoryName(rs.getString("category_name"));
            v.setDailyRate(rs.getBigDecimal("daily_rate"));
            v.setStatus(rs.getString("status"));
            v.setImageFilename(rs.getString("image_filename"));
            return v;
        };
    }

    @Override protected ParamBinder<Vehicle> insertBinder() {
        return (ps, v) -> {
            ps.setString(1, v.getPlateNumber());
            ps.setString(2, v.getName());
            ps.setInt(3, v.getCategoryId());
            ps.setBigDecimal(4, v.getDailyRate());
            ps.setString(5, v.getStatus());
            ps.setString(6, v.getImageFilename());
        };
    }

    @Override protected ParamBinder<Vehicle> updateBinder() {
        return (ps, v) -> {
            ps.setString(1, v.getPlateNumber());
            ps.setString(2, v.getName());
            ps.setInt(3, v.getCategoryId());
            ps.setBigDecimal(4, v.getDailyRate());
            ps.setString(5, v.getStatus());
            ps.setString(6, v.getImageFilename());
            ps.setInt(7, v.getId()); // WHERE vehicle_id=?
        };
    }

    /** Duplicate check done by the database (COUNT ... WHERE): is this plate number already used by ANOTHER vehicle? */
    public boolean plateExists(String plate, int excludeVehicleId) {
        String sql = "SELECT COUNT(*) FROM vehicles WHERE UPPER(plate_number) = ? AND vehicle_id <> ?";
        try (java.sql.Connection c = database.DatabaseConnection.getConnection();
             java.sql.PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, plate.trim().toUpperCase());
            ps.setInt(2, excludeVehicleId);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (java.sql.SQLException e) {
            throw new RuntimeException("plateExists failed: " + e.getMessage(), e);
        }
    }

    /** Is this vehicle out on a rental that has not been returned yet? (SELECT COUNT ... WHERE) */
    public boolean hasOngoingRental(int vehicleId) {
        String sql = "SELECT COUNT(*) FROM rentals WHERE vehicle_id = ? AND status = 'ongoing'";
        try (java.sql.Connection c = database.DatabaseConnection.getConnection();
             java.sql.PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, vehicleId);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (java.sql.SQLException e) {
            throw new RuntimeException("hasOngoingRental failed: " + e.getMessage(), e);
        }
    }
}
