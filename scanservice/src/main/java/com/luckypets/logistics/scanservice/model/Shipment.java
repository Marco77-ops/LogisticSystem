package com.luckypets.logistics.scanservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "shipments")
public class Shipment {

    @Id
    private String shipmentId;

    private String destination;

    private LocalDateTime createdAt;

    // Constructors
    public Shipment() {}

    public Shipment(String shipmentId, String destination, LocalDateTime createdAt) {
        this.shipmentId = shipmentId;
        this.destination = destination;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public String getShipmentId() { return shipmentId; }

    public void setShipmentId(String shipmentId) { this.shipmentId = shipmentId; }

    public String getDestination() { return destination; }

    public void setDestination(String destination) { this.destination = destination; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
