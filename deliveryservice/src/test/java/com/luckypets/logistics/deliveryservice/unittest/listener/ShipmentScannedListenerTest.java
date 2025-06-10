package com.luckypets.logistics.deliveryservice.unittest.listener;

import com.luckypets.logistics.deliveryservice.listener.ShipmentScannedListener;
import com.luckypets.logistics.deliveryservice.model.ShipmentEntity;
import com.luckypets.logistics.deliveryservice.service.DeliveryServiceImpl; // Import the concrete service
import com.luckypets.logistics.shared.model.ShipmentStatus;
import com.luckypets.logistics.shared.events.ShipmentDeliveredEvent;
import com.luckypets.logistics.shared.events.ShipmentScannedEvent;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
// Removed @ExtendWith(MockitoExtension.class) and @InjectMocks as we are manually instantiating
// and managing the listener and its dependencies for in-memory state.
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito; // Import Mockito class for static mock method
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

@DisplayName("ShipmentScannedListener Unit Tests")
class ShipmentScannedListenerTest {

    // Removed @Mock private ShipmentRepository repository;
    // Now we mock DeliveryServiceImpl
    @Mock
    private DeliveryServiceImpl deliveryService;

    @Mock
    private KafkaTemplate<String, ShipmentDeliveredEvent> kafkaTemplate;

    // Manually instantiate listener in setUp
    private ShipmentScannedListener listener;

    private ShipmentScannedEvent event;
    private static final String SHIPMENT_ID = "SHIP-001";
    private static final String LOCATION = "WAREHOUSE_A";
    private static final String DESTINATION = "Berlin";
    private static final String DELIVERED_TOPIC = "shipment-delivered";

