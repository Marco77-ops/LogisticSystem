package com.luckypets.logistics.scanservice.unittest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.luckypets.logistics.shared.events.ShipmentCreatedEvent;
import com.luckypets.logistics.shared.events.ShipmentScannedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Event Serialization/Deserialization Tests")
public class EventSerializationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("Should serialize ShipmentCreatedEvent to JSON")
    void shouldSerializeShipmentCreatedEventToJson() throws Exception {
        ShipmentCreatedEvent event = new ShipmentCreatedEvent(
                "SHIP-001",
                "Berlin, Germany",
                LocalDateTime.of(2024, 1, 15, 10, 30, 45),
                "corr-123"
        );
        String json = objectMapper.writeValueAsString(event);

        assertNotNull(json);
        assertTrue(json.contains("SHIP-001"));
        assertTrue(json.contains("Berlin, Germany"));
        assertTrue(json.contains("corr-123"));
    }

    @Test
    @DisplayName("Should deserialize JSON to ShipmentCreatedEvent")
    void shouldDeserializeJsonToShipmentCreatedEvent() throws Exception {
        String json = """
            {
                "shipmentId": "SHIP-001",
                "destination": "Berlin, Germany",
                "createdAt": "2024-01-15T10:30:45",
                "correlationId": "corr-123"
            }
            """;

        ShipmentCreatedEvent event = objectMapper.readValue(json, ShipmentCreatedEvent.class);

        assertEquals("SHIP-001", event.getShipmentId());
        assertEquals("Berlin, Germany", event.getDestination());
        assertNotNull(event.getCreatedAt());
        assertEquals("corr-123", event.getCorrelationId());
    }

    @Test
    @DisplayName("Should serialize immutable ShipmentScannedEvent to JSON")
    void shouldSerializeImmutableShipmentScannedEventToJson() throws Exception {
        LocalDateTime scannedTime = LocalDateTime.of(2024, 1, 15, 10, 30, 45);
        ShipmentScannedEvent event = new ShipmentScannedEvent(
                "SHIP-001",
                "WAREHOUSE_A",
                scannedTime,
                "Berlin, Germany",
                "corr-123"
        );

        String json = objectMapper.writeValueAsString(event);

        assertNotNull(json);
        assertTrue(json.contains("SHIP-001"));
        assertTrue(json.contains("WAREHOUSE_A"));
        assertTrue(json.contains("Berlin, Germany"));
        assertTrue(json.contains("corr-123"));
    }

    @Test
    @DisplayName("Should deserialize JSON to immutable ShipmentScannedEvent")
    void shouldDeserializeJsonToImmutableShipmentScannedEvent() throws Exception {
        String json = """
            {
                "shipmentId": "SHIP-001",
                "location": "WAREHOUSE_A",
                "scannedAt": "2024-01-15T10:30:45",
                "destination": "Berlin, Germany",
                "correlationId": "corr-123"
            }
            """;

        ShipmentScannedEvent event = objectMapper.readValue(json, ShipmentScannedEvent.class);

        assertEquals("SHIP-001", event.getShipmentId());
        assertEquals("WAREHOUSE_A", event.getLocation());
        assertEquals("Berlin, Germany", event.getDestination());
        assertEquals("corr-123", event.getCorrelationId());
        assertNotNull(event.getScannedAt());
    }

    @Test
    @DisplayName("Should handle round-trip serialization for immutable ShipmentScannedEvent")
    void shouldHandleRoundTripSerializationForImmutableShipmentScannedEvent() throws Exception {
        LocalDateTime scannedTime = LocalDateTime.now();
        ShipmentScannedEvent originalEvent = new ShipmentScannedEvent(
                "SHIP-001",
                "WAREHOUSE_A",
                scannedTime,
                "Berlin, Germany",
                "corr-123"
        );

        String json = objectMapper.writeValueAsString(originalEvent);
        ShipmentScannedEvent deserializedEvent = objectMapper.readValue(json, ShipmentScannedEvent.class);

        assertEquals(originalEvent.getShipmentId(), deserializedEvent.getShipmentId());
        assertEquals(originalEvent.getLocation(), deserializedEvent.getLocation());
        assertEquals(originalEvent.getDestination(), deserializedEvent.getDestination());
        assertEquals(originalEvent.getCorrelationId(), deserializedEvent.getCorrelationId());
        assertEquals(originalEvent.getScannedAt(), deserializedEvent.getScannedAt());
        assertEquals(originalEvent.getEventType(), deserializedEvent.getEventType());
    }

    @Test
    @DisplayName("Should handle round-trip serialization for ShipmentCreatedEvent")
    void shouldHandleRoundTripSerializationForShipmentCreatedEvent() throws Exception {
        ShipmentCreatedEvent originalEvent = new ShipmentCreatedEvent(
                "SHIP-001",
                "Berlin, Germany",
                LocalDateTime.now(),
                "corr-123"
        );

        String json = objectMapper.writeValueAsString(originalEvent);
        ShipmentCreatedEvent deserializedEvent = objectMapper.readValue(json, ShipmentCreatedEvent.class);

        assertEquals(originalEvent.getShipmentId(), deserializedEvent.getShipmentId());
        assertEquals(originalEvent.getDestination(), deserializedEvent.getDestination());
        assertEquals(originalEvent.getCreatedAt(), deserializedEvent.getCreatedAt());
        assertEquals(originalEvent.getCorrelationId(), deserializedEvent.getCorrelationId());
    }

    @Test
    @DisplayName("Should verify ShipmentScannedEvent immutability")
    void shouldVerifyShipmentScannedEventImmutability() {
        LocalDateTime scannedTime = LocalDateTime.of(2024, 1, 15, 10, 30, 45);
        ShipmentScannedEvent event = new ShipmentScannedEvent(
                "SHIP-001",
                "WAREHOUSE_A",
                scannedTime,
                "Berlin, Germany",
                "corr-123"
        );

        assertEquals("SHIP-001", event.getShipmentId());
        assertEquals("WAREHOUSE_A", event.getLocation());
        assertEquals("Berlin, Germany", event.getDestination());
        assertEquals("corr-123", event.getCorrelationId());
        assertEquals(scannedTime, event.getScannedAt());
        assertEquals("ShipmentScannedEvent", event.getEventType());
    }
}
