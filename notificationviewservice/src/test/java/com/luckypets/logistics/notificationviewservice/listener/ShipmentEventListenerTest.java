package com.luckypets.logistics.notificationviewservice.listener;

import com.luckypets.logistics.notificationviewservice.model.Notification;
import com.luckypets.logistics.notificationviewservice.model.NotificationType;
import com.luckypets.logistics.notificationviewservice.service.NotificationService;
import com.luckypets.logistics.shared.events.ShipmentCreatedEvent;
import com.luckypets.logistics.shared.events.ShipmentDeliveredEvent;
import com.luckypets.logistics.shared.events.ShipmentScannedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShipmentEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private Acknowledgment acknowledgment;

    private ShipmentEventListener shipmentEventListener;

    @BeforeEach
    void setUp() {
        shipmentEventListener = new ShipmentEventListener(notificationService);
    }

    @Test
    void handleShipmentDelivered_shouldCreateNotification_withCorrectMessage() {
        // Arrange
        ShipmentDeliveredEvent event = new ShipmentDeliveredEvent(
                "SHIP-123",
                "Berlin",
                "Berlin",
                LocalDateTime.now(),
                "corr-id-001"
        );

        // Act
        shipmentEventListener.handleShipmentDelivered(
                event,
                "shipment-delivered",
                0,
                0L,
                acknowledgment
        );

        // Assert
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationService).save(notificationCaptor.capture());
        verify(acknowledgment).acknowledge();

        Notification capturedNotification = notificationCaptor.getValue();
        assertThat(capturedNotification.getShipmentId()).isEqualTo("SHIP-123");
        assertThat(capturedNotification.getType()).isEqualTo(NotificationType.SHIPMENT_DELIVERED);
        assertThat(capturedNotification.getMessage()).contains("has been delivered to its destination Berlin");
    }

    @Test
    void handleShipmentDelivered_withNullEvent_shouldAcknowledgeAndNotSaveNotification() {
        // Act
        shipmentEventListener.handleShipmentDelivered(
                null,
                "shipment-delivered",
                0,
                0L,
                acknowledgment
        );

        // Assert
        verify(notificationService, never()).save(any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void handleShipmentDelivered_withEmptyShipmentId_shouldAcknowledgeAndNotSaveNotification() {
        // Arrange
        ShipmentDeliveredEvent event = new ShipmentDeliveredEvent(
                "",
                "Berlin",
                "Berlin",
                LocalDateTime.now(),
                "corr-id-001"
        );

        // Act
        shipmentEventListener.handleShipmentDelivered(
                event,
                "shipment-delivered",
                0,
                0L,
                acknowledgment
        );

        // Assert
        verify(notificationService, never()).save(any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void handleShipmentCreated_shouldCreateNotification_withCorrectMessage() {
        // Arrange
        ShipmentCreatedEvent event = new ShipmentCreatedEvent(
                "SHIP-456",
                "Munich",
                LocalDateTime.now(),
                "corr-id-002"
        );

        // Act
        shipmentEventListener.handleShipmentCreated(
                event,
                "shipment-created",
                0,
                0L,
                acknowledgment
        );

        // Assert
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationService).save(notificationCaptor.capture());
        verify(acknowledgment).acknowledge();

        Notification capturedNotification = notificationCaptor.getValue();
        assertThat(capturedNotification.getShipmentId()).isEqualTo("SHIP-456");
        assertThat(capturedNotification.getType()).isEqualTo(NotificationType.SHIPMENT_CREATED);
        assertThat(capturedNotification.getMessage()).contains("has been created with destination Munich");
    }

    @Test
    void handleShipmentScanned_shouldCreateNotification_withCorrectMessage() {
        // Arrange
        ShipmentScannedEvent event = new ShipmentScannedEvent(
                "SHIP-789",
                "Frankfurt",
                LocalDateTime.now(),
                "Berlin",
                "corr-id-003"
        );

        // Act
        shipmentEventListener.handleShipmentScanned(
                event,
                "shipment-scanned",
                0,
                0L,
                acknowledgment
        );

        // Assert
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationService).save(notificationCaptor.capture());
        verify(acknowledgment).acknowledge();

        Notification capturedNotification = notificationCaptor.getValue();
        assertThat(capturedNotification.getShipmentId()).isEqualTo("SHIP-789");
        assertThat(capturedNotification.getType()).isEqualTo(NotificationType.SHIPMENT_SCANNED);
        assertThat(capturedNotification.getMessage()).contains("has been scanned at location Frankfurt");
    }
}
