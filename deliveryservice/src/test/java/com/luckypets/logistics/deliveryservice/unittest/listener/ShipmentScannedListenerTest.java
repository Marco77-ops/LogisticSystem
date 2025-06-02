package com.luckypets.logistics.deliveryservice.unittest.listener;

import com.luckypets.logistics.deliveryservice.listener.ShipmentScannedListener;
import com.luckypets.logistics.deliveryservice.persistence.ShipmentRepository;
import com.luckypets.logistics.shared.events.ShipmentDeliveredEvent;
import com.luckypets.logistics.shared.events.ShipmentScannedEvent;
import com.luckypets.logistics.shared.model.ShipmentEntity;
import com.luckypets.logistics.shared.model.ShipmentStatus;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShipmentScannedListener Unit Tests")
class ShipmentScannedListenerTest {

    @Mock
    private ShipmentRepository repository;

    @Mock
    private KafkaTemplate<String, ShipmentDeliveredEvent> kafkaTemplate;

    @InjectMocks
    private ShipmentScannedListener listener;

    private ShipmentScannedEvent event;
    private static final String SHIPMENT_ID = "SHIP-001";
    private static final String LOCATION = "WAREHOUSE_A";
    private static final String DESTINATION = "Berlin";
    private static final String DELIVERED_TOPIC = "shipment-delivered";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(listener, "deliveredTopic", DELIVERED_TOPIC);

