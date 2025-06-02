package com.luckypets.logistics.deliveryservice.model;

/**
 * Request model for delivery operations.
 */
public class DeliveryRequest {
    private String shipmentId;
    private String location;

    // Default constructor for Jackson
    public DeliveryRequest() {
    }

    public DeliveryRequest(String shipmentId, String location) {
        this.shipmentId = shipmentId;
        this.location = location;
    }

    public String getShipmentId() {
        return shipmentId;
    }

    public void setShipmentId(String shipmentId) {
        this.shipmentId = shipmentId;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    @Override
    public String toString() {
        return "DeliveryRequest{" +
                "shipmentId='" + shipmentId + '\'' +
                ", location='" + location + '\'' +
                '}';
    }
}