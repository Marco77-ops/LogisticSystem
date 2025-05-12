package com.luckypets.logistics.shared.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public class ShipmentDeliveredEvent extends AbstractEvent {
    private String shipmentId;
    private String destination;
    private String location;
    private Instant timestamp;

    // Default-Konstruktor für Tests
    public ShipmentDeliveredEvent() {
        super(null);
    }

    @JsonCreator
    public ShipmentDeliveredEvent(
            @JsonProperty("shipmentId") String shipmentId,
            @JsonProperty("destination") String destination,
            @JsonProperty("location") String location,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("correlationId") String correlationId
    ) {
        super(correlationId);
        this.shipmentId = shipmentId;
        this.destination = destination;
        this.location = location;
        this.timestamp = timestamp;
    }

    // Getter und Setter
    public String getShipmentId() {
        return shipmentId;
    }

    public void setShipmentId(String shipmentId) {
        this.shipmentId = shipmentId;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String getAggregateId() {
        return shipmentId;
    }

    @Override
    public String getEventType() {
        return "ShipmentDeliveredEvent";
    }
}