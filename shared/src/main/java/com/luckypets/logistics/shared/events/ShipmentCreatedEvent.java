package com.luckypets.logistics.shared.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ShipmentCreatedEvent extends AbstractEvent {
    private String shipmentId;
    private String destination;
    private LocalDateTime createdAt;


    @JsonCreator
    public ShipmentCreatedEvent(
            @JsonProperty("shipmentId") String shipmentId,
            @JsonProperty("destination") String destination,
            @JsonProperty("createdAt") LocalDateTime createdAt,
            @JsonProperty("correlationId") String correlationId
    ) {
        super(correlationId);
        this.shipmentId = shipmentId;
        this.destination = destination;
        this.createdAt = createdAt;
    }

    public String getShipmentId() {
        return shipmentId;
    }

    public String getDestination() {
        return destination;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }


    @Override
    public String getEventType() {
        return "ShipmentCreatedEvent";
    }
}
