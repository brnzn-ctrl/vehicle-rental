import java.time.LocalDate;

public class Reservation extends Entity {
    public int customerId;
    public int vehicleId;
    public String vehicleName;   // filled by JOIN for display
    public LocalDate startDate;
    public LocalDate endDate;
    public String status;        // pending | approved | rejected | cancelled | converted
    public Integer discountId;   // nullable
    public String discountEventName; // filled by JOIN, may be null

    @Override public String toString() { return vehicleName + " (" + status + ")"; }
}
