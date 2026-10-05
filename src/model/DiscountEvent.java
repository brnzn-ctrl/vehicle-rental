package model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Model class: discountevent. Fields are private (encapsulation); use the getters and setters. */
public class DiscountEvent extends Entity {
    private String eventName;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal discountPercent;
    private boolean active = true;

    /** Empty constructor: used when a row is read from the database or a form is filled in. */
    public DiscountEvent() { }

    /** Full constructor. */
    public DiscountEvent(int id, String eventName, LocalDate startDate, LocalDate endDate, BigDecimal discountPercent, boolean active) {
        super(id);
        this.eventName = eventName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.discountPercent = discountPercent;
        this.active = active;
    }

    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public BigDecimal getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(BigDecimal discountPercent) { this.discountPercent = discountPercent; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override public String toString() { return eventName + " (" + discountPercent + "% off)"; }
}
