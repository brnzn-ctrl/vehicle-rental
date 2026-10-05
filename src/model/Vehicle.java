package model;

import java.math.BigDecimal;

/** Model class: vehicle. Fields are private (encapsulation); use the getters and setters. */
public class Vehicle extends Entity {
    private String plateNumber;
    private String name;
    private int categoryId;
    private String categoryName;   // filled by JOIN, convenient for the table view
    private BigDecimal dailyRate;
    private String status;   // available | rented | maintenance | inactive
    private String imageFilename;   // e.g. "12.png", stored in VehicleImages/ next to the .jar

    /** Empty constructor: used when a row is read from the database or a form is filled in. */
    public Vehicle() { }

    /** Full constructor. */
    public Vehicle(int id, String plateNumber, String name, int categoryId, String categoryName, BigDecimal dailyRate, String status, String imageFilename) {
        super(id);
        this.plateNumber = plateNumber;
        this.name = name;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.dailyRate = dailyRate;
        this.status = status;
        this.imageFilename = imageFilename;
    }

    public String getPlateNumber() { return plateNumber; }
    public void setPlateNumber(String plateNumber) { this.plateNumber = plateNumber; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public BigDecimal getDailyRate() { return dailyRate; }
    public void setDailyRate(BigDecimal dailyRate) { this.dailyRate = dailyRate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getImageFilename() { return imageFilename; }
    public void setImageFilename(String imageFilename) { this.imageFilename = imageFilename; }

    @Override public String toString() { return name; }
}
