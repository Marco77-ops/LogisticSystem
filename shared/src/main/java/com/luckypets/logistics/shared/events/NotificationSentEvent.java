package com.luckypets.logistics.shared.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationSentEvent extends AbstractEvent {
    private final String notificationId;
    private final String shipmentId;
    private final String type;
    private final String message;

    @JsonCreator
    public NotificationSentEvent(
            @JsonProperty("notificationId") String notificationId,
            @JsonProperty("shipmentId") String shipmentId,
            @JsonProperty("type") String type,
            @JsonProperty("message") String message,
            @JsonProperty("correlationId") String correlationId,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("version") String version
    ) {
        super(correlationId, timestamp, version);
        this.notificationId = notificationId;
        this.shipmentId = shipmentId;
        this.type = type;
        this.message = message;
    }

    @Override
    public String getEventType() {
        return "NotificationSentEvent";
    }

    public String getNotificationId() { return notificationId; }
    public String getShipmentId() { return shipmentId; }
    public String getType() { return type; }
    public String getMessage() { return message; }
}
