package model;

/** Model class: customer. Fields are private (encapsulation); use the getters and setters. */
public class Customer extends Entity {
    private String username;
    private String passwordHash;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String address;
    private String licenseNumber;
    private boolean active = true;

    /** Empty constructor: used when a row is read from the database or a form is filled in. */
    public Customer() { }

    /** Full constructor. */
    public Customer(int id, String username, String passwordHash, String firstName, String lastName, String email, String phone, String address, String licenseNumber, boolean active) {
        super(id);
        this.username = username;
        this.passwordHash = passwordHash;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.licenseNumber = licenseNumber;
        this.active = active;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override public String toString() { return firstName + " " + lastName; }
}
