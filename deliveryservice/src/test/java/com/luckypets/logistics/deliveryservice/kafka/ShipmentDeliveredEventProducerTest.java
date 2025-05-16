package com.luckypets.logistics.deliveryservice.kafka;

import com.luckypets.logistics.shared.events.ShipmentDeliveredEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class ShipmentDeliveredEventProducerTest {

    @Mock
    private KafkaTemplate<String, ShipmentDeliveredEvent> kafkaTemplate;

    private ShipmentDeliveredEventProducer producer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        producer = new ShipmentDeliveredEventProducer(kafkaTemplate);

        // Mock the CompletableFuture returned by kafkaTemplate.send()
        when(kafkaTemplate.send(anyString(), anyString(), any(ShipmentDeliveredEvent.class)))
            .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));
    }

    @Test
    void testShipmentDeliveredEventIsSentToKafka() {
        // Arrange
        String topic = "shipment-delivered";
        String shipmentId = "manual-123";
        ShipmentDeliveredEvent event = new ShipmentDeliveredEvent(
            Objects.requireNonNull(shipmentId, "shipmentId darf nicht null sein"),
            "Hamburg",  // unterschiedliche Werte für bessere Testbarkeit
            "Berlin",
            LocalDateTime.now(),  // LocalDateTime statt Instant
            "test-correlation-id"
        );

        // Act – sende das Event
        producer.sendShipmentDeliveredEvent(event);

        // Assert
        // Verify that kafkaTemplate.send() was called with the correct arguments
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ShipmentDeliveredEvent> eventCaptor = ArgumentCaptor.forClass(ShipmentDeliveredEvent.class);

        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo(topic);
        assertThat(keyCaptor.getValue()).isEqualTo(shipmentId);
        assertThat(eventCaptor.getValue().getLocation()).isEqualTo("Berlin");

        System.out.println("✅ Event gesendet: " + eventCaptor.getValue());
    }
}
