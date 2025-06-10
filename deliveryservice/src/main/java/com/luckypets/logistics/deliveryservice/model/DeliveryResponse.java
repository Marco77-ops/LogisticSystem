package com.luckypets.logistics.deliveryservice.model;

import com.luckypets.logistics.shared.model.ShipmentStatus;

import java.time.LocalDateTime;

/**
 * Response model for delivery operations.
 */
public class DeliveryResponse {
    private String shipmentId;
    private String status;
    private String location;
    private LocalDateTime deliveredAt;
    private boolean success;
    private String errorMessage;

    public DeliveryResponse() {}

    public DeliveryResponse(String shipmentId, ShipmentStatus status, String location, LocalDateTime deliveredAt) {
        this.shipmentId = shipmentId;
        this.status = status.name();
        this.location = location;
        this.deliveredAt = deliveredAt;
        this.success = true;
    }

    public static DeliveryResponse error(String errorMessage) {
        DeliveryResponse response = new DeliveryResponse();
        response.setSuccess(false);
        response.setErrorMessage(errorMessage);
        return response;
    }

    public String getShipmentId() {
        return shipmentId;
    }

    public void setShipmentId(String shipmentId) {
        this.shipmentId = shipmentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public String toString() {
        return "DeliveryResponse{" +
                "shipmentId='" + shipmentId + '\'' +
                ", status='" + status + '\'' +
                ", location='" + location + '\'' +
                ", deliveredAt=" + deliveredAt +
                ", success=" + success +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
