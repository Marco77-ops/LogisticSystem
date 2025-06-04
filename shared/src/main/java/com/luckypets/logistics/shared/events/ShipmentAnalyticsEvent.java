package com.luckypets.logistics.shared.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ShipmentAnalyticsEvent(String location, long deliveryCount, Instant windowStart) {
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
}