        event = new ShipmentScannedEvent(
                SHIPMENT_ID,
                LOCATION,
                LocalDateTime.now(),
                DESTINATION,
                UUID.randomUUID().toString()
        );
    }

    @Test
    @DisplayName("Should update existing shipment when scanned")
    void onShipmentScanned_shouldUpdateExistingShipment() {
        // Given
        ShipmentEntity existingShipment = new ShipmentEntity();
        existingShipment.setShipmentId(SHIPMENT_ID);
        existingShipment.setStatus(ShipmentStatus.CREATED);
        existingShipment.setLastLocation("ORIGIN");
        existingShipment.setDestination(DESTINATION);

        when(repository.findById(SHIPMENT_ID)).thenReturn(Optional.of(existingShipment));
        when(repository.save(any(ShipmentEntity.class))).thenReturn(existingShipment);

        // When
        listener.onShipmentScanned(event);

        // Then
        ArgumentCaptor<ShipmentEntity> shipmentCaptor = ArgumentCaptor.forClass(ShipmentEntity.class);
        verify(repository).save(shipmentCaptor.capture());

        ShipmentEntity savedShipment = shipmentCaptor.getValue();
        assertEquals(SHIPMENT_ID, savedShipment.getShipmentId());
        assertEquals(LOCATION, savedShipment.getLastLocation());
        assertEquals(ShipmentStatus.IN_TRANSIT, savedShipment.getStatus());
        assertEquals(event.getScannedAt(), savedShipment.getLastScannedAt());

        // Verify no Kafka event was sent (not at destination)
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    @DisplayName("Should create new shipment when not found")
    void onShipmentScanned_shouldCreateNewShipment_whenNotFound() {
        // Given
        when(repository.findById(SHIPMENT_ID)).thenReturn(Optional.empty());
        when(repository.save(any(ShipmentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        listener.onShipmentScanned(event);

        // Then
        ArgumentCaptor<ShipmentEntity> shipmentCaptor = ArgumentCaptor.forClass(ShipmentEntity.class);
        verify(repository).save(shipmentCaptor.capture());

        ShipmentEntity savedShipment = shipmentCaptor.getValue();
        assertEquals(SHIPMENT_ID, savedShipment.getShipmentId());
        assertEquals(LOCATION, savedShipment.getLastLocation());
        assertEquals(ShipmentStatus.IN_TRANSIT, savedShipment.getStatus());
        assertEquals(DESTINATION, savedShipment.getDestination());
        assertNotNull(savedShipment.getCreatedAt());
    }

    @Test
    @DisplayName("Should mark as delivered and send event when at destination")
    void onShipmentScanned_shouldMarkAsDelivered_whenAtDestination() {
        // Given
        ShipmentScannedEvent eventAtDestination = new ShipmentScannedEvent(
                SHIPMENT_ID,
                DESTINATION,                 // location
                LocalDateTime.now(),         // scannedAt
                DESTINATION,                 // destination
                UUID.randomUUID().toString() // correlationId
        );

        ShipmentEntity existingShipment = new ShipmentEntity();
        existingShipment.setShipmentId(SHIPMENT_ID);
        existingShipment.setStatus(ShipmentStatus.IN_TRANSIT);
        existingShipment.setLastLocation("WAREHOUSE_B");
        existingShipment.setDestination(DESTINATION);

        when(repository.findById(SHIPMENT_ID)).thenReturn(Optional.of(existingShipment));
        when(repository.save(any(ShipmentEntity.class))).thenReturn(existingShipment);

        // Hier wird ein echtes RecordMetadata erzeugt:
        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition(DELIVERED_TOPIC, 1),
                42L, 0L, 0L, null, 0, 0
        );
        SendResult<String, ShipmentDeliveredEvent> sendResult = new SendResult<>(null, metadata);

        when(kafkaTemplate.send(eq(DELIVERED_TOPIC), eq(SHIPMENT_ID), any(ShipmentDeliveredEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        // When
        listener.onShipmentScanned(eventAtDestination);

        // Then
        ArgumentCaptor<ShipmentEntity> shipmentCaptor = ArgumentCaptor.forClass(ShipmentEntity.class);
        verify(repository).save(shipmentCaptor.capture());

        ShipmentEntity savedShipment = shipmentCaptor.getValue();
        assertEquals(SHIPMENT_ID, savedShipment.getShipmentId());
        assertEquals(DESTINATION, savedShipment.getLastLocation());
        assertEquals(ShipmentStatus.DELIVERED, savedShipment.getStatus());
        assertNotNull(savedShipment.getDeliveredAt());

        // Verify Kafka event was sent
        ArgumentCaptor<ShipmentDeliveredEvent> eventCaptor = ArgumentCaptor.forClass(ShipmentDeliveredEvent.class);
        verify(kafkaTemplate).send(eq(DELIVERED_TOPIC), eq(SHIPMENT_ID), eventCaptor.capture());

        ShipmentDeliveredEvent deliveredEvent = eventCaptor.getValue();
        assertEquals(SHIPMENT_ID, deliveredEvent.getShipmentId());
        assertEquals(DESTINATION, deliveredEvent.getLocation());
        assertEquals(eventAtDestination.getCorrelationId(), deliveredEvent.getCorrelationId());
    }

    @Test
    @DisplayName("Should handle Kafka send exception")
    void onShipmentScanned_shouldHandleKafkaSendException() {
        // Given
        ShipmentScannedEvent eventAtDestination = new ShipmentScannedEvent(
                SHIPMENT_ID,
                DESTINATION,
                LocalDateTime.now(),
                DESTINATION,
                UUID.randomUUID().toString()
        );

        ShipmentEntity existingShipment = new ShipmentEntity();
        existingShipment.setShipmentId(SHIPMENT_ID);
        existingShipment.setStatus(ShipmentStatus.IN_TRANSIT);
        existingShipment.setLastLocation("WAREHOUSE_B");
        existingShipment.setDestination(DESTINATION);

        when(repository.findById(SHIPMENT_ID)).thenReturn(Optional.of(existingShipment));
        when(repository.save(any(ShipmentEntity.class))).thenReturn(existingShipment);

        // Mock Kafka send with exception
        CompletableFuture<SendResult<String, ShipmentDeliveredEvent>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka send failed"));
        when(kafkaTemplate.send(eq(DELIVERED_TOPIC), eq(SHIPMENT_ID), any(ShipmentDeliveredEvent.class))).thenReturn(future);

        // When
        listener.onShipmentScanned(eventAtDestination);

        // Then
        // Verify the shipment was still saved despite Kafka error
        verify(repository).save(any(ShipmentEntity.class));
    }

    @Test
    @DisplayName("Should handle case-insensitive destination matching")
    void onShipmentScanned_shouldHandleCaseInsensitiveDestinationMatching() {
        // Given
        ShipmentScannedEvent eventAtDestination = new ShipmentScannedEvent(
                SHIPMENT_ID,
                "berlin", // location (lowercase)
                LocalDateTime.now(),
                "BERLIN", // destination (uppercase)
                UUID.randomUUID().toString()
        );

        ShipmentEntity existingShipment = new ShipmentEntity();
        existingShipment.setShipmentId(SHIPMENT_ID);
        existingShipment.setStatus(ShipmentStatus.IN_TRANSIT);
        existingShipment.setLastLocation("WAREHOUSE_B");
        existingShipment.setDestination("Berlin"); // mixed case

        when(repository.findById(SHIPMENT_ID)).thenReturn(Optional.of(existingShipment));
        when(repository.save(any(ShipmentEntity.class))).thenReturn(existingShipment);

        // Hier wieder echtes Metadata verwenden!
        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition(DELIVERED_TOPIC, 1),
                42L, 0L, 0L, null, 0, 0
        );
        SendResult<String, ShipmentDeliveredEvent> sendResult = new SendResult<>(null, metadata);

        when(kafkaTemplate.send(eq(DELIVERED_TOPIC), eq(SHIPMENT_ID), any(ShipmentDeliveredEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        // When
        listener.onShipmentScanned(eventAtDestination);

        // Then
        ArgumentCaptor<ShipmentEntity> shipmentCaptor = ArgumentCaptor.forClass(ShipmentEntity.class);
        verify(repository).save(shipmentCaptor.capture());

        ShipmentEntity savedShipment = shipmentCaptor.getValue();
        assertEquals(ShipmentStatus.DELIVERED, savedShipment.getStatus());

        // Verify Kafka event was sent
        verify(kafkaTemplate).send(eq(DELIVERED_TOPIC), eq(SHIPMENT_ID), any(ShipmentDeliveredEvent.class));
    }
}
