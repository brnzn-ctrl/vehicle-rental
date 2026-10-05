package dao;


import model.VehicleCategory;



public class VehicleCategoryDAO extends AbstractDAO<VehicleCategory> {

    @Override protected String selectAllSql()  { return "SELECT * FROM vehicle_categories ORDER BY category_name"; }
    @Override protected String[] searchColumns() { return new String[]{ "category_id", "category_name" }; }
    @Override protected String selectByIdSql() { return "SELECT * FROM vehicle_categories WHERE category_id=?"; }
    @Override protected String insertSql()     { return "INSERT INTO vehicle_categories(category_name) VALUES (?)"; }
    @Override protected String updateSql()     { return "UPDATE vehicle_categories SET category_name=? WHERE category_id=?"; }
    @Override protected String deleteSql()     { return "DELETE FROM vehicle_categories WHERE category_id=?"; }

    @Override protected RowMapper<VehicleCategory> mapper() {
        return rs -> new VehicleCategory(rs.getInt("category_id"), rs.getString("category_name"));
    }

    @Override protected ParamBinder<VehicleCategory> insertBinder() {
        return (ps, c) -> ps.setString(1, c.getName());
    }

    @Override protected ParamBinder<VehicleCategory> updateBinder() {
        return (ps, c) -> { ps.setString(1, c.getName()); ps.setInt(2, c.getId()); };
    }
}
