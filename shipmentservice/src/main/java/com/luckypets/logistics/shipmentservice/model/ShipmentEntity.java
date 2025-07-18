package com.luckypets.logistics.shipmentservice.model;

import com.luckypets.logistics.shared.model.ShipmentStatus;
// Remove these imports if you remove the annotations
// import jakarta.persistence.Column;
// import jakarta.persistence.Entity;
// import jakarta.persistence.EnumType;
// import jakarta.persistence.Enumerated;
// import jakarta.persistence.Id;
// import jakarta.persistence.Table;
import java.time.LocalDateTime;


public class ShipmentEntity {


    private String shipmentId;

    private String origin;
    private String destination;
    private String customerId;
    private String lastLocation;
    private LocalDateTime createdAt;
    private LocalDateTime lastScannedAt;
    private LocalDateTime deliveredAt;


    private ShipmentStatus status;

    public ShipmentEntity() {
    }

    public ShipmentEntity(String shipmentId, String destination, LocalDateTime createdAt) {
        this.shipmentId = shipmentId;
        this.destination = destination;
        this.createdAt = createdAt;
    }

    // Getter & Setter

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