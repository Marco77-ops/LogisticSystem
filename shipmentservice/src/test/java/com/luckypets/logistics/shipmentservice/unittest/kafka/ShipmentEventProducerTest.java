package com.luckypets.logistics.shipmentservice.unittest.kafka;

import com.luckypets.logistics.shared.events.ShipmentCreatedEvent;
import com.luckypets.logistics.shipmentservice.kafka.ShipmentEventProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ShipmentEventProducerTest {

    private KafkaTemplate<String, ShipmentCreatedEvent> kafkaTemplate;
    private ShipmentEventProducer producer;

    @BeforeEach
    void setUp() {
        kafkaTemplate = mock(KafkaTemplate.class);
        producer = new ShipmentEventProducer(kafkaTemplate);
    }

    @Test
    void sendShipmentCreatedEvent_shouldSendEventWithCorrectKeyAndTopic() {
        // Arrange
        ShipmentCreatedEvent event = new ShipmentCreatedEvent(
                "abc-123",      // shipmentId
                "Berlin",                 // destination
                LocalDateTime.now(),      // createdAt
                "test-correlation-id"     // correlationId
        );

        // Simuliere das CompletableFuture
        CompletableFuture<SendResult<String, ShipmentCreatedEvent>> mockFuture =
                CompletableFuture.completedFuture(mock(SendResult.class));
        when(kafkaTemplate.send(anyString(), anyString(), any(ShipmentCreatedEvent.class))).thenReturn(mockFuture);

        // Act
        producer.sendShipmentCreatedEvent(event);

        // Assert: KafkaTemplate.send wurde mit den erwarteten Parametern aufgerufen
        verify(kafkaTemplate, times(1)).send(eq("shipment-created"), eq("abc-123"), eq(event));
    }
}
