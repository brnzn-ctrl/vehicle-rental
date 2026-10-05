package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Model class for the "payments" table: money received for a rental when the vehicle is returned. */
public class Payment extends Entity {
    private int rentalId;
    private String vehicleName;          // filled by JOIN for display
    private String customerName;         // filled by JOIN for display
    private String processedByName;      // filled by JOIN for display
    private BigDecimal amount;
    private String paymentMethod;        // cash | card | gcash | bank_transfer
    private LocalDateTime paymentDate;
    private int processedBy;             // staff_id

    /** Empty constructor: used when a row is read from the database. */
    public Payment() { }

    /** Full constructor. */
    public Payment(int id, int rentalId, String vehicleName, BigDecimal amount, String paymentMethod,
                   LocalDateTime paymentDate, int processedBy) {
        super(id);
        this.rentalId = rentalId;
        this.vehicleName = vehicleName;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.paymentDate = paymentDate;
        this.processedBy = processedBy;
    }

    public int getRentalId() { return rentalId; }
    public void setRentalId(int rentalId) { this.rentalId = rentalId; }
    public String getVehicleName() { return vehicleName; }
    public void setVehicleName(String vehicleName) { this.vehicleName = vehicleName; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getProcessedByName() { return processedByName; }
    public void setProcessedByName(String processedByName) { this.processedByName = processedByName; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public LocalDateTime getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }
    public int getProcessedBy() { return processedBy; }
    public void setProcessedBy(int processedBy) { this.processedBy = processedBy; }

    @Override public String toString() { return "Payment #" + getId() + " (rental " + rentalId + ")"; }
}
