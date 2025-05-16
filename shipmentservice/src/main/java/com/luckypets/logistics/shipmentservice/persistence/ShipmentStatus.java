package com.luckypets.logistics.shipmentservice.persistence;

public enum ShipmentStatus {
    CREATED,
    IN_TRANSIT,
    DELIVERED,
    LOST,
    RETURNED;

    @Override
    public String toString() {
        return name().replace('_', ' ');
    }
}