package model;

import java.time.LocalDate;

/** Model class: reservation. Fields are private (encapsulation); use the getters and setters. */
public class Reservation extends Entity {
    private int customerId;
    private int vehicleId;
    private String vehicleName;   // filled by JOIN for display
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;   // pending | approved | rejected | cancelled | converted
    private Integer discountId;   // nullable
    private String discountEventName;   // filled by JOIN, may be null

    /** Empty constructor: used when a row is read from the database or a form is filled in. */
    public Reservation() { }

    /** Full constructor. */
    public Reservation(int id, int customerId, int vehicleId, String vehicleName, LocalDate startDate, LocalDate endDate, String status, Integer discountId, String discountEventName) {
        super(id);
        this.customerId = customerId;
        this.vehicleId = vehicleId;
        this.vehicleName = vehicleName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.discountId = discountId;
        this.discountEventName = discountEventName;
    }

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }
    public int getVehicleId() { return vehicleId; }
    public void setVehicleId(int vehicleId) { this.vehicleId = vehicleId; }
    public String getVehicleName() { return vehicleName; }
    public void setVehicleName(String vehicleName) { this.vehicleName = vehicleName; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getDiscountId() { return discountId; }
    public void setDiscountId(Integer discountId) { this.discountId = discountId; }
    public String getDiscountEventName() { return discountEventName; }
    public void setDiscountEventName(String discountEventName) { this.discountEventName = discountEventName; }

    @Override public String toString() { return vehicleName + " (" + status + ")"; }
}
