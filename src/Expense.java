import java.math.BigDecimal;
import java.time.LocalDate;

public class Expense extends Entity {
    public String category;      // e.g. Maintenance, Fuel, Salary, Utilities
    public String description;
    public BigDecimal amount;
    public LocalDate expenseDate;
    public int recordedBy;       // staff_id
    public String recordedByName; // filled by JOIN for display

    public Expense() { }

    @Override public String toString() { return category; }
}
