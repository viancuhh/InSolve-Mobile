package com.angelfish.insolve;

public class IncidentReport {
    public String reportId, fullName, contactNumber, incidentType, description, exactAddress, imageUrl, status;
    public long timestamp;
    public String adminRemarks;

    public IncidentReport() {}

    public IncidentReport(String reportId, String fullName, String contactNumber,
                          String incidentType, String description, String exactAddress,
                          String imageUrl, long timestamp, String status) {

        this.reportId = reportId;
        this.fullName = fullName;
        this.contactNumber = contactNumber;
        this.incidentType = incidentType;
        this.description = description;
        this.exactAddress = exactAddress;
        this.imageUrl = imageUrl;
        this.timestamp = timestamp;
        this.status = status;
        this.adminRemarks = "";
    }
}