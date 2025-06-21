package com.luckypets.logistics.notificationviewservice.service;

import com.luckypets.logistics.notificationviewservice.kafka.NotificationSentEventProducer;
import com.luckypets.logistics.notificationviewservice.model.Notification;
import com.luckypets.logistics.notificationviewservice.model.NotificationType;
import com.luckypets.logistics.notificationviewservice.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository repository;

    @Mock
    private NotificationSentEventProducer eventProducer;

    private NotificationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new NotificationServiceImpl(repository, eventProducer);
    }

    @Test
    @DisplayName("Should save notification successfully")
    void shouldSaveNotificationSuccessfully() {
        // Given
        Notification notification = new Notification("SHIP-123", "Test message", NotificationType.SHIPMENT_CREATED);
        when(repository.save(any(Notification.class))).thenReturn(notification);

        // When
        Notification result = service.save(notification);

        // Then
        assertThat(result).isEqualTo(notification);
        verify(repository).save(notification);
        verify(eventProducer).sendNotificationSentEvent(any());
    }

    @Test
    @DisplayName("Should throw exception when notification is null")
    void shouldThrowExceptionWhenNotificationIsNull() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> service.save(null));
        verifyNoInteractions(repository);
        verifyNoInteractions(eventProducer);
    }

    @Test
    @DisplayName("Should throw exception when shipmentId is null")
    void shouldThrowExceptionWhenShipmentIdIsNull() {
        // Given
        Notification notification = new Notification(null, "Test message", NotificationType.SHIPMENT_CREATED);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> service.save(notification));
        verifyNoInteractions(repository);
        verifyNoInteractions(eventProducer);
    }

    @Test
    @DisplayName("Should throw exception when shipmentId is empty")
    void shouldThrowExceptionWhenShipmentIdIsEmpty() {
        // Given
        Notification notification = new Notification("", "Test message", NotificationType.SHIPMENT_CREATED);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> service.save(notification));
        verifyNoInteractions(repository);
        verifyNoInteractions(eventProducer);
    }

    @Test
    @DisplayName("Should throw exception when message is null")
    void shouldThrowExceptionWhenMessageIsNull() {
        // Given
        Notification notification = new Notification("SHIP-123", null, NotificationType.SHIPMENT_CREATED);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> service.save(notification));
        verifyNoInteractions(repository);
        verifyNoInteractions(eventProducer);
    }

    @Test
    @DisplayName("Should throw exception when message is empty")
    void shouldThrowExceptionWhenMessageIsEmpty() {
        // Given
        Notification notification = new Notification("SHIP-123", "", NotificationType.SHIPMENT_CREATED);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> service.save(notification));
        verifyNoInteractions(repository);
        verifyNoInteractions(eventProducer);
    }

    @Test
    @DisplayName("Should throw exception when type is null")
    void shouldThrowExceptionWhenTypeIsNull() {
        // Given
        Notification notification = new Notification("SHIP-123", "Test message", null);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> service.save(notification));
        verifyNoInteractions(repository);
        verifyNoInteractions(eventProducer);
    }

    @Test
    @DisplayName("Should handle repository exception during save")
    void shouldHandleRepositoryExceptionDuringSave() {
        // Given
        Notification notification = new Notification("SHIP-123", "Test message", NotificationType.SHIPMENT_CREATED);
        when(repository.save(any(Notification.class))).thenThrow(new RuntimeException("Repository error"));

        // When & Then
        assertThrows(RuntimeException.class, () -> service.save(notification));
        verify(repository).save(notification);
        verifyNoInteractions(eventProducer);
    }

    @Test
    @DisplayName("Should find all notifications")
    void shouldFindAllNotifications() {
        // Given
        List<Notification> notifications = List.of(
                new Notification("SHIP-1", "Message 1", NotificationType.SHIPMENT_CREATED),
                new Notification("SHIP-2", "Message 2", NotificationType.SHIPMENT_DELIVERED)
        );
        when(repository.findAll()).thenReturn(notifications);

        // When
        List<Notification> result = service.findAll();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).isEqualTo(notifications);
        verify(repository).findAll();
    }

    @Test
    @DisplayName("Should handle exception during findAll")
    void shouldHandleExceptionDuringFindAll() {
        // Given
        when(repository.findAll()).thenThrow(new RuntimeException("Repository error"));

        // When & Then
        assertThrows(RuntimeException.class, () -> service.findAll());
        verify(repository).findAll();
    }

    @Test
    @DisplayName("Should find notification by id")
    void shouldFindNotificationById() {
        // Given
        String id = "test-id";
        Notification notification = new Notification("SHIP-123", "Test message", NotificationType.SHIPMENT_CREATED);
        when(repository.findById(id)).thenReturn(Optional.of(notification));

        // When
        Optional<Notification> result = service.findById(id);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(notification);
        verify(repository).findById(id);
    }

    @Test
    @DisplayName("Should return empty when notification not found by id")
    void shouldReturnEmptyWhenNotificationNotFoundById() {
        // Given
        String id = "non-existent-id";
        when(repository.findById(id)).thenReturn(Optional.empty());

        // When
        Optional<Notification> result = service.findById(id);

        // Then
        assertThat(result).isEmpty();
        verify(repository).findById(id);
    }

    @Test
    @DisplayName("Should return empty when id is null")
    void shouldReturnEmptyWhenIdIsNull() {
        // When
        Optional<Notification> result = service.findById(null);

        // Then
        assertThat(result).isEmpty();
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Should return empty when id is empty")
    void shouldReturnEmptyWhenIdIsEmpty() {
        // When
        Optional<Notification> result = service.findById("");

        // Then
        assertThat(result).isEmpty();
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Should find notifications by shipment id")
    void shouldFindNotificationsByShipmentId() {
        // Given
        String shipmentId = "SHIP-123";
        List<Notification> notifications = List.of(
                new Notification(shipmentId, "Message 1", NotificationType.SHIPMENT_CREATED),
                new Notification(shipmentId, "Message 2", NotificationType.SHIPMENT_SCANNED)
        );
        when(repository.findByShipmentId(shipmentId)).thenReturn(notifications);

        // When
        List<Notification> result = service.findByShipmentId(shipmentId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).isEqualTo(notifications);
        verify(repository).findByShipmentId(shipmentId);
    }

    @Test
    @DisplayName("Should return empty list when shipment id is null")
    void shouldReturnEmptyListWhenShipmentIdIsNull() {
        // When
        List<Notification> result = service.findByShipmentId(null);

        // Then
        assertThat(result).isEmpty();
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Should return empty list when shipment id is empty")
    void shouldReturnEmptyListWhenShipmentIdIsEmpty() {
        // When
        List<Notification> result = service.findByShipmentId("");

        // Then
        assertThat(result).isEmpty();
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Should delete notification by id")
    void shouldDeleteNotificationById() {
        // Given
        String id = "test-id";
        when(repository.existsById(id)).thenReturn(true);

        // When
        service.deleteById(id);

        // Then
        verify(repository).existsById(id);
        verify(repository).deleteById(id);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent notification")
    void shouldThrowExceptionWhenDeletingNonExistentNotification() {
        // Given
        String id = "non-existent-id";
        when(repository.existsById(id)).thenReturn(false);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> service.deleteById(id));
        verify(repository).existsById(id);
        verify(repository, never()).deleteById(id);
    }

    @Test
    @DisplayName("Should throw exception when deleting with null id")
    void shouldThrowExceptionWhenDeletingWithNullId() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> service.deleteById(null));
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Should throw exception when deleting with empty id")
    void shouldThrowExceptionWhenDeletingWithEmptyId() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> service.deleteById(""));
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Should delete all notifications")
    void shouldDeleteAllNotifications() {
        // Given
        when(repository.count()).thenReturn(5L);

        // When
        service.deleteAll();

        // Then
        verify(repository).count();
        verify(repository).deleteAll();
    }

    @Test
    @DisplayName("Should handle exception during deleteAll")
    void shouldHandleExceptionDuringDeleteAll() {
        // Given
        when(repository.count()).thenThrow(new RuntimeException("Repository error"));

        // When & Then
        assertThrows(RuntimeException.class, () -> service.deleteAll());
        verify(repository).count();
        verify(repository, never()).deleteAll();
    }
}