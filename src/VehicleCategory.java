
public class VehicleCategory extends Entity {
    public String name;

    public VehicleCategory() { }
    public VehicleCategory(int id, String name) { this.id = id; this.name = name; }

    @Override public String toString() { return name; } // so it looks right inside a JComboBox
}
