package com.luckypets.logistics.shared.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ShipmentScannedEvent extends AbstractEvent {

    private final String shipmentId;
    private final String location;
    private final LocalDateTime scannedAt;
    private final String destination;

    @JsonCreator
    public ShipmentScannedEvent(
            @JsonProperty("shipmentId") String shipmentId,
            @JsonProperty("location") String location,
            @JsonProperty("scannedAt") LocalDateTime scannedAt,
            @JsonProperty("destination") String destination,
            @JsonProperty("correlationId") String correlationId
    ) {
        super(correlationId);
        this.shipmentId = shipmentId;
        this.location = location;
        this.scannedAt = scannedAt;
        this.destination = destination;
    }



    @Override
    public String getEventType() {
        return "ShipmentScannedEvent";
    }

    public String getShipmentId() {
        return shipmentId;
    }

    public String getLocation() {
        return location;
    }

    public LocalDateTime getScannedAt() {
        return scannedAt;
    }

    public String getDestination() {
        return destination;
    }
}
