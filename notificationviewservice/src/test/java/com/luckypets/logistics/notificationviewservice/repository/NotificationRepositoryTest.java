package com.luckypets.logistics.notificationviewservice.repository;

import com.luckypets.logistics.notificationviewservice.model.Notification;
import com.luckypets.logistics.notificationviewservice.model.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NotificationRepositoryTest {

    private NotificationRepository repository;

    @BeforeEach
    void setUp() {
        repository = new NotificationRepository();
        repository.clearInMemoryStorageForTests(); // OK in tests
    }

    @Test
    @DisplayName("Should save notification")
    void shouldSaveNotification() {
        // Given
        Notification notification = new Notification("SHIP-123", "Test message", NotificationType.SHIPMENT_CREATED);

        // When
        Notification saved = repository.save(notification);

        // Then
        assertThat(saved).isEqualTo(notification);
        assertThat(saved.getId()).isNotNull();
    }

    @Test
    @DisplayName("Should throw exception when saving null notification")
    void shouldThrowExceptionWhenSavingNullNotification() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> repository.save(null));
    }

    @Test
    @DisplayName("Should find all notifications")
    void shouldFindAllNotifications() {
        // Given
        Notification notification1 = new Notification("SHIP-1", "Message 1", NotificationType.SHIPMENT_CREATED);
        Notification notification2 = new Notification("SHIP-2", "Message 2", NotificationType.SHIPMENT_DELIVERED);

        repository.save(notification1);
        repository.save(notification2);

        // When
        List<Notification> result = repository.findAll();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder(notification1, notification2);
    }

    @Test
    @DisplayName("Should find notification by id")
    void shouldFindNotificationById() {
        // Given
        Notification notification = new Notification("SHIP-123", "Test message", NotificationType.SHIPMENT_CREATED);
        Notification saved = repository.save(notification);

        // When
        Optional<Notification> result = repository.findById(saved.getId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(saved);
    }

    @Test
    @DisplayName("Should return empty when notification not found by id")
    void shouldReturnEmptyWhenNotificationNotFoundById() {
        // When
        Optional<Notification> result = repository.findById("non-existent-id");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should find notifications by shipment id")
    void shouldFindNotificationsByShipmentId() {
        // Given
        String shipmentId = "SHIP-123";
        Notification notification1 = new Notification(shipmentId, "Message 1", NotificationType.SHIPMENT_CREATED);
        Notification notification2 = new Notification(shipmentId, "Message 2", NotificationType.SHIPMENT_SCANNED);
        Notification notification3 = new Notification("SHIP-456", "Message 3", NotificationType.SHIPMENT_DELIVERED);

        repository.save(notification1);
        repository.save(notification2);
        repository.save(notification3);

        // When
        List<Notification> result = repository.findByShipmentId(shipmentId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder(notification1, notification2);
    }

    @Test
    @DisplayName("Should delete notification by id")
    void shouldDeleteNotificationById() {
        // Given
        Notification notification = new Notification("SHIP-123", "Test message", NotificationType.SHIPMENT_CREATED);
        Notification saved = repository.save(notification);

        // When
        repository.deleteById(saved.getId());

        // Then
        assertThat(repository.findById(saved.getId())).isEmpty();
        assertThat(repository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should delete all notifications")
    void shouldDeleteAllNotifications() {
        // Given
        repository.save(new Notification("SHIP-1", "Message 1", NotificationType.SHIPMENT_CREATED));
        repository.save(new Notification("SHIP-2", "Message 2", NotificationType.SHIPMENT_DELIVERED));

        // When
        repository.deleteAll();

        // Then
        assertThat(repository.findAll()).isEmpty();
        assertThat(repository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should check if notification exists by id")
    void shouldCheckIfNotificationExistsById() {
        // Given
        Notification notification = new Notification("SHIP-123", "Test message", NotificationType.SHIPMENT_CREATED);
        Notification saved = repository.save(notification);

        // When & Then
        assertThat(repository.existsById(saved.getId())).isTrue();
        assertThat(repository.existsById("non-existent-id")).isFalse();
    }

    @Test
    @DisplayName("Should count notifications")
    void shouldCountNotifications() {
        // Given
        repository.save(new Notification("SHIP-1", "Message 1", NotificationType.SHIPMENT_CREATED));
        repository.save(new Notification("SHIP-2", "Message 2", NotificationType.SHIPMENT_DELIVERED));
        repository.save(new Notification("SHIP-3", "Message 3", NotificationType.SHIPMENT_SCANNED));

        // When
        long count = repository.count();

        // Then
        assertThat(count).isEqualTo(3);
    }
}