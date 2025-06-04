package com.luckypets.logistics.shared.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public abstract class AbstractEvent implements BaseEvent {
    private final Instant timestamp;
    private final String version;
    private final String correlationId;

    // Default constructor for Jackson
    protected AbstractEvent() {
        this.timestamp = Instant.now();
        this.version = "v1";
        this.correlationId = null;
    }

    @JsonCreator
    protected AbstractEvent(
            @JsonProperty("correlationId") String correlationId,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("version") String version
    ) {
        this.correlationId = correlationId;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.version = version != null ? version : "v1";
    }

    protected AbstractEvent(String correlationId) {
        this(correlationId, Instant.now(), "v1");
    }

    @Override
    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getCorrelationId() {
        return correlationId;
    }

    @Override
    public String toString() {
        return String.format("%s{correlationId='%s', timestamp=%s, version='%s'}",
                getClass().getSimpleName(), correlationId, timestamp, version);
    }
}