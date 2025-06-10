package com.luckypets.logistics.analyticservice.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public class DeliveryCount {
    private final String location;
    private final long count;
    private final LocalDateTime windowStart;
    private final LocalDateTime windowEnd;

    @JsonCreator
    public DeliveryCount(
            @JsonProperty("location") String location,
            @JsonProperty("count") long count,
            @JsonProperty("windowStart") LocalDateTime windowStart,
            @JsonProperty("windowEnd") LocalDateTime windowEnd
    ) {
        this.location = location;
        this.count = count;
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
    }

    public DeliveryCount(String location, long count) {
        this(location, count, null, null);
    }

    public String getLocation() { return location; }
    public long getCount() { return count; }
    public LocalDateTime getWindowStart() { return windowStart; }
    public LocalDateTime getWindowEnd() { return windowEnd; }

    public DeliveryCount add(long additional) {
        return new DeliveryCount(location, count + additional, windowStart, windowEnd);
    }

    @Override
    public String toString() {
        return String.format("DeliveryCount{location='%s', count=%d, window=%s-%s}",
                location, count, windowStart, windowEnd);
    }
}