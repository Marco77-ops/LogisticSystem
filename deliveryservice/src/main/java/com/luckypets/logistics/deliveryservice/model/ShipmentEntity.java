package com.luckypets.logistics.deliveryservice.model;

import com.luckypets.logistics.shared.model.ShipmentStatus;
// Removed jakarta.persistence imports as this entity will no longer be JPA-managed
import java.time.LocalDateTime;

// This is a plain Java object (POJO) for the deliveryservice's internal in-memory representation.
// It does NOT have JPA annotations.
public class ShipmentEntity {

    private String shipmentId;
    private String origin; // Added for completeness, if it comes from ShipmentCreatedEvent
    private String destination;
    private String customerId; // Added for completeness, if it comes from ShipmentCreatedEvent
    private String lastLocation;
    private LocalDateTime createdAt;
    private LocalDateTime lastScannedAt;
    private LocalDateTime deliveredAt;

    private ShipmentStatus status; // Uses the shared ShipmentStatus enum

    public ShipmentEntity() {}

    // Constructor for initial creation from event data, e.g., ShipmentCreatedEvent
    public ShipmentEntity(String shipmentId, String origin, String destination, String customerId, LocalDateTime createdAt, ShipmentStatus status) {
        this.shipmentId = shipmentId;
        this.origin = origin;
        this.destination = destination;
        this.customerId = customerId;
        this.createdAt = createdAt;
        this.status = status;
    }


    // --- Getters and Setters ---
    public String getShipmentId() { return shipmentId; }
    public void setShipmentId(String shipmentId) { this.shipmentId = shipmentId; }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public String getLastLocation() { return lastLocation; }
    public void setLastLocation(String lastLocation) { this.lastLocation = lastLocation; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastScannedAt() { return lastScannedAt; }
    public void setLastScannedAt(LocalDateTime lastScannedAt) { this.lastScannedAt = lastScannedAt; }

    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }

    public ShipmentStatus getStatus() { return status; }
    public void setStatus(ShipmentStatus status) { this.status = status; }
}
