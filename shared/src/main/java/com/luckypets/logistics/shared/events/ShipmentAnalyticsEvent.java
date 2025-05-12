package com.luckypets.logistics.shared.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public class ShipmentAnalyticsEvent {
    private final String location;
    private final long count;
    private final Instant windowStart;

    @JsonCreator
    public ShipmentAnalyticsEvent(
            @JsonProperty("location") String location,
            @JsonProperty("count") long count,
            @JsonProperty("windowStart") Instant windowStart
    ) {
        this.location = location;
        this.count = count;
        this.windowStart = windowStart;
    }

    public String getLocation() {
        return location;
    }

    public long getCount() {
        return count;
    }

    public Instant getWindowStart() {
        return windowStart;
    }
}
