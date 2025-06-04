package com.luckypets.logistics.shared.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public class ShipmentAnalyticsEvent {
    private final String location;
    private final long deliveryCount;
    private final Instant windowStart;

    @JsonCreator
    public ShipmentAnalyticsEvent(
            @JsonProperty("location") String location,
            @JsonProperty("deliveryCount") long deliveryCount,
            @JsonProperty("windowStart") Instant windowStart
    ) {
        this.location = location;
        this.deliveryCount = deliveryCount;
        this.windowStart = windowStart;
    }

    public String getLocation() {
        return location;
    }

    public long getDeliveryCount() {
        return deliveryCount;
    }

    public Instant getWindowStart() {
        return windowStart;
    }
}