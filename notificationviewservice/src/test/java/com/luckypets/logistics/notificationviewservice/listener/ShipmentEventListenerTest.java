package com.luckypets.logistics.notificationviewservice.listener;

import com.luckypets.logistics.notificationviewservice.service.NotificationService;
import com.luckypets.logistics.shared.events.ShipmentCreatedEvent;
import com.luckypets.logistics.shared.events.ShipmentDeliveredEvent;
import com.luckypets.logistics.shared.events.ShipmentScannedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShipmentEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private Acknowledgment acknowledgment;

    private ShipmentEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new ShipmentEventListener(notificationService);
    }

    @Test
    @DisplayName("Should acknowledge when ShipmentCreatedEvent is null")
    void shouldAcknowledgeWhenShipmentCreatedEventIsNull() {
        // When
        listener.handleShipmentCreated(null, "topic", 0, 0L, acknowledgment);

        // Then
        verify(acknowledgment).acknowledge();
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("Should acknowledge when shipmentId is null in ShipmentCreatedEvent")
    void shouldAcknowledgeWhenShipmentIdIsNullInShipmentCreatedEvent() {
        // Given
        ShipmentCreatedEvent event = new ShipmentCreatedEvent(
                null, "Berlin", LocalDateTime.now(), UUID.randomUUID().toString());

        // When
        listener.handleShipmentCreated(event, "topic", 0, 0L, acknowledgment);

        // Then
        verify(acknowledgment).acknowledge();
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("Should acknowledge when shipmentId is empty in ShipmentCreatedEvent")
    void shouldAcknowledgeWhenShipmentIdIsEmptyInShipmentCreatedEvent() {
        // Given
        ShipmentCreatedEvent event = new ShipmentCreatedEvent(
                "", "Berlin", LocalDateTime.now(), UUID.randomUUID().toString());

        // When
        listener.handleShipmentCreated(event, "topic", 0, 0L, acknowledgment);

        // Then
        verify(acknowledgment).acknowledge();
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("Should throw exception when service fails for ShipmentCreatedEvent")
    void shouldThrowExceptionWhenServiceFailsForShipmentCreatedEvent() {
        // Given
        ShipmentCreatedEvent event = new ShipmentCreatedEvent(
                "SHIP-123", "Berlin", LocalDateTime.now(), UUID.randomUUID().toString());

        when(notificationService.save(any())).thenThrow(new RuntimeException("Service error"));

        // When & Then
        assertThrows(RuntimeException.class, () ->
                listener.handleShipmentCreated(event, "topic", 0, 0L, acknowledgment));

        verify(notificationService).save(any());
        verifyNoInteractions(acknowledgment);
    }

    @Test
    @DisplayName("Should acknowledge when ShipmentCreatedEvent processing succeeds")
    void shouldAcknowledgeWhenShipmentCreatedEventProcessingSucceeds() {
        // Given
        ShipmentCreatedEvent event = new ShipmentCreatedEvent(
                "SHIP-123", "Berlin", LocalDateTime.now(), UUID.randomUUID().toString());

        // When
        listener.handleShipmentCreated(event, "topic", 0, 0L, acknowledgment);

        // Then
        verify(notificationService).save(any());
        verify(acknowledgment).acknowledge();
    }

    // Similar tests for ShipmentScannedEvent
    @Test
    @DisplayName("Should handle ShipmentScannedEvent correctly")
    void shouldHandleShipmentScannedEventCorrectly() {
        // Given
        ShipmentScannedEvent event = new ShipmentScannedEvent(
                "SHIP-456", "Frankfurt", LocalDateTime.now(), "Berlin", UUID.randomUUID().toString());

        // When
        listener.handleShipmentScanned(event, "topic", 0, 0L, acknowledgment);

        // Then
        verify(notificationService).save(any());
        verify(acknowledgment).acknowledge();
    }

    // Similar tests for ShipmentDeliveredEvent
    @Test
    @DisplayName("Should handle ShipmentDeliveredEvent correctly")
    void shouldHandleShipmentDeliveredEventCorrectly() {
        // Given
        ShipmentDeliveredEvent event = new ShipmentDeliveredEvent(
                "SHIP-789", "Munich", "Munich", LocalDateTime.now(), UUID.randomUUID().toString());

        // When
        listener.handleShipmentDelivered(event, "topic", 0, 0L, acknowledgment);

        // Then
        verify(notificationService).save(any());
        verify(acknowledgment).acknowledge();
    }
}