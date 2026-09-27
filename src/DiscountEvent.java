import java.math.BigDecimal;
import java.time.LocalDate;

public class DiscountEvent extends Entity {
    public String eventName;
    public LocalDate startDate;
    public LocalDate endDate;
    public BigDecimal discountPercent;
    public boolean active = true;

    @Override public String toString() { return eventName + " (" + discountPercent + "% off)"; }
}
