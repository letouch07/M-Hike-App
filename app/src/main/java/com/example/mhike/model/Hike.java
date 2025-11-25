package com.example.mhike.model;

/**
 * Model class representing a single hike record in the M-Hike application.
 * Corresponds to the required fields in the coursework specification.
 */
public class Hike {

    private int id; // Primary key for SQLite
    private String name; // Required field
    private String location; // Required field
    private String date; // Required field
    private String parkingAvailable; // Required field (\"Yes\" or \"No\")
    private double length; // Required field (use double for numerical length)
    private String difficulty; // Required field
    private String description; // Optional field
    private String customField1; // Optional field (Your invention)
    private String customField2; // Optional field (Your invention)

    // Default Constructor
    public Hike() {
        // Empty constructor needed for various Android database operations
    }

    // Constructor for creating a new hike (excluding the ID)
    public Hike(String name, String location, String date, String parkingAvailable, double length, String difficulty, String description, String customField1, String customField2) {
        this.name = name;
        this.location = location;
        this.date = date;
        this.parkingAvailable = parkingAvailable;
        this.length = length;
        this.difficulty = difficulty;
        this.description = description;
        this.customField1 = customField1;
        this.customField2 = customField2;
    }

    // --- Getters and Setters ---
    // Note: Android Studio can auto-generate these for efficiency

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getParkingAvailable() {
        return parkingAvailable;
    }

    public void setParkingAvailable(String parkingAvailable) {
        this.parkingAvailable = parkingAvailable;
    }

    public double getLength() {
        return length;
    }

    public void setLength(double length) {
        this.length = length;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCustomField1() {
        return customField1;
    }

    public void setCustomField1(String customField1) {
        this.customField1 = customField1;
    }

    public String getCustomField2() {
        return customField2;
    }

    public void setCustomField2(String customField2) {
        this.customField2 = customField2;
    }
}