package model;


public class VehicleCategory extends Entity {
    private String name;

    /** Empty constructor: used when a row is read from the database or a form is filled in. */
    public VehicleCategory() { }

    /** Full constructor. */
    public VehicleCategory(int id, String name) {
        super(id);
        this.name = name;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Override public String toString() { return name; }   // so it looks right inside a JComboBox
}
