import java.math.BigDecimal;
import java.time.LocalDateTime;

public class InventoryStock extends Entity {
    public int supplyId;
    public String supplyName;    // filled by JOIN for display
    public String unit;          // filled by JOIN for display
    public int quantity;
    public BigDecimal unitCost;
    public LocalDateTime dateReceived;

    public InventoryStock() { }

    @Override public String toString() { return supplyName; }
}
