package com.luckypets.logistics.notificationviewservice.listener;

import com.luckypets.logistics.notificationviewservice.model.Notification;
import com.luckypets.logistics.notificationviewservice.model.NotificationType;
import com.luckypets.logistics.notificationviewservice.service.NotificationService;
import com.luckypets.logistics.notificationviewservice.service.ServerlessNotificationService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShipmentEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private ServerlessNotificationService serverlessNotificationService;

    @Mock
    private Acknowledgment acknowledgment;

    private ShipmentEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new ShipmentEventListener(notificationService, serverlessNotificationService);
    }

    @Test
    @DisplayName("Should acknowledge when ShipmentCreatedEvent is null")
    void shouldAcknowledgeWhenShipmentCreatedEventIsNull() {
        // When
        listener.handleShipmentCreated(null, "topic", 0, 0L, acknowledgment);

        // Then
        verify(acknowledgment).acknowledge();
        verifyNoInteractions(notificationService);
        verifyNoInteractions(serverlessNotificationService);
    }

    @Test
    @DisplayName("Should acknowledge when shipmentId is null in ShipmentCreatedEvent")
    void shouldAcknowledgeWhenShipmentIdIsNullInShipmentCreatedEvent() {
        // Given - KORRIGIERT: Nur destination, createdAt und correlationId
        ShipmentCreatedEvent event = new ShipmentCreatedEvent(
                null, "Berlin", LocalDateTime.now(), UUID.randomUUID().toString());

        // When
        listener.handleShipmentCreated(event, "topic", 0, 0L, acknowledgment);

        // Then
        verify(acknowledgment).acknowledge();
        verifyNoInteractions(notificationService);
        verifyNoInteractions(serverlessNotificationService);
    }

    @Test
    @DisplayName("Should acknowledge when shipmentId is empty in ShipmentCreatedEvent")
    void shouldAcknowledgeWhenShipmentIdIsEmptyInShipmentCreatedEvent() {
        // Given - KORRIGIERT: Nur destination, createdAt und correlationId
        ShipmentCreatedEvent event = new ShipmentCreatedEvent(
                "", "Berlin", LocalDateTime.now(), UUID.randomUUID().toString());

        // When
        listener.handleShipmentCreated(event, "topic", 0, 0L, acknowledgment);

        // Then
        verify(acknowledgment).acknowledge();
        verifyNoInteractions(notificationService);
        verifyNoInteractions(serverlessNotificationService);
    }

    @Test
    @DisplayName("Should throw exception when service fails for ShipmentCreatedEvent")
    void shouldThrowExceptionWhenServiceFailsForShipmentCreatedEvent() {
        // Given - KORRIGIERT: Nur destination, createdAt und correlationId
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
        // Given - KORRIGIERT: Nur destination, createdAt und correlationId
        ShipmentCreatedEvent event = new ShipmentCreatedEvent(
                "SHIP-123", "Berlin", LocalDateTime.now(), UUID.randomUUID().toString());

        Notification savedNotification = new Notification("SHIP-123", "Test message", NotificationType.SHIPMENT_CREATED);
        when(notificationService.save(any())).thenReturn(savedNotification);

        // When
        listener.handleShipmentCreated(event, "topic", 0, 0L, acknowledgment);

        // Then
        verify(notificationService).save(any());
        verify(serverlessNotificationService).triggerServerlessFunction(
                eq("shipment-created"), eq("SHIP-123"), eq(null), eq(null), eq("Berlin")); // KORRIGIERT
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("Should handle ShipmentScannedEvent correctly")
    void shouldHandleShipmentScannedEventCorrectly() {
        // Given - KORRIGIERT: location, scannedAt, destination, correlationId
        ShipmentScannedEvent event = new ShipmentScannedEvent(
                "SHIP-456", "Frankfurt", LocalDateTime.now(), "Berlin", UUID.randomUUID().toString());

        Notification savedNotification = new Notification("SHIP-456", "Test message", NotificationType.SHIPMENT_SCANNED);
        when(notificationService.save(any())).thenReturn(savedNotification);

        // When
        listener.handleShipmentScanned(event, "topic", 0, 0L, acknowledgment);

        // Then
        verify(notificationService).save(any());
        verify(serverlessNotificationService).triggerServerlessFunction(
                eq("shipment-scanned"), eq("SHIP-456"), eq(null), eq("Frankfurt"), eq("Berlin")); // KORRIGIERT
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("Should handle ShipmentDeliveredEvent correctly")
    void shouldHandleShipmentDeliveredEventCorrectly() {
        // Given - KORRIGIERT: destination, location, deliveredAt, correlationId
        ShipmentDeliveredEvent event = new ShipmentDeliveredEvent(
                "SHIP-789", "Munich", "Munich", LocalDateTime.now(), UUID.randomUUID().toString());

        Notification savedNotification = new Notification("SHIP-789", "Test message", NotificationType.SHIPMENT_DELIVERED);
        when(notificationService.save(any())).thenReturn(savedNotification);

        // When
        listener.handleShipmentDelivered(event, "topic", 0, 0L, acknowledgment);

        // Then
        verify(notificationService).save(any());
        verify(serverlessNotificationService).triggerServerlessFunction(
                eq("shipment-delivered"), eq("SHIP-789"), eq(null), eq(null), eq("Munich")); // KORRIGIERT: getLocation()
        verify(acknowledgment).acknowledge();
    }

    // Weitere Tests für null/empty shipmentId in anderen Events...

    @Test
    @DisplayName("Should acknowledge when shipmentId is null in ShipmentScannedEvent")
    void shouldAcknowledgeWhenShipmentIdIsNullInShipmentScannedEvent() {
        // Given
        ShipmentScannedEvent event = new ShipmentScannedEvent(
                null, "Frankfurt", LocalDateTime.now(), "Berlin", UUID.randomUUID().toString());

        // When
        listener.handleShipmentScanned(event, "topic", 0, 0L, acknowledgment);

        // Then
        verify(acknowledgment).acknowledge();
        verifyNoInteractions(notificationService);
        verifyNoInteractions(serverlessNotificationService);
    }

    @Test
    @DisplayName("Should acknowledge when shipmentId is null in ShipmentDeliveredEvent")
    void shouldAcknowledgeWhenShipmentIdIsNullInShipmentDeliveredEvent() {
        // Given
        ShipmentDeliveredEvent event = new ShipmentDeliveredEvent(
                null, "Munich", "Munich", LocalDateTime.now(), UUID.randomUUID().toString());

        // When
        listener.handleShipmentDelivered(event, "topic", 0, 0L, acknowledgment);

        // Then
        verify(acknowledgment).acknowledge();
        verifyNoInteractions(notificationService);
        verifyNoInteractions(serverlessNotificationService);
    }

    @Test
    @DisplayName("Should throw exception when service fails for ShipmentScannedEvent")
    void shouldThrowExceptionWhenServiceFailsForShipmentScannedEvent() {
        // Given
        ShipmentScannedEvent event = new ShipmentScannedEvent(
                "SHIP-456", "Frankfurt", LocalDateTime.now(), "Berlin", UUID.randomUUID().toString());

        when(notificationService.save(any())).thenThrow(new RuntimeException("Service error"));

        // When & Then
        assertThrows(RuntimeException.class, () ->
                listener.handleShipmentScanned(event, "topic", 0, 0L, acknowledgment));

        verify(notificationService).save(any());
        verifyNoInteractions(acknowledgment);
    }

    @Test
    @DisplayName("Should throw exception when service fails for ShipmentDeliveredEvent")
    void shouldThrowExceptionWhenServiceFailsForShipmentDeliveredEvent() {
        // Given
        ShipmentDeliveredEvent event = new ShipmentDeliveredEvent(
                "SHIP-789", "Munich", "Munich", LocalDateTime.now(), UUID.randomUUID().toString());

        when(notificationService.save(any())).thenThrow(new RuntimeException("Service error"));

        // When & Then
        assertThrows(RuntimeException.class, () ->
                listener.handleShipmentDelivered(event, "topic", 0, 0L, acknowledgment));

        verify(notificationService).save(any());
        verifyNoInteractions(acknowledgment);
    }
}