import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Rental extends Entity {
    public Integer reservationId;    // nullable — walk-in rentals may skip reservation
    public int customerId;
    public String customerName;      // filled by JOIN for display
    public int vehicleId;
    public String vehicleName;       // filled by JOIN for display
    public int staffId;
    public LocalDateTime rentOutDate;
    public LocalDate dueDate;
    public BigDecimal dailyRateSnapshot;
    public BigDecimal discountPercentSnapshot = BigDecimal.ZERO;
    public String status;            // ongoing | returned | cancelled

    public Rental() { }

    @Override public String toString() { return vehicleName + " → " + customerName; }
}
