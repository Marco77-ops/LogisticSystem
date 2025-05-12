package com.luckypets.logistics.shared.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public abstract class AbstractEvent implements BaseEvent {
    private  Instant timestamp;
    private  String version;
    private  String correlationId;


    public AbstractEvent() {}

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
}
