package com.luckypets.logistics.notificationservice.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class Notification {
    private String id;
    private String shipmentId;
    private String message;
    private NotificationType type;
    private LocalDateTime timestamp;

    public Notification() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
    }

    public Notification(String shipmentId, String message, NotificationType type) {
        this();
        this.shipmentId = shipmentId;
        this.message = message;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getShipmentId() {
        return shipmentId;
    }

    public void setShipmentId(String shipmentId) {
        this.shipmentId = shipmentId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "Notification{" +
                "id='" + id + '\'' +
                ", shipmentId='" + shipmentId + '\'' +
                ", message='" + message + '\'' +
                ", type=" + type +
                ", timestamp=" + timestamp +
                '}';
    }
}