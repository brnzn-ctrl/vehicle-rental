package model;

/** Model class: supplier. Fields are private (encapsulation); use the getters and setters. */
public class Supplier extends Entity {
    public static final String TYPE_CAR = "car";
    public static final String TYPE_SUPPLIES = "supplies";

    private String name;
    private String contactNumber;
    private String address;
    private String type = TYPE_SUPPLIES;

    /** Empty constructor: used when a row is read from the database or a form is filled in. */
    public Supplier() { }

    /** Full constructor. */
    public Supplier(int id, String name, String contactNumber, String address) {
        super(id);
        this.name = name;
        this.contactNumber = contactNumber;
        this.address = address;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTypeLabel() {
        return TYPE_CAR.equals(type) ? "Car Supplier" : "Car Supplies Supplier";
    }

    @Override public String toString() { return name; }
}
