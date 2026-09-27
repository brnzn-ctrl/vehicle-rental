
public class Staff extends Entity {
    public String username;
    public String passwordHash;
    public String firstName;
    public String lastName;
    public String role;      // "admin" | "employee"
    public boolean active = true;

    @Override public String toString() { return firstName + " " + lastName; }
}
