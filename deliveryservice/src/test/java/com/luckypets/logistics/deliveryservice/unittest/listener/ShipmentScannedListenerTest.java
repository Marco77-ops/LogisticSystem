package com.luckypets.logistics.deliveryservice.unittest.listener;

import com.luckypets.logistics.deliveryservice.listener.ShipmentScannedListener;
import com.luckypets.logistics.deliveryservice.model.ShipmentEntity;
import com.luckypets.logistics.deliveryservice.service.DeliveryServiceImpl;
import com.luckypets.logistics.shared.model.ShipmentStatus;
import com.luckypets.logistics.shared.events.ShipmentDeliveredEvent;
import com.luckypets.logistics.shared.events.ShipmentScannedEvent;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
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
    private DeliveryServiceImpl deliveryService;

    @Mock
    private KafkaTemplate<String, ShipmentDeliveredEvent> kafkaTemplate;

    @Mock
    private Acknowledgment acknowledgment;

    private ShipmentScannedListener listener;

    private ShipmentScannedEvent event;
    private static final String SHIPMENT_ID = "SHIP-001";
    private static final String LOCATION = "WAREHOUSE_A";
    private static final String DESTINATION = "Berlin";
    private static final String DELIVERED_TOPIC = "shipment-delivered";

    @BeforeEach
    void setUp() {
        listener = new ShipmentScannedListener(deliveryService, kafkaTemplate);
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
        existingShipment.setCreatedAt(LocalDateTime.now().minusDays(2));

        when(deliveryService.findShipmentEntityById(SHIPMENT_ID)).thenReturn(Optional.of(existingShipment));

        // When
        listener.onShipmentScanned(event, acknowledgment);

        // Then
        ArgumentCaptor<ShipmentEntity> shipmentCaptor = ArgumentCaptor.forClass(ShipmentEntity.class);
        verify(deliveryService).updateShipmentState(shipmentCaptor.capture());

        ShipmentEntity updatedShipment = shipmentCaptor.getValue();
        assertEquals(SHIPMENT_ID, updatedShipment.getShipmentId());
        assertEquals(LOCATION, updatedShipment.getLastLocation());
        assertEquals(ShipmentStatus.IN_TRANSIT, updatedShipment.getStatus());
        assertEquals(event.getScannedAt(), updatedShipment.getLastScannedAt());

        verify(kafkaTemplate, never()).send(any(), any(), any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("Should create new shipment when not found")
    void onShipmentScanned_shouldCreateNewShipment_whenNotFound() {
        // Given
        when(deliveryService.findShipmentEntityById(SHIPMENT_ID)).thenReturn(Optional.empty());

        // When
        listener.onShipmentScanned(event, acknowledgment);

        // Then
        ArgumentCaptor<ShipmentEntity> shipmentCaptor = ArgumentCaptor.forClass(ShipmentEntity.class);
        verify(deliveryService).updateShipmentState(shipmentCaptor.capture());

        ShipmentEntity newShipment = shipmentCaptor.getValue();
        assertEquals(SHIPMENT_ID, newShipment.getShipmentId());
        assertEquals(LOCATION, newShipment.getLastLocation());
        assertEquals(ShipmentStatus.IN_TRANSIT, newShipment.getStatus());
        assertEquals(DESTINATION, newShipment.getDestination());
        assertNotNull(newShipment.getCreatedAt());
        assertNotNull(newShipment.getLastScannedAt());

        verify(kafkaTemplate, never()).send(any(), any(), any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("Should mark as delivered and send event when at destination")
    void onShipmentScanned_shouldMarkAsDelivered_whenAtDestination() {
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
        existingShipment.setCreatedAt(LocalDateTime.now().minusDays(2));

        when(deliveryService.findShipmentEntityById(SHIPMENT_ID)).thenReturn(Optional.of(existingShipment));

        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition(DELIVERED_TOPIC, 1),
                42L, 0L, 0L, null, 0, 0
        );
        SendResult<String, ShipmentDeliveredEvent> sendResult = new SendResult<>(null, metadata);
        when(kafkaTemplate.send(eq(DELIVERED_TOPIC), eq(SHIPMENT_ID), any(ShipmentDeliveredEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        // When
        listener.onShipmentScanned(eventAtDestination, acknowledgment);

        // Then
        ArgumentCaptor<ShipmentEntity> shipmentCaptor = ArgumentCaptor.forClass(ShipmentEntity.class);
        verify(deliveryService).updateShipmentState(shipmentCaptor.capture());

        ShipmentEntity updatedShipment = shipmentCaptor.getValue();
        assertEquals(SHIPMENT_ID, updatedShipment.getShipmentId());
        assertEquals(DESTINATION, updatedShipment.getLastLocation());
        assertEquals(ShipmentStatus.DELIVERED, updatedShipment.getStatus());
        assertNotNull(updatedShipment.getDeliveredAt());

        ArgumentCaptor<ShipmentDeliveredEvent> eventCaptor = ArgumentCaptor.forClass(ShipmentDeliveredEvent.class);
        verify(kafkaTemplate).send(eq(DELIVERED_TOPIC), eq(SHIPMENT_ID), eventCaptor.capture());

        ShipmentDeliveredEvent deliveredEvent = eventCaptor.getValue();
        assertEquals(SHIPMENT_ID, deliveredEvent.getShipmentId());
        assertEquals(DESTINATION, deliveredEvent.getLocation());
        assertEquals(eventAtDestination.getCorrelationId(), deliveredEvent.getCorrelationId());
        assertEquals(DESTINATION, deliveredEvent.getDestination());
        assertNotNull(deliveredEvent.getDeliveredAt());

        verify(acknowledgment).acknowledge();
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
        existingShipment.setCreatedAt(LocalDateTime.now().minusDays(2));

        when(deliveryService.findShipmentEntityById(SHIPMENT_ID)).thenReturn(Optional.of(existingShipment));

        CompletableFuture<SendResult<String, ShipmentDeliveredEvent>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka send failed"));
        when(kafkaTemplate.send(eq(DELIVERED_TOPIC), eq(SHIPMENT_ID), any(ShipmentDeliveredEvent.class))).thenReturn(future);

        // When
        listener.onShipmentScanned(eventAtDestination, acknowledgment);

        // Then
        verify(deliveryService).updateShipmentState(any(ShipmentEntity.class));
        verify(kafkaTemplate).send(eq(DELIVERED_TOPIC), eq(SHIPMENT_ID), any(ShipmentDeliveredEvent.class));
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("Should handle case-insensitive destination matching")
    void onShipmentScanned_shouldHandleCaseInsensitiveDestinationMatching() {
        // Given
        ShipmentScannedEvent eventAtDestination = new ShipmentScannedEvent(
                SHIPMENT_ID,
                "berlin", // lowercase
                LocalDateTime.now(),
                "BERLIN", // uppercase
                UUID.randomUUID().toString()
        );

        ShipmentEntity existingShipment = new ShipmentEntity();
        existingShipment.setShipmentId(SHIPMENT_ID);
        existingShipment.setStatus(ShipmentStatus.IN_TRANSIT);
        existingShipment.setLastLocation("WAREHOUSE_B");
        existingShipment.setDestination("Berlin"); // mixed case
        existingShipment.setCreatedAt(LocalDateTime.now().minusDays(2));

        when(deliveryService.findShipmentEntityById(SHIPMENT_ID)).thenReturn(Optional.of(existingShipment));

        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition(DELIVERED_TOPIC, 1),
                42L, 0L, 0L, null, 0, 0
        );
        SendResult<String, ShipmentDeliveredEvent> sendResult = new SendResult<>(null, metadata);
        when(kafkaTemplate.send(eq(DELIVERED_TOPIC), eq(SHIPMENT_ID), any(ShipmentDeliveredEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        // When
        listener.onShipmentScanned(eventAtDestination, acknowledgment);

        // Then
        ArgumentCaptor<ShipmentEntity> shipmentCaptor = ArgumentCaptor.forClass(ShipmentEntity.class);
        verify(deliveryService).updateShipmentState(shipmentCaptor.capture());
        ShipmentEntity updatedShipment = shipmentCaptor.getValue();
        assertEquals(ShipmentStatus.DELIVERED, updatedShipment.getStatus());

        verify(kafkaTemplate).send(eq(DELIVERED_TOPIC), eq(SHIPMENT_ID), any(ShipmentDeliveredEvent.class));
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("Should not change status if already DELIVERED, but still update location/scan time")
    void onShipmentScanned_shouldNotChangeStatusIfAlreadyDelivered() {
        // Given
        ShipmentScannedEvent eventAlreadyDelivered = new ShipmentScannedEvent(
                SHIPMENT_ID,
                "Somewhere Else",
                LocalDateTime.now(),
                DESTINATION,
                UUID.randomUUID().toString()
        );

        ShipmentEntity existingDeliveredShipment = new ShipmentEntity();
        existingDeliveredShipment.setShipmentId(SHIPMENT_ID);
        existingDeliveredShipment.setStatus(ShipmentStatus.DELIVERED);
        existingDeliveredShipment.setLastLocation(DESTINATION);
        existingDeliveredShipment.setDeliveredAt(LocalDateTime.now().minusHours(1));
        existingDeliveredShipment.setDestination(DESTINATION);
        existingDeliveredShipment.setCreatedAt(LocalDateTime.now().minusDays(5));

        when(deliveryService.findShipmentEntityById(SHIPMENT_ID)).thenReturn(Optional.of(existingDeliveredShipment));

        // When
        listener.onShipmentScanned(eventAlreadyDelivered, acknowledgment);

        // Then
        ArgumentCaptor<ShipmentEntity> shipmentCaptor = ArgumentCaptor.forClass(ShipmentEntity.class);
        verify(deliveryService).updateShipmentState(shipmentCaptor.capture());

        ShipmentEntity updatedShipment = shipmentCaptor.getValue();
        assertEquals(ShipmentStatus.DELIVERED, updatedShipment.getStatus());
        assertEquals("Somewhere Else", updatedShipment.getLastLocation());
        assertNotNull(updatedShipment.getLastScannedAt());

        verify(kafkaTemplate, never()).send(eq(DELIVERED_TOPIC), any(), any(ShipmentDeliveredEvent.class));
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("Should send delivered event if already IN_TRANSIT and arrives at destination")
    void onShipmentScanned_inTransitToDelivered() {
        // Given
        ShipmentScannedEvent eventAtDestination = new ShipmentScannedEvent(
                SHIPMENT_ID,
                DESTINATION,
                LocalDateTime.now(),
                DESTINATION,
                UUID.randomUUID().toString()
        );

        ShipmentEntity existingInTransitShipment = new ShipmentEntity();
        existingInTransitShipment.setShipmentId(SHIPMENT_ID);
        existingInTransitShipment.setStatus(ShipmentStatus.IN_TRANSIT);
        existingInTransitShipment.setLastLocation("Midway");
        existingInTransitShipment.setDestination(DESTINATION);
        existingInTransitShipment.setCreatedAt(LocalDateTime.now().minusDays(2));

        when(deliveryService.findShipmentEntityById(SHIPMENT_ID)).thenReturn(Optional.of(existingInTransitShipment));

        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition(DELIVERED_TOPIC, 1),
                42L, 0L, 0L, null, 0, 0
        );
        SendResult<String, ShipmentDeliveredEvent> sendResult = new SendResult<>(null, metadata);
        when(kafkaTemplate.send(eq(DELIVERED_TOPIC), eq(SHIPMENT_ID), any(ShipmentDeliveredEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        // When
        listener.onShipmentScanned(eventAtDestination, acknowledgment);

        // Then
        ArgumentCaptor<ShipmentEntity> shipmentCaptor = ArgumentCaptor.forClass(ShipmentEntity.class);
        verify(deliveryService).updateShipmentState(shipmentCaptor.capture());
        ShipmentEntity updatedShipment = shipmentCaptor.getValue();

        assertEquals(ShipmentStatus.DELIVERED, updatedShipment.getStatus());
        verify(kafkaTemplate).send(eq(DELIVERED_TOPIC), eq(SHIPMENT_ID), any(ShipmentDeliveredEvent.class));
        verify(acknowledgment).acknowledge();
    }
}