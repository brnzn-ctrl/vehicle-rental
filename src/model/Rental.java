package model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Model class: rental. Fields are private (encapsulation); use the getters and setters. */
public class Rental extends Entity {
    private Integer reservationId;   // nullable - walk-in rentals may skip reservation
    private int customerId;
    private String customerName;   // filled by JOIN for display
    private int vehicleId;
    private String vehicleName;   // filled by JOIN for display
    private int staffId;
    private LocalDateTime rentOutDate;
    private LocalDate dueDate;
    private BigDecimal dailyRateSnapshot;
    private BigDecimal discountPercentSnapshot = BigDecimal.ZERO;
    private String status;   // ongoing | returned | cancelled

    /** Empty constructor: used when a row is read from the database or a form is filled in. */
    public Rental() { }

    /** Full constructor. */
    public Rental(int id, Integer reservationId, int customerId, String customerName, int vehicleId, String vehicleName, int staffId, LocalDateTime rentOutDate, LocalDate dueDate, BigDecimal dailyRateSnapshot, BigDecimal discountPercentSnapshot, String status) {
        super(id);
        this.reservationId = reservationId;
        this.customerId = customerId;
        this.customerName = customerName;
        this.vehicleId = vehicleId;
        this.vehicleName = vehicleName;
        this.staffId = staffId;
        this.rentOutDate = rentOutDate;
        this.dueDate = dueDate;
        this.dailyRateSnapshot = dailyRateSnapshot;
        this.discountPercentSnapshot = discountPercentSnapshot;
        this.status = status;
    }

    public Integer getReservationId() { return reservationId; }
    public void setReservationId(Integer reservationId) { this.reservationId = reservationId; }
    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public int getVehicleId() { return vehicleId; }
    public void setVehicleId(int vehicleId) { this.vehicleId = vehicleId; }
    public String getVehicleName() { return vehicleName; }
    public void setVehicleName(String vehicleName) { this.vehicleName = vehicleName; }
    public int getStaffId() { return staffId; }
    public void setStaffId(int staffId) { this.staffId = staffId; }
    public LocalDateTime getRentOutDate() { return rentOutDate; }
    public void setRentOutDate(LocalDateTime rentOutDate) { this.rentOutDate = rentOutDate; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public BigDecimal getDailyRateSnapshot() { return dailyRateSnapshot; }
    public void setDailyRateSnapshot(BigDecimal dailyRateSnapshot) { this.dailyRateSnapshot = dailyRateSnapshot; }
    public BigDecimal getDiscountPercentSnapshot() { return discountPercentSnapshot; }
    public void setDiscountPercentSnapshot(BigDecimal discountPercentSnapshot) { this.discountPercentSnapshot = discountPercentSnapshot; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override public String toString() { return vehicleName + " \u2192 " + customerName; }
}
