package com.luckypets.logistics.notificationservice.listener;

import com.luckypets.logistics.notificationservice.model.Notification;
import com.luckypets.logistics.notificationservice.model.NotificationType;
import com.luckypets.logistics.notificationservice.repository.NotificationRepository;
import com.luckypets.logistics.shared.events.ShipmentCreatedEvent;
import com.luckypets.logistics.shared.events.ShipmentScannedEvent;
import com.luckypets.logistics.shared.events.ShipmentDeliveredEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ShipmentEventListenerIntegrationTest {

    private NotificationRepository repository;
    private ShipmentEventListener listener;
    
    private ShipmentCreatedEvent createdEvent;
    private ShipmentScannedEvent scannedEvent;
    private ShipmentDeliveredEvent deliveredEvent;
    
    @BeforeEach
    void setUp() {
        repository = new NotificationRepository();
        listener = new ShipmentEventListener(repository);
        
        // Create events with constructor parameters
        createdEvent = new ShipmentCreatedEvent(
                "test-shipment-id",
                "Test Destination",
                LocalDateTime.now(),
                "test-correlation-id"
        );

        scannedEvent = new ShipmentScannedEvent(
                "test-shipment-id",
                "Test Location",
                LocalDateTime.now(),
                "Test Destination",
                "test-correlation-id"
        );

        deliveredEvent = new ShipmentDeliveredEvent(
                "test-shipment-id",
                "Test Destination",
                "Test Delivery Location",
                null,
                "test-correlation-id"
        );
    }
    
    @Test
    void handleShipmentCreated_shouldSaveNotification() {
        listener.handleShipmentCreated(createdEvent);
        
        List<Notification> notifications = repository.findByShipmentId(createdEvent.getShipmentId());
        assertEquals(1, notifications.size());
        
        Notification savedNotification = notifications.get(0);
        assertEquals(createdEvent.getShipmentId(), savedNotification.getShipmentId());
        assertEquals(NotificationType.SHIPMENT_CREATED, savedNotification.getType());
        assertEquals(String.format("Shipment %s has been created with destination %s", 
                createdEvent.getShipmentId(), createdEvent.getDestination()), 
                savedNotification.getMessage());
    }
    
    @Test
    void handleShipmentScanned_shouldSaveNotification() {
        listener.handleShipmentScanned(scannedEvent);
        
        List<Notification> notifications = repository.findByShipmentId(scannedEvent.getShipmentId());
        assertEquals(1, notifications.size());
        
        Notification savedNotification = notifications.get(0);
        assertEquals(scannedEvent.getShipmentId(), savedNotification.getShipmentId());
        assertEquals(NotificationType.SHIPMENT_SCANNED, savedNotification.getType());
        assertEquals(String.format("Shipment %s has been scanned at location %s", 
                scannedEvent.getShipmentId(), scannedEvent.getLocation()), 
                savedNotification.getMessage());
    }
    
    @Test
    void handleShipmentDelivered_shouldSaveNotification() {
        listener.handleShipmentDelivered(deliveredEvent);
        
        List<Notification> notifications = repository.findByShipmentId(deliveredEvent.getShipmentId());
        assertEquals(1, notifications.size());
        
        Notification savedNotification = notifications.get(0);
        assertEquals(deliveredEvent.getShipmentId(), savedNotification.getShipmentId());
        assertEquals(NotificationType.SHIPMENT_DELIVERED, savedNotification.getType());
        assertEquals(String.format("Shipment %s has been delivered to %s", 
                deliveredEvent.getShipmentId(), deliveredEvent.getLocation()), 
                savedNotification.getMessage());
    }
}