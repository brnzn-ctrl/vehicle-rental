package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Model class: inventorystock. Fields are private (encapsulation); use the getters and setters. */
public class InventoryStock extends Entity {
    private int supplyId;
    private String supplyName;   // filled by JOIN for display
    private String unit;   // filled by JOIN for display
    private int quantity;
    private BigDecimal unitCost;
    private LocalDateTime dateReceived;

    /** Empty constructor: used when a row is read from the database or a form is filled in. */
    public InventoryStock() { }

    /** Full constructor. */
    public InventoryStock(int id, int supplyId, String supplyName, String unit, int quantity, BigDecimal unitCost, LocalDateTime dateReceived) {
        super(id);
        this.supplyId = supplyId;
        this.supplyName = supplyName;
        this.unit = unit;
        this.quantity = quantity;
        this.unitCost = unitCost;
        this.dateReceived = dateReceived;
    }

    public int getSupplyId() { return supplyId; }
    public void setSupplyId(int supplyId) { this.supplyId = supplyId; }
    public String getSupplyName() { return supplyName; }
    public void setSupplyName(String supplyName) { this.supplyName = supplyName; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
    public LocalDateTime getDateReceived() { return dateReceived; }
    public void setDateReceived(LocalDateTime dateReceived) { this.dateReceived = dateReceived; }

    @Override public String toString() { return supplyName; }
}
