package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Model class for the "returns" table: what happened when a rented vehicle came back
 * (condition notes, late days and the late/damage fees). Named ReturnRecord because "return" is a Java keyword.
 */
public class ReturnRecord extends Entity {
    private int rentalId;
    private String vehicleName;          // filled by JOIN for display
    private String customerName;         // filled by JOIN for display
    private String receivedByName;       // filled by JOIN for display
    private LocalDateTime returnDate;
    private String conditionNotes;
    private int lateDays;
    private BigDecimal lateFee = BigDecimal.ZERO;
    private BigDecimal damageFee = BigDecimal.ZERO;
    private int receivedBy;              // staff_id

    /** Empty constructor: used when a row is read from the database. */
    public ReturnRecord() { }

    /** Full constructor. */
    public ReturnRecord(int id, int rentalId, String vehicleName, LocalDateTime returnDate, String conditionNotes,
                        int lateDays, BigDecimal lateFee, BigDecimal damageFee, int receivedBy) {
        super(id);
        this.rentalId = rentalId;
        this.vehicleName = vehicleName;
        this.returnDate = returnDate;
        this.conditionNotes = conditionNotes;
        this.lateDays = lateDays;
        this.lateFee = lateFee;
        this.damageFee = damageFee;
        this.receivedBy = receivedBy;
    }

    public int getRentalId() { return rentalId; }
    public void setRentalId(int rentalId) { this.rentalId = rentalId; }
    public String getVehicleName() { return vehicleName; }
    public void setVehicleName(String vehicleName) { this.vehicleName = vehicleName; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getReceivedByName() { return receivedByName; }
    public void setReceivedByName(String receivedByName) { this.receivedByName = receivedByName; }
    public LocalDateTime getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDateTime returnDate) { this.returnDate = returnDate; }
    public String getConditionNotes() { return conditionNotes; }
    public void setConditionNotes(String conditionNotes) { this.conditionNotes = conditionNotes; }
    public int getLateDays() { return lateDays; }
    public void setLateDays(int lateDays) { this.lateDays = lateDays; }
    public BigDecimal getLateFee() { return lateFee; }
    public void setLateFee(BigDecimal lateFee) { this.lateFee = lateFee; }
    public BigDecimal getDamageFee() { return damageFee; }
    public void setDamageFee(BigDecimal damageFee) { this.damageFee = damageFee; }
    public int getReceivedBy() { return receivedBy; }
    public void setReceivedBy(int receivedBy) { this.receivedBy = receivedBy; }

    @Override public String toString() { return "Return #" + getId() + " (rental " + rentalId + ")"; }
}
