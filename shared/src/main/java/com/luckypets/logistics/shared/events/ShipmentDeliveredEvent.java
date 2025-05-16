package com.luckypets.logistics.shared.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class ShipmentDeliveredEvent extends AbstractEvent {
    private final String shipmentId;
    private final String destination;
    private final String location;
    private final LocalDateTime deliveredAt;


    @JsonCreator
    public ShipmentDeliveredEvent(
            @JsonProperty("shipmentId") String shipmentId,
            @JsonProperty("destination") String destination,
            @JsonProperty("location") String location,
            @JsonProperty("deliveredAt") LocalDateTime deliveredAt,
            @JsonProperty("correlationId") String correlationId
    ) {
        super(correlationId);
        this.shipmentId = shipmentId;
        this.destination = destination;
        this.location = location;
        this.deliveredAt = deliveredAt;
    }

    public String getShipmentId() {
        return shipmentId;
    }

    public String getDestination() {
        return destination;
    }

    public String getLocation() {
        return location;
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
