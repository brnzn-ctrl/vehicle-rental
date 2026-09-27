
public class Supply extends Entity {
    public String name;
    public String unit;             // pcs, liters, etc.
    public Integer supplierId;      // nullable
    public String supplierName;     // filled by JOIN for display
    public int reorderLevel = 5;

    public Supply() { }

    @Override public String toString() { return name; }
}
