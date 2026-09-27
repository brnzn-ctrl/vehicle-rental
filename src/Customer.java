
public class Customer extends Entity {
    public String username;
    public String passwordHash;
    public String firstName;
    public String lastName;
    public String email;
    public String phone;
    public String address;
    public String licenseNumber;
    public boolean active = true;

    @Override public String toString() { return firstName + " " + lastName; }
}
