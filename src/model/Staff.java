package model;

/** Model class: staff. Fields are private (encapsulation); use the getters and setters. */
public class Staff extends Entity {
    private String username;
    private String passwordHash;
    private String firstName;
    private String lastName;
    private String role;   // "admin" | "employee"
    private boolean active = true;

    /** Empty constructor: used when a row is read from the database or a form is filled in. */
    public Staff() { }

    /** Full constructor. */
    public Staff(int id, String username, String passwordHash, String firstName, String lastName, String role, boolean active) {
        super(id);
        this.username = username;
        this.passwordHash = passwordHash;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
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
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override public String toString() { return firstName + " " + lastName; }
}
