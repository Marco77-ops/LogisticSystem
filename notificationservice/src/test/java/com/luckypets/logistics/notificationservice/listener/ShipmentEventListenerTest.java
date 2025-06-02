package com.luckypets.logistics.notificationservice.listener;

import com.luckypets.logistics.notificationservice.model.Notification;
import com.luckypets.logistics.notificationservice.model.NotificationType;
import com.luckypets.logistics.notificationservice.repository.NotificationRepository;
import com.luckypets.logistics.shared.events.ShipmentCreatedEvent;
import com.luckypets.logistics.shared.events.ShipmentScannedEvent;
import com.luckypets.logistics.shared.events.ShipmentDeliveredEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the ShipmentEventListener class.
 * 
 * These tests verify that the listener correctly processes shipment events
 * and creates appropriate notifications in the repository.
 */
@ExtendWith(MockitoExtension.class)
class ShipmentEventListenerTest {

    @Mock
    private NotificationRepository repository;

    private ShipmentEventListener listener;
    
    private ShipmentCreatedEvent createdEvent;
    private ShipmentScannedEvent scannedEvent;
    private ShipmentDeliveredEvent deliveredEvent;
    
    @BeforeEach
    void setUp() {
        listener = new ShipmentEventListener(repository);
        
        // Create test events
        createdEvent = new ShipmentCreatedEvent(
                "SHIP-123",
                "Berlin, Germany",
                LocalDateTime.now(),
                "corr-id-123"
        );

        scannedEvent = new ShipmentScannedEvent(
                "SHIP-123",
                "Hamburg, Germany",
                LocalDateTime.now(),
                "Berlin, Germany",
                "corr-id-456"
        );

        deliveredEvent = new ShipmentDeliveredEvent(
                "SHIP-123",
                "Berlin, Germany",
                "Berlin, Germany",
                LocalDateTime.now(),
                "corr-id-789"
        );
    }
    
    @Test
    @DisplayName("handleShipmentCreated_WithValidEvent_SavesNotification")
    void handleShipmentCreated_WithValidEvent_SavesNotification() {
        // Arrange
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        
        // Act
        listener.handleShipmentCreated(createdEvent);
        
        // Assert
        verify(repository).save(notificationCaptor.capture());
        
        Notification savedNotification = notificationCaptor.getValue();
        assertEquals("SHIP-123", savedNotification.getShipmentId());
        assertEquals(NotificationType.SHIPMENT_CREATED, savedNotification.getType());
        assertEquals("Shipment SHIP-123 has been created with destination Berlin, Germany", 
                savedNotification.getMessage());
    }
    
    @Test
    @DisplayName("handleShipmentScanned_WithValidEvent_SavesNotification")
    void handleShipmentScanned_WithValidEvent_SavesNotification() {
        // Arrange
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        
        // Act
        listener.handleShipmentScanned(scannedEvent);
        
        // Assert
        verify(repository).save(notificationCaptor.capture());
        
        Notification savedNotification = notificationCaptor.getValue();
        assertEquals("SHIP-123", savedNotification.getShipmentId());
        assertEquals(NotificationType.SHIPMENT_SCANNED, savedNotification.getType());
        assertEquals("Shipment SHIP-123 has been scanned at location Hamburg, Germany", 
                savedNotification.getMessage());
    }
    
    @Test
    @DisplayName("handleShipmentDelivered_WithValidEvent_SavesNotification")
    void handleShipmentDelivered_WithValidEvent_SavesNotification() {
        // Arrange
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        
        // Act
        listener.handleShipmentDelivered(deliveredEvent);
        
        // Assert
        verify(repository).save(notificationCaptor.capture());
        
        Notification savedNotification = notificationCaptor.getValue();
        assertEquals("SHIP-123", savedNotification.getShipmentId());
        assertEquals(NotificationType.SHIPMENT_DELIVERED, savedNotification.getType());
        assertEquals("Shipment SHIP-123 has been delivered to Berlin, Germany", 
                savedNotification.getMessage());
    }
    
    @Test
    @DisplayName("handleMultipleEvents_SavesMultipleNotifications")
    void handleMultipleEvents_SavesMultipleNotifications() {
        // Arrange
        
        // Act
        listener.handleShipmentCreated(createdEvent);
        listener.handleShipmentScanned(scannedEvent);
        listener.handleShipmentDelivered(deliveredEvent);
        
        // Assert
        verify(repository, times(3)).save(any(Notification.class));
    }
}