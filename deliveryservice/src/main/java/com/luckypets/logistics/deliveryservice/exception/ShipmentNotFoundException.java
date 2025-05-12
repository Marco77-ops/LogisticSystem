package com.luckypets.logistics.deliveryservice.exception;

public class ShipmentNotFoundException extends RuntimeException {
    public ShipmentNotFoundException(String shipmentId) {
        super("Shipment nicht gefunden: " + shipmentId);
    }
}
