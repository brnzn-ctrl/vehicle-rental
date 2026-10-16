package model;

/** Model class: supply. Fields are private (encapsulation); use the getters and setters. */
public class Supply extends Entity {
    private String name;
    private String unit;   // pcs, liters, etc.
    private Integer supplierId;   // nullable
    private String supplierName;   // filled by JOIN for display
    private int reorderThreshold = 5;
    private String stockLevel = "CRITICAL";

    /** Empty constructor: used when a row is read from the database or a form is filled in. */
    public Supply() { }

    /** Full constructor. */
    public Supply(int id, String name, String unit, Integer supplierId, String supplierName, int reorderThreshold) {
        super(id);
        this.name = name;
        this.unit = unit;
        this.supplierId = supplierId;
        this.supplierName = supplierName;
        this.reorderThreshold = reorderThreshold;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public Integer getSupplierId() { return supplierId; }
    public void setSupplierId(Integer supplierId) { this.supplierId = supplierId; }
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public int getReorderThreshold() { return reorderThreshold; }
    public void setReorderThreshold(int reorderThreshold) { this.reorderThreshold = reorderThreshold; }
    public String getStockLevel() { return stockLevel; }
    public void setStockLevel(String stockLevel) { this.stockLevel = stockLevel; }

    @Override public String toString() { return name; }
}
