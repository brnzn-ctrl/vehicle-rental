package model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Model class: expense. Fields are private (encapsulation); use the getters and setters. */
public class Expense extends Entity {
    private String category;   // e.g. Maintenance, Fuel, Salary, Utilities
    private String description;
    private BigDecimal amount;
    private LocalDate expenseDate;
    private int recordedBy;   // staff_id
    private String recordedByName;   // filled by JOIN for display

    /** Empty constructor: used when a row is read from the database or a form is filled in. */
    public Expense() { }

    /** Full constructor. */
    public Expense(int id, String category, String description, BigDecimal amount, LocalDate expenseDate, int recordedBy, String recordedByName) {
        super(id);
        this.category = category;
        this.description = description;
        this.amount = amount;
        this.expenseDate = expenseDate;
        this.recordedBy = recordedBy;
        this.recordedByName = recordedByName;
    }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDate getExpenseDate() { return expenseDate; }
    public void setExpenseDate(LocalDate expenseDate) { this.expenseDate = expenseDate; }
    public int getRecordedBy() { return recordedBy; }
    public void setRecordedBy(int recordedBy) { this.recordedBy = recordedBy; }
    public String getRecordedByName() { return recordedByName; }
    public void setRecordedByName(String recordedByName) { this.recordedByName = recordedByName; }

    @Override public String toString() { return category; }
}
