package com.luckypets.logistics.notificationservice.repository;

import com.luckypets.logistics.notificationservice.model.Notification;
import com.luckypets.logistics.notificationservice.model.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class NotificationRepositoryTest {

    private NotificationRepository repository;
    private Notification testNotification;

    @BeforeEach
    void setUp() {
        repository = new NotificationRepository();
        testNotification = new Notification("test-shipment-id", "Test message", NotificationType.SHIPMENT_CREATED);
    }

    @Test
    void save_shouldStoreNotification() {
        Notification saved = repository.save(testNotification);
        
        assertEquals(testNotification.getId(), saved.getId());
        assertEquals(testNotification.getMessage(), saved.getMessage());
        assertEquals(testNotification.getShipmentId(), saved.getShipmentId());
        assertEquals(testNotification.getType(), saved.getType());
    }

    @Test
    void findById_shouldReturnNotification_whenExists() {
        repository.save(testNotification);
        
        Optional<Notification> found = repository.findById(testNotification.getId());
        
        assertTrue(found.isPresent());
        assertEquals(testNotification.getId(), found.get().getId());
    }

    @Test
    void findById_shouldReturnEmpty_whenNotExists() {
        Optional<Notification> found = repository.findById("non-existent-id");
        
        assertFalse(found.isPresent());
    }

    @Test
    void findByShipmentId_shouldReturnNotifications_whenExists() {
        repository.save(testNotification);
        
        List<Notification> found = repository.findByShipmentId(testNotification.getShipmentId());
        
        assertEquals(1, found.size());
        assertEquals(testNotification.getId(), found.get(0).getId());
    }

    @Test
    void findByShipmentId_shouldReturnEmptyList_whenNotExists() {
        List<Notification> found = repository.findByShipmentId("non-existent-shipment-id");
        
        assertTrue(found.isEmpty());
    }

    @Test
    void findAll_shouldReturnAllNotifications() {
        repository.save(testNotification);
        Notification anotherNotification = new Notification("another-shipment-id", "Another message", NotificationType.SHIPMENT_SCANNED);
        repository.save(anotherNotification);
        
        List<Notification> all = repository.findAll();
        
        assertEquals(2, all.size());
    }

    @Test
    void deleteById_shouldRemoveNotification() {
        repository.save(testNotification);
        
        repository.deleteById(testNotification.getId());
        
        Optional<Notification> found = repository.findById(testNotification.getId());
        assertFalse(found.isPresent());
    }

    @Test
    void deleteAll_shouldRemoveAllNotifications() {
        repository.save(testNotification);
        Notification anotherNotification = new Notification("another-shipment-id", "Another message", NotificationType.SHIPMENT_SCANNED);
        repository.save(anotherNotification);
        
        repository.deleteAll();
        
        List<Notification> all = repository.findAll();
        assertTrue(all.isEmpty());
    }
}