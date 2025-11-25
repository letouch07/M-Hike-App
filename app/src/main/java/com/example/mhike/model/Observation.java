package com.example.mhike.model;

/**
 * Model class representing an observation linked to a specific hike.
 */
public class Observation {

    private int id; // Primary key for SQLite
    private int hikeId; // Foreign key linking to the parent hike
    private String observationDetail; // Required field
    private String timeOfObservation; // Required field (ideally defaults to current time)
    private String additionalComments; // Optional field
    private String imagePath;

    // Default Constructor
    public Observation() {
    }

    // --- Getters and Setters ---

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getHikeId() {
        return hikeId;
    }

    public void setHikeId(int hikeId) {
        this.hikeId = hikeId;
    }

    public String getObservationDetail() {
        return observationDetail;
    }

    public void setObservationDetail(String observationDetail) {
        this.observationDetail = observationDetail;
    }

    public String getTimeOfObservation() {
        return timeOfObservation;
    }

    public void setTimeOfObservation(String timeOfObservation) {
        this.timeOfObservation = timeOfObservation;
    }

    public String getAdditionalComments() {
        return additionalComments;
    }

    public void setAdditionalComments(String additionalComments) {
        this.additionalComments = additionalComments;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
}