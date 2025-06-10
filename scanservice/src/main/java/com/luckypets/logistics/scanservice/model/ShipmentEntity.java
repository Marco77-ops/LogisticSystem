package com.luckypets.logistics.scanservice.model;

import com.luckypets.logistics.shared.model.ShipmentStatus; // Import the shared ShipmentStatus
import java.time.LocalDateTime;

// This is a plain Java object (POJO) for the scanservice's internal representation.
// It does NOT have JPA annotations as this service is not using a database.
public class ShipmentEntity {
    private String shipmentId;
    private String origin;      // Might be populated from ShipmentCreatedEvent
    private String destination; // Populated from ShipmentCreatedEvent
    private String customerId;  // Might be populated from ShipmentCreatedEvent
    private String lastLocation;
    private LocalDateTime createdAt; // Populated from ShipmentCreatedEvent
    private LocalDateTime lastScannedAt;
    private LocalDateTime deliveredAt; // Might be populated from ShipmentDeliveredEvent
    private ShipmentStatus status; // Uses the shared ShipmentStatus enum

    public ShipmentEntity() {}

    // Constructor for initial creation from event data
    public ShipmentEntity(String shipmentId, String origin, String destination, String customerId, LocalDateTime createdAt, ShipmentStatus status) {
        this.shipmentId = shipmentId;
        this.origin = origin;
        this.destination = destination;
        this.customerId = customerId;
        this.createdAt = createdAt;
        this.status = status;
    }

    // --- Getters and Setters ---
    public String getShipmentId() {
        return shipmentId;
    }

    public void setShipmentId(String shipmentId) {
        this.shipmentId = shipmentId;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getLastLocation() {
        return lastLocation;
    }

    public void setLastLocation(String lastLocation) {
        this.lastLocation = lastLocation;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastScannedAt() {
        return lastScannedAt;
    }

    public void setLastScannedAt(LocalDateTime lastScannedAt) {
        this.lastScannedAt = lastScannedAt;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public ShipmentStatus getStatus() {
        return status;
    }

    public void setStatus(ShipmentStatus status) {
        this.status = status;
    }
}
