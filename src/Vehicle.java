
import java.math.BigDecimal;

public class Vehicle extends Entity {
    public String plateNumber;
    public String name;
    public int categoryId;
    public String categoryName;      // filled by JOIN, convenient for the table view
    public BigDecimal dailyRate;
    public String status;            // available | rented | maintenance | inactive
    public String imageFilename;     // e.g. "12.png", stored in VehicleImages/ next to the .jar

    public Vehicle() { }

    @Override public String toString() { return name; }
}
