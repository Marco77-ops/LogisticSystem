package com.luckypets.logistics.shared.events;

import java.time.Instant;

public interface BaseEvent {
    String getAggregateId();
    Instant getTimestamp();
    String getVersion();
    String getCorrelationId();
    String getEventType();
}