    @BeforeEach
    void setUp() {
        // Initialize mocks
        deliveryService = Mockito.mock(DeliveryServiceImpl.class);
        kafkaTemplate = Mockito.mock(KafkaTemplate.class);

        // Manually instantiate the listener with the mocks
        listener = new ShipmentScannedListener(deliveryService, kafkaTemplate);

        // Set the deliveredTopic using ReflectionTestUtils (since it's @Value injected)
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
        existingShipment.setStatus(ShipmentStatus.CREATED); // Start as CREATED
        existingShipment.setLastLocation("ORIGIN");
        existingShipment.setDestination(DESTINATION);
        existingShipment.setCreatedAt(LocalDateTime.now().minusDays(2)); // Set a creation date

        // Mock the behavior of deliveryService.findShipmentEntityById
        when(deliveryService.findShipmentEntityById(SHIPMENT_ID)).thenReturn(Optional.of(existingShipment));

        // When
        listener.onShipmentScanned(event);

        // Then
        // Verify that deliveryService.updateShipmentState was called
        ArgumentCaptor<ShipmentEntity> shipmentCaptor = ArgumentCaptor.forClass(ShipmentEntity.class);
        verify(deliveryService).updateShipmentState(shipmentCaptor.capture());

        ShipmentEntity updatedShipment = shipmentCaptor.getValue();
        assertEquals(SHIPMENT_ID, updatedShipment.getShipmentId());
        assertEquals(LOCATION, updatedShipment.getLastLocation());
        assertEquals(ShipmentStatus.IN_TRANSIT, updatedShipment.getStatus()); // Should transition to IN_TRANSIT
        assertEquals(event.getScannedAt(), updatedShipment.getLastScannedAt());

        // Verify no Kafka delivered event was sent (not at destination)
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    @DisplayName("Should create new shipment when not found")
    void onShipmentScanned_shouldCreateNewShipment_whenNotFound() {
        // Given
        when(deliveryService.findShipmentEntityById(SHIPMENT_ID)).thenReturn(Optional.empty());

        // When
        listener.onShipmentScanned(event);

        // Then
        // Verify that deliveryService.updateShipmentState was called with a new entity
        ArgumentCaptor<ShipmentEntity> shipmentCaptor = ArgumentCaptor.forClass(ShipmentEntity.class);
        verify(deliveryService).updateShipmentState(shipmentCaptor.capture());

        ShipmentEntity newShipment = shipmentCaptor.getValue();
        assertEquals(SHIPMENT_ID, newShipment.getShipmentId());
        assertEquals(LOCATION, newShipment.getLastLocation());
        assertEquals(ShipmentStatus.IN_TRANSIT, newShipment.getStatus()); // New shipment also goes IN_TRANSIT
        assertEquals(DESTINATION, newShipment.getDestination());
        assertNotNull(newShipment.getCreatedAt()); // CreatedAt should be set
        assertNotNull(newShipment.getLastScannedAt()); // LastScannedAt should be set from event

        // Verify no Kafka delivered event was sent (not at destination)
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    @DisplayName("Should mark as delivered and send event when at destination")
    void onShipmentScanned_shouldMarkAsDelivered_whenAtDestination() {
        // Given
        ShipmentScannedEvent eventAtDestination = new ShipmentScannedEvent(
                SHIPMENT_ID,
                DESTINATION,                 // location is same as destination
                LocalDateTime.now(),         // scannedAt
                DESTINATION,                 // destination
                UUID.randomUUID().toString() // correlationId
        );

        ShipmentEntity existingShipment = new ShipmentEntity();
        existingShipment.setShipmentId(SHIPMENT_ID);
        existingShipment.setStatus(ShipmentStatus.IN_TRANSIT); // Can be IN_TRANSIT or CREATED
        existingShipment.setLastLocation("WAREHOUSE_B");
        existingShipment.setDestination(DESTINATION);
        existingShipment.setCreatedAt(LocalDateTime.now().minusDays(2));

        when(deliveryService.findShipmentEntityById(SHIPMENT_ID)).thenReturn(Optional.of(existingShipment));

        // Mock the KafkaTemplate.send to return a completed future with a SendResult
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
        // Verify that deliveryService.updateShipmentState was called
        ArgumentCaptor<ShipmentEntity> shipmentCaptor = ArgumentCaptor.forClass(ShipmentEntity.class);
        verify(deliveryService).updateShipmentState(shipmentCaptor.capture());

        ShipmentEntity updatedShipment = shipmentCaptor.getValue();
        assertEquals(SHIPMENT_ID, updatedShipment.getShipmentId());
        assertEquals(DESTINATION, updatedShipment.getLastLocation());
        assertEquals(ShipmentStatus.DELIVERED, updatedShipment.getStatus()); // Should transition to DELIVERED
        assertNotNull(updatedShipment.getDeliveredAt()); // DeliveredAt should be set

        // Verify Kafka delivered event was sent
        ArgumentCaptor<ShipmentDeliveredEvent> eventCaptor = ArgumentCaptor.forClass(ShipmentDeliveredEvent.class);
        verify(kafkaTemplate).send(eq(DELIVERED_TOPIC), eq(SHIPMENT_ID), eventCaptor.capture());

        ShipmentDeliveredEvent deliveredEvent = eventCaptor.getValue();
        assertEquals(SHIPMENT_ID, deliveredEvent.getShipmentId());
        assertEquals(DESTINATION, deliveredEvent.getLocation());
        assertEquals(eventAtDestination.getCorrelationId(), deliveredEvent.getCorrelationId());
        assertEquals(DESTINATION, deliveredEvent.getDestination());
        assertNotNull(deliveredEvent.getDeliveredAt());
    }

    @Test
    @DisplayName("Should handle Kafka send exception")
    void onShipmentScanned_shouldHandleKafkaSendException() {
        // Given
        ShipmentScannedEvent eventAtDestination = new ShipmentScannedEvent(
                SHIPMENT_ID,
                DESTINATION, // location is same as destination
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

        // Mock Kafka send with exception
        CompletableFuture<SendResult<String, ShipmentDeliveredEvent>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka send failed"));
        when(kafkaTemplate.send(eq(DELIVERED_TOPIC), eq(SHIPMENT_ID), any(ShipmentDeliveredEvent.class))).thenReturn(future);

        // When
        listener.onShipmentScanned(eventAtDestination);

        // Then
        // Verify the shipment was still updated in the service despite Kafka error
        verify(deliveryService).updateShipmentState(any(ShipmentEntity.class));
        // Verify that the Kafka send method was indeed called (even if it threw an exception)
        verify(kafkaTemplate).send(eq(DELIVERED_TOPIC), eq(SHIPMENT_ID), any(ShipmentDeliveredEvent.class));
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
        existingShipment.setDestination("Berlin"); // mixed case, to be matched case-insensitively
        existingShipment.setCreatedAt(LocalDateTime.now().minusDays(2));


        when(deliveryService.findShipmentEntityById(SHIPMENT_ID)).thenReturn(Optional.of(existingShipment));

        // Mock the KafkaTemplate.send to return a completed future with a SendResult
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
        // Verify the shipment was updated to DELIVERED status
        ArgumentCaptor<ShipmentEntity> shipmentCaptor = ArgumentCaptor.forClass(ShipmentEntity.class);
        verify(deliveryService).updateShipmentState(shipmentCaptor.capture());
        ShipmentEntity updatedShipment = shipmentCaptor.getValue();
        assertEquals(ShipmentStatus.DELIVERED, updatedShipment.getStatus());

        // Verify Kafka delivered event was sent
        verify(kafkaTemplate).send(eq(DELIVERED_TOPIC), eq(SHIPMENT_ID), any(ShipmentDeliveredEvent.class));
    }

    @Test
    @DisplayName("Should not change status if already DELIVERED, but still update location/scan time")
    void onShipmentScanned_shouldNotChangeStatusIfAlreadyDelivered() {
        // Given
        ShipmentScannedEvent eventAlreadyDelivered = new ShipmentScannedEvent(
                SHIPMENT_ID,
                "Somewhere Else", // New location after delivery
                LocalDateTime.now(),
                DESTINATION,
                UUID.randomUUID().toString()
        );

        ShipmentEntity existingDeliveredShipment = new ShipmentEntity();
        existingDeliveredShipment.setShipmentId(SHIPMENT_ID);
        existingDeliveredShipment.setStatus(ShipmentStatus.DELIVERED); // Already delivered
        existingDeliveredShipment.setLastLocation(DESTINATION); // Previous delivered location
        existingDeliveredShipment.setDeliveredAt(LocalDateTime.now().minusHours(1));
        existingDeliveredShipment.setDestination(DESTINATION);
        existingDeliveredShipment.setCreatedAt(LocalDateTime.now().minusDays(5));

        when(deliveryService.findShipmentEntityById(SHIPMENT_ID)).thenReturn(Optional.of(existingDeliveredShipment));

        // When
        listener.onShipmentScanned(eventAlreadyDelivered);

        // Then
        // Verify that deliveryService.updateShipmentState was called
        ArgumentCaptor<ShipmentEntity> shipmentCaptor = ArgumentCaptor.forClass(ShipmentEntity.class);
        verify(deliveryService).updateShipmentState(shipmentCaptor.capture());

        ShipmentEntity updatedShipment = shipmentCaptor.getValue();
        assertEquals(ShipmentStatus.DELIVERED, updatedShipment.getStatus()); // Status should remain DELIVERED
        assertEquals("Somewhere Else", updatedShipment.getLastLocation()); // Location should still update
        assertNotNull(updatedShipment.getLastScannedAt()); // Scan time should still update

        // Since it was already delivered and this scan is not at destination (implicitly),
        // no *new* ShipmentDeliveredEvent should be sent.
        verify(kafkaTemplate, never()).send(eq(DELIVERED_TOPIC), any(), any(ShipmentDeliveredEvent.class));
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
        listener.onShipmentScanned(eventAtDestination);

        // Then
        ArgumentCaptor<ShipmentEntity> shipmentCaptor = ArgumentCaptor.forClass(ShipmentEntity.class);
        verify(deliveryService).updateShipmentState(shipmentCaptor.capture());
        ShipmentEntity updatedShipment = shipmentCaptor.getValue();

        assertEquals(ShipmentStatus.DELIVERED, updatedShipment.getStatus());
        verify(kafkaTemplate).send(eq(DELIVERED_TOPIC), eq(SHIPMENT_ID), any(ShipmentDeliveredEvent.class));
    }
}
